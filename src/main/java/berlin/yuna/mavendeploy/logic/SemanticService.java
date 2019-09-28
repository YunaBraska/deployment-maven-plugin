package berlin.yuna.mavendeploy.logic;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.PluginSession;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static berlin.yuna.mavendeploy.util.MojoUtil.isEmpty;
import static berlin.yuna.mavendeploy.util.MojoUtil.isNumeric;
import static berlin.yuna.mavendeploy.util.MojoUtil.splitAtDigits;

public class SemanticService {

    private final String[] semanticFormat;
    private final GitService gitService;
    private String branchNameRefLog;
    private final Logger log;

    public Optional<String> getBranchNameRefLog() {
        if (isEmpty(branchNameRefLog) && gitService != null) {
            branchNameRefLog = gitService.getBranchNameRefLog().orElse(null);
            log.debug("Original branch name [%s]", branchNameRefLog);
        }
        return Optional.ofNullable(branchNameRefLog);
    }

    //TODO: session for debug logging
    public SemanticService(final PluginSession session, final GitService gitService, final String semanticFormat) {
        this.semanticFormat = (isEmpty(semanticFormat) ? "\\.:none" : semanticFormat).split("::");
        this.gitService = gitService;
        this.log = session.getLog();
    }

    public String getNextSemanticVersion(final String currentVersion, final String fallback) {
        final Optional<String> branchName = getBranchNameRefLog();
        final int semanticPosition = getSemanticPosition(branchName.orElse(null));
        if (branchName.isPresent() && semanticPosition != -1) {
            return getNextSemanticVersion(currentVersion, semanticPosition);
        }
        return fallback;
    }

    //FIXME: separator bug, multiple separators will be replaced by the first one (e.g. 1.2-3 will be replaced by 1.2.3)
    private String getNextSemanticVersion(final String versionOrg, final int semanticPosition) {
        final String separator = getSemanticSeparator(versionOrg);
        final StringBuilder nextVersion = new StringBuilder();
        for (String digit : prepareNextSemanticVersion(versionOrg, semanticPosition)) {
            nextVersion.append(digit).append(separator);
        }
        return nextVersion.delete((nextVersion.length() - 1), nextVersion.length()).toString();
    }

    private String getSemanticSeparator(final String versionOrg) {
        final Matcher matcher = Pattern.compile(semanticFormat[0]).matcher(versionOrg);
        return matcher.find() ? matcher.group(0) : ".";
    }

    private String[] prepareNextSemanticVersion(final String versionOrg, final int semanticPos) {
        final String[] version = versionOrg.split(semanticFormat[0]);
        version[semanticPos] = increaseNumbersInString(version[semanticPos], true);
        for (int i = (semanticPos + 1); i < version.length; i++) {
            version[i] = increaseNumbersInString(version[i], false);
        }
        return version;
    }

    String increaseNumbersInString(final String versionPart, final boolean increase) {
        final List<String> parts = splitAtDigits(versionPart);
        for (int i = 0; i < parts.size(); i++) {
            if (isNumeric(parts.get(i))) {
                parts.set(i, (increase ? String.valueOf((Integer.valueOf(parts.get(i)) + 1)) : "0"));
            }
        }
        return String.join("", parts);
    }

    private int getSemanticPosition(final String branchName) {
        for (int i = 1; i < semanticFormat.length; i++) {
            if (Pattern.compile(semanticFormat[i]).matcher(branchName).find()) {
                log.debug("Its a match! branch name [%s] semantic format [%s]", branchName, semanticFormat[i]);
                return i - 1;
            }
            log.debug("branch name [%s] did not match semantic format [%s]", branchName, semanticFormat[i]);
        }
        return -1;
    }
}
