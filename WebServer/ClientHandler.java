import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket clientSocket) throws IOException {
        this.client = clientSocket;
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            while (true) {
                String request = in.readLine();
                System.out.println(request);
                //Checking our URI is not GET, sending 501 html
                if (request.contains("POST") || request.contains("DELETE") || request.contains("HEAD") || request.contains("PUT")) {
                    sendPage("501");
                    break;
                }
                else if(request.contains("GET")){ //Controls for GET request
                    String[] requestHold = request.split("/");
                    String [] numWanted = requestHold[1].split(" ");
                    String numberParsed = numWanted[0];
                    try {
                        int convertedNum = Integer.parseInt(numberParsed);
                        //If URI is valid, sending 200 html with requested size
                        if(convertedNum<=20000 && convertedNum>=100){
                            createHTMLpage(convertedNum);
                            sendPage("200");
                            break;
                        }
                        else{ //If URI is not in interval, sending 400 html
                            out.println("HTTP/1.1 400 Bad Request - Not in interval");
                            sendPage("400");
                            break;
                        }
                    }
                    catch(NumberFormatException er) { //If URI is not an integer, sending 400 html
                        if(numberParsed.matches(".*\\d.*"))
                            out.println("HTTP/1.1 400 Bad Request - Number not an integer.");
                        else //If URI includes numbers with letters, sending 400 html
                            out.println("HTTP/1.1 400 Bad Request");
                        sendPage("400");
                        break;
                    }
                }
                else{ //If there is no appropriate state, sending 400 html
                    out.println("HTTP/1.1 400 Bad Request");
                    sendPage("400");
                    break;
                }

            }
        } catch (IOException e) {
            try {
                this.client.shutdownInput();
                this.client.shutdownOutput();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    //Creates html page with size of given integer number
    public void createHTMLpage(int bytesWanted){
        try {
            File myObj = new File("src\\200.html");
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        try {
            FileWriter myWriter = new FileWriter("src\\200.html");
            //myWriter.write("<!DOCTYPE html>" + "\n");
            myWriter.write("<html>" + "\n");
            myWriter.write("<head>" + "\n");
            myWriter.write("<title>"+bytesWanted+" bytes</title>" + "\n");
            myWriter.write("</head>" + "\n");
            myWriter.write("<body>");
            int count = 73 + Integer.toString(bytesWanted).length();
            int letterNum = bytesWanted-count;
            for(int j=0; j<letterNum; j++){
                myWriter.write("D");
            }
            myWriter.write("</body>" + "\n");
            myWriter.write("</html>" + "\n");
            myWriter.write("\n"+"\n"+"\n"+"\n"+"\n"+"\n"+"\n");
            myWriter.close();
            System.out.println("Successfully wrote to the HTML file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    //Sending appropriate html pages to socket with valid headers
    public void sendPage(String status) throws Exception {
        System.out.println("Page writter called");

        File index = new File("src\\"+status+".html");

        BufferedReader reader = new BufferedReader(new FileReader(index));// grab a file and put it into the buffer
        // print HTTP headers
        /*if(status.equals("400")){
            out.println("HTTP/1.1 400 Bad Request");
        }*/
        if(status.equals("501")){
            out.println("HTTP/1.1 501 Not Implemented");
        }
        else{
            out.println("HTTP/1.1 200 Success");
        }

        out.println("Content-Type: text/html");
        out.println("Content-Length: " + index.length());
        out.println("\r\n");
        String line = reader.readLine();// line to go line by line from file
        while (line != null)// repeat till the file is read
        {
            out.println(line);// print current line

            line = reader.readLine();// read next line
        }
        reader.close();// close the reader
    }
}
