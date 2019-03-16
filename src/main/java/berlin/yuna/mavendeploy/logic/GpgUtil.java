package berlin.yuna.mavendeploy.logic;

import berlin.yuna.clu.logic.Terminal;
import org.apache.maven.plugin.logging.Log;

import java.io.File;

import static java.lang.String.format;

public class GpgUtil {

    private final Log LOG;

    private static final String CMD_MVN_REPO_PATH = "mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\\[INFO\\]'";
    private static final File WORK_DIR = new File(System.getProperty("java.io.tmpdir"));
    private static final String URL_MVN_GPG_PLUGIN = "https://github.com/YunaBraska/maven-gpg-plugin.git";
    private static final String NAME_MAVEN_GPG_PLUGIN = "maven-gpg-plugin";

    public GpgUtil(final Log LOG) {
        this.LOG = LOG;
    }

    public synchronized void downloadMavenGpgIfNotExists(final File projectDir) {
        //FIXME: find out how to use GPG 2.1 on command line with original apache maven-gpg-plugin
        final Terminal terminal = new Terminal().consumerError(LOG::error).dir(projectDir);
        final String MVN_REPO_PATH = terminal.execute(CMD_MVN_REPO_PATH).consoleInfo();
        if (!new File(MVN_REPO_PATH, "berlin/yuna/maven-gpg-plugin").exists()) {
            installMavenGpgPlugin(terminal, MVN_REPO_PATH);
        } else
            LOG.info(format("SUCCESSFULLY FOUND GPG PLUGIN FORK FROM [berlin.yuna] IN [%s]", MVN_REPO_PATH));
    }

    private void installMavenGpgPlugin(final Terminal terminal, final String MVN_REPO_PATH) {
        LOG.warn(format("START INSTALLING GPG PLUGIN FORK FROM [berlin.yuna] TO [%s]", MVN_REPO_PATH));
        terminal.timeoutMs(-1).clearConsole().dir(WORK_DIR);
        terminal.execute("rm -rf " + NAME_MAVEN_GPG_PLUGIN);
        new GitService(LOG, WORK_DIR).clone(URL_MVN_GPG_PLUGIN, NAME_MAVEN_GPG_PLUGIN);
        terminal.execute("mvn clean install -f=maven-gpg-plugin -Drat.ignoreErrors=true --quiet");
        terminal.execute("rm -rf " + NAME_MAVEN_GPG_PLUGIN);
        LOG.warn(format("FINISHED INSTALLING GPG PLUGIN FORK FROM [berlin.yuna] TO [%s]", MVN_REPO_PATH));
    }
}
