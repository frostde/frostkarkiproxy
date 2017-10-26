import java.io.*;
import java.util.*;

public class HttpRequest {
    final static String CRLF = "\r\n";
    final static int HTTP_PORT = 80;
    String method;
    String URI;
    String version;
    String headers = "";
    private String host;
    private int port;
    Map<String, String> requestHeaders;

    public HttpRequest(BufferedReader from) {
        requestHeaders = new HashMap<>();
        String[] tmp;
        if (from != null) {
            String firstLine = "";
            try {
                firstLine = from.readLine();
            } catch (IOException e) {
            }
            if (firstLine != "" && firstLine != null) {
                tmp = firstLine.split(" ");

                method = tmp[0];
                URI = tmp[1];
                version = tmp[2];


                if (!method.equals("GET")) {
                    //System.out.println("Error: Method not GET");
                }
            }
        }
        try {
            if (from != null) {
                String line = from.readLine();
                if (line != null) {
                    while (line.length() != 0) {
                        headers += line + CRLF;
                        if (line.indexOf(":") > 0){
                            tmp = Threads.splitFirstIndex(line, ':');
                            requestHeaders.put(tmp[0], tmp[1]);
                        }
                        if (line.startsWith("Host:")) {
                            tmp = line.split(" ");
                            if (tmp[1].indexOf(':') > 0) {
                                String[] tmp2 = tmp[1].split(":");
                                host = tmp2[0];
                                port = Integer.parseInt(tmp2[1]);
                            } else {
                                host = tmp[1];
                                port = HTTP_PORT;
                            }
                        }
                        line = from.readLine();
                    }
                }
            }
        } catch (IOException e) {
            return;
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }


    public String toString() {
        String req = "";

        req = method + " " + URI + " " + version + CRLF;
        req += headers;
        //req += "Connection: close" + CRLF;
        req += CRLF;


        return req;
    }

}