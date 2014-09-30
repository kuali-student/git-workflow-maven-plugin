/**
 * 
 */
package org.kuali.git.workflow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kuali.common.util.execute.StorePropertiesExecutable;
import org.kuali.git.workflow.model.PullRequestRefs;
import org.kuali.student.git.model.GitRepositoryUtils;
import org.kuali.student.git.utils.ExternalGitUtils;

/**
 * @author ocleirig
 * 
 * Fetch the list of open pull requests from the git repository.
 * 
 * We want all of the pull requests against a certain branch.
 *
 */
@Mojo (name="fetchOpenPullRequests")
@Execute (goal="fetchOpenPullRequests", lifecycle="initialize")
public class FetchOpenPullRequestsMojo extends AbstractGitRepositoryAwareMojo {

	@Component
	private MavenProject project;
	
	/**
	 * Certain operations are slow for JGit so this allows us to run them using C git.
	 * 
	 */
	@Parameter(property="git-flow.cGitCommand", defaultValue="git")
	protected String externalCGitCommand;

	
	
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
	
	@Parameter(property="git-flow.specificPullRequest")
	private Integer specificPullRequest;
	
	/**
	 * This was the head commit id of the pull request when this job was triggered.
	 * We just check in the specific pull request case that this still aligns with what the api
	 * is telling us.
	 */
	@Parameter (property="git-flow.expectedPullRequestHeadCommitId")
	private String expectedPullRequestHeadCommitId;
	
	/**
	 * @param project the project to set
	 */
	public void setProject(MavenProject project) {
		this.project = project;
	}
	

	/**
	 * @param repositoryRelativePath the repositoryRelativePath to set
	 */
	public void setRepositoryRelativePath(String repositoryRelativePath) {
		this.repositoryRelativePath = repositoryRelativePath;
	}




	/**
	 * @param sourceGithubBranch the sourceGithubBranch to set
	 */
	public void setSourceGithubBranch(String sourceGithubBranch) {
		this.sourceGithubBranch = sourceGithubBranch;
	}



