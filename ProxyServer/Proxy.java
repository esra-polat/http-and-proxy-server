import java.io.*;
import java.net.*;
import java.util.*;

// The proxy listens on a port and creates new threads per request.-
public class Proxy implements Runnable {

    private ServerSocket serverSocket;
    // Semaphore for Proxy
    private volatile boolean stateBool = true;
    static HashMap<String, File> cacheMap;
    static ArrayList<Thread> threads;

    @SuppressWarnings("unchecked")
    public Proxy(int port) {

        cacheMap = new HashMap<>();
        threads = new ArrayList<>();
        // Start dynamic manager on a separate thread
        new Thread(this).start();

        try {
            File cacheList = new File("cacheList.txt");
            if (cacheList.exists()) {
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(cacheList));
                if (objectInputStream.readObject() instanceof HashMap) {
                    cacheMap = (HashMap<String, File>) objectInputStream.readObject();
                }
                objectInputStream.close();
            } else {
                System.out.println("There is no cache!");
            }
        } catch (IOException | ClassNotFoundException exception) { }

        try {
            // Create the Server Socket for the Proxy
            serverSocket = new ServerSocket(port);
            System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "..");
            stateBool = true;
        } catch (IOException ignored) {
        }
    }

    public void run() {
        Scanner s = new Scanner(System.in);
        String input;

        while (stateBool) {
            System.out.println("\"cache\" : Cache List\n\"exit\" : Terminate Proxy");
            input = s.nextLine();
            if (input.equalsIgnoreCase("cache")) {
                System.out.println("\n\nCached Sites in the Buffer");
                for (String key : cacheMap.keySet()) {
                    System.out.println(key);
                }
                System.out.println();
            } else if (input.equals("exit")) {
                stateBool = false;
                terminateProxy();
            } else {
                System.out.println("Command cannot be understood, try again.");
            }
        }
        s.close();
    }

    // Listens to port and accepts new socket connections
    // Creates a new thread to process the request
    public void listen() {

        while (stateBool) {
            try {
                Socket socket = serverSocket.accept();
                Thread thread = new Thread(new Request(socket));
                threads.add(thread);
                thread.start();
            } catch (SocketException e) {
                System.out.println("Server closed");
            } catch (IOException ignored) {
            }
        }
    }

    private void terminateProxy() {
        System.out.println("\nTerminating Proxy Server..");
        stateBool = false;
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("cacheList.txt"));
            objectOutputStream.writeObject(cacheMap);
            objectOutputStream.close();

            try {
                for (Thread thread : threads) {
                    if (thread.isAlive()) {
                        System.out.print("Waiting on " + thread.getId() + " to close..");
                        thread.join();
                        System.out.println(" closed");
                    }
                }
            } catch (InterruptedException ignored) {
            }
        } catch (IOException ignored) {
        }
        try {
            System.out.println("Terminating Connection");
            serverSocket.close();
        } catch (Exception ignored) {
        }
    }

    public static File getCachedPage(String url) {
        return cacheMap.get(url);
    }

    public static void addCachedPage(String urlString, File fileToCache) {
        cacheMap.put(urlString, fileToCache);
    }
}