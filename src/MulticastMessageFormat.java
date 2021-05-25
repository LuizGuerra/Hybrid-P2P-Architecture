public class MulticastMessageFormat {
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
