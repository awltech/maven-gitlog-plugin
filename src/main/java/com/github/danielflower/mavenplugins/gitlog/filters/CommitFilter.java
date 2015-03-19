package com.github.danielflower.mavenplugins.gitlog.filters;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

public interface CommitFilter {

	/**
	 * Returns true if the commit should be rendered; otherwise false.
	 */
	public boolean renderCommit(RevCommit commit, Repository repository);

}
