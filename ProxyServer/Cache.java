import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class Cache {

    //Sending cache page
    public static void sendCachedPage(File file) {
        try {
            BufferedReader cacheReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            Request.clientWriter.write("HTTP/1.0 200 Ok\n" + "Proxy-agent: ProxyServer/1.0\n" + "\r\n");
            Request.clientWriter.flush();
            String line;
            while ((line = cacheReader.readLine()) != null) { Request.clientWriter.write(line); }
            Request.clientWriter.flush();
            cacheReader.close();

            if (Request.clientWriter != null) { Request.clientWriter.close(); }

        } catch (IOException ignored) { }
    }

    //Saving to cache page
    public static boolean saveToCache(String urlString) {

        try {
            int fileExtIndex = urlString.lastIndexOf(".");
            String fileExtTmp = urlString.substring(fileExtIndex);
            String fileName = urlString.substring(0, fileExtIndex);
            fileName = fileName.substring(fileName.indexOf('.') + 1);
            fileName = fileName.replace("/", "__");
            fileName = fileName.replace('.', '_');

            if (fileExtTmp.contains("/")) {
                fileExtTmp = fileExtTmp.replace("/", "__");
                fileExtTmp = fileExtTmp.replace('.', '_');
                fileExtTmp += ".html";
            }

            fileName = fileName + fileExtTmp;
            boolean cacheActive = true;
            File cacheFile = null;
            BufferedWriter cacheWriter = null;

            try {
                cacheFile = new File("cached/" + fileName);
                cacheWriter = new BufferedWriter(new FileWriter(cacheFile));
            } catch (IOException e) {
                System.out.println("Couldn't cache: " + fileName);
                cacheActive = false;
            } catch (NullPointerException e) { }

            URL remoteURL = new URL(urlString);

            HttpURLConnection proxyServerConnection = (HttpURLConnection) remoteURL.openConnection();
            proxyServerConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            proxyServerConnection.setRequestProperty("Content-Language", "en-US");
            proxyServerConnection.setUseCaches(false);
            proxyServerConnection.setDoOutput(true);

            BufferedReader proxyReader = new BufferedReader(new InputStreamReader(proxyServerConnection.getInputStream()));

            String line = "HTTP/1.0 200 Ok\n" + "Proxy-agent: ProxyServer/1.0\n" + "\r\n";
            Request.clientWriter.write(line);

            while ((line = proxyReader.readLine()) != null) {
                Request.clientWriter.write(line);
                if (cacheActive) {
                    assert cacheWriter != null;
                    cacheWriter.write(line);
                }
            }
            Request.clientWriter.flush();
            proxyReader.close();

            if (cacheActive) {
                assert cacheWriter != null;
                cacheWriter.flush();
                Proxy.addCachedPage(urlString, cacheFile);
            }

            if (cacheWriter != null) { cacheWriter.close(); }

            if (Request.clientWriter != null) { Request.clientWriter.close(); }

        } catch (Exception e) {
            return false;
        }
        return true;
    }
}