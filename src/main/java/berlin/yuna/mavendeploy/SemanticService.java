package berlin.yuna.mavendeploy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SemanticService {

    private final String[] SEMANTIC_FORMAT;

    SemanticService(final String semanticFormat) {
        SEMANTIC_FORMAT = semanticFormat.split("::");
    }

    String getNextSemanticVersion(final String versionOrg, final String branchName) {
        final String separator = getSemanticSeparator(versionOrg);
        final StringBuilder nextVersion = new StringBuilder();
        for (String digit : prepareNextSemanticVersion(versionOrg, branchName)) {
            nextVersion.append(digit).append(separator);
        }
        return nextVersion.delete((nextVersion.length() - 1), nextVersion.length()).toString();
    }

    private String getSemanticSeparator(final String versionOrg) {
        final Matcher matcher = Pattern.compile(SEMANTIC_FORMAT[0]).matcher(versionOrg);
        return matcher.find() ? matcher.group(0) : ".";
    }

    private String[] prepareNextSemanticVersion(final String versionOrg, final String branchName) {
        final int position = getSemanticPosition(branchName);
        final String[] version = versionOrg.split(SEMANTIC_FORMAT[0]);
        version[position] = ((Integer) (Integer.valueOf(version[position]) + 1)).toString();
        for (int i = (position + 1); i < version.length; i++) {
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
