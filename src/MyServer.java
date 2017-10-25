import java.net.*;
import java.io.*;
import java.lang.*;

public class MyServer {
    private static int port;
    private static ServerSocket socket;
    private static LRUCache lrucache;
    public static int cacheMiss = 0;
    public static int cacheHit = 0;
    public static int cacheAlgo = 1;
    public static LFUCache lfuCache;
    public static int capacity = 50;



    public static String getName(String s) {
        if (s.indexOf("?") > 0) {
            s = s.replace(s.substring(s.indexOf("?")), "");
        }
        s = s.substring(s.lastIndexOf("/"));
        if (s.indexOf(".") > 0) {
            s = s.replace(s.substring(s.lastIndexOf(".")), "").trim();
        }
        if (s.equals("/")) s = "_index";
        if (s.startsWith("/")) s = s.replace("/", "_");
        return s;
    }


    public synchronized static CachedFile uncaching(String URI) throws IOException{
        File cachedFile;
        FileInputStream fileIn;
        String hashfile;
        CachedFile cf;


            if ((hashfile = get(getName(URI))) != "") {
                cf = new CachedFile();
                cacheHit++;
                cachedFile = new File(hashfile);
                fileIn = new FileInputStream(cachedFile);
                cf.content = new byte[(int) cachedFile.length()];
                fileIn.read(cf.content);
                cf.lastModified = cachedFile.lastModified();
                cf.fileName = hashfile.substring(hashfile.lastIndexOf("/"));
                return cf;
            } else {
                cacheMiss++;
                cf = null;
                return cf;
            }


    }


    public static void init(int p) {
        port = p;
        try {
            if (cacheAlgo == 1) {
                lrucache = new LRUCache(capacity);
            } else {
                lfuCache = new LFUCache(capacity);
            }
            socket = new ServerSocket(port);
        } catch (IOException e) {
            System.exit(-1);
        }
    }

    public static String get(String URI) {
        String returnValue;
        switch (cacheAlgo) {
            case 1: {
                returnValue = lrucache.get(URI);
                break;
            }
            default: {
                returnValue = lfuCache.get(URI);
                break;
            }
        }
        return returnValue;
    }



    public static void main(String args[]) {
        int myPort = 8900;
        File cachedir = new File("cache/");
        if (!cachedir.exists()){cachedir.mkdir();}

        try {
            myPort = Integer.parseInt(args[0]);
            capacity = Integer.parseInt(args[1]);
            cacheAlgo = Integer.parseInt(args[2]);
            String algo = (cacheAlgo == 1) ? " LRU" : " LFU";
            System.out.println("Proxy running on port " + myPort + " with a cache capacity of " + capacity + " using the" +
                    algo + " replacement algorithm.");
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Need port number as argument");
            System.exit(-1);
        } catch (NumberFormatException e) {
            System.out.println("Please give port number as integer.");
            System.exit(-1);
        }

        init(myPort);
        Thread t = new Thread(new ReportThread());
        Runtime.getRuntime().addShutdownHook(t);
        Socket client = null;

        while (true) {
            try {
                client = socket.accept();
                (new Thread(new Threads(client))).start();
            } catch (IOException e) {
                //System.out.println("Error reading request from client: " + e);

                continue;
            }
        }

    }



}
