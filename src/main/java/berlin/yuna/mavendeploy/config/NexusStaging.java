package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.goal;

public class NexusStaging extends MojoBase {

    public NexusStaging(final PluginSession session) {
        super("org.sonatype.plugins", "nexus-staging-maven-plugin", "1.6.8", session);
    }

    public static NexusStaging build(final PluginSession session) {
        return new NexusStaging(session);
    }

    public NexusStaging deploy() throws MojoExecutionException {
        final String goal = "deploy";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("altStagingDirectory"),
                        prop("serverId", session.getParamFallback("deploy.id", null)),
                        prop("nexusUrl", session.getParamFallback("deploy.url", null)),
                        prop("description"),
                        prop("keepStagingRepositoryOnFailure"),
                        prop("keepStagingRepositoryOnCloseRuleFailure"),
                        prop("skipNexusStagingDeployMojo"),
                        prop("skipLocalStaging"),
                        prop("skipStagingRepositoryClose"),
                        prop("skipStaging"),
                        prop("autoReleaseAfterClose", "false"),
                        prop("autoDropAfterRelease", "true")
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }
}
