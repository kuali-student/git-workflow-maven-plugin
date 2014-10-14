Git Workflow Maven Plugin
==========================

This plugin is not for the Git Flow model of git branching but rather a set of
mojo's that are used to orchestrate the Kuali Student Git conversion.

For the Kuali Student conversion the https://github.com/kuali-student/pull-request-processing project was created to store the base plugin setup
and associated helper scripts that would be called by the various Jenkins Build Steps.



Mojo's:
========

IdentifyPullRequestChangesThroughApiMojo
----------------------------------------

Identifies which top level module has changes and if there are sql changes.

Each module change will cause a properties file named target/$module-changes.dat to be created.

These will contain the PULL_REQUEST_NUMBER and the MODULE variables.

An sql change will cause a properties file named target/sql-changes.dat to be created.

CI can be configured to spawn downstream jobs based on the existense of these files.

ListOpenPullRequestsMojo
------------------------

For each open pull request where the tip commit does not have completed or pending CI a file is created like:

open-pull-requests.x

Where x is the pull request number.

Within each file are two parameters:
  * PULL_REQUEST_NUMBER : the pull request number.
  * PULL_REQUEST_COMMIT_ID	: the current commit id of the head of the pull request.

The existense of these files can be used to spawn downstream CI jobs to perform pull request processing.

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
mvn org.kuali.maven.plugins.git-workflow-maven-plugin:0.0.9:fetchOpenPullRequests -e -Dgit-flow.sourceGithubUer=user -Dgit-flow.sourceGithubRepo=repo -Dgit-flow.sourceGithubBranch=master -N

```

This will pull down the pull request branch tips for the open applicable branches.

Applicable if the base branch of the pull request matches the git-flow.sourceGithubBranch string value.

PushGitReferenceMojo 	
--------------------

JGit based Mojo that will push a specified refspec to a specified remote.

The credentials come from environment variables.

This mojo can be invoked in a Jenkins shell which will allow pushing a reference into github.  

For example: 
https://github.com/kuali-student/pull-request-processing/blob/master/pull-request-builder/run-manual-impex-process.sh

 GithubCommentsMojo
------------------

Proof of Concept to list the comments on a pull request.

GithubStatusMojo.java 
----------------------

Proof of Concept to add a pending build status and to list of existing statuses.

IdentifyChangesInGitMojo.java 	
------------------------------

Proof of concept to compare the file differences between two git tree's.  



