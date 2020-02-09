package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.function.Predicate;

import static berlin.yuna.mavendeploy.model.Parameter.BASE_DIR;
import static berlin.yuna.mavendeploy.model.Parameter.TARGET;
import static berlin.yuna.mavendeploy.plugin.PluginSession.addSecret;
import static berlin.yuna.mavendeploy.plugin.PluginSession.hideSecrets;
import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
import static berlin.yuna.mavendeploy.util.MojoUtil.isEmpty;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.stream;

public class PropertyWriter extends MojoBase {

    final String[] EXCLUDES = {"java.command", "args"};

    public PropertyWriter(final PluginSession session) {
        super("berlin.yuna", "property-writer", "0.0.1", session);
    }

    public static PropertyWriter build(final PluginSession session) {
        return new PropertyWriter(session);
    }

    public PropertyWriter write() {
        final String goal = "write";
        logGoal(goal, true);
        writeToFile();
        logGoal(goal, false);
        return this;
    }

    private void writeToFile() {
        final File output = getOutputFile();
        try {
            session.getProperties().forEach((key, value) -> addSecret(String.valueOf(key), String.valueOf(value)));
            final StringBuilder stringBuilder = new StringBuilder();
            session.getProperties().entrySet().stream().filter(excludeProps()).map(this::entryToString).sorted().forEach(r -> stringBuilder.append(r).append(lineSeparator()));
            log.info("%s Writing properties to file [%s]", unicode(0x1F4D1), output.getAbsolutePath());
            if (!output.getParentFile().exists()) {
                Files.createDirectories(output.getParentFile().toPath());
            }
            Files.write(output.toPath(), hideSecrets(stringBuilder.toString()).getBytes());
            if (output.exists()) {
                log.info("%s Properties [file://%s]", unicode(0x1F516), output.toURI().getRawPath());
            }
        } catch (Exception e) {
            log.error("Could not write properties to file [%s] %s[%s]", output, lineSeparator(), e);
        }
    }

    private Predicate<Map.Entry<Object, Object>> excludeProps() {
        return e -> stream(EXCLUDES).noneMatch(k -> String.valueOf(e.getKey()).toLowerCase().contains(k.toLowerCase()));
    }

    protected String entryToString(final Map.Entry<Object, Object> entry) {
        return (entry.getKey() + " = " + (isEmpty(String.valueOf(entry.getValue())) ? "" : String.valueOf(entry.getValue()))
        ).replace("\r", " ").replace("\n", " ").replace("\t", " ");
    }

    private File getOutputFile() {
        return session.getBoolean("properties.print").orElse(false) ?
                new File(session.getParamFallback(BASE_DIR.key(), ""), TARGET.maven() + File.separator + "all.properties") :
                new File(session.getParamFallback("properties.print", ""));
    }
}
