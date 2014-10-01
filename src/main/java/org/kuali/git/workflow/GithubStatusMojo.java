/**
 * 
 */
package org.kuali.git.workflow;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.kuali.git.workflow.model.PullRequestRefs;
import org.kuali.git.workflow.model.utils.GithubApiUtils;
import org.kuali.student.git.model.GitRepositoryUtils;

/**
 * @author ocleirig
 * 
 * Read and Create commit statuses using github's commit status api.
 * 
 * When CI is successful it will use this mojo to create a status indicating a successful build.
 * 
 * This data will then be used by the FetchOpenPullRequestsMojo when downloading applicable branches.
 * 
 *
 */
@Mojo (name="githubStatus")
@Execute (goal="githubStatus", lifecycle="initialize")
public class GithubStatusMojo extends AbstractGithubAuthorizedMojo {

	/**
	 * Certain operations are slow for JGit so this allows us to run them using C git.
	 * 
	 */
	@Parameter(property="git-flow.cGitCommand", defaultValue="git")
	protected String externalCGitCommand;

	private Repository projectRepository;
	
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
	
	@Parameter(required=true, property="git-flow.targetCommitId")
	private String targetCommitId;
	
	
	/**
	 * @param projectRepository the projectRepository to set
	 */
	public void setProjectRepository(Repository projectRepository) {
		this.projectRepository = projectRepository;
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
	 * @param targetCommitId the targetCommitId to set
	 */
	public void setTargetCommitId(String targetCommitId) {
		this.targetCommitId = targetCommitId;
	}

	/**
	 * @param externalCGitCommand the externalCGitCommand to set
	 */
	public void setExternalCGitCommand(String externalCGitCommand) {
		this.externalCGitCommand = externalCGitCommand;
	}

	
	/**
	 * 
	 */
	public GithubStatusMojo() {
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		try {
			
			projectRepository = GitRepositoryUtils.buildFileRepository(project.getBasedir(), false, false);
			
			GitHub github = super.authorizeFromCredentials();
			
			String targetRepository = sourceGithubUser + "/" + sourceGithubRepo;
			
			GHRepository repo = github.getRepository(targetRepository);
			
			GithubApiUtils githubUtils = new GithubApiUtils(repo, getLog());
			
			githubUtils.registerCommitStatus(targetCommitId, GHCommitState.PENDING, null, "testing status", "verify-merge-to-trunk-pull-request");
			
			PagedIterable<GHCommitStatus> currentCommitStatuses = repo.listCommitStatuses(targetCommitId);
			
			for (GHCommitStatus ghCommitStatus : currentCommitStatuses) {
				getLog().info(String.format ("status: (context, state) = (%s, %s)", ghCommitStatus.getContext(), ghCommitStatus.getState().name() ));
			}
			
			
			
		} catch (IOException e) {
			throw new MojoExecutionException("FetchOpenPullRequestsMojo failed: ", e);
		}
		

	}




	private Set<String> reportOnTopLevelDirectoriesWithSQLChanges(
			Set<String> changes) {
		
		Set<String>topLevelSqlChanges = new HashSet<String>();
		
		for (String change : changes) {
			
			if (change.endsWith(".sql")) {
				
				int offset = change.indexOf("/");
				
				if (offset == -1) {
					topLevelSqlChanges.add(".");
					continue;
				}
				
				String topLevel = change.substring(0, offset);
				
				topLevelSqlChanges.add(topLevel);
			}
		}
		
		return topLevelSqlChanges;
	}




	private Set<String> reportOnTopLevelDirectoryChanges(Set<String> changes) {

		Set<String>topLevelChanges = new HashSet<String>();
		
		for (String change : changes) {
			
			int offset = change.indexOf("/");
			
			if (offset == -1) {
				topLevelChanges.add(".");
				continue;
			}
			
			String topLevel = change.substring(0, offset);
			
			topLevelChanges.add(topLevel);
		}
		
		return topLevelChanges;
	}

	
}
