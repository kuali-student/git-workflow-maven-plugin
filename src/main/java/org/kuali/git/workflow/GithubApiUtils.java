/**
 * 
 */
package org.kuali.git.workflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedIterable;

/**
 * @author ocleirig
 * 
 * Helper methods for interacting with the Github Api.
 *
 */
public class GithubApiUtils {

	private GHRepository repo;
	private Log log;

	/**
	 * @param log 
	 * 
	 */
	public GithubApiUtils(GHRepository repo, Log log) {
		this.repo = repo;
		this.log = log;
	}

	public void reportPullRequestCommitStatuses (int pullRequestNumber) throws IOException {
		
		List<PullRequestStatus> statuses = this.getPullRequestCommitStatus(pullRequestNumber);
		
		for (PullRequestStatus pullRequestStatus : statuses) {
			
			log.info(String.format("pr-%d: commit-> %s", pullRequestStatus.getPullRequestNumber(), pullRequestStatus.getCommitId()));
			
			for (GHCommitStatus status : pullRequestStatus.getStatuses()) {
				
				log.info(status.getState().name());
			}
		}
	}
	
	
	/**
	 * Return all of the commit statuses for the pull request given.
	 * 
	 * @param pullRequestNumber
	 * @return
	 * @throws IOException
	 */
	public List<PullRequestStatus> getPullRequestCommitStatus(int pullRequestNumber) throws IOException {
		
		GHPullRequest pullRequest = repo.getPullRequest(pullRequestNumber);
		
		GHCommitPointer base = pullRequest.getBase();
		GHCommitPointer head = pullRequest.getHead();
		
		List<PullRequestStatus> statuses = new ArrayList<PullRequestStatus>();
		
		// linked hash set because we want it to work like a queue first in first out.
		Set<String>currentCommitIds = new LinkedHashSet<String>();
		
		currentCommitIds.add(head.getSha());
		
		boolean foundBaseCommit = false;
		
		while (currentCommitIds.size() > 0) {
			
			Iterator<String> iterator = currentCommitIds.iterator();
			
			String currentCommitId = iterator.next();
			
			iterator.remove();
			
			PagedIterable<GHCommitStatus> currentCommitStatuses = repo.listCommitStatuses(currentCommitId);
			
			statuses.add(new PullRequestStatus(pullRequestNumber, currentCommitId, currentCommitStatuses));
			
			GHCommit currentCommit = repo.getCommit(currentCommitId);
			
			List<String> parents = currentCommit.getParentSHA1s();

			if (!foundBaseCommit) {
				
				if (parents.contains(base.getSha())) {
					foundBaseCommit = true;
				}
				else {
					currentCommitIds.addAll(parents);
				}
			}
			
		}
		
		return statuses;
		
	}

	/**
	 * Register the commit status for the pull request number.
	 * 
	 * @param pullRequestNumber
	 * @throws IOException 
	 */
	public void registerCommitStatus(String commitId, GHCommitState state, String ciUrl, String description, String context) throws IOException {
		
		repo.createCommitStatus(commitId, state, ciUrl, description, context);
		
		
	}
}
