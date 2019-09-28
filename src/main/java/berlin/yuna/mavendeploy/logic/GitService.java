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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
import static berlin.yuna.mavendeploy.util.MojoUtil.isEmpty;
import static java.util.stream.Collectors.toMap;

//TODO can be replaced by SCM Plugin?
public class GitService {

    private final Logger log;
    private final boolean fake;
    private final File workDir;
    private final boolean active;
    private static final Pattern PATTERN_BRANCH_NAME_REF = Pattern.compile("(.*refs\\/(?<prefix>\\w*\\/)(origin\\/)*)(?<branchName>.*)(?<suffix>@.*)");
    private static final String SHA_REF_PATTERN = "([0-9a-f]{5,40}\\s*refs\\/heads\\/)";

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
//            getString("git tag -l | xargs git tag -d && git fetch --tags --force");
            getString("git fetch --tags --force");
        } catch (Exception ignored) {
        } finally {
            tag = getString("git describe --tag --always --abbrev=0").orElse(null);
        }
        return tag;
    }

    @SuppressWarnings("Annotator")
    public Map<String, String> getConfig() {
        if (!active)
            return new HashMap<>();
        final Properties properties = new Properties();
        final Optional<String> config = getString("git config -l | cat");
        if (config.isPresent()) {
            try {
                properties.load(new StringReader(config.get().replaceAll("[\\n|\\r|\\s|$|]", "\n")));
            } catch (IOException e) {
                log.error("Could not read git config due [%s] %s", config, e);
            }
        }
        return properties.entrySet().stream()
                .collect(toMap(entry -> String.valueOf(entry.getKey()), entry -> String.valueOf(entry.getValue())));
    }

    private Collection<ReflogEntry> getRefLog() {
        if (!active)
            return null;
        try {
            return Git.open(workDir).reflog().call();
        } catch (GitAPIException | IOException e) {
            log.error("Could not read git directory due %s", e);
            return null;
        }
    }

    //TODO: test stash create and apply
    //TODO: test last git tag
    //TODO: get origin url
    public Optional<String> getOriginUrl() {
        return active ? getString("git config --get remote.origin.url") : Optional.empty();
    }

    public boolean gitHasChanges() {
        try {
            return active && Git.open(workDir).status().call().isClean();
        } catch (GitAPIException | IOException e) {
            log.error("Could not read git directory due %s", e);
            return false;
        }
    }

    public boolean gitStash() {
        if (fake) {
            log.warn("Fake stash");
        }
        try {
            return active && Git.open(workDir).stashCreate().call() != null;
        } catch (GitAPIException | IOException e) {
            log.error("Failed to stash %s", e);
            return false;
        }
//        return fake ? "fake stash" : getTerminal().execute("git stash clear; git stash").consoleInfo().trim();
    }

    public String gitLoadStash() {
        if (fake) {
            log.warn("Fake load stash");
        }

        try {
            return active ? Git.open(workDir).stashApply().call().toObjectId().getName() : null;
        } catch (GitAPIException | IOException e) {
            log.debug("Failed to load stash " + e);
            getTerminal().breakOnError(false).execute("git stash pop");
            return "failed";
        }
    }

    public Optional<String> getBranchNameRefLog() {
        if (!active)
            return Optional.empty();
        final String[] refLog = getString("git reflog show --all | grep \"refs/heads/\" | head -n90").orElse("").split(SHA_REF_PATTERN);
        for (String refLogEntry : refLog) {
            final Matcher matcher = PATTERN_BRANCH_NAME_REF.matcher(refLogEntry);
            if (matcher.find()) {
                return Optional.ofNullable(matcher.group("branchName").trim().split("\\s")[0]);
            }
        }
        return getBranchName();
    }

    public Optional<String> getBranchName() {
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
            log.error("Could not read git directory due %e", e);
            return Optional.empty();
        }
    }

    private void logFakeMessage(final Logger log) {
        if (fake) {
            log.warn("Faked [%s]", getClass().getSimpleName());
        }
    }

    private Terminal getTerminal() {
        return new Terminal().timeoutMs(30000).breakOnError(true).dir(workDir).consumerError(log::error);
    }

    private Optional<String> getString(final String command) {
//        workaround as terminal cannot handle empty strings yet
        final String result = getTerminal().execute("tmp=$(" + command + "); if [ -z \"${tmp}\" ]; then echo null; else echo ${tmp}; fi").consoleInfo();
        return Optional.ofNullable(result.equalsIgnoreCase("null") || isEmpty(result) ? null : result.trim());
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
