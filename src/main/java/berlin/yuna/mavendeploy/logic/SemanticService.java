package berlin.yuna.mavendeploy.logic;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static berlin.yuna.mavendeploy.util.MojoUtil.isEmpty;

public class SemanticService {

    private final String[] SEMANTIC_FORMAT;
    private final GitService gitService;
    private String branchName;

    public Optional<String> getBranchName() {
        if (isEmpty(branchName) && gitService != null) {
            branchName = gitService.findOriginalBranchName().orElse(null);
        }
        return Optional.ofNullable(branchName);
    }

    public SemanticService(final GitService gitService, final String semanticFormat) {
        SEMANTIC_FORMAT = (isEmpty(semanticFormat) ? "\\.:none" : semanticFormat).split("::");
        this.gitService = gitService;
    }

    public String getNextSemanticVersion(final String currentVersion, final String fallback) {
        final Optional<String> branchName = getBranchName();
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
        final Matcher matcher = Pattern.compile(SEMANTIC_FORMAT[0]).matcher(versionOrg);
        return matcher.find() ? matcher.group(0) : ".";
    }

    private String[] prepareNextSemanticVersion(final String versionOrg, final int semanticPosition) {
        final String[] version = versionOrg.split(SEMANTIC_FORMAT[0]);
        version[semanticPosition] = ((Integer) (Integer.valueOf(version[semanticPosition]) + 1)).toString();
        for (int i = (semanticPosition + 1); i < version.length; i++) {
            version[i] = "0";
        }
        return version;
    }

    private int getSemanticPosition(final String branchName) {
        for (int i = 1; i < SEMANTIC_FORMAT.length; i++) {
            if (Pattern.compile(SEMANTIC_FORMAT[i]).matcher(branchName).find()) {
                return i - 1;
            }
        }
        return -1;
    }
}
