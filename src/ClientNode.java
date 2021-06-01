import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientNode {

    static final String HELLO = "HELLO";
    static final String EXIT = "EXIT";

    static final String CLIENT_HELLO = "CLIENT_HELLO";
    static final String CLIENT_EXIT = "CLIENT_EXIT";
    static final String REQUEST_RESOURCES = "REQUEST_RESOURCES";
    static final String COMMIT_RESOURCES = "COMMIT_RESOURCES";
    static final String HEARTBEAT = "HEARTBEAT";
    static final String SYSTEM_EXIT = "SYSTEM_EXIT";
    static final String FILE_REQUEST = "FILE_REQUEST";

    String ip = InetAddress.getLocalHost().getHostAddress();

    String group = "224.0.2.2";
    MulticastController controller;
    String name;
    int superNodePort;
    boolean wantResources;

    Timer timer;

    Scanner scanner;
    List<Resource> localResources;

    public ClientNode(String name, int superNodePort) throws IOException {
        this.controller = new MulticastController(name, group, superNodePort);
        this.name = name;
        this.superNodePort = superNodePort;
        timer = new Timer();
        wantResources = false;
    }

    class Heartbeat extends TimerTask {
        @Override
        public void run() {
            try {
                controller.send(HEARTBEAT, System.currentTimeMillis());
                System.out.println("Heartbeat sent");
            } catch (Exception e) {
                System.out.println("An error occurred while sending a heartbeat");
            }
        }
    }

    public void run() throws IOException {
        timer.schedule(new Heartbeat(), 5_000, 5_000);

        System.out.println("Usage:");
        System.out.println("Send EXIT to exit.");
        System.out.println("Send R to ask for all resources.\n");
        System.out.println("Send E to stop sending heartbeats to server.\n");

        String input = "";
        String superNode = "";
        localResources = getResources();

        System.out.println("Sending hello...");
        controller.send(CLIENT_HELLO, localResources);
        System.out.println("Hello sent.\n");
        scanner = new Scanner(System.in);

        boolean onlyWatching = false;

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
                        if (wantResources) {
                            System.out.println("Received all resources from supernode:");
                            Resource resource = selectResource(mmf.bodyToResourcesList());
                            if(resource == null) { break; }
                            System.out.println("Selected " + resource.resourceName);
                            receiveFile(resource);
                        }
                        break;
                    case SYSTEM_EXIT:
                        System.out.println("Received system exit from supernode");
                        System.out.println("Exiting...");
                        input = EXIT;
                        break;
                    case FILE_REQUEST:
                        String[] vars = mmf.body.split("\\s");
                        if(vars[0].equals(ip) && !wantResources) {
                            System.out.println("Received file request from " + mmf.sender);
                            System.out.println("File: " + vars[3]);
                            sendFile(mmf);
                        }
                        break;
                    default:
                        System.out.println("Not expected input received:\n" + mmf);
                }
            } catch (Exception ignored) {
            }
            if (!onlyWatching && System.in.available() > 0) {
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
                        break;
                    case "E":
                        System.out.println("Silently exiting application while still hearing supernode");
                        onlyWatching = true;
                        timer.cancel();
                        break;
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
        timer.cancel();
        System.out.println("Application successfully ended.");
    }

    private void receiveFile(Resource r) throws IOException {
        System.out.println("Asking for selected resource.");
        controller.send(
                FILE_REQUEST,
                r.ip + " " + ip + " " + 1234 + " " +  r.resourceName
        );
        ReceiveFile receiveFile = new ReceiveFile(1234);
        try {
            receiveFile.run();
            receiveFile.close();
        } catch (Exception ignored) {}
        wantResources = false;
    }

    // Body: File owner ip + destination ip + port + file name
    private void sendFile(MulticastMessageFormat mmf) throws IOException {
        System.out.println("Attempting to send file");
        String[] vars = mmf.body.split("\\s");
        String ip = vars[1];
        Integer destinPort = Integer.parseInt(vars[2]);
        String filePath = localResources.stream()
                .filter(x -> x.resourceName.contains(vars[3]))
                .collect(Collectors.toList()).get(0).resourceName;
        if(filePath == null) {
            System.out.println("Requested file (" + vars[3] + ") no longer exists.");
            return;
        }
        SendFile sendFile = new SendFile(ip, destinPort, filePath, true);
        try {
            sendFile.run();
            sendFile.close();
        } catch (Exception ignored) {}
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

    Resource selectResource(List<Resource> resources) {
        List<Resource> resourcesThatUserDontHave = resources.stream()
                .filter(x -> !localResources.contains(x))
                .collect(Collectors.toList());
        if(resourcesThatUserDontHave.isEmpty()) {
            System.out.println("You already are up-to-date with other users!");
            return null;
        }
        System.out.println("Select a resource typing it's number:");
        for(int i = 0; i < resourcesThatUserDontHave.size(); i++) {
            System.out.println("[" + (i+1) + "] " + resourcesThatUserDontHave.get(i).resourceName);
        }
        int index = 0;
        while (index == 0) {
            try {
                index = Integer.parseInt(scanner.nextLine());
            } catch (Exception e) {
                System.out.println("Please type a valid number");
            }
            if (index == 0) { continue; }
            if (index < 1 || index > resourcesThatUserDontHave.size()) {
                index = 0;
                System.out.println("Please type a value between 1 and " + resourcesThatUserDontHave.size());
            }
        }
        return resourcesThatUserDontHave.get(index-1);
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
