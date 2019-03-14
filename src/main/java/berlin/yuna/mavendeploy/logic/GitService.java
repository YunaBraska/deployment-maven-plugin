package berlin.yuna.mavendeploy.logic;

import berlin.yuna.clu.logic.Terminal;

import java.io.File;

//FIXME: add GitLibrary like JGit
public class GitService {

    private final File workDir;
    private final Terminal terminal;

    public GitService(final File workDir) {
        this.workDir = workDir;
        terminal = new Terminal(GitService.class).timeoutMs(30000).breakOnError(true).dir(workDir).consumerError(System.err::println);
    }

    public File clone(final String url, final String name) {
        terminal.execute("git clone " + url + " " + name);
        return new File(workDir, name);
    }

    public String getLastGitTag() {
        return terminal.execute("git describe --tags --always | sed 's/\\(.*\\)-.*/\\1/'").consoleInfo().trim();
    }

    public String getLastRefLog() {
        return terminal.execute("git reflog show --all | grep $(git log --pretty=format:'%h' -n 1)").consoleInfo();
    }

}
