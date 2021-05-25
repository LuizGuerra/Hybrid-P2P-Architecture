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
    static final String CLIENT_ALIVE = "CLIENT_ALIVE";
    static final String REQUEST_RESOURCES = "REQUEST_RESOURCES";

    MulticastController superNodeController;
    MulticastController clientsNodeController;

    Set<String> superNodes;
    Set<ClientInfo> clientNodes;
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
                if (mmf.sender.equals(name) || !containsClient(name)) {
                    continue;
                }
                switch (mmf.request) {
                    case CLIENT_HELLO:
                        System.out.println("Received client hello from " + mmf.sender);
                        if (addClient(mmf.sender)) {
                            clientResources.addAll(resources(mmf.body));
                            System.out.println("Sending back a hello");
                            clientsNodeController.send(HELLO);
                            System.out.println("Connection with " + mmf.sender + " established.");
                        }
                        break;
                    case CLIENT_EXIT:
                        removeFromClients(mmf.sender);
                        System.out.println("Removed client " + mmf.sender);
                        break;
                    case CLIENT_ALIVE:
                        clientSentMessage(mmf.sender, System.currentTimeMillis());
                    case REQUEST_RESOURCES:
                        break;
                    default:
                        System.out.println("Not expected input received:\n" + mmf.toString());
                }
                // Remove client if it does not send messages after 5 seconds
                for (ClientInfo client : clientNodes) {
                    long currentTime = System.currentTimeMillis();
                    if (client.lastMessageTime - currentTime > 5_000) {
                        clientNodes.remove(client);
                    }
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

    private void clientSentMessage(String name, long time) {
        for (ClientInfo client: clientNodes) {
            if (client.name.equals(name)) {
                client.lastMessageTime = time;
            }
        }
    }

    private boolean containsClient(String name) {
        for (ClientInfo client: clientNodes) {
            if (client.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean addClient(String name) {
        long currentTime = System.currentTimeMillis();
        boolean isNewClient = false;
        for (ClientInfo client: clientNodes) {
            if (client.name.equals(name)) {
                client.lastMessageTime = currentTime;
            } else {
                clientNodes.add(new ClientInfo(name, currentTime));
                isNewClient = true;
            }
        }
        return isNewClient;
    }

    private void removeFromClients(String name/*, String ip*/) {
        boolean hasRemoved = clientNodes.removeIf(clientInfo -> {
            return clientInfo.name.equals(name);
        });
        // if (hasRemoved) {
        //   clientResources.removeIf(resource -> {
        //     return resource.ip.equals(ip);
        //   });
        // }
    }
}