## Contributing to this Repository

### Branching strategy
* `main` - The main branch holds the code for the currently released version.
* Minor version branch folders (eg `4.0/dev`, `4.2/dev`, etc) - Each minor version has its own dev branch.
* Releases are tagged before being merged into the main branch (eg `v4.0.0`)
* Work branches should:
  * Be based on the current version's dev branch (eg `4.0/dev`)
  * Also be within the current versions's dev branch folder (eg `4.0/dev/my_new_feature`, `4.2/dev/a_bug_fix`)
  * If there is a github issue associated with the branch, please also include it in the branch name preceded by a # (eg `4.0/dev/#12_my_new_feature`, `4.2/dev/#15_another_bug_fix`)

### Got a question, or a problem?
In an effort to keep the issues section as clean, and streamlined as possible, please check the discussions tab to see if your question, or issue has already been asked/addressed. Most times, you're better off posting in the discussion forums first. You will be asked to create an issue, if code changes do need to happen.

### Filing Issues

Filing issues is a great way to contribute to SweetBlue. Here are some guidelines:

* Include as much detail as you can about the problem/request
* Point to a test repository (eg, hosted on GitHub) that can help reproduce the issue (if a bug). This works better than trying to describe step by step how to reproduce the issue.
* Github supports markdown, so when filing issues, make sure you check the formatting before clicking submit.


### Submitting Pull Requests

If you don't know what a pull request is, read this: [https://help.github.com/articles/using-pull-requests](https://help.github.com/articles/using-pull-requests).

* Please make sure you follow the code styling, and design patterns found within the library. (Please see the conventions page in the wiki)
* Create a fork from the sweetblue repository as it is described in [GitHub docs](https://docs.github.com/en/get-started/quickstart/fork-a-repo#forking-a-repository)
* Create a branch for your new work, based on the current version dev branch, within the same folder (eg 4.0/dev/a_cool_new_feature see [Branching strategy](#branching-strategy))
* Make sure there are tests to cover your change.
  * This applies whether you are adding a new feature, or fixing a bug
  * If it's a new feature:
    * Make sure to expose the new feature in the Rx module (if applicable)
    * Also make sure there are rx tests, if applicable
* Do not increase the android lint issue count
* Run the SweetBlue-CI job before submitting your PR. Make sure the job does not fail (fix any issues if it does fail before submitting your PR)

