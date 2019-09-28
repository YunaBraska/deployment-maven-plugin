package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.goal;

//TODO: import mojo-executor without any dependencies...
public class Clean extends MojoBase {

    public Clean(final PluginSession session) {
        super("org.apache.maven.plugins", "maven-clean-plugin", "3.1.0", session);
    }

    public static Clean build(final PluginSession session) {
        return new Clean(session);
    }

    public Clean clean() throws MojoExecutionException {
        final String goal = "clean";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("failOnError"),
                        prop("followSymLinks"),
                        prop("excludeDefaultDirectories")
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }
}
