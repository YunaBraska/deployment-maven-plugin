package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;

public class Deploy extends MojoBase {

    public Deploy(final PluginSession session) {
        super("org.apache.maven.plugins", "maven-deploy-plugin", "3.0.0-M1", session);
    }

    public static Deploy build(final PluginSession session) {
        return new Deploy(session);
    }

    public Deploy deploy() throws MojoExecutionException {
        final String goal = "deploy";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("altDeploymentRepository"),
                        prop("altReleaseDeploymentRepository"),
                        prop("altSnapshotDeploymentRepository"),
                        prop("deployAtEnd"),
                        prop("retryFailedDeploymentCount"),
                        prop("maven.deploy.skip")
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }
}
