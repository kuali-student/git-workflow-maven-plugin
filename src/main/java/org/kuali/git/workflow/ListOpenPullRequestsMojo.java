/**
 * 
 */
package org.kuali.git.workflow;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Repository;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kuali.student.git.model.GitRepositoryUtils;

/**
 * @author ocleirig
 * 
 * Fetch the list of open pull requests from the git repository.
 * 
 * We want all of the pull requests against a certain branch.
 * 
 * We want only those that have new commits without any CI running against them.
 * 
 * We output a report file that can then be used to trigger a downstream job for each pull request.
 *
 */
@Mojo (name="listOpenPullRequests")
@Execute (goal="listOpenPullRequests", lifecycle="initialize")
public class ListOpenPullRequestsMojo extends AbstractGithubAuthorizedMojo {

	/**
	 * For example: kuali/ks-development.
	 * 
	 * The name of the github user or organization [slash] the name of the git repository.
	 * 
	 * The pull requests are resolved from this location.
	 * 
	 */
	@Parameter(required=true, property="git-flow.sourceGithubUser")
	private String sourceGithubUser;
	
	@Parameter(required=true, property="git-flow.sourceGithubRepo")
	private String sourceGithubRepo;
	
	@Parameter(required=true, property="git-flow.sourceGithubBranch")
	private String sourceGithubBranch;
	
	@Parameter (required=true, property="git-flow.reportFileNamePrefix", defaultValue="open-pull-requests")
	private String reportFileNamePrefix;
	
	/**
	 * @return the reportFileNamePrefix
	 */
	public String getReportFileNamePrefix() {
		return reportFileNamePrefix;
	}

	/**
	 * @param reportFileNamePrefix the reportFileNamePrefix to set
	 */
	public void setReportFileNamePrefix(String reportFileNamePrefix) {
		this.reportFileNamePrefix = reportFileNamePrefix;
	}

	/**
	 * @param sourceGithubBranch the sourceGithubBranch to set
	 */
	public void setSourceGithubBranch(String sourceGithubBranch) {
		this.sourceGithubBranch = sourceGithubBranch;
	}

	/**
	 * @param sourceGithubUser the sourceGithubUser to set
	 */
	public void setSourceGithubUser(String sourceGithubUser) {
		this.sourceGithubUser = sourceGithubUser;
	}

	/**
	 * @param sourceGithubRepo the sourceGithubRepo to set
	 */
	public void setSourceGithubRepo(String sourceGithubRepo) {
		this.sourceGithubRepo = sourceGithubRepo;
	}

	/**
	 * 
	 */
	public ListOpenPullRequestsMojo() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		try {
			
			GitHub github = super.authorizeFromCredentials();
			
			String targetRepository = sourceGithubUser + "/" + sourceGithubRepo;
			
			GHRepository repo = github.getRepository(targetRepository);
			
			List<GHPullRequest> openPullRequests = repo.getPullRequests(GHIssueState.OPEN);
			
			
			for (GHPullRequest pullRequest : openPullRequests) {
				
				GHCommitPointer head = pullRequest.getHead();
				
				GHRepository remoteRepository = head.getRepository();
				
				GHCommitPointer base = pullRequest.getBase();
				
				if (!sourceGithubBranch.equals(base.getRef())) {
					getLog().info("Skipping pull request: " + pullRequest.getNumber() + " because it does not apply to branch: " + sourceGithubBranch);
					continue; 
				}

				String commitId = head.getSha();
				
				// check that the commit id has no ci running.
				
				// once github-api 1.59 is released used that but for now just access the commit through the main repository.
				// it seems like a github api bug but the non merged commits are available through the base repository.
				// it may be related to their test merge support.
				GHCommit headCommit = repo.getCommit(commitId);

				List<GHCommitStatus> commitStatuses = headCommit.listStatuses().asList();
				
				if (commitStatuses.size() != 0) {
					// there are statuses so skip over
					getLog().warn("skipping pull-request-" + pullRequest.getNumber() + " because it has existing commit statuses: ");
					
					for (GHCommitStatus status : commitStatuses) {
						
						getLog().info("pull-request-" + pullRequest.getNumber() + " status: context =  '" + status.getContext() + "', name='" + status.getState().name() + "', description = '" + status.getDescription() + "'");
						
					}
					
					continue;
				}
				else {
					getLog().info("pull-request-" +pullRequest.getNumber() + " added to report.");
				}
				
				/*
				 * Emit a file per pull request because this is easier to consume in Jenkins.
				 * 
				 * For every matching file, invoke one build.
				 * For every property file, invoke one build.
				 */
				PrintWriter pw = new PrintWriter(new File (reportFileNamePrefix + "." + pullRequest.getNumber()).getAbsoluteFile());
				
				pw.println(String.format ("PULL_REQUEST_NUMBER=%d", pullRequest.getNumber()));
				pw.println(String.format ("PULL_REQUEST_COMMIT_ID=%s", pullRequest.getHead().getSha()));
				
				pw.close();
				
			}
			
			
			
			
			
		} catch (IOException e) {
			throw new MojoExecutionException("ListOpenPullRequestsMojo failed: ", e);
		}
		

	}

	
}
