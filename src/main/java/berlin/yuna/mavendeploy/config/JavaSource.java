package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;

public class JavaSource extends MojoBase {

    public JavaSource(final MojoExecutor.ExecutionEnvironment environment, final Log log) {
        super("org.apache.maven.plugins", "maven-source-plugin", environment, log);
        version = "3.0.1";
    }

    public static JavaSource build(final MojoExecutor.ExecutionEnvironment environment, final Log log) {
        return new JavaSource(environment, log);
    }

    public JavaSource jarNoFork() throws MojoExecutionException {
        final String goal = "jar-no-fork";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(environment,
                        prop("maven.source.attach"),
                        prop("maven.source.classifier"),
                        prop("maven.source.excludeResources"),
                        prop("maven.source.forceCreation"),
                        prop("maven.source.includePom"),
                        prop("maven.source.skip"),
                        prop("maven.source.useDefaultExcludes"),
                        prop("maven.source.useDefaultManifestFile")
                ), environment
        );
        logGoal(goal, false);
        return this;
    }
}
