/**
 * 
 */
package org.kuali.git.workflow;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommit.File;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

/**
 * Use the github api to check the comment of a branch in a remote repository for a particular string.
 * 
 * This was created to support a KS impex use case.  
 * 
 * We want to know if the impex changes are upto date.
 * 
 * 
 * @author ocleirig
 *
 */
@Mojo (name="crossCheckBranchContent")
@Execute (goal="crossCheckBranchContent", lifecycle="initialize")
public class CheckCrossProjectBranchContentThroughApiMojo extends
		AbstractGithubAuthorizedMojo {

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
	 * Any variables specified here will be included in the variables written into the files for the downstream jobs.
	 * 
	 * Expecting a comma seperated list of string values.
	 */
	@Parameter(property="git-flow.environmentVariablesToInclude")
	private List<String> environmentVariablesToInclude;
	
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
	 * @param sourceGithubBranch the sourceGithubBranch to set
	 */
	public void setSourceGithubBranch(String sourceGithubBranch) {
		this.sourceGithubBranch = sourceGithubBranch;
	}

	/**
	 * @param specificPullRequest the specificPullRequest to set
	 */
	public void setSpecificPullRequest(Integer specificPullRequest) {
		this.specificPullRequest = specificPullRequest;
	}

	
	public void setEnvironmentVariablesToInclude(
			List<String> environmentVariablesToInclude) {
		this.environmentVariablesToInclude = environmentVariablesToInclude;
	}

	/**
	 * 
	 */
	public CheckCrossProjectBranchContentThroughApiMojo() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		try {
			GitHub github = super.authorizeFromCredentials();
			
			String repositoryName = sourceGithubUser + "/" + sourceGithubRepo;
			
			GHRepository repo = github.getRepository(repositoryName);
			
			GHPullRequest pr = repo.getPullRequest(specificPullRequest);
			
			String baseCommitId = pr.getBase().getSha();
			
			String headCommitId = pr.getHead().getSha();
			
			GHCompare compare = repo.getCompare(baseCommitId, headCommitId);
			
			Set<String>changes = new HashSet<String>();
			
			for (File file : getFiles(compare)) {
				
				changes.add(file.getFileName());
			}
			
			java.io.File reportsBase = new java.io.File("target");
			
			reportsBase.mkdirs();
			
			Set<String> sqlModuleChanges = reportOnTopLevelDirectoriesWithSQLChanges(changes);
			
			if (sqlModuleChanges.size() > 0) {
				PrintWriter pw = new PrintWriter(new java.io.File (reportsBase, "sql-changes.dat"));
				
				pw.println("PULL_REQUEST_NUMBER=" + specificPullRequest);
				
				if (environmentVariablesToInclude != null && environmentVariablesToInclude.size() > 0) {
					
					for (String var : environmentVariablesToInclude) {
						
						String key = var.trim();
						
						String value = System.getenv(key);

						if (value != null) {
							pw.println(key + "=" + value);
						}
					}
			}
				
				pw.close();
			}
			
			Set<String> moduleChanges = reportOnTopLevelDirectoryChanges(changes);
			
			for (String module : moduleChanges) {
				PrintWriter pw = new PrintWriter(new java.io.File(reportsBase, module + "-changes.dat"));
				
				pw.println("PULL_REQUEST_NUMBER=" + specificPullRequest);
				pw.println("MODULE=" + module);
				
				if (environmentVariablesToInclude != null && environmentVariablesToInclude.size() > 0) {
						
						for (String var : environmentVariablesToInclude) {
							
							String key = var.trim();
							
							String value = System.getenv(key);

							if (value != null) {
								pw.println(key + "=" + value);
							}
						}
				}
				pw.close();
			}
			
			getLog().info("Changes to " + changes.size() + " files between pull request base and head.");
			
			getLog().info("Top Level Directory Changes to : " + StringUtils.join(moduleChanges, ", "));
			
			getLog().info("Top Level Directory SQL Changes to : " + StringUtils.join(sqlModuleChanges, ", "));
			
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to authorize from Credentials", e);
		}
		
	}
	




	private GHCommit.File[] getFiles(GHCompare compare) throws MojoExecutionException {
//		once github-api 1.59 is released use this instead
//		return compare.getFiles();
		
		try {
			Field f = compare.getClass().getDeclaredField("files");
			
			f.setAccessible(true);
			
			return (File[]) f.get(compare);
			
		} catch (NoSuchFieldException e) {
			throw new MojoExecutionException("failed to read compare.files using reflection", e);
		} catch (SecurityException e) {
			throw new MojoExecutionException("failed to read compare.files using reflection", e);
		} catch (IllegalArgumentException e) {
			throw new MojoExecutionException("failed to read compare.files using reflection", e);
		} catch (IllegalAccessException e) {
			throw new MojoExecutionException("failed to read compare.files using reflection", e);
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
