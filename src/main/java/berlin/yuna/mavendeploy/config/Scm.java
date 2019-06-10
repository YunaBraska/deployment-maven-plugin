package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.getBoolean;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;
import static java.lang.String.format;

public class Scm extends MojoBase {

    protected String pushChanges = null;
    protected String remoteTagging = null;

    public Scm(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        super("org.apache.maven.plugins", "maven-scm-plugin", environment, log);
        version = "1.11.2";
    }

    public static Scm build(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        final Scm scm = new Scm(environment, log);
        final Boolean fake = getBoolean(environment.getMavenSession(), "fake", false);
        if (fake) {
            scm.pushChanges = "false";
            scm.remoteTagging = "false";
            log.info(format("+ [%s] default key [pushChanges] value [%s]", scm.getClass().getSimpleName(), scm.pushChanges));
            log.info(format("+ [%s] default key [remoteTagging] value [%s]", scm.getClass().getSimpleName(), scm.remoteTagging));
        }
        return scm;
    }

    public Scm tag() throws MojoExecutionException {
        final String goal = "tag";
        logGoal(goal, true);


        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
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
                )
                , environment
        );
        logGoal(goal, false);
        return this;
    }
}
