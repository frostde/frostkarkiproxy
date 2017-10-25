import java.io.*;

public class HttpResponse {
    final static String CRLF = "\r\n";
    final static int BUF_SIZE = 8192;
    final static int MAX_OBJECT_SIZE = 300000;
    String version;
    int status = 0;
    String statusLine = "";
    String headers = "";
    byte[] body = new byte[MAX_OBJECT_SIZE];

    public HttpResponse(DataInputStream fromServer) {
        int length = -1;
        boolean gotStatusLine = false;
        String[] tokens;

        try {
            if (fromServer != null) {
                String line = fromServer.readLine();
                if (line != null) {
                    while (line.length() != 0) {
                        if (!gotStatusLine) {
                            statusLine = line;
                            tokens = statusLine.split(" ");
                            status = Integer.parseInt(tokens[1]);
                            gotStatusLine = true;
                        } else {
                            headers += line + CRLF;
                        }

                        if (line.startsWith("Content-Length:") ||
                                line.startsWith("Content-length:")) {
                            String[] tmp = line.split(" ");
                            length = Integer.parseInt(tmp[1]);
                        }
                        line = fromServer.readLine();
                    }
                }
            }
        } catch (IOException e) {
            //System.out.println("Error generating response:" + e);
        }

        try {
            int bytesRead = 0;
            byte buf[] = new byte[BUF_SIZE];
            boolean loop = false;


            if (length == -1) {
                loop = true;
            }


            while (bytesRead < length || loop) {
                int res = fromServer.read(buf, 0, BUF_SIZE);
                if (res == -1) {
                    break;
                }

                for (int i = 0;
                     i < res && (i + bytesRead) < MAX_OBJECT_SIZE;
                     i++) {
                    body[bytesRead + i] = buf[i];
                }
                bytesRead += res;
            }
        } catch (IOException e) {
            //System.out.println("Error reading response body: " + e);
            return;
        }


    }

    public String toString() {
        String res = "";

        res = statusLine + CRLF;
        res += headers;
        res += CRLF;

        return res;
    }

}
