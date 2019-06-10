package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;

public class Gpg extends MojoBase {

    public Gpg(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        super("org.apache.maven.plugins", "maven-gpg-plugin", environment, log);
        version = "1.6";
    }

    public static Gpg build(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        return new Gpg(environment, log);
    }

    public Gpg sign() throws MojoExecutionException {
        final String goal = "sign";
        logGoal(goal, true);

        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("ascDirectory"),
                        prop("gpg.defaultKeyring"),
                        prop("excludes"),
                        prop("gpg.executable"),
                        prop("gpg.homedir"),
                        prop("gpg.keyname"),
                        prop("gpg.lockMode"),
                        //TODO: parse XML structure at value or read file for xml structures
                        prop("gpgArguments", prop("arg", "--pinentry-mode"), prop("arg", "loopback")),
                        prop("gpg.passphraseServerId"),
                        prop("gpg.publicKeyring"),
                        prop("gpg.secretKeyring"),
                        prop("gpg.skip"),
                        prop("gpg.useagent")
                )
                , environment
        );
        logGoal(goal, false);
        return this;
    }
}
