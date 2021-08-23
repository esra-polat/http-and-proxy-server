import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

// Request processes each client's request through the proxy server
public class Request implements Runnable {

    public static Socket clientSocket;
    public static BufferedReader clientReader;
    public static BufferedWriter clientWriter;
    Thread clientThreads; // to transmit data read from the client to the server
    private final PrintWriter printWriter;

    public Request(Socket clientSocket) throws IOException {
        printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        Request.clientSocket = clientSocket;
        try {
            Request.clientSocket.setSoTimeout(2000);
            clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (IOException ignored) { }
    }

    // It reads and examines the reqTmp string
    // and calls the appropriate method according to request
    public void run() {
        // Get Request from client
        String reqTmp = null;
        try {
            reqTmp = clientReader.readLine();
        } catch (IOException ignored) { }

        // Parse URL
        assert reqTmp != null;
        String request = reqTmp.substring(0, reqTmp.indexOf(' '));
        String urlTmp = reqTmp.substring(reqTmp.indexOf(' ') + 1);
        urlTmp = urlTmp.substring(0, urlTmp.indexOf(' '));

        if (!urlTmp.startsWith("http")) {
            String temp = "http://";
            urlTmp = temp + urlTmp;
        }

        // Check request type
        if (request.equals("CONNECT")) {
            System.out.println("HTTPS Request for : " + urlTmp + "\n");
            anyWebServerRequest(urlTmp);
        } else {
            // Check cache
            File file;
            if ((file = Proxy.getCachedPage(urlTmp)) != null) {
                Cache.sendCachedPage(file);
            } else {
                String tempUrl = urlTmp.split("/")[3];
                int convertedNum = Integer.parseInt(tempUrl);
                if(convertedNum<=9999){
                    System.out.println("HTTP GET : " + urlTmp + "\n");
                    boolean value = Cache.saveToCache(urlTmp);
                    if (!value){ //bağlantı kontrolü için
                        try {
                            sendCache("404");
                        } catch (Exception ignored) { }
                    }
                }
                else{ //boyut kontrolü için
                    try {
                        sendCache("414");
                    } catch (Exception ignored) { }
                }
            }
        }
    }

    // Sends the specified cached file to the client
    public void sendCache(String status) throws Exception {

        File index = new File("./"+status+".html");

        BufferedReader reader = new BufferedReader(new FileReader(index));// grab a file and put it into the buffer

        if(status.equals("414")){
            printWriter.println("HTTP/1.1 414 Request-URI Too Long");
        }
        else{
            printWriter.println("HTTP/1.1 404 Not Found");
        }

        printWriter.println("Content-Type: text/html");
        printWriter.println("Content-Length: " + index.length());
        printWriter.println("\r\n");
        String line = reader.readLine();// line to go line by line from file
        while (line != null)// repeat till the file is read
        {
            printWriter.println(line);// print current line
            line = reader.readLine();// read next line
        }
        reader.close();// close the reader
        printWriter.close();
    }

    // Sends the contents of the file specified by the urlTmp string to the client
    private void anyWebServerRequest(String urlTmp) {
        String url = urlTmp.substring(7);
        String[] arr = url.split(":");
        url = arr[0];
        int port = Integer.parseInt(arr[1]);

        try {
            for (int i = 0; i < 5; i++) {
                clientReader.readLine();
            }

            InetAddress inetAddress = InetAddress.getByName(url);
            Socket proxySocket = new Socket(inetAddress, port);
            proxySocket.setSoTimeout(5000);
            clientWriter.write("HTTP/1.0 200 Connection established\r\n" + "Proxy-Agent: ProxyServer/1.0\r\n" + "\r\n");
            clientWriter.flush();

            clientThreads = new Thread(new HttpsTransmit(clientSocket.getInputStream(), proxySocket.getOutputStream()));
            clientThreads.start();

            try {
                byte[] buffer = new byte[4096];
                int read;
                do {
                    read = proxySocket.getInputStream().read(buffer);
                    if (read > 0) {
                        clientSocket.getOutputStream().write(buffer, 0, read);
                        if (proxySocket.getInputStream().available() < 1) {
                            clientSocket.getOutputStream().flush();
                        }
                    }
                } while (read >= 0);
            } catch (IOException ignored) { }

            proxySocket.close();

            if (clientWriter != null) {
                clientWriter.close();
            }

        } catch (SocketTimeoutException e) {
            try {
                clientWriter.flush();
            } catch (IOException ignored) { }
        } catch (Exception ignored) { }
    }
}