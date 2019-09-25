package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;

public class Gpg extends MojoBase {

    public Gpg(final PluginSession session) {
        super("org.apache.maven.plugins", "maven-gpg-plugin", "1.6", session);
    }

    public static Gpg build(final PluginSession session) {
        return new Gpg(session);
    }

    public Gpg sign() throws MojoExecutionException {
        final String goal = "sign";
        logGoal(goal, true);

        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("ascDirectory"),
                        prop("gpg.defaultKeyring"),
                        prop("excludes"),
                        prop("passphrase", session.getParamFallback("gpg.passphrase", null)),
                        prop("executable", session.getParamFallback("gpg.executable", "gpg")),
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
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }
}
