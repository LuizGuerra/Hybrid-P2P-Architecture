import java.util.Objects;

public class ClientInfo {
    public final String name;
    public long lastMessageTime;

    public ClientInfo(String name, long time) {
        this.name = name;
        this.lastMessageTime = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientInfo that = (ClientInfo) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "ClientInfo{" +
                "name='" + name + '\'' +
                ", lastMessageTime=" + lastMessageTime +
                '}';
    }
}
