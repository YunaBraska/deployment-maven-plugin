package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;
import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.configuration;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;

public class Versions extends MojoBase {

    public Versions(final MojoExecutor.ExecutionEnvironment environment, final Log log) {
        super("org.codehaus.mojo", "versions-maven-plugin", environment, log);
        version = "2.7";
    }

    public static Versions build(final MojoExecutor.ExecutionEnvironment environment, final Log log) {
        return new Versions(environment, log);
    }

    public Versions updateParent() throws MojoExecutionException {
        final String goal = "update-parent";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("allowSnapshots"),
                        prop("generateBackupPoms")
//                        prop( "parentVersion"),
//                        prop( "maven.version.rules"),
                ), environment
        );
        logGoal(goal, false);
        return this;
    }

    public Versions updateProperties() throws MojoExecutionException {
        final String goal = "update-properties";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("allowDowngrade"),
                        prop("allowIncrementalUpdates"),
                        prop("allowMajorUpdates"),
                        prop("allowMinorUpdates"),
                        prop("allowSnapshots"),
                        prop("autoLinkItems"),
                        prop("excludeReactor"),
                        prop("generateBackupPoms"),
                        prop("processDependencies"),
                        prop("processDependencyManagement"),
                        prop("processParent")
//                        prop( "maven.version.rules"),
                ), environment
        );
        logGoal(goal, false);
        return this;
    }

    public Versions updateChildModules() throws MojoExecutionException {
        final String goal = "update-child-modules";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("allowSnapshots"),
                        prop("generateBackupPoms")
//                        prop( "maven.version.rules"),
                ), environment
        );
        logGoal(goal, false);
        return this;
    }

    public Versions useLatestReleases() throws MojoExecutionException {
        final String goal = "use-latest-releases";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("allowIncrementalUpdates"),
                        prop("allowMajorUpdates"),
                        prop("allowMinorUpdates"),
                        prop("allowSnapshots"),
                        prop("excludeReactor"),
                        prop("generateBackupPoms"),
                        prop("processDependencyManagement"),
                        prop("processParent")
//                        prop( "maven.version.rules"),
                ), environment
        );
        logGoal(goal, false);
        return this;
    }

    public Versions useNextSnapshots() throws MojoExecutionException {
        final String goal = "use-next-snapshots";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("allowIncrementalUpdates"),
                        prop("allowMajorUpdates"),
                        prop("allowMinorUpdates"),
                        prop("allowSnapshots"),
                        prop("excludeReactor"),
                        prop("generateBackupPoms"),
                        prop("processDependencies"),
                        prop("processDependencyManagement"),
                        prop("processParent")
//                        prop( "maven.version.rules"),
                ), environment
        );
        logGoal(goal, false);
        return this;
    }

    public Versions useLatestVersions() throws MojoExecutionException {
        final String goal = "use-latest-versions";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("allowIncrementalUpdates"),
                        prop("allowMajorUpdates"),
                        prop("allowMinorUpdates"),
                        prop("allowSnapshots"),
                        prop("excludeReactor"),
                        prop("generateBackupPoms"),
                        prop("processDependencies"),
                        prop("processDependencyManagement"),
                        prop("processParent")
//                        prop( "maven.version.rules"),
                ), environment
        );
        logGoal(goal, false);
        return this;
    }

    public Versions set() throws MojoExecutionException {
        final String goal = "set";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("allowSnapshots"),
                        prop("artifactId"),
                        prop("generateBackupPoms"),
                        prop("groupId"),
                        prop("nextSnapshot"),
                        prop("oldVersion"),
                        prop("processAllModules"),
                        prop("processDependencies"),
                        prop("processParent"),
                        prop("processPlugins"),
                        prop("removeSnapshot"),
                        prop("updateMatchingVersions")
//                        prop( "maven.version.rules"),
                ), environment
        );
        logGoal(goal, false);
        return this;
    }

    public Versions commit() throws MojoExecutionException {
        final String goal = "commit";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                configuration(),
                environment
        );
        logGoal(goal, false);
        return this;
    }
}
