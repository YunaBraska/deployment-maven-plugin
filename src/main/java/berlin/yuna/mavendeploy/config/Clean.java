package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;
import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;

//TODO: auto detect version like generate pom file with plugins, update in test and parse on runtime
//TODO: import mojo-executor without any dependencies...
//TODO: make interface or abstract super class

public class Clean extends MojoBase {

    public Clean(final MojoExecutor.ExecutionEnvironment environment, final Log log) {
        super("org.apache.maven.plugins", "maven-clean-plugin", environment, log);
        version = "3.1.0";
    }

    public static Clean build(final MojoExecutor.ExecutionEnvironment environment, final Log log) {
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
