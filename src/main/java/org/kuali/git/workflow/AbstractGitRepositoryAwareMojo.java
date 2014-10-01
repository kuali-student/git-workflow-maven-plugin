/**
 * 
 */
package org.kuali.git.workflow;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * A base class to hold the repository location.
 * 
 * @author ocleirig
 *
 */
public abstract class AbstractGitRepositoryAwareMojo extends
		AbstractGithubAuthorizedMojo {

	/**
	 * Certain operations are slow for JGit so this allows us to run them using C git.
	 * 
	 */
	@Parameter(property="git-flow.cGitCommand", defaultValue="git")
	protected String externalCGitCommand;
	
	/*
	 * 
	 */
	@Parameter (property="git-flow.repositoryRelativePath", required=true, defaultValue="target/git-repository")
	protected String repositoryRelativePath;
	
	/**
	 * 
	 */
	public AbstractGitRepositoryAwareMojo() {
		super();
	}

	public final void setRepositoryRelativePath(String repositoryRelativePath) {
		this.repositoryRelativePath = repositoryRelativePath;
	}

	public final void setExternalCGitCommand(String externalCGitCommand) {
		this.externalCGitCommand = externalCGitCommand;
	}

	
	

}
