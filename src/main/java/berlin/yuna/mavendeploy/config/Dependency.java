package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.goal;

public class Dependency extends MojoBase {

    public Dependency(final PluginSession session) {
        super("org.apache.maven.plugins", "maven-dependency-plugin", "3.1.1", session);
    }

    public static Dependency build(final PluginSession session) {
        return new Dependency(session);
    }

    public Dependency resolvePlugins() throws MojoExecutionException {
        final String goal = "resolve-plugins";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("appendOutput"),
                        prop("excludeClassifiers"),
                        prop("excludeReactor"),
                        prop("excludeScope"),
                        prop("excludeTransitive"),
                        prop("excludeTypes"),
                        prop("includeArtifactIds"),
                        prop("includeClassifiers"),
                        prop("includeGroupIds"),
                        prop("includeScope"),
                        prop("includeTypes"),
                        prop("markersDirectory"),
                        prop("outputAbsoluteArtifactFilename"),
                        prop("overWriteIfNewer"),
                        prop("overWriteReleases"),
                        prop("overWriteSnapshots"),
                        prop("prependGroupId"),
                        prop("silent"),
                        prop("skip", session.getBoolean("fake").orElse(false).toString())
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }

    public Dependency purgeLocalRepository() throws MojoExecutionException {
        final String goal = "purge-local-repository";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("actTransitively"),
                        prop("reResolve"),
                        prop("resolutionFuzziness"),
                        prop("snapshotsOnly"),
                        prop("skip", session.getBoolean("fake").orElse(false).toString())
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }
}
