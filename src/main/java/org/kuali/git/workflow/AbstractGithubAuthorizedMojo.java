/**
 * 
 */
package org.kuali.git.workflow;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.kohsuke.github.GitHub;

/**
 * 
 * Authorization for github can come from a ~/.github file
 * 
 * or it can be read from the process environment using:
 * 
 * <br/>
 * GITHUB_AUTH_USERNAME
 * <br/>
 * GITHUB_AUTH_PASSWORD
 * <br/>
 * 
 * @author ocleirig
 */
public abstract class AbstractGithubAuthorizedMojo extends AbstractMojo {

	private static final String GITHUB_AUTH_PASSWORD = "GITHUB_AUTH_PASSWORD";
	private static final String GITHUB_AUTH_USERNAME = "GITHUB_AUTH_USERNAME";

	/**
	 * 
	 */
	public AbstractGithubAuthorizedMojo() {
		
	}

	protected GitHub authorizeFromCredentials () throws IOException {
		
		try {
			return GitHub.connect();
			
		} catch (FileNotFoundException e) {
			return authorizeFromEnvironment(GITHUB_AUTH_USERNAME, GITHUB_AUTH_PASSWORD);
		}
		
		
	}
	
	private GitHub authorizeFromEnvironment(String userNameVariableName, String passwordVariableName) throws IOException {
		
		String user = System.getenv(userNameVariableName);
		
		String password = System.getenv(passwordVariableName);
		
		return GitHub.connectUsingPassword(user, password);
		
		
	}
	

}
