import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientNode {

    static final String HELLO = "HELLO";
    static final String CLIENT_HELLO = "CLIENT_HELLO";
    static final String CLIENT_EXIT = "CLIENT_EXIT";
    static final String CLIENT_ALIVE = "CLIENT_ALIVE";
    static final String REQUEST_RESOURCES = "REQUEST_RESOURCES";

    String ip = InetAddress.getLocalHost().getHostAddress();

    String group = "224.0.2.1";
    MulticastController controller;
    String name;
    int superNodePort;

    public ClientNode(String name, int superNodePort) throws IOException {
        this.controller = new MulticastController(name, group, superNodePort);
        this.name = name;
        this.superNodePort = superNodePort;
    }

    public void run() throws IOException {
        System.out.println("Send EXIT to exit.\n");

        String input = "";
        String superNode = "";
        List<Resource> localResources = getResources();

        System.out.println("Sending hello...");
        controller.send(CLIENT_HELLO, convertToString(localResources));
        System.out.println("Hello sent.\n");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            // Read if anything arrived
            try {
                String received = controller.receive();
                MulticastMessageFormat mmf = new MulticastMessageFormat(received);
                if (mmf.sender.equals(name)) {
                    continue;
                }
                switch (mmf.request) {
                    case HELLO:
                        System.out.println("Received hello from " + mmf.sender);
                        if (superNode.isEmpty()) {
                            superNode = mmf.sender;
                            System.out.println("Connection with " + mmf.sender + " established.");
                        }
                        System.out.println();
                        break;
                    case CLIENT_HELLO:
                    case CLIENT_EXIT:
                        break;
                    default:
                        System.out.println("Not expected input received:\n" + mmf.toString());
                }
            } catch (Exception e) {
            }
            if (System.in.available() > 0) {
                input = scanner.nextLine().trim().toUpperCase();
                switch (input) {
                    default:
                        break;
                }
            }
        }
    }

    private String convertToString(List<Resource> resources) {
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

    private List<Resource> getResources() {
        try (Stream<Path> walk = Files.walk(Paths.get(System.getProperty("user.dir")))) {
            return walk.filter(Files::isRegularFile)
                    .map(x -> x.toString())
                    .map(x -> {
                        try {
                            return new Resource(x, ip);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(x -> x != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java ClientNode <name> <supernode port>");
            System.exit(1);
        }
        int superNodePort = Integer.parseInt(args[1]);
        ClientNode node = new ClientNode(args[0], superNodePort);
        node.run();
    }
}
