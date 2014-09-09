/**
 * 
 */
package org.kuali.github;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.HttpClientUtils;
import org.junit.Assert;
import org.junit.Test;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ocleirig
 *
 */
public class TestCLAVerification {

	private static Logger log = LoggerFactory.getLogger(TestCLAVerification.class);
	
	/**
	 * 
	 */
	public TestCLAVerification() {
	}
	
	@Test
	public void testCLAVerification() throws IOException {
		
		GitHub github = GitHub.connect(); 
		
		GHRepository repo = github.getRepository("kuali-student/ks-development");
		
		Assert.assertNotNull(repo);
		
		List<GHPullRequest> openPullRequests = repo.getPullRequests(GHIssueState.OPEN);
		
		// sort by pull request number ascending
		Collections.sort(openPullRequests, new Comparator<GHPullRequest>() {

			@Override
			public int compare(GHPullRequest o1, GHPullRequest o2) {
				
				Integer i1 = o1.getNumber();
				Integer i2 = o2.getNumber();
				
				return i1.compareTo(i2);
			}
			
		});
		
		for (GHPullRequest pullRequest : openPullRequests) {
			
			GHCommitPointer base = pullRequest.getBase();
			
			GHCommit commit = repo.getCommit(base.getSha());
			
			commit.listStatuses();
			
			URL patch = pullRequest.getPatchUrl();
			
			byte[] patchBytes = IOUtils.toByteArray(patch.openStream());
			
			String patchString = new String (patchBytes);
			
			log.info("patchString =" + patchString.length());
		}
		
		
	}

}
