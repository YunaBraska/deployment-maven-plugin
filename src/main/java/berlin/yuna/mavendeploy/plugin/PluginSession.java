package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.model.Prop;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static berlin.yuna.mavendeploy.plugin.MojoExecutor.configuration;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.element;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.name;
import static berlin.yuna.mavendeploy.util.MojoUtil.isEmpty;
import static berlin.yuna.mavendeploy.util.MojoUtil.isPresent;
import static berlin.yuna.mavendeploy.util.MojoUtil.toSecret;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.util.Arrays.stream;

public class PluginSession {

    private final MojoExecutor.ExecutionEnvironment environment;
    private final Logger log;

    public PluginSession(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        this.environment = environment;
        this.log = log;
    }

    public boolean isTrue(final String... keys) {
        return getBoolean(keys).orElse(false);
    }

    public boolean hasText(final String... keys) {
        return isPresent(getParam(keys).orElse(null));
    }

    private Optional<Boolean> getBoolean(final String... keys) {
        return stream(keys).map(this::getBoolean).filter(Optional::isPresent).findFirst().orElseGet(Optional::empty);
    }

    public Optional<Boolean> getBoolean(final String key) {
        final Optional<String> value = getParam(key);
        if (value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(parseBoolean(value.get()));
    }

    public String getParamFallback(final String key, final String fallback) {
        return getParamPresent(key).orElse(fallback);
    }

    private Optional<String> getParam(final String... keys) {
        return stream(keys).map(this::getString).filter(Optional::isPresent).findFirst().orElseGet(Optional::empty);
    }

    public Optional<String> getParamPresent(final String... keys) {
        return stream(keys).map(this::getString).filter(Optional::isPresent)
                .filter(s -> isPresent(s.get())).findFirst().orElseGet(Optional::empty);
    }

    private Optional<String> getString(final String key) {
        final Properties props = new Properties();
        props.putAll(environment.getMavenSession().getSystemProperties());
        props.putAll(environment.getMavenSession().getUserProperties());
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            if (entry.getKey() instanceof String && matchKey(key, (String) entry.getKey())) {
                return Optional.of(String.valueOf(entry.getValue()));
            }
        }
        return Optional.empty();
    }

    public Xpp3Dom prepareXpp3Dom(final Prop... prop) {
        final MojoExecutor.Element[] elements = stream(prop)
                .map(this::prepareElement)
                .filter(element -> !isEmpty(element.toDom().getValue()) || element.toDom().getChildCount() > 0)
                .toArray(MojoExecutor.Element[]::new);
        return configuration(elements);
    }

    private MojoExecutor.Element prepareElement(final Prop prop) {
        final Properties properties = environment.getMavenSession().getUserProperties();

        final List<MojoExecutor.Element> childElements = new ArrayList<>();
        for (Prop childProp : prop.childProps) {
            childElements.add(prepareElement(childProp));
        }

        final String key = prop.key;
        final String value = prop.value;
        final boolean overwrite = isPresent(value) && value.startsWith("!");
        final String resultValue = overwrite ? value.substring(1) : properties.getProperty(key, value);
        if (childElements.isEmpty()) {
            log.debug(format("Config property [%s] + [%s] = [%s]",
                    key,
                    toSecret(key, value),
                    toSecret(key, resultValue)));
            return element(name(key), resultValue);
        }
        return element(name(key), childElements.toArray(new MojoExecutor.Element[0]));
    }

    public MojoExecutor.ExecutionEnvironment getEnvironment() {
        return environment;
    }

    public Logger getLog() {
        return log;
    }

    public MavenSession getMavenSession() {
        return environment.getMavenSession();
    }

    public String toString(final Server server) {
        return format("[%s] id [%s] user [%s] pass [%s]",
                Server.class.getSimpleName(),
                server.getId(),
                server.getUsername(),
                toSecret(server.getPassword()));
    }

    public static String unicode(final int unicode) {
        return String.valueOf(Character.toChars(unicode));
    }

    private boolean matchKey(final String key1, final String key2) {
        return key1.replace(".", "_").replace("-", "_").equalsIgnoreCase(
                key2.replace(".", "_").replace("-", "_")
        );

    }
}
