import java.util.Objects;

public class Tuple {
    String name;
    Long f;

    public Tuple(String name, Long f) {
        this.name = name;
        this.f = f;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple tuple = (Tuple) o;
        return name.equals(tuple.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, f);
    }
}
