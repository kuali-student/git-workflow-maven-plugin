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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kuali.student.git.utils.ExternalGitUtils;

/**
 * 
 * Will push the identified git local commit reference (branch or tag) to a
 * specified remote.
 * 
 * Will use a Credential Store to skip the login requirement.
 * 
 * @author ocleirig
 */
@Mojo(name = "pushRef")
@Execute(goal = "pushRef", lifecycle = "initialize")
public class PushGitReferenceMojo extends AbstractGitRepositoryAwareMojo {

	@Parameter(property = "localRef", required = true)
	private String localRef;

	@Parameter(property = "remoteName", required = true)
	private String remoteName;

	/*
	 * Name of the environment variable containing the userName of the user with
	 * push rights to remoteName.
	 */
	@Parameter(property = "userNameEnvVarName", required = true)
	private String userNameEnvVarName;

	/*
	 * Name of the environment variable containing the password of the user with
	 * push rights to remoteName.
	 */
	@Parameter(property = "passwordEnvVarName", required = true)
	private String passwordEnvVarName;

	@Parameter(property = "timeoutInSeconds")
	private int timeoutInSeconds = 30;

	/**
	 * 
	 */
	public PushGitReferenceMojo() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param localRef
	 *            the localRef to set
	 */
	public void setLocalRef(String localRef) {
		this.localRef = localRef;
	}

	/**
	 * @param remoteName
	 *            the remoteName to set
	 */
	public void setRemoteName(String remoteName) {
		this.remoteName = remoteName;
	}

	/**
	 * @param userNameEnvVarName
	 *            the userNameEnvVarName to set
	 */
	public void setUserNameEnvVarName(String userNameEnvVarName) {
		this.userNameEnvVarName = userNameEnvVarName;
	}

	/**
	 * @param passwordEnvVarName
	 *            the passwordEnvVarName to set
	 */
	public void setPasswordEnvVarName(String passwordEnvVarName) {
		this.passwordEnvVarName = passwordEnvVarName;
	}

	/**
	 * @param timeoutInSeconds
	 *            the timeoutInSeconds to set
	 */
	public void setTimeoutInSeconds(int timeoutInSeconds) {
		this.timeoutInSeconds = timeoutInSeconds;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	protected void onExecute() throws MojoExecutionException,
			MojoFailureException {

		String userName = System.getenv(userNameEnvVarName);

		if (userName == null)
			throw new MojoExecutionException("No varaible of name "
					+ userNameEnvVarName
					+ " for the userName exists in the environment.");

		String password = System.getenv(passwordEnvVarName);

		if (password == null)
			throw new MojoExecutionException("No varaible of name "
					+ passwordEnvVarName
					+ " for the password exists in the environment.");

		Git git = new Git(repository);

		PushCommand pushCommand = git.push();

		try {
			Iterable<PushResult> results = pushCommand
					.setRemote(remoteName)
					.add(localRef)
					.setTimeout(timeoutInSeconds)
					.setCredentialsProvider(
							new UsernamePasswordCredentialsProvider(userName,
									password)).call();

			for (PushResult pushResult : results) {

				for (RemoteRefUpdate rru : pushResult.getRemoteUpdates()) {

					getLog().info(
							"Ref Update: " + rru.getRemoteName() + " to "
									+ rru.getSrcRef() + " status: "
									+ rru.getStatus().name());
				}
			}
		} catch (InvalidRemoteException e) {
			throw new MojoExecutionException("Failed to push " + localRef
					+ " to remote: " + remoteName, e);
		} catch (TransportException e) {
			throw new MojoExecutionException("Failed to push " + localRef
					+ " to remote: " + remoteName, e);
		} catch (GitAPIException e) {
			throw new MojoExecutionException("Failed to push " + localRef
					+ " to remote: " + remoteName, e);
		}

	}

}
