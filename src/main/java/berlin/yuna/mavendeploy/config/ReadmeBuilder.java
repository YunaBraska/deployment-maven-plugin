package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

public class ReadmeBuilder extends MojoBase {

    private static final String NAME_PLACEHOLDER = "PLACEHOLDER";
    private static final String NAME_VARIABLE = "VARIABLE";
    private static final String NAME_INCLUDE = "INCLUDE";
    private static final String NAME_TEXT = "TEXT";
    private static final String NAME_ESCAPE = "ESCAPED";
    private static final String BUILDER_FILE_PATTERN = "(?<name>.*)(?<value>\\.builder\\.)(?<type>\\w*)(?<end>\\#+)";
    private static final Pattern BUILDER_VAR_PATTERN = Pattern.compile("\\[(?<type>var)\\s+(?<name>.*)\\]\\:\\s+\\#\\s+\\((?<value>.*)\\)");
    private static final Pattern BUILDER_INCLUDE_PATTERN = Pattern.compile("(?<name>.*)\\[(?<type>include)" +
            "\\]\\:\\s+\\#\\s+\\((?<value>.*)\\)");
    private static final Pattern BUILDER_PLACEHOLDER_PATTERN = Pattern.compile("\\!\\{(?<name>.*)\\}");

    public ReadmeBuilder(final PluginSession session) {
        super("berlin.yuna", "readme-buider", "0.0.1", session);
    }

    public static ReadmeBuilder build(final PluginSession session) {
        return new ReadmeBuilder(session);
    }

    public ReadmeBuilder render() throws IOException {
        final String goal = "render";
        logGoal(goal, true);
        final Path start = session.getEnvironment().getMavenProject().getBasedir().toPath();
        Files.walk(start, 99)
                .filter(path -> Files.isRegularFile(path))
                .filter(file -> (file.getFileName().toString() + "#end").split(BUILDER_FILE_PATTERN).length > 1)
                .forEach(this::render);
        logGoal(goal, false);
        return this;
    }

    private void render(final Path builderPath) {
        render(builderPath, true);
    }

    private String render(final Path builderPath, final boolean writeFile) {
        try {
            log.debug("Rendering [%s]", builderPath.getFileName());
            List<Content> content = new ArrayList<>();
            content.add(new Content(NAME_TEXT, Files.readString(builderPath)));
            content = splitContentAt(content, Pattern.compile("\\\\" + BUILDER_VAR_PATTERN), NAME_ESCAPE);
            content = splitContentAt(content, Pattern.compile("\\\\" + BUILDER_PLACEHOLDER_PATTERN), NAME_ESCAPE);
            content = splitContentAt(content, Pattern.compile("\\\\" + BUILDER_INCLUDE_PATTERN), NAME_ESCAPE);
            content = splitContentAt(content, BUILDER_VAR_PATTERN, NAME_VARIABLE);
            content = splitContentAt(content, BUILDER_INCLUDE_PATTERN, NAME_INCLUDE);
            content = splitContentAt(content, BUILDER_PLACEHOLDER_PATTERN, NAME_PLACEHOLDER);

            final HashMap<String, String> variables = readVariables(content);
            content = resolvePlaceholders(content, variables);
            content = resolveIncludes(content, variables, builderPath);
            content = removeVariablesAndEscapes(content);

            final String result = content.stream().map(c -> c.value).collect(Collectors.joining("")).trim();
            if (writeFile) {
                writeFile(builderPath, result, variables.get("target"));
            }
            return result;
        } catch (IOException e) {
            log.error("%s %s", unicode(0x1F940), e);
        }
        return "";
    }

    private HashMap<String, String> readVariables(final List<Content> content) {
        final HashMap<String, String> variables = new HashMap<>();
        session.getEnvironment().getMavenSession().getUserProperties().forEach((k, v) -> variables.put(String.valueOf(k), String.valueOf(v)));
        variables.putAll(readVariables(content, variables));
        return variables;
    }

