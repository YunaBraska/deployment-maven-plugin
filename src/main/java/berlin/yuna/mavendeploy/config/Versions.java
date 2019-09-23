package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.configuration;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;

public class Versions extends MojoBase {

    public Versions(final PluginSession session) {
        super("org.codehaus.mojo", "versions-maven-plugin", "2.7", session);
    }

    public static Versions build(final PluginSession session) {
        return new Versions(session);
    }

    public Versions updateParent() throws MojoExecutionException {
        final String goal = "update-parent";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("allowSnapshots"),
                        prop("generateBackupPoms")
//                        prop( "parentVersion"),
//                        prop( "maven.version.rules"),
                ), session.getEnvironment()
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
                session.prepareXpp3Dom(
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
                ), session.getEnvironment()
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
                session.prepareXpp3Dom(
                        prop("allowSnapshots"),
                        prop("generateBackupPoms")
//                        prop( "maven.version.rules"),
                ), session.getEnvironment()
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
                session.prepareXpp3Dom(
                        prop("allowIncrementalUpdates"),
                        prop("allowMajorUpdates"),
                        prop("allowMinorUpdates"),
                        prop("allowSnapshots"),
                        prop("excludeReactor"),
                        prop("generateBackupPoms"),
                        prop("processDependencyManagement"),
                        prop("processParent")
//                        prop( "maven.version.rules"),
                ), session.getEnvironment()
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
                session.prepareXpp3Dom(
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
                ), session.getEnvironment()
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
                session.prepareXpp3Dom(
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
                ), session.getEnvironment()
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
                session.prepareXpp3Dom(
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
                ), session.getEnvironment()
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
                session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }
}
