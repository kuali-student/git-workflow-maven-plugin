/**
 * 
 */
package org.kuali.git.workflow;

import java.util.Collections;
import java.util.List;

import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.PagedIterable;

/**
 * @author ocleirig
 *
 */
public class PullRequestStatus {

	private int pullRequestNumber;
	
	private String commitId;
	
	// ordered most recent to oldest.
	private PagedIterable<GHCommitStatus> statuses;

	public PullRequestStatus(int pullRequestNumber, String commit,
			PagedIterable<GHCommitStatus> statuses) {
		super();
		this.pullRequestNumber = pullRequestNumber;
		this.commitId = commit;
		this.statuses = statuses;
	}

	/**
	 * @return the pullRequestNumber
	 */
	public int getPullRequestNumber() {
		return pullRequestNumber;
	}

	/**
	 * @return the commit
	 */
	public String getCommitId() {
		return commitId;
	}

	/**
	 * @return the statuses
	 */
	public PagedIterable<GHCommitStatus> getStatuses() {
		return statuses;
	}

	
	

}
