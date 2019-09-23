package berlin.yuna.mavendeploy.model;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class Prop {
    public final String key;
    public final String value;
    public List<Prop> childProps = new ArrayList<>();

    private Prop(final String key, final String value, final List<Prop> childProps) {
        this.key = key;
        this.value = childProps != null ? null : value;
        this.childProps = childProps == null ? this.childProps : childProps;
    }

    public static Prop prop(final String key) {
        return new Prop(key, null, null);
    }

    public static Prop prop(final String key, final String value) {
        return new Prop(key, value, null);
    }

    public static Prop prop(final String key, final Prop... childProps) {
        return new Prop(key, null, asList(childProps));
    }

    public boolean isEmpty() {
        return (value == null || value.trim().isEmpty()) && childProps.isEmpty();
    }
}