	/**
	 * @param externalCGitCommand the externalCGitCommand to set
	 */
	public void setExternalCGitCommand(String externalCGitCommand) {
		this.externalCGitCommand = externalCGitCommand;
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
	public FetchOpenPullRequestsMojo() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param specificPullRequest the specificPullRequest to set
	 */
	public void setSpecificPullRequest(Integer specificPullRequest) {
		this.specificPullRequest = specificPullRequest;
	}



	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		try {
			/*
			 * Create the repository for download of the pull requests.
			 */
			Repository projectRepository = GitRepositoryUtils.buildFileRepository(new File(project.getBasedir(), repositoryRelativePath), true, false);
			
			Map<GHRepository, List<PullRequestRefs>>repositoryToPullRequestsMap= new HashMap<GHRepository, List<PullRequestRefs>>();
			
			GitHub github = super.authorizeFromCredentials();
			
			String targetRepository = sourceGithubUser + "/" + sourceGithubRepo;
			
			GHRepository repo = github.getRepository(targetRepository);
			
			if (specificPullRequest != null && specificPullRequest > 0) {
				
				getLog().info("Fetching specific pull request " + specificPullRequest);
				
				GHPullRequest pullRequest = repo.getPullRequest(specificPullRequest);
				
				if (pullRequest == null)
					throw new MojoFailureException("No pull request found in " + targetRepository + " for pr-" + specificPullRequest);
				
				if (!expectedPullRequestHeadCommitId.equals(pullRequest.getHead().getSha()))
						throw new MojoFailureException("Pull Request " + pullRequest.getNumber() + " has been updated and its head is at " + pullRequest.getHead().getSha() + " instead of the expected " + expectedPullRequestHeadCommitId);
						
				storePullRequest (pullRequest, repositoryToPullRequestsMap);
			}
			else {
				getLog().info("Fetching all open pull requests");
				
				List<GHPullRequest> openPullRequests = repo.getPullRequests(GHIssueState.OPEN);
				
				for (GHPullRequest pullRequest : openPullRequests) {
					
					storePullRequest (pullRequest, repositoryToPullRequestsMap);
					
				}
			}
			
			/*
			 * We have to retrieve the commit graph pointed at by the head revision of the branch for each pull request.
			 * 
			 * After the fetch has been made we need to make a local branch for the pull request at the sha1 given in the pull request
			 * which may be different than what was given in the 
			 * 
			 */
	
			for (Entry<GHRepository, List<PullRequestRefs>> entry : repositoryToPullRequestsMap.entrySet()) {
				
				GHRepository remoteRepo = entry.getKey();
				
				String remoteRepositoryName = remoteRepo.getFullName();
				
				if (!remoteExists(remoteRepositoryName, projectRepository))
					projectRepository.getConfig().setString("remote", remoteRepositoryName, "url", remoteRepo.getGitTransportUrl());
				
				List<String>refSpecs = new ArrayList<String>();
				
				for (PullRequestRefs pullRequest : entry.getValue()) {
					
					String refSpec = String.format ("refs/heads/%s:refs/remotes/%s/pull-request-%s", pullRequest.getRefName(), remoteRepositoryName, pullRequest.getIssueNumber());
					
					refSpecs.add(refSpec);
					
				}
				
				projectRepository.getConfig().setStringList("remote", remoteRepositoryName, "fetch", refSpecs);
				
				projectRepository.getConfig().save();
				
				// deep fetch because we want to know which are based in the current branch.
				ExternalGitUtils.fetch (externalCGitCommand, projectRepository, remoteRepositoryName, 1, System.out);
				
			}
			
			List<ReceiveCommand> commands = new ArrayList<ReceiveCommand>();
			
			for (Entry<GHRepository, List<PullRequestRefs>> entry : repositoryToPullRequestsMap.entrySet()) {
				
				for (PullRequestRefs pullRequest : entry.getValue()) {
					
					// next create a local branch for each issue.
					commands.add(new ReceiveCommand(null, ObjectId.fromString(pullRequest.getCommitId()), String.format("refs/heads/pull-request-%d", pullRequest.getIssueNumber())));
					
					
				}
				
			}
			
			ExternalGitUtils.batchRefUpdate(externalCGitCommand, projectRepository, commands, System.out);
			
		} catch (IOException e) {
			throw new MojoExecutionException("FetchOpenPullRequestsMojo failed: ", e);
		}
		

	}

	private boolean storePullRequest(GHPullRequest pullRequest, Map<GHRepository, List<PullRequestRefs>> repositoryToPullRequestsMap) {
		GHCommitPointer head = pullRequest.getHead();
		
		GHRepository remoteRepository = head.getRepository();
		
		List<PullRequestRefs>refs = repositoryToPullRequestsMap.get(remoteRepository);
		
		if (refs == null) {
			refs = new ArrayList<PullRequestRefs>();
			repositoryToPullRequestsMap.put(remoteRepository, refs);
		}
		
		GHCommitPointer base = pullRequest.getBase();
		
		if (!sourceGithubBranch.equals(base.getRef())) {
			getLog().info("Skipping pull request: " + pullRequest.getNumber() + " because it does not apply to branch: " + sourceGithubBranch);
			return false;
		}

		String refName = head.getRef();
		
		String commitId = head.getSha();
		
		refs.add (new PullRequestRefs(pullRequest.getNumber(), remoteRepository, refName, commitId));
		
		return true;
		
	}



	private boolean remoteExists(String name, Repository projectRepository) {
		
		Config projectConfig = projectRepository.getConfig();
		
		Set<String> remotes = projectConfig.getSubsections("remote");
		
		if (remotes.contains(name))
			return true;
		else
			return false;
	}

}
