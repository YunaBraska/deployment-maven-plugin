package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;
import static java.lang.String.format;

public class Scm extends MojoBase {

    private String pushChanges = null;
    private String remoteTagging = null;

    public Scm(final PluginSession session) {
        super("org.apache.maven.plugins", "maven-scm-plugin", "1.11.2", session);
    }

    public static Scm build(final PluginSession session) {
        final Scm scm = new Scm(session);
        //TODO: replace with original PluginSession
        final Boolean fake = session.getBoolean("fake").orElse(false);
        if (fake) {
            scm.pushChanges = "false";
            scm.remoteTagging = "false";
            session.getLog().info(format("+ [%s] default key [pushChanges] value [%s]", scm.getClass().getSimpleName(), scm.pushChanges));
            session.getLog().info(format("+ [%s] default key [remoteTagging] value [%s]", scm.getClass().getSimpleName(), scm.remoteTagging));
        }
        return scm;
    }

    public Scm tag() throws MojoExecutionException {
        final String goal = "tag";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("basedir"),
                        prop("tag"),
                        prop("addTimestamp"),
                        prop("connectionType"),
                        prop("connectionUrl"),
                        prop("developerConnectionUrl"),
                        prop("excludes"),
                        prop("includes"),
                        prop("message"),
                        prop("passphrase"),
                        prop("password"),
                        prop("pinExternals"),
                        prop("privateKey"),
                        prop("pushChanges", pushChanges),
                        prop("sign"),
                        prop("tagBase"),
                        prop("timestampFormat"),
                        prop("timestampPosition"),
                        prop("timestampPrefix"),
                        prop("username"),
                        prop("workItem"),
                        prop("workingDirectory"),
                        prop("remoteTagging", remoteTagging)
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }
}
