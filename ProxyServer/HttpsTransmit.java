import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// Listen to data from client and transmits it to server
class HttpsTransmit implements Runnable {

    InputStream proxyClientIS;
    OutputStream proxyServerOS;

    public HttpsTransmit(InputStream proxyClientIS, OutputStream proxyServerOS) {
        this.proxyClientIS = proxyClientIS;
        this.proxyServerOS = proxyServerOS;
    }

    public void run() {
        try {
            // Read byte by byte from client and send directly to server
            byte[] buffer = new byte[4096];
            int read;
            do {
                read = proxyClientIS.read(buffer);
                if (read > 0) {
                    proxyServerOS.write(buffer, 0, read);
                    if (proxyClientIS.available() < 1) {
                        proxyServerOS.flush();
                    }
                }
            } while (read >= 0);
        } catch (IOException ignored) { }
    }
}