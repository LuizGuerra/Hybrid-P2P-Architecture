import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveFile implements Runnable, Closeable {

    static String thisPath = System.getProperty("user.dir");

    int port;

    ServerSocket serverSocket;

    public ReceiveFile(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
    }

    @Override
    public void close() throws IOException {
        this.serverSocket.close();
    }

    @Override
    public void run() {
        byte[] bytes;
        // input socket data
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String fileName = reader.readLine();
                System.out.println("Receiving data...");
                System.out.println("File name: " + fileName);

                byte[] buffer = new byte[1024*8];
                int bytesRead;

                File downloadPath = new File(thisPath + "/downloads/");
                File newFile = new File(thisPath + "/downloads/"+ fileName);
                if(!downloadPath.exists()) { downloadPath.mkdirs(); }
                OutputStream outputStream = new FileOutputStream(newFile);

                while ( (bytesRead = inputStream.read(buffer)) != -1 ) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                System.out.println("Successfully created file");

                System.out.println();
                socket.close();
                inputStream.close();
                outputStream.close();
            } catch (Exception e) {
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
        }
    }

    public static void main(String[] args) throws IOException {
        // necessary info
        int port = 1234;
        ReceiveFile receiveFile = new ReceiveFile(port);
        receiveFile.run();
    }
}
