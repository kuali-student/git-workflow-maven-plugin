Git Workflow Maven Plugin
==========================

This plugin is not for the Git Flow model of git branching but rather a set of
mojo's that are used to orchestrate the Kuali Student Git conversion.

Mojo's:
========

FetchOpenPullRequestsMojo
--------------------------

These variables can be set using the -D operator:
git-flow.sourceGithubUser
git-flow.sourceGithubRepo
git-flow.sourceGithubBranch


Checkout the latest commit on master:

git clone git://some-server.com/repo.git --depth 1 repo

cd repo

```
mvn org.kuali.maven.plugins.git-workflow-maven-plugin:0.0.1-SNAPSHOT:fetchOpenPullRequests -e -Dgit-flow.sourceGithubUer=user -Dgit-flow.sourceGithubRepo=repo -Dgit-flow.sourceGithubBranch=master -N

```

This will pull down the pull request branch tips for the open applicable branches.

Applicable if the base branch of the pull request matches the git-flow.sourceGithubBranch string value.



