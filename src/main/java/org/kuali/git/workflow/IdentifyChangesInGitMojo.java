/**
 * 
 */
package org.kuali.git.workflow;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.kuali.student.git.model.GitRepositoryUtils;

/**
 * @author ocleirig
 * 
 * Identify the changes between two git commits.
 * 
 * We will emit the modules and if there are sql changes or not.
 * 
 * This is useful for routing a set of changes (i.e. if impex is required more time will be required)
 *
 */
@Mojo (name="identifyChangesInGit")
@Execute (goal="identifyChangesInGit", lifecycle="initialize")
public class IdentifyChangesInGitMojo extends AbstractGitRepositoryAwareMojo {

	@Parameter (required = true, property="git-flow.sourceBranch")
	private String sourceBranch;
	
	@Parameter (required = true, property="git-flow.targetBranch")
	private String targetBranch;
	
	
	/**
	 * The directory where the changes report file is written
	 */
	@Parameter (property="git-flow.changesReportTargetDirectory", defaultValue="target")
	private String changesReportTargetDirectory;
	
	
	
	/**
	 * @param sourceBranch the sourceBranch to set
	 */
	public void setSourceBranch(String sourceBranch) {
		this.sourceBranch = sourceBranch;
	}




	/**
	 * @param targetBranch the targetBranch to set
	 */
	public void setTargetBranch(String targetBranch) {
		this.targetBranch = targetBranch;
	}




	/**
	 * 
	 */
	public IdentifyChangesInGitMojo() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		try {
			
			Repository projectRepository = GitRepositoryUtils.buildFileRepository(new File (project.getBasedir(), super.repositoryRelativePath), false, false);
			
			Ref sourceRef = projectRepository.getRef(sourceBranch);
			
			if (sourceRef == null)
				throw new MojoFailureException("no ref found for sourceBranch: " + sourceBranch);
			
			Ref targetRef = projectRepository.getRef(targetBranch);
			
			if (targetRef == null)
				throw new MojoFailureException("no ref found for targetBranch: " + targetBranch);
			
			RevWalk rw = new RevWalk (projectRepository);
			
			RevCommit sourceCommit = rw.parseCommit(sourceRef.getObjectId());
			RevCommit targetCommit = rw.parseCommit(targetRef.getObjectId());
			
			TreeWalk tw = new TreeWalk(projectRepository);
			
			tw.addTree(sourceCommit.getTree().getId());
			tw.addTree(targetCommit.getTree().getId());
			
			tw.setRecursive(true);
			
			Set<String>changes = new HashSet<String>();
			
			
			while (tw.next()) {
				
				ObjectId origId = tw.getObjectId(0);
				
				ObjectId newId = tw.getObjectId(1);
				
				if (((origId == null || newId == null) && (origId != newId)) || !origId.equals(newId)) {
					// change of some kinds.
					
					String path = tw.getPathString();
					
					changes.add(path);
				}
				
			}
			
			Set<String>topLevelDirectoryChanges = reportOnTopLevelDirectoryChanges(changes);
			
			Set<String>topLevelDirectoriesWithSQLChanges = reportOnTopLevelDirectoriesWithSQLChanges (changes);
			
			getLog().info("changes to : " + StringUtils.join(changes, ", "));
			
			getLog().info("Top Level Directory Changes to : " + StringUtils.join(topLevelDirectoryChanges, ", "));
			
			getLog().info("Top Level Directory SQL Changes to : " + StringUtils.join(topLevelDirectoriesWithSQLChanges, ", "));
			
			tw.release();
			rw.release();
			
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
