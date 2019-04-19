package berlin.yuna.mavendeploy.model;

public class Prop {
    public String key;
    public String value;

    private Prop(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public static Prop prop(final String key) {
        return new Prop(key, null);
    }

    public static Prop prop(final String key, final String value) {
        return new Prop(key, value);
    }
}