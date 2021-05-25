import java.io.IOException;
import java.util.Scanner;

public class ClientNode {

    static final String CLIENT_HELLO = "CLIENT_HELLO";
    static final String CLIENT_EXIT = "CLIENT_HELLO";
    static final String REQUEST_RESOURCES = "REQUEST_RESOURCES";

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
        System.out.println("Sending hello...");
        controller.send(CLIENT_HELLO);
        System.out.println("Hello sent.\n");
        Scanner scanner = new Scanner(System.in);
        String input = "";
        String superNode = "";

        while (true) {
            // Read if anything arrived
            try {
                String received = controller.receive();
                MulticastMessageFormat mmf = new MulticastMessageFormat(received);
                if(mmf.sender.equals(name)) {
                    continue;
                }
                switch (mmf.request) {
                    case CLIENT_HELLO:
                        System.out.println("Received hello from " + mmf.sender);
                        if(superNode.isEmpty()) {
                            superNode = mmf.sender;
                            System.out.println("Sending back a hello");
                            controller.send(CLIENT_HELLO);
                            System.out.println("Connection with " + mmf.sender + " established.");
                        }
                        System.out.println();
                        break;
                    default:
                        System.out.println("Not expected input received:\n" + mmf.toString());
                }
            } catch (Exception e) {}
        }
    }

    public static void main(String[] args) throws IOException {
        if(args.length != 2) {
            System.out.println("Usage: java ClientNode <name> <supernode port>");
            System.exit(1);
        }
        int superNodePort = Integer.parseInt(args[1]);
        ClientNode node = new ClientNode(args[0], superNodePort);
        node.run();
    }
}
