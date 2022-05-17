String defaultBranch = "3.2/dev"

properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '')),
            parameters([gitParameter(branch: '', branchFilter: '.*', defaultValue: "origin/${defaultBranch}", description: 'The branch to base the deployment from.', name: 'branch', quickFilterEnabled: false, selectedValue: 'DEFAULT', sortMode: 'ASCENDING_SMART', tagFilter: '*', type: 'PT_BRANCH')]),
            pipelineTriggers([
                // Generic webhook trigger, which listens for webhooks from Bitbucket, which contain
                // a certain token (this is to avoid running the job for all webhooks)
                GenericTrigger(causeString: 'SweetBlue Bitbucket Webhook', genericVariables: [
                    [defaultValue: '', key: 'PUSH', regexpFilter: '', value: '$.push'],
                    [defaultValue: '', key: 'PULL_REQUEST', regexpFilter: '', value: '$.pullrequest'],
                    [defaultValue: '', key: 'COMMENT', regexpFilter: '', value: '$.comment']],
                    printContributedVariables: true, printPostContent: true, regexpFilterExpression: '',
                    regexpFilterText: '', token: 'SweetBlue_V3')
            ])
])

@Library('jenkins_common')

import com.idevices.BuildStatus
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException


def gitUrl = 'git@bitbucket.org:idevices/idevices_sweetblue.git'
def branch = env.branch
def checkForChanges = true

