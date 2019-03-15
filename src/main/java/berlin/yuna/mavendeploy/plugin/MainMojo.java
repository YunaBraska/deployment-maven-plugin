package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.logic.Ci;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

@Mojo(name = "run")
public class MainMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;

    @Parameter(property = "args", defaultValue = "")
    private List<String> args;

    public void execute() {
        final Log log = getLog();
        log.info("Preparing information");
        final String mavenCommand = new Ci(log, args.toArray(new String[0])).prepareMaven();
        log.info("Run setup");
        new Terminal().consumerInfo(log::info).consumerError(log::error).dir(basedir).execute(mavenCommand);
        log.info("done setup");
    }

    public void setBasedir(final File basedir) {
        this.basedir = basedir;
    }

    public void setArgs(final List<String> args) {
        this.args = args;
    }
}
