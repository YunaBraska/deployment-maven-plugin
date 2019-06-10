package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.getBoolean;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;

public class Dependency extends MojoBase {

    public Dependency(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        super("org.apache.maven.plugins", "maven-dependency-plugin", environment, log);
        version = "3.1.1";
    }

    public static Dependency build(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        return new Dependency(environment, log);
    }

    public Dependency resolvePlugins() throws MojoExecutionException {
        final String goal = "resolve-plugins";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
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
                        prop("skip", getBoolean(environment.getMavenSession(), "fake", false).toString())
                ), environment
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
                prepareXpp3Dom(log, environment,
                        prop("actTransitively"),
                        prop("reResolve"),
                        prop("resolutionFuzziness"),
                        prop("snapshotsOnly"),
                        prop("skip", getBoolean(environment.getMavenSession(), "fake", false).toString())
                ), environment
        );
        logGoal(goal, false);
        return this;
    }
}
