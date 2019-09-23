package berlin.yuna.mavendeploy.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

public class MojoUtil {

    public static boolean isPresent(final String test) {
        return !isEmpty(test);
    }

    public static boolean isEmpty(final String test) {
        return test == null || test.trim().isEmpty();
    }

    private static String nullToEmpty(final String test) {
        return isEmpty(test) ? "" : test;
    }

    public static String toSecret(final String value) {
        return toSecret("pass", value);
    }

    public static String toSecret(final String key, final String value) {
        final boolean isSecret
                = !isEmpty(key)
                && !isEmpty(value)
                && (key.toLowerCase().contains("pass") || key.toLowerCase().contains("secret"));
        return isSecret ? String.join("", Collections.nCopies(value.length(), "*")) : value;
    }

    //TODO: move to CLU
    public static List<HashMap<String, String>> regex(final Pattern pattern, final CharSequence input, final String... groups) {
        return regex(pattern.matcher(input), groups);
    }

    public static HashMap<String, String> regexFirst(final Matcher matcher, final String... groupNames) {
        final List<HashMap<String, String>> result = regex(matcher, groupNames);
        return result.isEmpty() ? new HashMap<>() : result.get(0);
    }

    public static Optional<String> regexSimple(final Matcher matcher, final String groupName) {
        return regexSimpleList(matcher, groupName).stream().findFirst();
    }

    private static List<String> regexSimpleList(final Matcher matcher, final String groupName) {
        return regex(matcher, groupName).stream().filter(map -> !map.isEmpty())
                .map(map -> map.values().iterator().next()).filter(MojoUtil::isPresent).collect(toList());
    }

    private static List<HashMap<String, String>> regex(final Matcher matcher, final String... groupNames) {
        final List<HashMap<String, String>> result = new ArrayList<>();
        while (matcher.find()) {
            final HashMap<String, String> groups = new HashMap<>();
            for (String groupName : groupNames) {
                groups.put(groupName, matcher.group(groupName));
            }
            result.add(groups);
        }
        return result;
    }
}
