package berlin.yuna.mavendeploy.config;

import berlin.yuna.clu.logic.SystemUtil;
import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Activation;
import org.apache.maven.settings.Profile;

import java.util.Properties;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
import static berlin.yuna.mavendeploy.util.MojoUtil.isPresent;

public class Gpg extends MojoBase {

    public Gpg(final PluginSession session) {
        super("org.apache.maven.plugins", "maven-gpg-plugin", "1.6", session);
    }

    public static Gpg build(final PluginSession session) {
        return new Gpg(session);
    }

    public Gpg sign() throws MojoExecutionException {
        final String goal = "sign";
        addGpgToSettings();
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

    public void addGpgToSettings() {
        final Profile profile = new Profile();
        final Activation activation = new Activation();
        final Properties properties = new Properties();
        final String gpgPath = session.getParamPresent("gpg.executable").orElse(getGpgPath(log));
        final String passPhrase = session.getParamPresent("gpg.pass", "gpg.passphrase").orElse("");
        session.setNewParam("gpg.executable", gpgPath);
        activation.setActiveByDefault(true);
        profile.setId("gpg");
        profile.setActivation(activation);
        properties.setProperty("gpg.executable", gpgPath);
        properties.setProperty("gpg.passphrase", passPhrase);
        profile.setProperties(properties);
        if (session.getMavenSession().getSettings().getProfiles().stream().noneMatch(p -> p.getId().equals(profile.getId()))) {
            log.info("%s Created profile id [%s] passphrase [%s] path [%s]", unicode(0x1F4D1), profile.getId(), passPhrase, gpgPath);
            session.getMavenSession().getSettings().getProfiles().add(profile);
        } else {
            log.debug("%s Profile id [%s] already exists", unicode(0x26A0), profile.getId());
        }
    }

    public static String getGpgPath(final Logger log) {
        final String result;
        final Terminal t = new Terminal().consumerError(log::info).timeoutMs(5000).breakOnError(false);
        if (SystemUtil.isWindows()) {
            //FIXME: test on windows if installed, gpg || gpg2, same as with unix
            result = t.execute("where gpg").consoleInfo();
        } else {
            result = t.execute("if which gpg2 >/dev/null 2>&1; then which gpg2; "
                    + "elif which gpg >/dev/null 2>&1; then which gpg; else echo \"gpg\"; fi"
            ).consoleInfo();
            return result;
        }
        return isPresent(result) ? result : "gpg";
    }
}
