package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReadmeBuilder extends MojoBase {

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

    private void render(final Path builderPath) {
        try {
            String content = escapeVariables(new String(Files.readAllBytes(builderPath)), false);
            final HashMap<String, String> variables = new HashMap<>();
            environment.getMavenSession().getUserProperties().forEach((k, v) -> variables.put(String.valueOf(k), String.valueOf(v)));
            variables.putAll(readVariables(content));

            content = resolveContent(content, compileVariables(variables));
            content = content.replaceAll(BUILDER_VAR_PATTERN.pattern(), "");
            content = escapeVariables(escapePlaceholder(content, true), true);
            writeFile(builderPath, content.trim(), variables.get("target"));
        } catch (IOException e) {
            log.error(e);
        }
    }

    private String resolveContent(final String content, final HashMap<String, String> variables) {
        String result = escapePlaceholder(content, false);
        for (Map.Entry<String, String> var : variables.entrySet()) {
            result = result.replace("\\!{" + var.getKey() + "}", "\\!_{" + var.getKey() + "}");
            result = result.replace("!{" + var.getKey() + "}", var.getValue());
        }
        return result;
    }

    private HashMap<String, String> readVariables(final String file) {
        final HashMap<String, String> variables = new HashMap<>();
        final Matcher matcher = BUILDER_VAR_PATTERN.matcher(file);
        while (matcher.find()) {
//            final String type = matcher.group("type");
            final String name = matcher.group("name");
            final String value = matcher.group("value");
            variables.put(name, value);
        }
        return variables;
    }

    private HashMap<String, String> compileVariables(final HashMap<String, String> variables) {
        final HashMap<String, String> result = new HashMap<>(variables);
        final List<Map.Entry<String, String>> vars = variables
                .entrySet().stream()
                .filter(var -> ("#" + var.getValue() + "#").split(BUILDER_PLACEHOLDER_PATTERN.pattern()).length > 1)
                .collect(Collectors.toList());

        for (Map.Entry<String, String> var : vars) {
            result.put(var.getKey(), resolveContent(var.getValue(), variables));
        }
        return result;
    }

    private String escapePlaceholder(final String content, final boolean liftEscape) {
        if (liftEscape) {
            return content.replaceAll("\\\\!_" + BUILDER_PLACEHOLDER_PATTERN.pattern().substring(2), "!{${name}}");
        }
        return content.replaceAll("\\\\" + BUILDER_PLACEHOLDER_PATTERN.pattern(), "\\\\!_{${name}}");
    }

    private String escapeVariables(final String content, final boolean liftEscape) {
        if (liftEscape) {
            return content.replaceAll("\\\\!]" + BUILDER_VAR_PATTERN.pattern().substring(2), "[${type} ${name}]: # \\(${value}\\)");
        }
        return content.replaceAll("\\\\" + BUILDER_VAR_PATTERN.pattern(), "\\\\!]${type} ${name}]: # \\(${value}\\)");
    }

    private void writeFile(final Path builderPath, final String content, final String optionalPath) throws IOException {
        final Path basePath = builderPath.getParent();
        final File outputBase = (optionalPath == null ? basePath.toFile() : new File(environment.getMavenProject().getBasedir(), optionalPath));

        if (!outputBase.exists()) {
            Files.createDirectories(outputBase.toPath());
        }

        final String fileName = (builderPath.getFileName().toString() + "#").replaceAll(BUILDER_FILE_PATTERN, "${name}.${type}");
        Files.write(new File(outputBase, fileName).toPath(), content.getBytes());
    }
}
