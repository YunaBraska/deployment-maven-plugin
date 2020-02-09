package berlin.yuna.mavendeploy.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

public class MojoUtil {

    public static String SECRET_URL_PATTERN = "(?<prefix>.*\\/\\/)(?<credentials>.*@)(?<suffix>.*)";

    public static boolean isPresent(final String test) {
        return !isEmpty(test);
    }

    public static boolean isEmpty(final String test) {
        return test == null || test.trim().isEmpty();
    }

    private static String nullToEmpty(final String test) {
        return isEmpty(test) ? "" : test;
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

    //big method cause i was not able to split on digit groups and keep the groups/delimiters
    public static List<String> splitAtDigits(final String versionPart) {
        final List<String> parts = new ArrayList<>();
        final char[] chars = versionPart.toCharArray();
        final StringBuilder digit = new StringBuilder();
        final StringBuilder text = new StringBuilder();
        for (char aChar : chars) {
            if (Character.isDigit(aChar)) {
                if (isPresent(text.toString())) {
                    parts.add(text.toString());
                    text.delete(0, text.length());
                }
                digit.append(aChar);
            } else {
                if (isPresent(digit.toString())) {
                    parts.add(digit.toString());
                    digit.delete(0, digit.length());
                }
                text.append(aChar);
            }
        }
        parts.add(digit.toString() + text.toString());
        return parts;
    }

    public static boolean isNumeric(final String number) {
        return isPresent(number) && number.chars().mapToObj(i -> (char) i).allMatch(Character::isDigit);
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

    public static boolean deletePath(final Path dir) {
        if (Files.isRegularFile(dir)) {
            deleteFile(dir);
            return true;
        }
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(MojoUtil::deleteFile);
        } catch (IOException ignored) {
            return false;
        }
        return true;
    }

    private static void deleteFile(final Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Can't remove file [" + path + "] " + e);
        }
    }
}
