package com.github.danielflower.mavenplugins.gitlog.filters;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

/**
 * Filter that checks whether files are in a given directory, before accepting them
 * @author mvaawl@gmail.com
 *
 */
public class ModuleCommitFilter implements CommitFilter {

	private String projectBaseDir;
	private Log log;

	public ModuleCommitFilter(String projectBaseDir, Log log) {
		this.projectBaseDir = projectBaseDir;
		this.log = log;
	}

	public boolean renderCommit(RevCommit commit, Repository repository) {
		
		if (log.isDebugEnabled()) {
			log.debug("[ModuleCommitFilter] Project located at : " + projectBaseDir);
			log.debug("[ModuleCommitFilter] Rendering commit : "+commit.getName());
		}
		File repositoryFile = repository.getDirectory();

		if (repositoryFile == null) {
			log.info("[ModuleCommitFilter] Repository is not local. Automatically accepts commit: "+commit.getName());
			return true;
		}
		if (commit.getParentCount() == 0) {
			log.info("[ModuleCommitFilter] No commit parent found. Automatically accepts commit: "+commit.getName());
			return true;
		}

		String repositoryBaseDir = repositoryFile.getParentFile().getPath();

		try {
			RevWalk rw = new RevWalk(repository);
			RevCommit parent = rw.parseCommit(commit.getParent(0).getId());
			DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
			df.setRepository(repository);
			df.setDiffComparator(RawTextComparator.DEFAULT);
			df.setDetectRenames(true);
			List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
			for (DiffEntry diff : diffs) {
				String diffFullPath = repositoryBaseDir + File.separator + diff.getNewPath();
				if (log.isDebugEnabled()) {
					log.debug("[ModuleCommitFilter] File identified in this commit : "+diffFullPath);
				}
				if (diffFullPath.startsWith(this.projectBaseDir)) {
					if (log.isDebugEnabled()) {
						log.debug("[ModuleCommitFilter] Accepted commit within this service : "+commit.getName());
					}
					return true;
				}
			}
		} catch (Exception e) {
			log.error(e);
			return true;
		}
		if (log.isDebugEnabled()) {
			log.debug("[ModuleCommitFilter] Commit not accepted for this service. : "+commit.getName());
		}
		return false;
	}

}
