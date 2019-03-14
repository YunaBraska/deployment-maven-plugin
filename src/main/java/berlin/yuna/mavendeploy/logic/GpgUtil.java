package berlin.yuna.mavendeploy.logic;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.Ci;
import org.slf4j.Logger;

import java.io.File;

import static org.slf4j.LoggerFactory.getLogger;

public class GpgUtil {

    private static final Logger LOG = getLogger(Ci.class);
    private static final String CMD_MVN_REPO_PATH = "mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\\[INFO\\]'";
    private static final File WORK_DIR = new File(System.getProperty("java.io.tmpdir"));
    private static final String URL_MVN_GPG_PLUGIN = "https://github.com/YunaBraska/maven-gpg-plugin.git";
    private static final String NAME_MAVEN_GPG_PLUGIN = "maven-gpg-plugin";

    public static synchronized void downloadMavenGpgIfNotExists(final File projectDir) {
        //FIXME: find out how to use GPG 2.1 on command line with original apache maven-gpg-plugin
        final Terminal terminal = new Terminal(GpgUtil.class).dir(projectDir).consumerError(System.err::println);
        final String MVN_REPO_PATH = terminal.execute(CMD_MVN_REPO_PATH).consoleInfo();
        if (!new File(MVN_REPO_PATH, "berlin/yuna/maven-gpg-plugin").exists()) {
            installMavenGpgPlugin(terminal, MVN_REPO_PATH);
        } else
            LOG.info("SUCCESSFULLY FOUND GPG PLUGIN FORK FROM [berlin.yuna] IN [{}]", MVN_REPO_PATH);
    }

    private static void installMavenGpgPlugin(final Terminal terminal, final String MVN_REPO_PATH) {
        LOG.warn("START INSTALLING GPG PLUGIN FORK FROM [berlin.yuna] TO [{}]", MVN_REPO_PATH);
        terminal.timeoutMs(-1).clearConsole().dir(WORK_DIR);
        terminal.execute("rm -rf " + NAME_MAVEN_GPG_PLUGIN);
        new GitService(WORK_DIR).clone(URL_MVN_GPG_PLUGIN, NAME_MAVEN_GPG_PLUGIN);
        terminal.execute("mvn clean install -f=maven-gpg-plugin -Drat.ignoreErrors=true --quiet");
        terminal.execute("rm -rf " + NAME_MAVEN_GPG_PLUGIN);
        LOG.warn("FINISHED INSTALLING GPG PLUGIN FORK FROM [berlin.yuna] TO [{}]", MVN_REPO_PATH);
    }
}
