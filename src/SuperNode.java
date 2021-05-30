import java.io.IOException;
import java.util.*;

public class SuperNode {

    // Node information
    String name;

    // Multicasts
    MulticastController superNodeController;
    MulticastController clientsNodeController;

    // Node communication
    static final String HELLO = "HELLO";
    static final String EXIT = "EXIT";
    static final int superNodePort = 5000;
    Set<String> superNodes;
    String superNodeGroup = "224.0.2.1";

    // Client and super node communication
    static final String CLIENT_HELLO = "CLIENT_HELLO";
    static final String CLIENT_EXIT = "CLIENT_EXIT";
    static final String CLIENT_ALIVE = "CLIENT_ALIVE";
    static final String REQUEST_RESOURCES = "REQUEST_RESOURCES";
    static final String COMMIT_RESOURCES = "COMMIT_RESOURCES";
    int clientNodesPort;
    Set<ClientInfo> clientNodes;
    List<Resource> clientResources;
    String clientNodeGroup = "224.0.2.2";

    public SuperNode(String name, int port) throws IOException {
        this.superNodeController = new MulticastController(name, superNodeGroup, superNodePort);
        this.clientsNodeController = new MulticastController(name, clientNodeGroup, port);
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

        // Is waiting super node resources or request from other supernode
        String askedForResources = "";
        String resourceStrings = "";
        int counter = 0;


        while (true) {
            // If can send resources back to client
            if (!askedForResources.isEmpty() && counter == superNodes.size()) {
                System.out.println("\nReceived all resources. Sending package...");
                clientsNodeController.send(COMMIT_RESOURCES, resourceStrings);
                askedForResources = "";
                resourceStrings = "";
                counter = 0;
                System.out.println("Resources package sent to all connected clients.\n");
            }
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
                    case REQUEST_RESOURCES:
                        System.out.println("\nReceived super node resource request from " + mmf.sender);
                        superNodeController.send(COMMIT_RESOURCES, clientResources);
                        System.out.println("Sent resources");
                        break;
                    case COMMIT_RESOURCES:
                        if(!askedForResources.isEmpty()) {
                            System.out.println("Received resources from super node " + mmf.sender);
                            resourceStrings += " " + mmf.body;
                            counter++;
                        }
                        break;
                    default:
                        System.out.println("Not expected input received:\n" + mmf);
                }
            } catch (Exception ignored) {
            }
            // Read if anything arrived from clients
            try {
                String received = clientsNodeController.receive();
                MulticastMessageFormat mmf = new MulticastMessageFormat(received);
                if (mmf.sender.equals(name)) { //  || !containsClient(name)
                    continue;
                }
                switch (mmf.request) {
                    case CLIENT_HELLO:
                        System.out.println("Received client hello from " + mmf.sender);
                        if (clientNodes.add(new ClientInfo(mmf.sender, System.currentTimeMillis()))) {
                            clientResources.addAll(mmf.bodyToResourcesList());
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
                        if (mmf.sender.equals(askedForResources)) {
                            break;
                        }
                        System.out.println("Client " + mmf.sender + " requested resources");
                        superNodeController.send(REQUEST_RESOURCES);
                        resourceStrings = MulticastMessageFormat.resourceToString(clientResources);
                        askedForResources = mmf.sender;
                        break;
                    default:
                        System.out.println("Not expected input received:\n" + mmf);
                }
                // Remove client if it does not send 2 alive messages
//                for (ClientInfo client : clientNodes) {
//                    long currentTime = System.currentTimeMillis();
//                    if (client.lastMessageTime - currentTime > 11_000) {
//                        clientNodes.remove(client);
//                    }
//                }
            } catch (Exception ignored) {
            }
            if (System.in.available() > 0) {
                input = scanner.nextLine().trim().toUpperCase();
                if (EXIT.equals(input)) {
                    superNodeController.send(EXIT);
                    break;
                }
            }
        }
        superNodeController.end();
        scanner.close();
    }

    private void clientSentMessage(String name, long time) {
        for (ClientInfo client: clientNodes) {
            if (client.name.equals(name)) {
                client.lastMessageTime = time;
            }
        }
    }

    private boolean containsClient(String name) {
        return clientNodes.stream().anyMatch( (x) -> x.name.equals(name));
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

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java ClientNode <name> <unique port>");
            System.out.println("<name>: user name");
            System.out.println("<unique port>: integer client communication port");
            System.exit(1);
        }
        int clientPort = Integer.parseInt(args[1]);
        SuperNode node = new SuperNode(args[0], clientPort);
        node.run();
    }
}
