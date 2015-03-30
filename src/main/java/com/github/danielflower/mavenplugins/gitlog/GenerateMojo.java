package com.github.danielflower.mavenplugins.gitlog;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.github.danielflower.mavenplugins.gitlog.filters.CommitFilter;
import com.github.danielflower.mavenplugins.gitlog.filters.ModuleCommitFilter;
import com.github.danielflower.mavenplugins.gitlog.renderers.ChangeLogRenderer;
import com.github.danielflower.mavenplugins.gitlog.renderers.Formatter;
import com.github.danielflower.mavenplugins.gitlog.renderers.GitHubIssueLinkConverter;
import com.github.danielflower.mavenplugins.gitlog.renderers.JiraIssueLinkConverter;
import com.github.danielflower.mavenplugins.gitlog.renderers.JsonRenderer;
import com.github.danielflower.mavenplugins.gitlog.renderers.MarkdownRenderer;
import com.github.danielflower.mavenplugins.gitlog.renderers.MavenLoggerRenderer;
import com.github.danielflower.mavenplugins.gitlog.renderers.MessageConverter;
import com.github.danielflower.mavenplugins.gitlog.renderers.NullMessageConverter;
import com.github.danielflower.mavenplugins.gitlog.renderers.PlainTextRenderer;
import com.github.danielflower.mavenplugins.gitlog.renderers.SimpleHtmlRenderer;

/**
 * Goal which generates a changelog based on commits made to the current git
 * repo.
 *
 * @goal generate
 * @phase prepare-package
 */
public class GenerateMojo extends AbstractMojo {

	/**
	 * @parameter expression="${project}"
	 */
	private MavenProject project;
	/**
	 * The directory to put the reports in. Defaults to the project build
	 * directory (normally target).
	 *
	 * @parameter default-value="${project.build.directory}"
	 *            expression="${gitlog.outputDirectory}"
	 * @required
	 */
	private File outputDirectory;

	/**
	 * The title of the reports. Defaults to: ${project.name}
	 * v${project.version} changelog
	 *
	 * @parameter expression="${project.name} v${project.version} changelog"
	 */
	private String reportTitle;

	/**
	 * If true, then a plain text changelog will be generated.
	 *
	 * @parameter default-value="true"
	 *            expression="${gitlog.generatePlainTextChangeLog}"
	 */
	private boolean generatePlainTextChangeLog;

	/**
	 * The filename of the plain text changelog, if generated.
	 *
	 * @parameter default-value="changelog.txt"
	 *            expression="${gitlog.plainTextChangeLogFilename}"
	 * @required
	 */
	private String plainTextChangeLogFilename;

	/**
	 * If true, then a markdown changelog will be generated.
	 *
	 * @parameter default-value="false"
	 *            expression="${gitlog.generateMarkdownChangeLog}"
	 */
	private boolean generateMarkdownChangeLog;

	/**
	 * The filename of the markdown changelog, if generated.
	 *
	 * @parameter default-value="changelog.md"
	 *            expression="${gitlog.markdownChangeLogFilename}"
	 * @required
	 */
	private String markdownChangeLogFilename;

	/**
	 * If true, then a simple HTML changelog will be generated.
	 *
	 * @parameter default-value="true"
	 *            expression="${gitlog.generateSimpleHTMLChangeLog}"
	 */
	private boolean generateSimpleHTMLChangeLog;

	/**
	 * The filename of the simple HTML changelog, if generated.
	 *
	 * @parameter default-value="changelog.html"
	 *            expression="${gitlog.simpleHTMLChangeLogFilename}"
	 * @required
	 */
	private String simpleHTMLChangeLogFilename;

	/**
	 * If true, then an HTML changelog which contains only a table element will
	 * be generated. This incomplete HTML page is suitable for inclusion in
	 * other webpages, for example you may want to embed it in a wiki page.
	 *
	 * @parameter default-value="false"
	 *            expression="${gitlog.generateHTMLTableOnlyChangeLog}"
	 */
	private boolean generateHTMLTableOnlyChangeLog;

	/**
	 * The filename of the HTML table changelog, if generated.
	 *
	 * @parameter default-value="changelogtable.html"
	 *            expression="${gitlog.htmlTableOnlyChangeLogFilename}"
	 * @required
	 */
	private String htmlTableOnlyChangeLogFilename;

	/**
	 * If true, then a JSON changelog will be generated.
	 *
	 * @parameter default-value="true"
	 *            expression="${gitlog.generateJSONChangeLog}"
	 */
	private boolean generateJSONChangeLog;

	/**
	 * The filename of the JSON changelog, if generated.
	 *
	 * @parameter default-value="changelog.json"
	 *            expression="${gitlog.jsonChangeLogFilename}"
	 * @required
	 */
	private String jsonChangeLogFilename;

	/**
	 * If true, the changelog will be printed to the Maven build log during
	 * packaging.
	 *
	 * @parameter default-value="false" expression="${gitlog.verbose}"
	 */
	private boolean verbose;

	/**
	 * Used to create links to your issue tracking system for HTML reports. If
	 * unspecified, it will try to use the value specified in the
	 * issueManagement section of your project's POM. The following values are
	 * supported: a value containing the string "github" for the GitHub Issue
	 * tracking software; a value containing the string "jira" for Jira tracking
	 * software. Any other value will result in no links being made.
	 *
	 * @parameter expression="${project.issueManagement.system}"
	 */
	private String issueManagementSystem;

	/**
	 * Used to create links to your issue tracking system for HTML reports. If
	 * unspecified, it will try to use the value specified in the
	 * issueManagement section of your project's POM.
	 *
	 * @parameter expression="${project.issueManagement.url}"
	 */
	private String issueManagementUrl;
	
