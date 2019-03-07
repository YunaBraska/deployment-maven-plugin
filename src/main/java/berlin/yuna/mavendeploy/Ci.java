package berlin.yuna.mavendeploy;

import berlin.yuna.clu.logic.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class Ci {

    static final Logger LOG = LoggerFactory.getLogger(Terminal.class);
    final File buildFile = new File(Paths.get("target").toAbsolutePath().getParent().toFile(), "ci.bash");
    Terminal terminal;

    void init() {
        assertThat(buildFile.exists(), is(true));
        terminal = new Terminal()
                .dir(prepareTestProject())
                .consumerInfo(System.out::println)
                .consumerError(System.err::println);
    }

    private File prepareTestProject() {
        final File testDir = new File(System.getProperty("java.io.tmpdir"), getClass().getSimpleName());
        if (!testDir.exists()) {
            assertThat(testDir.mkdirs(), is(true));
            assertThat(testDir.exists(), is(true));
            new Terminal()
                    .dir(testDir)
                    .consumerInfo(System.out::println)
                    .consumerError(System.err::println)
                    .execute("git clone https://github.com/YunaBraska/command-line-util");
            assertThat(testDir.list(), is(notNullValue()));
            assertThat(requireNonNull(testDir.list()).length, is(not(0)));
        }
        return new File(testDir, "command-line-util");
    }
}
