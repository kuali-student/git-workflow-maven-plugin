/**
 * 
 */
package org.kuali.git.workflow.model;

import org.kohsuke.github.GHRepository;

/**
 * @author ocleirig
 * 
 * Details about a pull request object head.
 *
 */
public class PullRequestRefs {

	private Integer issueNumber;
	
	private GHRepository remoteRepository;
	private String refName;
	private String commitId;

	public PullRequestRefs(int issueNumber, GHRepository remoteRepository, String refName,
			String commitId) {
				this.issueNumber = issueNumber;
				this.remoteRepository = remoteRepository;
				this.refName = refName;
				this.commitId = commitId;
	}

	/**
	 * @return the remoteRepository
	 */
	public GHRepository getRemoteRepository() {
		return remoteRepository;
	}

	/**
	 * @return the refName
	 */
	public String getRefName() {
		return refName;
	}

	/**
	 * @return the commitId
	 */
	public String getCommitId() {
		return commitId;
	}

	/**
	 * @return the issueNumber
	 */
	public Integer getIssueNumber() {
		return issueNumber;
	}

}