	/**
	 * @parameter expression="${project}"
	 */
	private MavenProject mavenProject;
	
	/**
	 * @parameter expression="${gitlog.filterOnModules}" default-value="false"
	 */
	private boolean filterOnModules;

	/**
	 * Used to set date format in log messages. If unspecified, will be used
	 * default format 'yyyy-MM-dd HH:mm:ss Z'.
	 * 
	 * @parameter default-value="yyyy-MM-dd HH:mm:ss Z"
	 *            expression="${gitlog.dateFormat}"
	 */
	private String dateFormat;

	/**
	 * If true, the changelog will include the full git message rather that the
	 * short git message
	 *
	 * @parameter default-value="false" expression="${gitlog.fullGitMessage}"
	 */
	private boolean fullGitMessage;

	/**
	 * Include in the changelog the commits after this parameter value.
	 * 
	 * @parameter default-value="1970-01-01 00:00:00 GMT"
	 *            expression="${gitlog.includeCommitsAfter}"
	 */
	private String includeCommitsAfter;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info(
				"Generating changelog in " + outputDirectory + " with title "
						+ reportTitle);
		File f = this.outputDirectory;
		if (!f.exists()) {
			f.mkdirs();
		}

		List<ChangeLogRenderer> renderers;
		try {
			renderers = createRenderers();
		} catch (IOException e) {
			getLog().warn(
					"Error while setting up gitlog renderers.  No changelog will be generated.",
					e);
			return;
		}
		List<CommitFilter> commitFilters = new ArrayList<CommitFilter>();
		commitFilters.addAll(Defaults.COMMIT_FILTERS);
//		if (this.filterOnModules && mavenProject != null) {
//			getLog().info(
//					"Filter on commits, to consider only the ones in given service is enabled.");
//			commitFilters.add(new ModuleCommitFilter(mavenProject.getBasedir().getPath(), getLog()));
//		}
		Generator generator = new Generator(renderers,commitFilters,
				getLog());

		try {
			generator.openRepository(project.getFile().getAbsolutePath());

		} catch (IOException e) {
			getLog().warn(
					"Error opening git repository.  Is this Maven project hosted in a git repository? "
							+ "No changelog will be generated.", e);
			return;
		} catch (NoGitRepositoryException e) {
			getLog().warn(
					"This maven project does not appear to be in a git repository, "
							+ "therefore no git changelog will be generated.");
			return;
		}

		if (!"".equals(dateFormat)) {
			Formatter.setFormat(dateFormat, getLog());
		}

		Date includeCommitsAfterDate = new Date(0);

		try {
			includeCommitsAfterDate = new SimpleDateFormat(dateFormat)
					.parse(includeCommitsAfter);
		} catch (ParseException pe) {
			getLog().warn(
					"Could not format date since pattern is not valid or date is not correct. Will retrieve all logs !",
					pe);
		} catch (IllegalArgumentException iae) {
			getLog().warn(
					"Could not format date since pattern is not valid or date is not correct. Will retrieve all logs !",
					iae);
		}

		try {
			generator.generate(reportTitle, includeCommitsAfterDate);
		} catch (IOException e) {
			getLog().warn(
					"Error while generating changelog.  Some changelogs may be incomplete or corrupt.",
					e);
		}
	}

	private List<ChangeLogRenderer> createRenderers() throws IOException {
		ArrayList<ChangeLogRenderer> renderers = new ArrayList<ChangeLogRenderer>();

		if (generatePlainTextChangeLog) {
			renderers.add(new PlainTextRenderer(getLog(), this.outputDirectory,
					plainTextChangeLogFilename, fullGitMessage));
		}

		if (generateSimpleHTMLChangeLog || generateHTMLTableOnlyChangeLog
				|| generateMarkdownChangeLog) {
			MessageConverter messageConverter = getCommitMessageConverter();
			if (generateSimpleHTMLChangeLog) {
				renderers.add(new SimpleHtmlRenderer(getLog(),
						this.outputDirectory, simpleHTMLChangeLogFilename,
						fullGitMessage, messageConverter, false));
			}
			if (generateHTMLTableOnlyChangeLog) {
				renderers.add(new SimpleHtmlRenderer(getLog(),
						this.outputDirectory, htmlTableOnlyChangeLogFilename,
						fullGitMessage, messageConverter, true));
			}
			if (generateMarkdownChangeLog) {
				renderers.add(new MarkdownRenderer(getLog(),
						this.outputDirectory, markdownChangeLogFilename,
						fullGitMessage, messageConverter));
			}
		}

		if (generateJSONChangeLog) {
			renderers.add(new JsonRenderer(getLog(), this.outputDirectory,
					jsonChangeLogFilename, fullGitMessage));
		}

		if (verbose) {
			renderers.add(new MavenLoggerRenderer(getLog()));
		}

		return renderers;
	}

	private MessageConverter getCommitMessageConverter() {
		getLog().debug(
				"Trying to load issue tracking info: " + issueManagementSystem
						+ " / " + issueManagementUrl);
		MessageConverter converter = null;
		try {
			if (issueManagementUrl != null
					&& issueManagementUrl.contains("://")) {
				String system = ("" + issueManagementSystem).toLowerCase();
				if (system.contains("jira")) {
					converter = new JiraIssueLinkConverter(getLog(),
							issueManagementUrl);
				} else if (system.contains("github")) {
					converter = new GitHubIssueLinkConverter(getLog(),
							issueManagementUrl);
				}
			}
		} catch (Exception ex) {
			getLog().warn(
					"Could not load issue management system information; no HTML links will be generated.",
					ex);
		}
		if (converter == null) {
			converter = new NullMessageConverter();
		}
		getLog().debug("Using tracker " + converter.getClass().getSimpleName());
		return converter;
	}

}
