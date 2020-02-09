package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.goal;

public class Jar extends MojoBase {

    public Jar(final PluginSession session) {
        super("org.apache.maven.plugins", "maven-jar-plugin", "3.2.0", session);
    }

    public static Jar build(final PluginSession session) {
        return new Jar(session);
    }

    public Jar jar() throws MojoExecutionException {
        final String goal = "jar";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("classesDirectory", session.getParamPresent("project.build.outputDirectory").orElse(null)),
                        prop("outputDirectory", session.getParamPresent("project.build.directory").orElse(null)),
                        prop("forceCreation", session.getParamPresent("maven.jar.forceCreation").orElse(null)),
                        prop("skipIfEmpty", session.getParamPresent("boolean").orElse(null)),
                        prop("maven.deploy.skip")
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }
}
