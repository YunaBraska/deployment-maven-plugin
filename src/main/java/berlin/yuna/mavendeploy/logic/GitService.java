package berlin.yuna.mavendeploy.logic;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.model.Logger;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

//FIXME: add GitLibrary like JGit
public class GitService {

    private final Logger log;
    private final boolean fake;
    private final File workDir;
    private static final Pattern PATTERN_ORIGINAL_BRANCH_NAME = Pattern.compile(
            "(?<prefix>.*refs\\/.*?\\/)(?<branchName>.*?)(?<suffix>@.*?)");

    public GitService(final Logger log, final File workDir, final boolean fake) {
        this.workDir = workDir;
        this.log = log;
        this.fake = fake;
        logFakeMessage(log);
    }

    public File clone(final String url, final String name) {
        getTerminal().breakOnError(false).execute("git clone " + url + " " + name);
        return new File(workDir, name);
    }

    public String getLastGitTag() {
        final String tag;
        try {
            getString("git fetch --tags --force");
        } catch (Exception ignored) {
        } finally {
            tag = getString("git describe --tag --always --abbrev=0");
        }
        return tag;
    }

    public String getLastRefLog(final int commitNumber) {
        return getString("git reflog show --all | grep \"refs/\"  | grep \": commit:\" | head -n" + commitNumber + " | tail -n1");
    }

    public String getOriginUrl() {
        return getString("git config --get remote.origin.url");
    }

    public boolean gitHasChanges() {
        return Boolean.valueOf(getTerminal().execute("if [[ `git status --porcelain` ]]; then echo true; else echo false; fi").consoleInfo().trim());
    }

    public String gitStash() {
        return fake ? "fake stash" : getTerminal().execute("git stash clear; git stash").consoleInfo().trim();
    }

    public String gitLoadStash() {
        return fake ? "fake load stash" : getTerminal().execute("git checkout stash -- .").consoleInfo().trim();
    }

    public String findOriginalBranchName(final int commitNumber) {
        final String refLog = getLastRefLog(commitNumber);
        final Matcher matcher = PATTERN_ORIGINAL_BRANCH_NAME.matcher(refLog == null ? "" : refLog);
        if (matcher.find()) {
            return matcher.group("branchName");
        }
        return getString("git rev-parse --abbrev-ref HEAD");
    }

    private void logFakeMessage(final Logger log) {
        if (fake) {
            log.warn(format("Faked [%s]", getClass().getSimpleName()));
        }
    }

    private Terminal getTerminal() {
        return new Terminal().timeoutMs(30000).breakOnError(true).dir(workDir).consumerError(log::error);
    }

    private String getString(final String command) {
//        workaround as terminal cannot handle empty strings yet
        final String result = getTerminal().execute("tmp=$(" + command + "); if [ -z \"${tmp}\" ]; then echo null; else echo ${tmp}; fi").consoleInfo();
        return result.equalsIgnoreCase("null") ? null : result.trim();
    }
}
