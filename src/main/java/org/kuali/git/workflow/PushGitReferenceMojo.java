/**
 * 
 */
package org.kuali.git.workflow;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.kuali.student.git.utils.ExternalGitUtils;

/**
 * 
 * Will push the identified git local commit reference (branch or tag) to a specified remote.
 * 
 * Will use a Credential Store to skip the login requirement.
 * 
 * @author ocleirig
 */
@Mojo (name="pushRef")
@Execute (goal="pushRef", lifecycle="initialize")
public class PushGitReferenceMojo extends AbstractGitRepositoryAwareMojo {

	@Parameter(property="localRef", required=true)
	private String localRef;
	
	@Parameter (property="remoteName", required=true)
	private String remoteName;
	
	@Parameter (property="remoteCredentialTemplate", required=true, defaultValue="https://%s:%s@github.com")
	private String remoteCredentialTemplate;
	
	/*
	 * Name of the environment variable containing the userName of the user with push rights to remoteName.
	 */
	@Parameter (property="userNameEnvVarName", required=true)
	private String userNameEnvVarName;
	
	/*
	 * Name of the environment variable containing the password of the user with push rights to remoteName.
	 */
	@Parameter (property="passwordEnvVarName", required=true)
	private String passwordEnvVarName;
	
	
	

	/**
	 * 
	 */
	public PushGitReferenceMojo() {
		// TODO Auto-generated constructor stub
	}

	


	

	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	protected void onExecute() throws MojoExecutionException,
			MojoFailureException {
		
		try {
			/*
			 * Create the repository for download of the pull requests.
			 */
			super.addRemoteCredentialsFromEnvironment(remoteCredentialTemplate, userNameEnvVarName, passwordEnvVarName);
			
			super.setupOnDiskCredentials();
			
			ExternalGitUtils.push(externalCGitCommand, super.repository, localRef, remoteName);
			
		} catch (IOException e) {
			throw new MojoExecutionException("FetchOpenPullRequestsMojo failed: ", e);
		}
		finally {
			super.cleanupOnDiskCredentials();
		}
		

	}

	
}
