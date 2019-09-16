package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.model.Prop;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static berlin.yuna.mavendeploy.plugin.MojoExecutor.configuration;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.element;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.name;
import static java.lang.String.format;

public class MojoHelper {

    public static Xpp3Dom prepareXpp3Dom(final Logger log, final MojoExecutor.ExecutionEnvironment environment, final Prop... prop) {
        final MojoExecutor.Element[] elements = Arrays.stream(prop)
                .map(prop1 -> prepareElement(log, environment, prop1))
                .filter(element -> !isEmpty(element.toDom().getValue()) || element.toDom().getChildCount() > 0)
                .toArray(MojoExecutor.Element[]::new);
        return configuration(elements);
    }

    public static MojoExecutor.Element prepareElement(final Logger log, final MojoExecutor.ExecutionEnvironment environment, final Prop prop) {
        final Properties properties = environment.getMavenSession().getUserProperties();

        final List<MojoExecutor.Element> childElements = new ArrayList<>();
        for (Prop childProp : prop.childProps) {
            childElements.add(prepareElement(log, environment, childProp));
        }

        if (childElements.isEmpty()) {
            log.debug(format("Config property [%s] + [%s] = [%s]",
                    prop.key,
                    getSecretValue(prop.key, prop.value),
                    getSecretValue(prop.key, properties.getProperty(prop.key, prop.value))));
            return element(name(prop.key), properties.getProperty(prop.key, prop.value));
        }
        return element(name(prop.key), childElements.toArray(new MojoExecutor.Element[0]));
    }

    public static Boolean getBoolean(final MavenSession session, final String key, final boolean fallback) {
        final boolean present = session.getUserProperties().containsKey(key);
        final String value = session.getUserProperties().getProperty(key);
        if (present && isEmpty(value)) {
            return true;
        }
        return getOrElse(value, fallback);
    }

    public static String getString(final MavenSession session, final String key, final String fallback) {
        return session.getUserProperties().getProperty(key, fallback);
    }

    public static boolean getOrElse(final String test, final boolean fallback) {
        return !isEmpty(test) ? Boolean.valueOf(test) : fallback;
    }

    public static boolean isEmpty(final String test) {
        return test == null || test.trim().isEmpty();
    }

    //TODO: move to utils
    public static String getSecretValue(final String key, final String value) {
        final boolean isSecret
                = !isEmpty(key)
                && !isEmpty(value)
                && (key.toLowerCase().contains("pass") || key.toLowerCase().contains("secret"));
        ;
        return isSecret ? String.join("", Collections.nCopies(value.length(), "*")) : value;
    }
}
