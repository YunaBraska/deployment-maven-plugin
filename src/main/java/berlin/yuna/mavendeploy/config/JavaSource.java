package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;

public class JavaSource extends MojoBase {

    public JavaSource(final PluginSession session) {
        super("org.apache.maven.plugins", "maven-source-plugin", "3.1.0", session);
    }

    public static JavaSource build(final PluginSession session) {
        return new JavaSource(session);
    }

    public JavaSource jarNoFork() throws MojoExecutionException {
        final String goal = "jar-no-fork";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("finalName"),
                        prop("maven.source.attach"),
                        prop("maven.source.classifier"),
                        prop("maven.source.excludeResources"),
                        prop("maven.source.forceCreation"),
                        prop("maven.source.includePom"),
                        prop("maven.source.skip"),
                        prop("maven.source.useDefaultExcludes"),
                        prop("maven.source.useDefaultManifestFile")
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }
}
