package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;

public class ReadmeBuilder extends MojoBase {

    public static final String NAME_PLACEHOLDER = "PLACEHOLDER";
    public static final String NAME_VARIABLE = "VARIABLE";
    public static final String NAME_TEXT = "TEXT";
    public static final String NAME_ESCAPE = "ESCAPED";
    public static final String BUILDER_FILE_PATTERN = "(?<name>.*)(?<value>\\.builder\\.)(?<type>\\w*)(?<end>\\#+)";
    public static Pattern BUILDER_VAR_PATTERN = Pattern.compile("\\[(?<type>var)\\s+(?<name>.*)\\]\\:\\s+\\#\\s+\\((?<value>.*)\\)");
    public static Pattern BUILDER_PLACEHOLDER_PATTERN = Pattern.compile("\\!\\{(?<name>.*)\\}");

    public ReadmeBuilder(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        super("berlin.yuna", "readme-buider", "0.0.1", environment, log);
    }

    public static ReadmeBuilder build(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        return new ReadmeBuilder(environment, log);
    }

    public ReadmeBuilder render() throws IOException {
        final String goal = "render";
        logGoal(goal, true);
        final Path start = environment.getMavenProject().getBasedir().toPath();
        Files.walk(start)
                .filter(path -> Files.isRegularFile(path))
                .filter(file -> (file.getFileName().toString() + "#end").split(BUILDER_FILE_PATTERN).length > 1)
                .forEach(this::render);
        logGoal(goal, false);
        return this;
    }

    //TODO: includes
    private void render(final Path builderPath) {
        try {
            log.debug("Rendering [%s]", builderPath);
            List<Content> content = new ArrayList<>();
            content.add(new Content(NAME_TEXT, new String(Files.readAllBytes(builderPath), UTF_8)));
            content = splitContentAt(content, Pattern.compile("\\\\" + BUILDER_VAR_PATTERN), NAME_ESCAPE);
            content = splitContentAt(content, Pattern.compile("\\\\" + BUILDER_PLACEHOLDER_PATTERN), NAME_ESCAPE);
            content = splitContentAt(content, BUILDER_VAR_PATTERN, NAME_VARIABLE);
            content = splitContentAt(content, BUILDER_PLACEHOLDER_PATTERN, NAME_PLACEHOLDER);

            final HashMap<String, String> variables = readVariables(content);
            content = resolvePlaceholders(content, variables);
            content = removeVariablesAndEscapes(content);

            final String result = content.stream().map(c -> c.value).collect(Collectors.joining("")).trim();
            writeFile(builderPath, result, variables.get("target"));
        } catch (IOException e) {
            log.error(e);
        }
    }

    private HashMap<String, String> readVariables(final List<Content> content) {
        final HashMap<String, String> variables = new HashMap<>();
        environment.getMavenSession().getUserProperties().forEach((k, v) -> variables.put(String.valueOf(k), String.valueOf(v)));
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

    private List<Content> removeVariablesAndEscapes(final List<Content> content) {
        final List<Content> result = new ArrayList<>(content);
        content.stream().filter(c -> NAME_VARIABLE.equals(c.key)).forEach(result::remove);
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
        final Path basePath = builderPath.getParent();
        final File outputBase = (optionalPath == null ? basePath.toFile() : new File(environment.getMavenProject().getBasedir(), optionalPath));

        if (!outputBase.exists()) {
            Files.createDirectories(outputBase.toPath());
        }

        final String fileName = (builderPath.getFileName().toString() + "#").replaceAll(BUILDER_FILE_PATTERN, "${name}.${type}");
        final Path outputPath = new File(outputBase, fileName).toPath();
        Files.write(outputPath, content.getBytes());
        log.info("Generated [%s]", outputPath);
    }

    public class Content {

        public String key;
        public String value;

        public Content(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Content content = (Content) o;

            if (key != null ? !key.equals(content.key) : content.key != null) return false;
            return value != null ? value.equals(content.value) : content.value == null;
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
