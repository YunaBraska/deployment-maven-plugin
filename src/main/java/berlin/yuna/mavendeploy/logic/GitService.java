package berlin.yuna.mavendeploy.logic;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.model.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class GitService {

    private final Logger log;
    private final boolean fake;
    private final File workDir;
    private static final Pattern PATTERN_ORIGINAL_BRANCH_NAME = Pattern.compile("(?<prefix>.*?\\:\\s)(?<branchName>.*)");
    private static final Pattern PATTERN_ORIGINAL_BRANCH_NAME_COMMAND_LINE = Pattern.compile("(?<prefix>.*refs\\/.*?\\/)(?<branchName>.*?)(?<suffix>@.*?)");

    public GitService(final Logger log, final File workDir, final boolean fake) {
        this.workDir = workDir;
        this.log = log;
        this.fake = fake;
        logFakeMessage(log);
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

    public Properties getConfig() {
        final Properties properties = new Properties();
        final String config = getString("git config -l | cat");
        if (config != null) {
            try {
                properties.load(new StringReader(config.replaceAll("[\\n|\\r|\\s|$|]", "\n")));
            } catch (IOException e) {
                log.error("Could not read git config due [%s]" + e, config);
            }
        }
        return properties;
    }

    public Collection<ReflogEntry> getRefLog() {
        try {
            return Git.open(workDir).reflog().call();
        } catch (GitAPIException | IOException e) {
            log.error("Could not read git directory due " + e);
            return null;
        }
    }

    //TODO: test stash create and apply
    //TODO: test last git tag
    //TODO: get origin url
    public String getOriginUrl() {
        return getString("git config --get remote.origin.url");
    }

    public boolean gitHasChanges() {
        try {
            return Git.open(workDir).status().call().isClean();
        } catch (GitAPIException | IOException e) {
            log.error("Could not read git directory due " + e);
            return false;
        }
    }

    public boolean gitStash() {
        if (fake) {
            log.warn("fake stash");
        }
        try {
            final RevCommit stash = Git.open(workDir).stashCreate().call();
            return stash != null;
        } catch (GitAPIException | IOException e) {
            log.error("Failed to stash " + e);
            return false;
        }
//        return fake ? "fake stash" : getTerminal().execute("git stash clear; git stash").consoleInfo().trim();
    }

    public String gitLoadStash() {
        if (fake) {
            log.warn("fake load stash");
        }

        try {
            return Git.open(workDir).stashApply().call().toObjectId().getName();
        } catch (GitAPIException | IOException e) {
            log.error("Failed to load stash " + e);
            return "failed";
        }
    }

    public String findOriginalBranchName() {
        final Collection<ReflogEntry> refLog = getRefLog().stream().filter(log -> !log.getComment().startsWith("merge")).collect(Collectors.toList());
        for (ReflogEntry reflogEntry : refLog) {
            final Matcher matcher = PATTERN_ORIGINAL_BRANCH_NAME.matcher(reflogEntry.getComment() == null ? "" : reflogEntry.getComment());
            if (matcher.find()) {
                return matcher.group("branchName");
            }
        }
        return getBranchName();
    }

    private String getBranchName() {
        //        return getString("git rev-parse --abbrev-ref HEAD");
        try {
            final ReflogEntry reflogEntry = getRefLog().iterator().next();
            final List<Ref> branches = Git.open(workDir).branchList().call();
            for (Ref branch : branches) {
                if (branch.getObjectId().equals(reflogEntry.getNewId())) {
                    return branch.getName();
                }
            }
            return branches.get(0).getName();
        } catch (GitAPIException | IOException e) {
            log.error("Could not read git directory due " + e);
            return null;
        }
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
