import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

final public class MulticastMessageFormat {
    public final String request;
    public final String sender;
    public final String body;
    public final String originalMessage;

    public MulticastMessageFormat(String str) {
        String vars[] = str.split("\\s");
        if(vars.length < 2) {
            throw new IllegalArgumentException("MulticastMessageFormat constructor bad entry:\n" + str);
        }
        request = vars[0];
        sender = vars[1];
        if(vars.length >= 3) {
            body = str.substring(request.length() + sender.length() + 2);
        } else {
            body = "";
        }
        originalMessage = str;
    }

    public List<Resource> bodyToResourcesList() {
        try {
            return stringToResource(body);
        } catch (Exception e) {
            System.out.println("Error bruv");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static String resourceToString(List<Resource> resources) {
        String str = "";
        for(int i = 0; i < resources.size(); i++) {
            if(i == resources.size() - 1) {
                str += resources.get(i).convertForMulticast();
            } else {
                str += resources.get(i).convertForMulticast() + " ";
            }
        }
        return str;
    }

    public static List<Resource> stringToResource(String str) {
        return Arrays.stream(str.split(" "))
                .map(Resource::parseString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "MulticastMessageFormat{" +
                "request='" + request + '\'' +
                ", sender='" + sender + '\'' +
                ", body='" + body + '\'' +
                ", originalMessage='" + originalMessage + '\'' +
                '}';
    }
}