node('android-ec2') {
    if (env.PUSH) {
        String push = idevicesCommon.parsePushBranch(env.PUSH).toString()
        if (!push.equalsIgnoreCase(defaultBranch)) {
            echo "(${push}) Not the default dev branch (${defaultBranch}), ignoring this push."
            return
        }
    }

    if (env.COMMENT) {
        String prComment = idevicesCommon.parsePRComment(env.COMMENT).toString()
        // I don't think we want this running on every PR comment, so only build when the
        // build command is given
        if (!prComment.toLowerCase().contains("--build--")) {
            echo "Found PR comment, but no build command was given, so there's nothing to do."
            return
        }
        // As this is "forcing" a build, we want to make sure the job doesnt just back out
        checkForChanges = false
    }

    def mergeSrcBranch
    def mergeDstBranch

    if (env.PULL_REQUEST) {
        def array = idevicesCommon.parsePullRequest(env.PULL_REQUEST)

        mergeSrcBranch = array[0]
        mergeDstBranch = array[1]

        if (mergeDstBranch != defaultBranch) {
            echo "Not the default branch, ignoring the pull request."
            return
        }
        branch = defaultBranch
    }

    def buildResult = BuildStatus.Failed
    def isCommitSinceLastTag = true
    def curVersion
    def branchToBuild = branch
    if (!branch.startsWith("origin/"))
        branchToBuild = "origin/${branch}"
    def rawBranch = branchToBuild.replaceAll("origin/", "")
    def commitId

    try {

        stage('Preparation') {
            // Clean up any previous run
            deleteDir()

            idevicesCommonAndroid.sendSlackFull(BuildStatus.Started, rawBranch, null)

            retry(2) {
                // Get the development branch from git repo
                checkout([$class: 'GitSCM', branches: [[name: "${branchToBuild}"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: '', trackingSubmodules: false]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8c5c3a3e-487a-42d7-b2a8-067a5a742988', url: gitUrl]]])
            }
            commitId = idevicesCommon.getCommitId()

            // PR build, let's merge the branches before running build/tests/analysis
            if (mergeSrcBranch) {
                sshagent(credentials: ["8c5c3a3e-487a-42d7-b2a8-067a5a742988"]) {
                    sh "git checkout ${mergeSrcBranch}"

                    commitId = idevicesCommon.getCommitId()

                    bitbucketStatusNotify(buildState: 'INPROGRESS', buildKey: 'prep', buildName: "Setup workspace", repoSlug: 'idevices_sweetblue', commitId: "${commitId}")
                    bitbucketStatusNotify(buildState: 'INPROGRESS', buildKey: 'build', buildName: "Build", repoSlug: 'idevices_sweetblue', commitId: "${commitId}")
                    bitbucketStatusNotify(buildState: 'INPROGRESS', buildKey: 'test', buildName: "Tests", repoSlug: 'idevices_sweetblue', commitId: "${commitId}")
                    bitbucketStatusNotify(buildState: 'INPROGRESS', buildKey: 'analysis', buildName: "Static Analysis", repoSlug: 'idevices_sweetblue', commitId: "${commitId}")

                    sh "git checkout ${branchToBuild}"
                    def mergeCmd = "git merge --squash origin/${mergeSrcBranch}"
                    def mergeStatus = sh(returnStatus: true, script: mergeCmd)
                    if (mergeStatus != 0) {
                        error "Merge failed!"
                    }
                }

                isCommitSinceLastTag = idevicesCommonAndroid.repoHasChanges()
            }

            curVersion = idevicesCommonAndroid.getVersion()

            if (mergeSrcBranch) {
                bitbucketStatusNotify(buildState: 'SUCCESSFUL', buildKey: 'prep', buildName: "Setup workspace", repoSlug: 'idevices_sweetblue', commitId: "${commitId}")
            }
        }

        if (isCommitSinceLastTag || !checkForChanges) {

            stage('Build') {
                sh "./gradlew turnOffTestModules"
                sshagent(credentials: ['8c5c3a3e-487a-42d7-b2a8-067a5a742988']) {
                    sh "./gradlew clean assembleRelease copyAndRenameJarsBuild"
                }
                if (mergeSrcBranch) {
                    bitbucketStatusNotify(buildState: 'SUCCESSFUL', buildKey: 'build', buildName: "Build", repoSlug: 'idevices_sweetblue', commitId: "${commitId}")
                }
            }

            stage('Test') {
                sh "./gradlew testDebugUnitTest sweet_api:test"
                // Wrap in a try/catch as the instrumentation tests are incredibly flaky
                try {
                    sh "./gradlew library:connectedAndroidTest jacocoTestReport"
                } catch (Exception e) {
                    // If we want to fail the build when these tests fail, then uncomment the below
                    //error e.toString()
                }

                junit "library/build/test-results/testDebugUnitTest/*.xml, library/build/outputs/androidTest-results/connected/*.xml, " +
                        "rx/build/test-results/testDebugUnitTest/*.xml, api/build/test-results/test/*.xml"

                if (mergeSrcBranch) {
                    bitbucketStatusNotify(buildState: 'SUCCESSFUL', buildKey: 'test', buildName: "Tests", repoSlug: 'idevices_sweetblue', commitId: "${commitId}")
                }
            }

            stage('Static Analysis') {
                sh "./gradlew library:lint rx:lint"

                androidLint canComputeNew: false, defaultEncoding: '', healthy: '', pattern: "library/build/reports/lint-results.xml, rx/build/reports/lint-results.xml", unHealthy: ''

                // Sonarqube can be flaky, so wrap this in a try catch because sonarqube being down shouldn't fail the build
                try {
                    withCredentials([string(credentialsId: 'sonar_login', variable: 'sonar_login')]) {
                        sh "./gradlew -Psonar_branch=${branch} sonarqube"
                    }
                } catch (Exception e) {}

                if (mergeSrcBranch) {
                    bitbucketStatusNotify(buildState: 'SUCCESSFUL', buildKey: 'analysis', buildName: "Static Analysis", repoSlug: 'idevices_sweetblue', commitId: "${commitId}")
                }
            }

            buildResult = BuildStatus.Successful
        }


    } catch (FlowInterruptedException exception) {
        buildResult = BuildStatus.Canceled
        echo exception.toString()
        throw exception
    } catch (Exception exception) {
        buildResult = BuildStatus.Failed
        echo exception.toString()
        throw exception
    } finally {
        idevicesCommonAndroid.sendSlackFull(buildResult, branchToBuild, curVersion)
    }

}