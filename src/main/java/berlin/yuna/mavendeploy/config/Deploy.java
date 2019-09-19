package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;

public class Deploy extends MojoBase {

    public Deploy(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        super("org.apache.maven.plugins", "maven-deploy-plugin", "3.0.0-M1", environment, log);
    }

    public static Deploy build(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        return new Deploy(environment, log);
    }

    public Deploy deploy() throws MojoExecutionException {
        final String goal = "deploy";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("altDeploymentRepository"),
                        prop("altReleaseDeploymentRepository"),
                        prop("altSnapshotDeploymentRepository"),
                        prop("deployAtEnd"),
                        prop("retryFailedDeploymentCount"),
                        prop("maven.deploy.skip")
                ), environment
        );
        logGoal(goal, false);
        return this;
    }
}
