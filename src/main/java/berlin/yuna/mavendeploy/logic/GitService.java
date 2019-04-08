package berlin.yuna.mavendeploy.logic;

import berlin.yuna.clu.logic.Terminal;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

//FIXME: add GitLibrary like JGit
public class GitService {

    private final boolean fake;
    private final File workDir;
    private final Terminal terminal;
    private static final Pattern PATTERN_ORIGINAL_BRANCH_NAME = Pattern.compile(
            "(?<prefix>.*refs\\/.*?\\/)(?<branchName>.*?)(?<suffix>@.*?)");

    public GitService(final Log log, final File workDir, final boolean fake) {
        this.workDir = workDir;
        this.fake = fake;
        logFakeMessage(log);
        terminal = new Terminal().timeoutMs(30000).breakOnError(true).dir(workDir).consumerError(log::error);
    }

    public File clone(final String url, final String name) {
        terminal.breakOnError(false).execute("git clone " + url + " " + name);
        return new File(workDir, name);
    }

    public String getLastGitTag() {
        final String tag;
        try {
            terminal.execute("git fetch --tags --force");
        } catch (Exception ignored) {
        } finally {
            tag = terminal.execute("git describe --tag --always --abbrev=0").consoleInfo().trim();
        }
        return tag;
    }

    public String getLastRefLog(final int commitNumber) {
        return terminal.execute("git reflog show --all | grep \"refs/\"  | grep \": commit:\" | head -n" + commitNumber + " | tail -n1").consoleInfo();
    }

    public boolean gitHasChanges() {
        return Boolean.valueOf(terminal.execute("if [[ `git status --porcelain` ]]; then echo true; else echo false; fi").consoleInfo().trim());
    }

    public String gitStash() {
        return fake ? "fake stash" : terminal.execute("git stash clear; git stash").consoleInfo().trim();
    }

    public String gitLoadStash() {
        return fake ? "fake load stash" : terminal.execute("git checkout stash -- .").consoleInfo().trim();
    }

    public String findOriginalBranchName(final int commitNumber) {
        final String refLog = getLastRefLog(commitNumber);
        final Matcher matcher = PATTERN_ORIGINAL_BRANCH_NAME.matcher(refLog);
        if (matcher.find()) {
            return matcher.group("branchName");
        }
        return null;
    }

    private void logFakeMessage(final Log log) {
        if (fake) {
            log.warn(format("Faked [%s]", getClass().getSimpleName()));
        }
    }
}
