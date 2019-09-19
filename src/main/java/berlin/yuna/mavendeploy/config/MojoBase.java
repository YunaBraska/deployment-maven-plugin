package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.model.Plugin;

import java.util.Objects;

import static berlin.yuna.mavendeploy.plugin.MojoExecutor.plugin;
import static java.lang.String.format;

public abstract class MojoBase {

    protected final String groupId;
    protected final String artifactId;
    protected final String version;

    protected final MojoExecutor.ExecutionEnvironment environment;
    protected final Logger log;

    protected MojoBase(final String groupId, final String artifactId, final String version, final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.environment = environment;
        this.log = log;
    }

    public String groupId() {
        return groupId;
    }

    public String artifactId() {
        return artifactId;
    }

    public String version() {
        return version;
    }

    public MojoExecutor.ExecutionEnvironment environment() {
        return environment;
    }

    public Logger log() {
        return log;
    }

    public Plugin toPlugin() {
        final Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        plugin.setVersion(version);
        return plugin;
    }

    protected Plugin getPlugin() {
        return plugin(
                MojoExecutor.groupId(groupId),
                MojoExecutor.artifactId(artifactId),
                MojoExecutor.version(version)
        );
    }

    protected void logGoal(final String goal, final boolean start) {
        log.info(format("--------------------------<=[ %s %s:%s:%s:%s ]=>--------------------------", start ? "Start" : "End", groupId, artifactId, goal, version));
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MojoBase mojoBase = (MojoBase) o;
        return Objects.equals(groupId, mojoBase.groupId) &&
                Objects.equals(artifactId, mojoBase.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

    @Override
    public String toString() {
        return "MojoBase{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                '}';
    }
}