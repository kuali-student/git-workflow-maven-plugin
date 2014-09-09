/**
 * 
 */
package org.kuali.git.workflow;

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
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
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
public class FetchOpenPullRequestsMojo extends AbstractMojo {

	@Component
	private MavenProject project;
	
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
	@Parameter(required=true, property="git-flow.sourceGithubUserAndRepo")
	private String sourceGithubUserAndRepo;
	
	
	/**
	 * @param project the project to set
	 */
	public void setProject(MavenProject project) {
		this.project = project;
	}

	/**
	 * @param externalCGitCommand the externalCGitCommand to set
	 */
	public void setExternalCGitCommand(String externalCGitCommand) {
		this.externalCGitCommand = externalCGitCommand;
	}

	/**
	 * @param projectRepository the projectRepository to set
	 */
	public void setProjectRepository(Repository projectRepository) {
		this.projectRepository = projectRepository;
	}

	/**
	 * @param sourceGithubUserAndRepo the sourceGithubUserAndRepo to set
	 */
	public void setSourceGithubUserAndRepo(String sourceGithubUserAndRepo) {
		this.sourceGithubUserAndRepo = sourceGithubUserAndRepo;
	}

	/**
	 * 
	 */
	public FetchOpenPullRequestsMojo() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		try {
			
			projectRepository = GitRepositoryUtils.buildFileRepository(project.getBasedir(), false, false);
			
			Map<GHRepository, List<PullRequestRefs>>repositoryToPullRequestsMap= new HashMap<GHRepository, List<PullRequestRefs>>();
			
			GitHub github = GitHub.connect(); 
			
			GHRepository repo = github.getRepository(sourceGithubUserAndRepo);
			
			List<GHPullRequest> openPullRequests = repo.getPullRequests(GHIssueState.OPEN);
			
			for (GHPullRequest pullRequest : openPullRequests) {
				
				GHCommitPointer head = pullRequest.getHead();
				
				GHRepository remoteRepository = head.getRepository();
				
				List<PullRequestRefs>refs = repositoryToPullRequestsMap.get(remoteRepository);
				
				if (refs == null) {
					refs = new ArrayList<PullRequestRefs>();
					repositoryToPullRequestsMap.put(remoteRepository, refs);
				}
				
				
				String refName = head.getRef();
				
				String commitId = head.getSha();
				
				refs.add (new PullRequestRefs(pullRequest.getNumber(), remoteRepository, refName, commitId));
				
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
				
				if (!remoteExists(remoteRepositoryName))
					projectRepository.getConfig().setString("remote", remoteRepositoryName, "url", remoteRepo.getGitTransportUrl());
				
				List<String>refSpecs = new ArrayList<String>();
				
				for (PullRequestRefs pullRequest : entry.getValue()) {
					
					String refSpec = String.format ("refs/heads/%s:refs/remotes/%s/pull-request-%s", pullRequest.getRefName(), remoteRepositoryName, pullRequest.getIssueNumber());
					
					refSpecs.add(refSpec);
					
				}
				
				projectRepository.getConfig().setStringList("remote", remoteRepositoryName, "fetch", refSpecs);
				
				
			}
			
			projectRepository.getConfig().save();
			
			ExternalGitUtils.fetchAll (externalCGitCommand, projectRepository, 1, System.out);
			
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

	private boolean remoteExists(String name) {
		
		Config projectConfig = projectRepository.getConfig();
		
		Set<String> remotes = projectConfig.getSubsections("remote");
		
		if (remotes.contains(name))
			return true;
		else
			return false;
	}

}
