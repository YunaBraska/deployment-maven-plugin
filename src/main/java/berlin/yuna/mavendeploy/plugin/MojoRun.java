package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.logic.Ci;
import berlin.yuna.mavendeploy.logic.GitService;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;

import static java.lang.System.exit;

@Mojo(name = "run")
public class MojoRun extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;

    @Parameter(property = "args")
    private List<String> args;

    //Versioning
    @Parameter(property = "project.version")
    private String PROJECT_VERSION;
    @Parameter(property = "semantic.format")
    private String SEMANTIC_FORMAT;
    @Parameter(property = "tag")
    private String TAG;
    @Parameter(property = "tag.break")
    private String TAG_BREAK;
    @Parameter(property = "update.major")
    private String UPDATE_MAJOR;
    @Parameter(property = "update.minor")
    private String UPDATE_MINOR;
    @Parameter(property = "commit")
    private String COMMIT;

    //building
    @Parameter(property = "clean")
    private String CLEAN;
    @Parameter(property = "clean.cache")
    private String CLEAN_CACHE;
    @Parameter(property = "java.doc")
    private String JAVA_DOC;
    @Parameter(property = "java.src")
    private String SOURCE;
    @Parameter(property = "profiles")
    private String PROFILES;
    @Parameter(property = "gpg.pass")
    private String GPG_PASS;
    @Parameter(property = "gpg.pass.alt")
    private String GPG_PASS_ALT;

    //deployment
    @Parameter(property = "deploy.id")
    private String DEPLOY_ID;
    @Parameter(property = "release")
    private String RELEASE;

    //Settings.xml FIXME: one line arg?
    @Parameter(property = "settings")
    private List<String> settings;
    @Parameter(property = "s.server")
    private String S_SERVER;
    @Parameter(property = "s.user")
    private String S_USERNAME;
    @Parameter(property = "s.password")
    private String S_PASSWORD;

    //misc
    @Parameter(property = "report")
    private String REPORT;
    @Parameter(property = "encoding")
    private String ENCODING;
    @Parameter(property = "java.version")
    private String JAVA_VERSION;
    @Parameter(property = "test")
    private Boolean TEST;

    public void execute() {
        final Log log = getLog();
        final GitService gitService = new GitService(log, basedir);
        final boolean gitStash = gitService.gitHasChanges();

        log.error("Howdy [" + TEST + "]");
        if (TEST) {
            log.error("Howdy");
            exit(0);
        }

        if (gitStash) {
            log.warn("Stashing uncommitted git changes");
            //gitService.gitStash();
        }

        try {
            log.info("Preparing information");
            final Ci ci = new Ci(log, args.toArray(new String[0]));
            final String mavenCommand = ci.prepareMaven();
            new Terminal()
                    .dir(basedir)
                    .consumerInfo(log::info)
                    .consumerError(log::error)
                    .execute(mavenCommand)
                    .status();

            if (ci.allowCommitMessage()) {
                final String commitMessage = ci.prepareCommitMessage();
                new Terminal().dir(basedir).execute("mvn scm:checkin -Dmessage='" + commitMessage + "'");
            }
        } finally {
            if (gitStash) {
                log.warn("Load uncommitted git changes");
                //gitService.gitLoadStash();
            }
        }
    }

    public void setBasedir(final File basedir) {
        this.basedir = basedir;
    }

    public void setArgs(final List<String> args) {
        this.args = args;
    }
}
