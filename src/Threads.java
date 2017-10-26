import java.time.Duration;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.lang.*;

public class Threads implements Runnable{

    private final Socket client;
    public CachedFile cf;
    String evictedFile;
    String cachedFile;
    String receivetime;
    public Duration timeElapsed;
    Instant start;
    Instant end;


    public Threads(Socket client) {
        this.client = client;
    }

    public static String[] splitFirstIndex(String s, char regex) {
        String[] returnValue = new String[2];
        try {
            char[] ourArray = s.toCharArray();
            int index = -1;
            for (int i = 0; i < ourArray.length; i++) {
                if (ourArray[i] == regex) {
                    index = i;
                    break;
                }
            }
            returnValue[0] = s.substring(0, index);
            returnValue[1] = s.substring(index + 1);
            return returnValue;
        } catch (Exception ex) {
            //System.out.println("Error splitting header: " + ex);
        } finally {
            return returnValue;
        }
    }

    private static String convertEDT_to_GMT(String s) {
        String newDate = "";
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            newDate = dateFormat.format(new Date(s));
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return newDate;
    }


    public void run() {
        Socket server = null;
        HttpRequest request = null;
        HttpResponse response = null;



        start = Instant.now();
        try {
            BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            request = new HttpRequest(fromClient);
        } catch (IOException e) {
            return;
        }

        receivetime = new Date().toString();

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            cf = MyServer.uncaching(request.URI, request);
            if (cf != null) {
                if (request.requestHeaders.get("If-Modified-Since") != null) {
                    Date d = new Date(request.requestHeaders.get("If-Modified-Since"));
                    String first = convertEDT_to_GMT(d.toString());
                    String second = convertEDT_to_GMT(new Date(cf.lastModified).toString());
                    String lastModified;
                    lastModified =  (new Date(first).getTime() > new Date(second).getTime()) ? first :  second;
                    request.requestHeaders.put("If-Modified-Since", lastModified);
                } else {
                    String lastModified = dateFormat.format(cf.lastModified);
                    String newTime = convertEDT_to_GMT(lastModified);
                    request.requestHeaders.put("If-Modified-Since", newTime);
                }
            }
        } catch (IOException ex) {
            //System.out.println("There was an error retrieving from the cache:" + ex);
        }

        try {
            server = new Socket(request.getHost(), request.getPort()); /* Raise socket */
            DataOutputStream toServer = new DataOutputStream(server.getOutputStream()); /* Create outputstream for server on socket */
            toServer.writeBytes(request.toString()); /* Write request to the outputstream*/
        } catch (UnknownHostException e) {
            //System.out.println("Unknown host: " + request.getHost());
            //System.out.println(e);
            return;
        } catch (IOException e) {
            //System.out.println("Error contacting host: " + e);
            return;
        }


        try {
            DataInputStream fromServer = new DataInputStream(server.getInputStream());
            response = new HttpResponse(fromServer);
            DataOutputStream toClient = new DataOutputStream(client.getOutputStream());



            switch (response.status) {
                case 200: {
                    toClient.writeBytes(response.toString());
                    toClient.write(response.body);


                    String[] pair = MyServer.caching(request, response);
                    if (pair[0] == "") {
                        String x = "";
                    }
                    evictedFile = pair[1];
                    cachedFile = pair[0];

                    toClient.flush();
                    break;
                }
                case 304: {
                    if (cf != null) {
                        if (cf.hit) {
                            cf.valid = true;
                            toClient.write(cf.content);
                            toClient.flush();
                        } else {
                            toClient.writeBytes(response.toString());
                            toClient.write(response.body);
                            toClient.flush();
                        }
                    } else {
                        toClient.writeBytes(response.toString());
                        toClient.write(response.body);
                        toClient.flush();
                    }
                    break;
                }
                default: {
                    toClient.writeBytes(response.toString());
                    toClient.write(response.body);
                    toClient.flush();
                }
            }
        } catch (IOException ex) {
            //System.out.println("Error writing to client:" + ex);
            //ex.printStackTrace();
        }
        end = Instant.now();
        timeElapsed = Duration.between(start, end);
        log(request, response);



    }

    public void log(HttpRequest request, HttpResponse response) {
        try {
            FileWriter fwriter = null;
            BufferedWriter bwriter = null;
            File f = new File("/Users/danielfrost/Desktop/frostkarkiproxyfinal/src/log.txt");
            String localAddress = InetAddress.getLocalHost().getHostAddress().toString();
            String remoteAddress = client.getRemoteSocketAddress().toString();
            if (remoteAddress.startsWith("/0:0:0:0")) {
                remoteAddress = localAddress;
            }
            fwriter = new FileWriter(f, true);
            bwriter = new BufferedWriter(fwriter);
            bwriter.write(System.lineSeparator());
            StringBuilder sb = new StringBuilder();
            sb.append(receivetime);
            if (timeElapsed != null) {
                sb.append("  " + timeElapsed.toMillis() + "ms");
            }
            if (cf != null) {
                sb.append("  Cache Hit");
                if (cf.valid) sb.append("  Validated:" + cf.fileName);
                else sb.append("  Validated:" + cf.fileName);
            } else {
                sb.append("  Cache Miss");
                if (cachedFile != "" && cachedFile != null) {
                    if (cachedFile.indexOf("_") > 0) {
                        cachedFile = cachedFile.substring(cachedFile.lastIndexOf("_"));
                    }
                    sb.append("  Cached:" + cachedFile);
                }
                if (evictedFile != "" && evictedFile != null) {
                    String algo = (MyServer.cacheAlgo == 1) ? "LRU" : "LFU";
                    evictedFile = MyServer.getName(evictedFile);
                    sb.append("  Evicted, " + algo + ":" + evictedFile.substring(evictedFile.lastIndexOf("_")));
                }
            }
            if (remoteAddress != null && remoteAddress != "") {
                sb.append("  " + remoteAddress);
            }
            if (response.status != 0) {
                sb.append("  " + response.status);
            }
            if (request.URI != null) {
                sb.append("  " + request.URI);
            }
            bwriter.write(sb.toString());
            bwriter.flush();
        } catch (Exception ex) {
            //System.out.println("Error logging file:" + ex);
        }
    }




}