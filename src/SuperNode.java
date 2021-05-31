import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    static final String HEARTBEAT = "HEARTBEAT";
    static final String SYSTEM_EXIT = "SYSTEM_EXIT";
    static final String FILE_REQUEST = "FILE_REQUEST";

    int clientNodesPort;
    Set<ClientInfo> clientNodes;
    List<Resource> clientResources;
    String clientNodeGroup = "224.0.2.2";
    Timer timer;

    public SuperNode(String name, int port) throws IOException {
        this.superNodeController = new MulticastController(name, superNodeGroup, superNodePort);
        this.clientsNodeController = new MulticastController(name, clientNodeGroup, port);
        this.superNodes = new HashSet<>();
        this.clientNodes = new HashSet<>();
        this.clientResources = new ArrayList<>();
        this.name = name;
        this.clientNodesPort = port;
        timer = new Timer();
    }

    class ClientIsAlive extends TimerTask {
        @Override
        public void run() {
            // Remove client if it does not send 2 alive messages
            for (ClientInfo client : clientNodes) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - client.lastMessageTime > 11_000) {
                    System.out.println("\nClient " + client.name + " failed to send 2 alive messages");
                    try {
                        clientsNodeController.send(SYSTEM_EXIT, client.name);
                    } catch (IOException e) {
                        System.out.println("Failed to send exit message to " + client.name);
                    }
                    clientNodes.remove(client);
                    System.out.println(client.name + " was removed.");
                }
            }
        }
    }

    public void run() throws IOException {
        timer.schedule(new ClientIsAlive(), 5_000, 5_000);
        System.out.println("Send EXIT to exit.\n");
        System.out.println("Sending hello...");
        superNodeController.send(HELLO);
        System.out.println("Hello sent.\n");
        Scanner scanner = new Scanner(System.in);
        String input;

        // Is waiting super node resources or request from other supernode
        String askedForResources = "";
        StringBuilder resourceStrings = new StringBuilder();
        int counter = 0;

        while (true) {
            // If can send resources back to client
            if (!askedForResources.isEmpty() && counter == superNodes.size()) {
                System.out.println("\nReceived all resources. Sending package...");
                clientsNodeController.send(COMMIT_RESOURCES, resourceStrings.toString());
                askedForResources = "";
                resourceStrings = new StringBuilder();
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
                            resourceStrings.append(" ").append(mmf.body);
                            counter++;
                        }
                        break;
                    case FILE_REQUEST:
                        System.out.println("Repassing file request from " + mmf.sender + ".");
                        clientsNodeController.send(mmf);
                        break;
                    default:
                        System.out.println("Not expected input received from supernodes:\n" + mmf);
                }
            } catch (Exception ignored) {
            }
            // Read if anything arrived from clients
            try {
                String received = clientsNodeController.receive();
                MulticastMessageFormat mmf = new MulticastMessageFormat(received);
                // If message received was mine, exit request
                if (mmf.sender.equals(name)) { //  || !containsClient(name)
                    continue;
                }
                // If sender isnt on list and isnt saying hello, exit request
                if(!containsClient(mmf.sender) && !mmf.request.equals(CLIENT_HELLO)) {
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
                        resourceStrings = new StringBuilder(MulticastMessageFormat.resourceToString(clientResources));
                        askedForResources = mmf.sender;
                        break;
                    case HEARTBEAT:
                        System.out.println("\nReceived " + mmf.sender + " heartbeat");
                        updateHeartBeat(mmf.sender, mmf.bodyToTime());
                        break;
                    case FILE_REQUEST:
                        System.out.println("Repassing file request from " + mmf.sender + ".");
                        superNodeController.send(mmf);
                        clientsNodeController.send(mmf);
                        break;
                    default:
                        System.out.println("Not expected input received from clients:\n" + mmf);
                }
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
        timer.cancel();
    }

    private void clientSentMessage(String name, long time) {
        for (ClientInfo client: clientNodes) {
            if (client.name.equals(name)) {
                client.lastMessageTime = time;
            }
        }
    }

    private void updateHeartBeat(String name, Long time) {
        clientNodes.stream()
                .filter(x -> x.name.equals(name))
                .collect(Collectors.toList())
                .get(0).lastMessageTime = time;
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
