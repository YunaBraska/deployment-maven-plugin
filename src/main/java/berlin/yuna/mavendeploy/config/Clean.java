package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;

//TODO: auto detect version like generate pom file with plugins, update in test and parse on runtime
//TODO: import mojo-executor without any dependencies...
//TODO: make interface or abstract super class

public class Clean extends MojoBase {

    public Clean(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        super("org.apache.maven.plugins", "maven-clean-plugin", "3.1.0", environment, log);
    }

    public static Clean build(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        return new Clean(environment, log);
    }

    public Clean clean() throws MojoExecutionException {
        final String goal = "clean";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("failOnError"),
                        prop("followSymLinks"),
                        prop("excludeDefaultDirectories")
                ), environment
        );
        logGoal(goal, false);
        return this;
    }
}
