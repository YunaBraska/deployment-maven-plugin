package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.execution.MavenSession;

import java.util.Properties;

import static berlin.yuna.mavendeploy.plugin.MojoExecutor.element;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.name;

public class MojoHelper {

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

    public static boolean getOrElse(final String test, final boolean fallback) {
        return !isEmpty(test) ? Boolean.valueOf(test) : fallback;
    }

    public static boolean isEmpty(final String test) {
        return test == null || test.trim().isEmpty();
    }
}
