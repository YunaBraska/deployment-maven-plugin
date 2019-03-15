package berlin.yuna.mavendeploy.logic;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemanticService {

    private final File WORK_DIR;
    private final String[] SEMANTIC_FORMAT;
    private static final Pattern PATTERN_ORIGINAL_BRANCH_NAME = Pattern.compile(
            "(?<prefix>.*refs\\/.*?\\/)(?<branchName>.*?)(?<suffix>@.*?)");

    public SemanticService(final String semanticFormat, final File workDir) {
        WORK_DIR = workDir;
        SEMANTIC_FORMAT = semanticFormat.split("::");
    }

    public String getNextSemanticVersion(final String currentVersion, final String fallback) {
        final String branchName = findOriginalBranchName();
        final int semanticPosition = getSemanticPosition(branchName);
        if (branchName != null && !branchName.trim().isEmpty() && semanticPosition != -1) {
            return getNextSemanticVersion(currentVersion, semanticPosition);
        }
        return fallback;
    }

    String getNextSemanticVersion(final String versionOrg, final int semanticPosition) {
        final String separator = getSemanticSeparator(versionOrg);
        final StringBuilder nextVersion = new StringBuilder();
        for (String digit : prepareNextSemanticVersion(versionOrg, semanticPosition)) {
            nextVersion.append(digit).append(separator);
        }
        return nextVersion.delete((nextVersion.length() - 1), nextVersion.length()).toString();
    }

    public String findOriginalBranchName() {
        final String refLog = new GitService(WORK_DIR).getLastRefLog();
        final Matcher matcher = PATTERN_ORIGINAL_BRANCH_NAME.matcher(refLog);
        if (matcher.find()) {
            return matcher.group("branchName");
        }
        return null;
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

    public int getSemanticPosition(final String branchName) {
        for (int i = 1; i < SEMANTIC_FORMAT.length; i++) {
            if (Pattern.compile(SEMANTIC_FORMAT[i]).matcher(branchName).find()) {
                return i - 1;
            }
        }
        return -1;
    }
}
