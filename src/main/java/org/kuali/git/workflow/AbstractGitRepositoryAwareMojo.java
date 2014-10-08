/**
 * 
 */
package org.kuali.git.workflow;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.lib.Repository;
import org.kuali.student.git.model.GitRepositoryUtils;
import org.kuali.student.git.utils.ExternalGitUtils;

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
	
	/*
	 * In the expected git credential format of: https://user:pass@example.com
	 */
	private List<String>remoteCredentials = new ArrayList<String>();
	
	protected Repository repository;
	
	protected boolean createRepository;
	
	protected boolean bareRepository;

	private File onDiskCredentialsFile;
	
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
	
	protected void addRemoteCredentialsFromEnvironment (String template, String userNameEnvironmentVariableName, String passwordEnvironmentVariableName) throws MojoExecutionException {
		
		String userName = System.getenv(userNameEnvironmentVariableName);

		if (userName == null)
			throw new MojoExecutionException("No varaible of name " + userNameEnvironmentVariableName + " for the userName exists in the environment.");
		
		String password = System.getenv(passwordEnvironmentVariableName);

		if (password == null)
			throw new MojoExecutionException("No varaible of name " + passwordEnvironmentVariableName + " for the password exists in the environment.");

		
		addRemoteCredentials(template, userName, password);
	}
	
	protected void addRemoteCredentials (String template, String userName, String password) {
		String credentials = String.format (template, userName, password);
		
		this.remoteCredentials.add(credentials);
	}
	
	

	protected void setupOnDiskCredentials() throws IOException {
		
		if (remoteCredentials != null && remoteCredentials.size() > 0) {
			
			onDiskCredentialsFile = File.createTempFile("git", "credentials");
	
			PrintWriter pw = new PrintWriter(onDiskCredentialsFile);
	
			for (String rc : remoteCredentials) {
				pw.println(rc);
			}
			
			pw.close();
			
//			 String fileStore = launcher.isUnix() ? store.getAbsolutePath() : "\\\"" + store.getAbsolutePath() + "\\\"";
             ExternalGitUtils.setupLocalCredentialHelper(externalCGitCommand, repository, onDiskCredentialsFile);
             
//             "config", "--local", "credential.helper", "store --file=" + tempFile.getAbsolutePath());
		
		}
		
	}
	
	protected void cleanupOnDiskCredentials() throws IOException {
		
		ExternalGitUtils.cleanupLocalCredentialHelper(externalCGitCommand, repository);
		
		onDiskCredentialsFile.delete();
		
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {

		try {
			repository = GitRepositoryUtils.buildFileRepository(new File(project.getBasedir(), repositoryRelativePath), createRepository, bareRepository);
		} catch (IOException e) {
			throw new MojoExecutionException("failed to initialize repository at: " + repositoryRelativePath, e);
		}
		
		onExecute();
	}

	protected abstract void onExecute() throws MojoExecutionException, MojoFailureException;
	
	
	

}
