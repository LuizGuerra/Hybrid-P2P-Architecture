import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Objects;

public class Resource {
    public final String hash;
    public final String resourceName;
    public final String ip;

    public Resource(String file, String ip) throws Exception {
        this.hash = fileToHash(file);
        String[] str = file.split("/");
        this.resourceName = str[str.length-1];
        this.ip = ip;
    }

    public Resource(String hash, String resourceName, String ip) {
        this.hash = hash;
        String[] str = resourceName.split("/");
        this.resourceName = str[str.length-1];
        this.ip = ip;
    }

    public static Resource parseString(String str) {
        String[] vars = str.split(";");
        try {
            return new Resource(vars[2], vars[1], vars[0]);
        } catch (Exception e) {
            System.out.println("Found problem parsing string: " + str);
            System.out.println("Probable cause might be that " +
                    "receiving package is too small. Check packet header.");
            e.printStackTrace();
        }
        return null;
    }

    public String convertForMulticast() {
        return resourceName + ";" +  ip + ";" + hash;
    }

    String fileToHash(String fileName) throws Exception {
        MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
        File file = new File(fileName);
        return getFileChecksum(shaDigest, file);
    }

    private static String getFileChecksum(MessageDigest digest, File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }
        fis.close();
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return hash.equals(resource.hash) && resourceName.equals(resource.resourceName) && ip.equals(resource.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, resourceName, ip);
    }

    @Override
    public String toString() {
        return "Resource{" +
                "hash='" + hash + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}
