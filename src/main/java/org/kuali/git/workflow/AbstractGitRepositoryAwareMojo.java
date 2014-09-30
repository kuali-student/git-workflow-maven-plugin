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

	/**
	 * @param repositoryRelativePath the repositoryRelativePath to set
	 */
	public void setRepositoryRelativePath(String repositoryRelativePath) {
		this.repositoryRelativePath = repositoryRelativePath;
	}

	

}
