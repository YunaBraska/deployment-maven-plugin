package berlin.yuna.mavendeploy.model;

@FunctionalInterface
public interface ThrowingFunction<E extends Exception> {
    void run() throws E;
}