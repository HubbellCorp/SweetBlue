properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '')),
            parameters([gitParameter(branch: '', branchFilter: '.*', defaultValue: 'origin/3.2/dev', description: 'The branch to base the deployment from.', name: 'branch', quickFilterEnabled: false, selectedValue: 'DEFAULT', sortMode: 'ASCENDING_SMART', tagFilter: '*', type: 'PT_BRANCH'),
                        booleanParam(defaultValue: false, description: 'If this is checked, then the library will only be released to the internal maven server (also will not upload the docs, or to big commerce).', name: 'internalOnly'),
                        booleanParam(defaultValue: true, description: 'If this is checked, the build will NOT fail if the JIRA version does not exist. It will create the version. ', name: 'createVersion')]), pipelineTriggers([])])


@Library('jenkins_common')

import com.idevices.BuildStatus
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException
import groovy.transform.Field


@Field
def gitUrl = 'git@bitbucket.org:idevices/idevices_sweetblue.git'

@Field
def project = 'SWEET'

def branch = env.branch

@Field
def hasJiraProject = true

def forceNewJiraVersion = createVersion.toString().toBoolean()
def internal = internalOnly.toString().toBoolean()


node('android-ec2') {
    deploySweetBlue(branch, internal, forceNewJiraVersion)
}


