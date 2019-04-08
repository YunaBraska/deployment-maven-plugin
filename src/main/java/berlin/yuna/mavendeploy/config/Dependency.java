package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import static berlin.yuna.mavendeploy.config.MojoHelper.getBoolean;
import static berlin.yuna.mavendeploy.config.MojoHelper.prepareElement;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.configuration;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.element;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;

public class Dependency extends MojoBase {

    public Dependency(final MojoExecutor.ExecutionEnvironment environment, final Log log) {
        super("org.apache.maven.plugins", "maven-dependency-plugin", environment, log);
        version = "3.1.1";
    }

    public static Dependency build(final MojoExecutor.ExecutionEnvironment environment, final Log log) {
        return new Dependency(environment, log);
    }

    public Dependency resolvePlugins() throws MojoExecutionException {
        logGoal("resolve-plugins", true);
        executeMojo(
                getPlugin(),
                goal("resolve-plugins"),
                configuration(
                        prepareElement(environment, "appendOutput", "false"),
                        prepareElement(environment, "excludeClassifiers", ""),
                        prepareElement(environment, "excludeReactor", "true"),
                        prepareElement(environment, "excludeScope", ""),
                        prepareElement(environment, "excludeTransitive", "false"),
                        prepareElement(environment, "excludeTypes", ""),
                        prepareElement(environment, "includeArtifactIds", ""),
                        prepareElement(environment, "includeClassifiers", ""),
                        prepareElement(environment, "includeGroupIds", ""),
                        prepareElement(environment, "includeScope", ""),
                        prepareElement(environment, "includeTypes", ""),
                        prepareElement(environment, "markersDirectory", environment.getMavenSession().getUserProperties().getProperty("project.build.directory") + "/dependency-maven-plugin-markers"),
                        prepareElement(environment, "outputAbsoluteArtifactFilename", "false"),
                        prepareElement(environment, "overWriteIfNewer", "true"),
                        prepareElement(environment, "overWriteReleases", "false"),
                        prepareElement(environment, "overWriteSnapshots", "false"),
                        prepareElement(environment, "prependGroupId", "false"),
                        prepareElement(environment, "silent", "false"),
                        element("skip", getBoolean(environment.getMavenSession(), "fake", false).toString())
                ), environment
        );
        logGoal("resolve-plugins", false);
        return this;
    }

    public Dependency purgeLocalRepository() throws MojoExecutionException {
        logGoal("purge-local-repository", true);
        executeMojo(
                getPlugin(),
                goal("purge-local-repository"),
                configuration(
                        prepareElement(environment, "actTransitively", "true"),
                        prepareElement(environment, "reResolve", "true"),
                        prepareElement(environment, "resolutionFuzziness", "version"),
                        prepareElement(environment, "snapshotsOnly", "false"),
                        element("skip", getBoolean(environment.getMavenSession(), "fake", false).toString())
                ), environment
        );
        logGoal("purge-local-repository", false);
        return this;
    }
}
