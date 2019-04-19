package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.mavendeploy.model.Prop;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Arrays;
import java.util.Properties;

import static berlin.yuna.mavendeploy.plugin.MojoExecutor.configuration;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.element;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.name;

public class MojoHelper {

    public static Xpp3Dom prepareXpp3Dom(final MojoExecutor.ExecutionEnvironment environment, final Prop... prop) {
        final MojoExecutor.Element[] elements = Arrays.stream(prop)
                .map(prop1 -> prepareElement(environment, prop1.key, prop1.value))
                .filter(element -> !isEmpty(element.toDom().getValue())).toArray(MojoExecutor.Element[]::new);
        return configuration(elements);
    }

    public static MojoExecutor.Element prepareElement(final MojoExecutor.ExecutionEnvironment environment, final String key, final String fallBack) {
        final Properties properties = environment.getMavenSession().getUserProperties();
        return element(name(key), properties.getProperty(key, fallBack));
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
}