def deploySweetBlue(String branch, Boolean internalOnly, Boolean createVersion) {
    def buildResult = BuildStatus.Not_Deployed
    String fakeTag = "Build_Deployed"
    def curVersion
    def branchToBuild = branch
    if (!branch.startsWith("origin/"))
        branchToBuild = "origin/${branch}"
    def rawBranch = branchToBuild.replaceAll("origin/", "")

    try {
        def isCommitSinceLastTag = true

        stage('Preparation') {
            // Clean up any previous run
            deleteDir()

            retry(2) {
                // Get the development branch from git repo
                checkout([$class: 'GitSCM', branches: [[name: "${branchToBuild}"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: '', trackingSubmodules: false]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8c5c3a3e-487a-42d7-b2a8-067a5a742988', url: "${gitUrl}"]]])
            }

            // Make sure there is no tag already referencing the current commit
            def tagReferencesCurrentCommit = sh(script: "git describe --exact-match HEAD", returnStatus: true)
            if (tagReferencesCurrentCommit == 0) {
                echo "There appears to be no further commits since the last build tag. Skipping buid and deploy."
                isCommitSinceLastTag = false
                return
            } else {
                def tagExists = sh(script: "git rev-list ${fakeTag}", returnStatus: true)
                if (tagExists == 0) {
                    // Delete our fake tag if it exists, as we'll be adding it once the build has completed
                    sh "git tag -d ${fakeTag}"
                    sshagent(credentials: ['8c5c3a3e-487a-42d7-b2a8-067a5a742988']) {
                        sh("git push origin :refs/tags/${fakeTag}")
                    }
                }
            }

            curVersion = idevicesCommonAndroid.getVersion()
            idevicesCommonAndroid.sendSlackFull(BuildStatus.Started, rawBranch, curVersion)
        }

        // Only proceed if there are commits since last version tag
        if (isCommitSinceLastTag) {

            stage('Build') {

                // We need to swap out the signing file so that we can get the archive signed properly
                sh "cp library/signing_server.gradle library/signing.gradle"
                sh "cp rx/signing_server.gradle rx/signing.gradle"
                sh "cp sweet_api/signing_server.gradle sweet_api/signing.gradle"
                sh "cp sweetunit/signing_server.gradle sweetunit/signing.gradle"

                // Build the project and run the tests
                sh "./gradlew turnOffTestModules"
                sshagent(credentials: ["8c5c3a3e-487a-42d7-b2a8-067a5a742988"]) {
                    sh "./gradlew clean assembleRelease copyAndRenameJarsDeploy"
                }
            }

            stage('Deploy') {
                // Deploy the library now (including all release-able submodules)
                // Inject necessary environment variables and upload to public and private maven repos
                sshagent(credentials: ["8c5c3a3e-487a-42d7-b2a8-067a5a742988"]) {
                    withCredentials([string(credentialsId: 'ARCHIVA_URL', variable: 'ARCHIVA_URL'), string(credentialsId: 'ARCHIVA_USER', variable: 'ARCHIVA_USER'),
                                     string(credentialsId: 'ARCHIVA_PW', variable: 'ARCHIVA_PW'), string(credentialsId: 'SIGN_ID', variable: 'SIGN_ID'),
                                     string(credentialsId: 'SIGN_PW', variable: 'SIGN_PW'), file(credentialsId: 'SIGN_RING', variable: 'SIGN_RING'),
                                     string(credentialsId: 'SWEETBLUE_SERVER_ADDRESS', variable: 'SWEETBLUE_SERVER_ADDRESS'),
                                     string(credentialsId: 'SWEETBLUE_COM_FTP_USERNAME', variable: 'SWEETBLUE_COM_FTP_USERNAME'),
                                     string(credentialsId: 'SWEETBLUE_COM_FTP_PASSWORD', variable: 'SWEETBLUE_COM_FTP_PASSWORD'),
                                     string(credentialsId: 'SWEETBLUE_BIG_COMMERCE_PASSWORD', variable: 'SWEETBLUE_BIG_COMMERCE_PASSWORD'),
                                     string(credentialsId: 'AZURE_ARTIFACTS_ENV_ACCESS_TOKEN', variable: 'AZURE_ARTIFACTS_ENV_ACCESS_TOKEN'),
                                     file(credentialsId: 'SB_COMM_LICENSE', variable: 'SB_COMM_LICENSE')]) {

                        // Update the version stamp, and this also updates the doc files with the version
                        sh "./gradlew updateVersionStamp"

                        if (internalOnly) {
                            // This only uploads the artifacts to our internal maven server. It does not upload any documentation
                            sh './gradlew internalRelease'
                        } else {
                            // This releases everything, and updates documentation, and uploads it. This includes releasing the submodules
                            // like rx, and sweetunit
                            sh './gradlew fullRelease'
                        }
                    }
                }

                // Update Jira issues with the fix version
                def issues = idevicesCommon.getIssues("${project}-")
                if (createVersion) {
                    idevicesCommon.updateIssuesForceNewJiraVersion(project, issues, curVersion)
                } else {
                    idevicesCommon.updateIssues(issues, curVersion)
                }

                // Set the new jira build version to released
                if (createVersion) {
                    idevicesCommon.releaseVersionForceNewJiraVersion(project, curVersion)
                } else {
                    idevicesCommon.releaseVersion(project, curVersion)
                }
            }

            stage('PostDeploy') {
                // Rest the test modules back
                sh "./gradlew resetModules"
                // Checkout the branch explicitly
                sh "git checkout ${branch}"

                // Remove the signing file before we go and push the repo back up
                sh "rm -rf library/signing.gradle"
                sh "touch library/signing.gradle"
                sh "rm -rf rx/signing.gradle"
                sh "touch rx/signing.gradle"
                sh "rm -rf sweetunit/signing.gradle"
                sh "touch sweetunit/signing.gradle"
                sh "rm -rf sweet_api/signing.gradle"
                sh "touch sweet_api/signing.gradle"

                // Create the release tag, and push it
                sshagent(credentials: ['8c5c3a3e-487a-42d7-b2a8-067a5a742988']) {
                    sh("./gradlew library:createAndPushGitTag")
                }

                // Checkout the branch explicitly (otherwise for some reason, the changes get put on the tag
                // created above)
                sh "git checkout ${branch}"

                // Bump up the version and update various version dependent items
                sh "./gradlew library:bumpVersionName"

                // Add the changes to the repo, and push it back up
                sh "git add -u"
                sh "git commit -m \"bump version\""
                sshagent(credentials: ['8c5c3a3e-487a-42d7-b2a8-067a5a742988']) {
                    sh("git push origin ${branch}")
                }

                // Add the fake tag so the build server doesn't try to release again
                sh "git tag -a ${fakeTag} -m \".\""
                sshagent(credentials: ['8c5c3a3e-487a-42d7-b2a8-067a5a742988']) {
                    sh("git push origin ${fakeTag}")
                }

                // Create new Jira version
                def newVersion = idevicesCommonAndroid.getVersion()
                def newJiraVersion = [name       : "${newVersion}",
                                      archived   : false,
                                      released   : false,
                                      description: '',
                                      project    : project]
                try {
                    jiraNewVersion(version: newJiraVersion)
                } catch (error) {
                    // echo error
                }

                buildResult = BuildStatus.Deployed
            }
        }
    } catch (FlowInterruptedException e) {
        buildResult = BuildStatus.Canceled
        echo exception.toString()
    } catch (Exception exception) {
        buildResult = BuildStatus.Failed
        echo exception.toString()
        throw exception
    } finally {
        idevicesCommonAndroid.sendSlackFull(buildResult, branchToBuild, curVersion)
    }
}