import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class SendFile implements Runnable, Closeable {

    String ip;
    InetAddress address;
    int port;
    boolean keepTrying = false;

    File file;
    BufferedReader reader;

    public SendFile(String ip, int port, String filePath) throws IOException {
        this.ip = ip;
        this.address = InetAddress.getByName(ip);
        this.port = port;
        this.file = new File(filePath);
        this.reader = new BufferedReader(new FileReader(file));
    }

    public SendFile(String ip, int port, String filePath, boolean keepTrying) throws IOException {
        this.ip = ip;
        this.address = InetAddress.getByName(ip);
        this.port = port;
        this.file = new File(filePath);
        this.reader = new BufferedReader(new FileReader(file));
        this.keepTrying = keepTrying;
    }

    @Override
    public void run() {
        boolean errorOcurred = false;
        do {
            String line = "";
            try {
                // output socket data
                Socket socket = new Socket(ip, port);
                OutputStream outputStream = socket.getOutputStream();
                // send file name
                outputStream.write((file.getName() + "\n").getBytes());
                outputStream.flush();
                // send file bytes
                FileInputStream fileInputStream = new FileInputStream(file);
                outputStream.write(fileInputStream.readAllBytes());
                outputStream.flush();
                // end output
                outputStream.close();
                fileInputStream.close();
                socket.close();
                errorOcurred = false;
            } catch (Exception e) {
                errorOcurred = true;
                e.printStackTrace();
            }
            // See how much memory was being used
            //finally {
            //    System.out.println("Meg used="+(Runtime.getRuntime().totalMemory()-
            //            Runtime.getRuntime().freeMemory())/(1000*1000)+"M");
            //    System.gc();
            //    System.out.println("Meg used="+(Runtime.getRuntime().totalMemory()-
            //            Runtime.getRuntime().freeMemory())/(1000*1000)+"M");
            //}
        } while (keepTrying && errorOcurred);
        System.out.println("File successfully sent.");
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    // For testing purposes
    public static void main(String[] args) throws IOException, InterruptedException {
        // necessary info
        String ip = "127.0.0.1";
        int port = 1234;
        String rtfFile = "/Users/luizpedrofranciscattoguerra/Desktop/planning.rtf";
        String imageFile = "/Users/luizpedrofranciscattoguerra/Desktop/TestImage.png";

        SendFile sendFile = new SendFile(ip, port, imageFile);
        sendFile.run();
    }
}
