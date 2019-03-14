package berlin.yuna.mavendeploy;

import berlin.yuna.clu.logic.Terminal;
import org.slf4j.Logger;

import java.io.File;

import static org.slf4j.LoggerFactory.getLogger;

public class GpgUtil {

    private static final Logger LOG = getLogger(Ci.class);
    public static final String CMD_MVN_REPO_PATH = "mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\\[INFO\\]'";

    public static synchronized void downloadMavenGpgIfNotExists(final File projectDir) {
        //FIXME: find out how to use GPG 2.1 on command line with original apache maven-gpg-plugin
        final Terminal terminal = new Terminal().dir(projectDir).consumerError(System.err::println);
        final String MVN_REPO_PATH = terminal.execute(CMD_MVN_REPO_PATH).consoleInfo();
        if (!new File(MVN_REPO_PATH, "berlin/yuna/maven-gpg-plugin").exists()) {
            LOG.warn("START INSTALLING GPG PLUGIN FORK FROM [berlin.yuna] TO [{}]", MVN_REPO_PATH);
            terminal.timeoutMs(-1).clearConsole().dir(System.getProperty("java.io.tmpdir"));
            terminal.execute("rm -rf maven-gpg-plugin");
            terminal.execute("git clone https://github.com/YunaBraska/maven-gpg-plugin maven-gpg-plugin");
            terminal.execute("mvn clean install -f=maven-gpg-plugin -Drat.ignoreErrors=true --quiet");
            terminal.execute("rm -rf maven-gpg-plugin");
            LOG.warn("FINISHED INSTALLING GPG PLUGIN FORK FROM [berlin.yuna] TO [{}]", MVN_REPO_PATH);
        } else
            LOG.info("SUCCESSFULLY FOUND GPG PLUGIN FORK FROM [berlin.yuna] IN [{}]", MVN_REPO_PATH);
    }
}
