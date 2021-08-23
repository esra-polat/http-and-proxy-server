import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    private static final int PROXY_PORT = 8888;

    public static void main(String[] args) throws IOException {

        Files.createDirectories(Paths.get("./cached/"));

        Proxy myProxy = new Proxy(PROXY_PORT);
        myProxy.listen();
    }
}
