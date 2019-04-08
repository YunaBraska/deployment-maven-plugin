package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import static berlin.yuna.mavendeploy.config.MojoHelper.prepareElement;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.configuration;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;

//TODO: auto detect version
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
        logGoal("clean", true);
        executeMojo(
                getPlugin(),
                goal("clean"),
                configuration(
                        prepareElement(environment, "failOnError", "true"),
                        prepareElement(environment, "followSymLinks", "false"),
                        prepareElement(environment, "excludeDefaultDirectories", "false")
                ), environment
        );
        logGoal("clean", false);
        return this;
    }
}
