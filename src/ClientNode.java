import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientNode {

    static final String HELLO = "HELLO";
    static final String EXIT = "EXIT";

    static final String CLIENT_HELLO = "CLIENT_HELLO";
    static final String CLIENT_EXIT = "CLIENT_EXIT";
    static final String CLIENT_ALIVE = "CLIENT_ALIVE";
    static final String REQUEST_RESOURCES = "REQUEST_RESOURCES";
    static final String COMMIT_RESOURCES = "COMMIT_RESOURCES";

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
        System.out.println("Usage:");
        System.out.println("Send EXIT to exit.");
        System.out.println("Send R to ask for all resources.\n");

        String input = "";
        String superNode = "";
        List<Resource> localResources = getResources();

        System.out.println("Sending hello...");
        controller.send(CLIENT_HELLO, localResources);
        System.out.println("Hello sent.\n");
        Scanner scanner = new Scanner(System.in);

        boolean wantResources = false;

        while (true) {
            // Read if anything arrived
            try {
                String received = controller.receive();
                MulticastMessageFormat mmf = new MulticastMessageFormat(received);
                // If we already have our super node
                // and if message coming is not from supernode
                if (!superNode.isEmpty() && !mmf.sender.equals(superNode)) {
                    continue;
                }
                switch (mmf.request) {
                    case HELLO:
                        System.out.println("Received hello from " + mmf.sender);
                        superNode = mmf.sender;
                        System.out.println("Connection with " + mmf.sender + " established.");
                        System.out.println();
                        break;
                    case CLIENT_HELLO:
                    case CLIENT_EXIT:
                    case REQUEST_RESOURCES:
                        break;
                    case COMMIT_RESOURCES:
                        if (mmf.sender.equals(superNode) && wantResources) {
                            System.out.println("Received resources from supernode.");
                            System.out.println("Received all resources from supernode:");
                            mmf.bodyToResourcesList().forEach(System.out::println);
                        }
                        break;
                    default:
                        System.out.println("Not expected input received:\n" + mmf);
                }
                System.out.println(mmf.sender);
                System.out.println(
                        "Is SN: " + mmf.sender.equals(superNode) + "\n" +
                        "Isn SN: " + !mmf.sender.equals(superNode) + "\n" +
                        "Is empty: " + superNode.isEmpty() + "\n" +
                        "Isnt empty: " + !superNode.isEmpty() + "\n"
                );
            } catch (Exception ignored) {
            }
            if (System.in.available() > 0) {
                input = scanner.nextLine().trim().toUpperCase();
                switch (input) {
                    case EXIT:
                        System.out.println("Exiting...");
                        controller.send(CLIENT_EXIT);
                        break;
                    case "R":
                        System.out.println("Requesting resources to super node.");
                        controller.send(REQUEST_RESOURCES);
                        System.out.println("Request successful");
                        wantResources = true;
                    default:
                        break;
                }
            }
            if (input.equals(EXIT)) {
                break;
            }
        }
        controller.end();
        scanner.close();
    }

    private List<Resource> getResources() {
        try (Stream<Path> walk = Files.walk(Paths.get(System.getProperty("user.dir")))) {
            return walk.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .map(x -> {
                        try {
                            return new Resource(x, ip);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
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
