package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;

public class Resources extends MojoBase {

    public Resources(final PluginSession session) {
        super("org.apache.maven.plugins", "maven-resources-plugin", "3.1.0", session);
    }

    public static Resources build(final PluginSession session) {
        return new Resources(session);
    }

    public Resources resource() throws MojoExecutionException {
        final String goal = "resources";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("encoding")
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }

    public Resources testResource() throws MojoExecutionException {
        final String goal = "testResources";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("encoding")
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }
}
