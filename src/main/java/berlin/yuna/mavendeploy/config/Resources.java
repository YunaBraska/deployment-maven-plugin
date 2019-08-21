package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;

public class Resources extends MojoBase {

    public Resources(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        super("org.apache.maven.plugins", "maven-resources-plugin", "3.1.0", environment, log);
    }

    public static Resources build(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        return new Resources(environment, log);
    }

    public Resources resource() throws MojoExecutionException {
        final String goal = "resources";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("encoding")
                ), environment
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
                prepareXpp3Dom(log, environment,
                        prop("encoding")
                ), environment
        );
        logGoal(goal, false);
        return this;
    }
}
