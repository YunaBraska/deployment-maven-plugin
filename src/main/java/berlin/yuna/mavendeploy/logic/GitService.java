package berlin.yuna.mavendeploy.logic;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.model.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.ReflogEntry;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;

//TODO can be replaced by SCM Plugin?
public class GitService {

    private final Logger log;
    private final boolean fake;
    private final File workDir;
    private final boolean active;
    private static final Pattern PATTERN_ORIGINAL_BRANCH_NAME = Pattern.compile("(?<prefix>.*?\\:\\s)(?<branchName>.*)");

    public GitService(final Logger log, final File workDir, final boolean fake) {
        final boolean hasGit = hasGit(log, workDir);
        this.workDir = workDir;
        this.log = log;
        this.fake = fake && !hasGit;
        this.active = hasGit;
        logFakeMessage(log);
    }

    public String getLastGitTag() {
        if (!active)
            return null;
        final String tag;
        try {
            getString("git fetch --tags --force");
        } catch (Exception ignored) {
        } finally {
            tag = getString("git describe --tag --always --abbrev=0");
        }
        return tag;
    }

    @SuppressWarnings("Annotator")
    public Properties getConfig() {
        if (!active)
            return new Properties();
        final Properties properties = new Properties();
        final String config = getString("git config -l | cat");
        if (config != null) {
            try {
                properties.load(new StringReader(config.replaceAll("[\\n|\\r|\\s|$|]", "\n")));
            } catch (IOException e) {
                log.error("%s Could not read git config due [%s] %s", unicode(0x1F940), config, e);
            }
        }
        return properties;
    }

    private Collection<ReflogEntry> getRefLog() {
        if (!active)
            return null;
        try {
            return Git.open(workDir).reflog().call();
        } catch (GitAPIException | IOException e) {
            log.error("%s Could not read git directory due %s", unicode(0x1F940), e);
            return null;
        }
    }

    //TODO: test stash create and apply
    //TODO: test last git tag
    //TODO: get origin url
    public String getOriginUrl() {
        return active ? getString("git config --get remote.origin.url") : null;
    }

    public boolean gitHasChanges() {
        try {
            return active && Git.open(workDir).status().call().isClean();
        } catch (GitAPIException | IOException e) {
            log.error("%s Could not read git directory due %s", unicode(0x1F940), e);
            return false;
        }
    }

    public boolean gitStash() {
        if (fake) {
            log.warn("%s Fake stash", unicode(0x26A0));
        }
        try {
            return active && Git.open(workDir).stashCreate().call() != null;
        } catch (GitAPIException | IOException e) {
            log.error("%s Failed to stash %s", unicode(0x1F940), e);
            return false;
        }
//        return fake ? "fake stash" : getTerminal().execute("git stash clear; git stash").consoleInfo().trim();
    }

    public String gitLoadStash() {
        if (fake) {
            log.warn("%s Fake load stash", unicode(0x26A0));
        }

        try {
            return active ? Git.open(workDir).stashApply().call().toObjectId().getName() : null;
        } catch (GitAPIException | IOException e) {
            log.debug("Failed to load stash " + e);
            getTerminal().breakOnError(false).execute("git stash pop");
            return "failed";
        }
    }

    public Optional<String> findOriginalBranchName() {
        if (!active)
            return Optional.empty();
        final Collection<ReflogEntry> refLog = getRefLog().stream().filter(log -> !log.getComment().startsWith("merge")).collect(Collectors.toList());
        for (ReflogEntry reflogEntry : refLog) {
            final Matcher matcher = PATTERN_ORIGINAL_BRANCH_NAME.matcher(reflogEntry.getComment() == null ? "" : reflogEntry.getComment());
            if (matcher.find()) {
                return Optional.ofNullable(matcher.group("branchName"));
            }
        }
        return getBranchName();
    }

    private Optional<String> getBranchName() {
        if (!active)
            return Optional.empty();
        //        return getString("git rev-parse --abbrev-ref HEAD");
        try {
            final ReflogEntry reflogEntry = getRefLog().iterator().next();
            final List<Ref> branches = Git.open(workDir).branchList().call();
            for (Ref branch : branches) {
                if (branch.getObjectId().equals(reflogEntry.getNewId())) {
                    return Optional.ofNullable(branch.getName());
                }
            }
            return Optional.ofNullable(branches.get(0).getName());
        } catch (GitAPIException | IOException e) {
            log.error("%s Could not read git directory due %e", unicode(0x1F940), e);
            return Optional.empty();
        }
    }

    private void logFakeMessage(final Logger log) {
        if (fake) {
            log.warn("%s Faked [%s]", unicode(0x26A0), getClass().getSimpleName());
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

    private boolean hasGit(final Logger log, final File workDir) {
        boolean hasGit;
        try {
            hasGit = Optional.ofNullable(Git.open(workDir).status().call()).isPresent();
        } catch (Exception e) {
            log.info("%s Project is not a git repository [%s]", unicode(0x1F4CD), workDir);
            hasGit = false;
        }
        return hasGit;
    }
}