    private Map<String, String> readVariables(final List<Content> content, final HashMap<String, String> variables) {
        final HashMap<String, String> result = new HashMap<>(variables);
        content.stream().filter(c -> NAME_VARIABLE.equals(c.key)).forEach(c -> {
            final Content variable = readVariable(c, BUILDER_VAR_PATTERN);
            result.put(variable.key, resolvePlaceholders(singletonList(variable), result).get(0).value);
        });
        return result;
    }

    private Content readVariable(final Content content, final Pattern pattern) {
        final Matcher matcher = pattern.matcher(content.value);
        if (matcher.find()) {
            return new Content(matcher.group("name"), matcher.group("value"));
        }
        throw new RuntimeException(format("No variable found in [%s]", content.value));
    }

    private List<Content> resolvePlaceholders(final List<Content> content, final HashMap<String, String> variables) {
        final List<Content> result = new ArrayList<>(content);
        result.stream().filter(o -> !NAME_ESCAPE.equals(o.key)).forEach(placeholder -> {
            for (Map.Entry<String, String> var : variables.entrySet()) {
                placeholder.value = placeholder.value.replace("!{" + var.getKey() + "}", var.getValue());
            }
        });
        return result;
    }

    private List<Content> resolveIncludes(final List<Content> content, final HashMap<String, String> variables, final Path builderPath) {
        final List<Content> result = new ArrayList<>(content);
        result.stream().filter(o -> NAME_INCLUDE.equals(o.key)).forEach(variable -> {
            final Content include = readVariable(variable, BUILDER_INCLUDE_PATTERN);
            final File file = include.value.startsWith("/") ?
                    new File(session.getEnvironment().getMavenProject().getBasedir(), include.value.substring(1)) :
                    new File(builderPath.getParent().toFile(), include.value);
            variable.value = include.key + render(file.toPath(), false);
            variable.key = NAME_TEXT;
        });
        return result;
    }

    private List<Content> removeVariablesAndEscapes(final List<Content> content) {
        final List<Content> result = new ArrayList<>(content);
        content.stream().filter(c -> NAME_VARIABLE.equals(c.key)).forEach(result::remove);
        content.stream().filter(c -> NAME_INCLUDE.equals(c.key)).forEach(result::remove);
        content.stream().filter(c -> NAME_ESCAPE.equals(c.key)).forEach(c -> c.value = c.value.substring(1));
        return result;
    }

    private List<Content> splitContentAt(final List<Content> content, final Pattern pattern, final String name) {
        final List<Content> result = new ArrayList<>();
        for (Content part : content) {
            if (part.key.equals(NAME_TEXT) && !part.value.isEmpty()) {
                final String value = part.value;
                final Matcher matcher = pattern.matcher(value);
                int endMatch = 0;
                while (matcher.find()) {
                    result.add(new Content(part.key, value.substring(endMatch, matcher.start())));
                    endMatch = matcher.end();
                    result.add(new Content(name, value.substring(matcher.start(), endMatch)));
                }
                result.add(new Content(part.key, value.substring(endMatch)));
            } else if (!part.value.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }

    private void writeFile(final Path builderPath, final String content, final String optionalPath) throws IOException {
        final Path outputPath = getOutputPath(builderPath, optionalPath);
        Files.write(outputPath, content.getBytes());
        log.info("%s Generated [%s]", unicode(0x1F4D1), outputPath);
    }

    private Path getOutputPath(final Path builderPath, final String optionalPath) throws IOException {
        final Path basePath = builderPath.getParent();
        final File outputBase = (optionalPath == null ? basePath.toFile() : new File(
                (optionalPath.startsWith("/") ? session.getEnvironment().getMavenProject().getBasedir() : basePath.toFile()),
                optionalPath
        ));

        if (!outputBase.exists()) {
            Files.createDirectories(outputBase.toPath());
        }

        final String fileName = (builderPath.getFileName().toString() + "#").replaceAll(BUILDER_FILE_PATTERN, "${name}.${type}");
        return new File(outputBase, fileName).toPath();
    }

    class Content {

        String key;
        String value;

        Content(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Content content = (Content) o;

            if (!Objects.equals(key, content.key)) return false;
            return Objects.equals(value, content.value);
        }

        @Override
        public int hashCode() {
            int result = key != null ? key.hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return format("[%S] [%s]", key, value);
        }
    }
}
