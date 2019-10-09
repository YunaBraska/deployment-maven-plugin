package berlin.yuna.mavendeploy.model;

import static berlin.yuna.mavendeploy.util.MojoUtil.isPresent;

public enum Parameter {

    BASE_DIR("base.dir", "project.basedir"),
    TEST_SKIP("test.skip", "maven.test.skip"),
    JAVA_VERSION("java.version", null),
    TEST_INT("test.int", null),
    TEST_INTEGRATION("test.integration", null),
    PROJECT_LIBRARY("project.library", null),

    //Maven
    NEW_VERSION(null, "newVersion"),
    POM_BACKUP(null, "generateBackupPoms"),
    SOURCE(null, "source"),
    TARGET(null, "target"),
    REMOVE_SNAPSHOT("remove.snapshot", "removeSnapshot");

    private final String key;
    private final String maven;

    Parameter(final String key, final String maven) {
        this.key = key;
        this.maven = maven;
    }

    public String maven() {
        return isPresent(maven) ? maven : key;
    }

    public String key() {
        return key;
    }

    @Override
    public String toString() {
        return key();
    }
}
