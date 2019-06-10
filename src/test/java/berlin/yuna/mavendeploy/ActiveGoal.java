package berlin.yuna.mavendeploy;

import berlin.yuna.mavendeploy.config.MojoBase;
import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import static java.lang.String.format;

public class ActiveGoal {

    private final Class<? extends MojoBase> activeMojo;
    private final String activeGoal;
    private final MojoBase dummyMojo;

    public ActiveGoal(final Class<? extends MojoBase> activeMojo, final String activeGoal) {
        this.activeMojo = activeMojo;
        this.activeGoal = activeGoal;
        dummyMojo = getDummyMojo();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ActiveGoal that = (ActiveGoal) o;
        return Objects.equals(activeMojo, that.activeMojo) &&
                Objects.equals(activeGoal, that.activeGoal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activeMojo, activeGoal);
    }

    public Class<? extends MojoBase> getActiveMojo() {
        return activeMojo;
    }

    public String getActiveGoal() {
        return activeGoal;
    }

    @Override
    public String toString() {
        return format("%s:%s:%s", dummyMojo.groupId(), dummyMojo.artifactId(), activeGoal);
    }

    private MojoBase getDummyMojo() {
        try {
            return activeMojo.getDeclaredConstructor(MojoExecutor.ExecutionEnvironment.class, Logger.class).newInstance(null, null);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
