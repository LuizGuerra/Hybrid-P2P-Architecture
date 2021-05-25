import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SuperNode {

    // Node communication
    static final int superNodePort = 5000;
    static final String HELLO = "HELLO";
    static final String EXIT = "EXIT";
    // Client and super node communication
    static final String CLIENT_HELLO = "CLIENT_HELLO";
    static final String CLIENT_EXIT = "CLIENT_EXIT";
    static final String REQUEST_RESOURCES = "REQUEST_RESOURCES";

    MulticastController superNodeController;
    MulticastController clientsNodeController;

    Set<String> superNodes;
    Set<String> clientNodes;
    List<Resource> clientResources;

    String name;
    int clientNodesPort;

    public SuperNode(String name, String group, int port) throws IOException {
        this.superNodeController = new MulticastController(name, group, superNodePort);
        this.clientsNodeController = new MulticastController(name, group, port);
        this.superNodes = new HashSet<>();
        this.clientNodes = new HashSet<>();
        this.clientResources = new ArrayList<>();
        this.name = name;
        this.clientNodesPort = port;
    }

    public void run() throws IOException {
        System.out.println("Send EXIT to exit.\n");
        System.out.println("Sending hello...");
        superNodeController.send(HELLO);
        System.out.println("Hello sent.\n");
        Scanner scanner = new Scanner(System.in);
        String input = "";
        while (true) {
            // Read if anything arrived from supernodes
            try {
                String received = superNodeController.receive();
                MulticastMessageFormat mmf = new MulticastMessageFormat(received);
                if (mmf.sender.equals(name)) {
                    continue;
                }
                switch (mmf.request) {
                    case HELLO:
                        System.out.println("Received hello from " + mmf.sender);
                        if (superNodes.add(mmf.sender)) {
                            System.out.println("Sending back a hello");
                            superNodeController.send(HELLO);
                            System.out.println("Connection with " + mmf.sender + " established.");
                        }
                        System.out.println();
                        break;
                    case EXIT:
                        superNodes.remove(mmf.sender);
                        break;
                    default:
                        System.out.println("Not expected input received:\n" + mmf.toString());
                }
            } catch (Exception e) {
            }
            // Read if anything arrived from clients
            try {
                String received = clientsNodeController.receive();
                MulticastMessageFormat mmf = new MulticastMessageFormat(received);
                if (mmf.sender.equals(name)) {
                    continue;
                }
                switch (mmf.request) {
                    case CLIENT_HELLO:
                        System.out.println("Received client hello from " + mmf.sender);
                        if (clientNodes.add(mmf.sender)) {
                            clientResources.addAll(resources(mmf.body));
                            System.out.println("Sending back a hello");
                            clientsNodeController.send(HELLO);
                            System.out.println("Connection with " + mmf.sender + " established.");
                        }
                        System.out.println(clientResources);
                        break;
                    case CLIENT_EXIT:
                        clientNodes.remove(mmf.sender);
                        System.out.println("Removed client " + mmf.sender);
                        break;
                    case REQUEST_RESOURCES:
                        break;
                    default:
                        System.out.println("Not expected input received:\n" + mmf.toString());
                }
            } catch (Exception e) {
            }
            if (System.in.available() > 0) {
                input = scanner.nextLine().trim().toUpperCase();
                switch (input) {
                    case EXIT:
                        superNodeController.send(EXIT);
                        break;
                }
            }
            if (input.equals(EXIT)) {
                break;
            }
        }
        superNodeController.end();
        scanner.close();
    }

    public List<Resource> resources(String body) {
        return Arrays.stream(body.split("\\s"))
                .map(Resource::parseString)
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: java ClientNode <name> <group> <unique port>");
            System.out.println("<name>: user name");
            System.out.println("<group>: group unique number");
            System.out.println("<unique port>: client communication port");
            System.exit(1);
        }
        int clientPort = Integer.parseInt(args[2]);
        SuperNode node = new SuperNode(args[0], args[1], clientPort);
        node.run();
    }
}
// String ip =