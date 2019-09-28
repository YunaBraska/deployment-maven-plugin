package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.model.Prop;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static berlin.yuna.mavendeploy.plugin.PluginExecutor.configuration;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.element;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.name;
import static berlin.yuna.mavendeploy.util.MojoUtil.isEmpty;
import static berlin.yuna.mavendeploy.util.MojoUtil.isPresent;
import static berlin.yuna.mavendeploy.util.MojoUtil.toSecret;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

public class PluginSession {

    private final PluginExecutor.ExecutionEnvironment environment;
    private final Logger log;

    public PluginSession(final PluginExecutor.ExecutionEnvironment environment, final Logger log) {
        this.environment = environment;
        this.log = log;
    }

    public boolean isTrue(final String... keys) {
        return getBoolean(keys).orElse(false);
    }

    public boolean hasText(final String... keys) {
        return isPresent(getParam(keys).orElse(null));
    }

    public Optional<Boolean> getBoolean(final String... keys) {
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
        for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
            if (entry.getKey() instanceof String && matchKey(key, (String) entry.getKey())) {
                return Optional.of(String.valueOf(entry.getValue()));
            }
        }
        return Optional.empty();
    }

    public void setParameter(final String key, final String value, final boolean... when) {
        for (boolean trigger : when) {
            if (trigger) {
                if (value == null && getParam(key).isPresent()) {
                    //[value == null && key is present]
                    log.debug("%s Config removed key [%s]", unicode(0x26A0), key);
                    getProject().getProperties().remove(key);
                    getMavenSession().getSystemProperties().remove(key);
                    getMavenSession().getUserProperties().remove(key);
                } else if (value != null && !value.equals(getString(key).orElse(null))) {
                    //[value != null && hasChanged]
                    log.info("%s Config added key [%s] value [%s]", unicode(0x271A), key, toSecret(key, value));
                    getMavenSession().getUserProperties().setProperty(key, value);
                }
                break;
            }
        }
    }

    public void setNewParam(final String key, final String value) {
        requireNonNull(key, "setNewParam key is null");
        final String cmdValue = getProperties().getProperty(key);
        if (isEmpty(cmdValue)) {
            setParameter(key, value, true);
        }
    }

    public Xpp3Dom prepareXpp3Dom(final Prop... prop) {
        final PluginExecutor.Element[] elements = stream(prop)
                .map(this::prepareElement)
                .filter(element -> !isEmpty(element.toDom().getValue()) || element.toDom().getChildCount() > 0)
                .toArray(PluginExecutor.Element[]::new);
        return configuration(elements);
    }

    private PluginExecutor.Element prepareElement(final Prop prop) {
        final List<PluginExecutor.Element> childElements = new ArrayList<>();
        for (Prop childProp : prop.childProps) {
            childElements.add(prepareElement(childProp));
        }

        final String key = prop.key;
        final String value = prop.value;
        final boolean overwrite = isPresent(value) && value.startsWith("!");
        final String resultValue = overwrite ? value.substring(1) : getProperties().getProperty(key, value);
        if (childElements.isEmpty()) {
            log.debug("Config property [%s] + [%s] = [%s]",
                    key,
                    toSecret(key, value),
                    toSecret(key, resultValue));
            return element(name(key), resultValue);
        }
        return element(name(key), childElements.toArray(new PluginExecutor.Element[0]));
    }

    public PluginExecutor.ExecutionEnvironment getEnvironment() {
        return environment;
    }

    public MavenProject getProject() {
        return environment.getMavenProject();
    }

    public Logger getLog() {
        return log;
    }

    public MavenSession getMavenSession() {
        return environment.getMavenSession();
    }

    public Properties getProperties() {
        final Properties properties = new Properties();
        properties.putAll(getProject().getProperties());
        properties.putAll(getMavenSession().getSystemProperties());
        properties.putAll(getMavenSession().getUserProperties());
        return properties;
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
        return removeSeparator(key1).equals(removeSeparator(key2));
    }

    private String removeSeparator(final String propertyKey) {
        final String result = propertyKey.startsWith("env.") ? propertyKey.substring("env.".length()) : propertyKey;
        return result.replace(".", "_").replace("-", "_").toLowerCase().trim();
    }
}
