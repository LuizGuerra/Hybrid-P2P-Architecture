import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.List;

public class MulticastController {
    final int port;
    final InetAddress group;
    final MulticastSocket socket;
    final String name;

    public MulticastController(String name, String group, int port) throws IOException {
        this.name = name;
        this.port = port;
        this.socket = new MulticastSocket(port);
        this.group = InetAddress.getByName(group);
        socket.joinGroup(this.group);
    }

    public void send(String request) throws IOException {
        byte[] message = (request + " " + name).getBytes();
        DatagramPacket packet = new DatagramPacket(message, message.length, group, port);
        socket.send(packet);
    }

    public void send(String request, String body) throws IOException {
        byte[] message = (request + " " + name + " " + body).getBytes();
        DatagramPacket packet = new DatagramPacket(message, message.length, group, port);
        socket.send(packet);
    }

    public void send(String request, List<Resource> resources) throws IOException {
        send(request, MulticastMessageFormat.resourceToString(resources));
    }

    public void send(MulticastMessageFormat mmf) throws IOException {
        send(mmf.request, mmf.body);
    }

    public void send(String request, Long time) throws IOException {
        send(request, time.toString());
    }

    public String receive() throws IOException {
        // Read
        byte[] entry = new byte[16_384]; // 2^14 bytes
        DatagramPacket packet = new DatagramPacket(entry, entry.length);
        socket.setSoTimeout(100);
        socket.receive(packet);
        // Parse
        return new String(packet.getData(), 0, packet.getLength());
    }

    // TODO: read big packet
//        try {
//            // Is big packet
//            return receiveBigPacket(
//                    Integer.parseInt(String.valueOf(message.charAt(0))),
//                    entry, packet);
//        } catch (Exception e) {
//            // Is normal packet
//            return message;
//        }
//    public String receiveBigPacket(int size, byte[] entry, DatagramPacket packet) {
//        StringBuilder message = new StringBuilder();
//        try {
//            for(int i = 1; i < size; i++) {
//                socket.receive(packet);
//                message.append(new String(packet.getData(), i, packet.getLength()));
//            }
//        } catch (Exception e) {
//            System.out.println("Exception parsing big packet");
//            e.printStackTrace();
//            message = new StringBuilder();
//        }
//        return message.toString();
//    }

    public void end() throws IOException {
        socket.leaveGroup(group);
        socket.close();
    }
}
