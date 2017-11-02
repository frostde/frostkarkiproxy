import java.net.*;
import java.io.*;
import java.lang.*;

public class MyServer {
    private static int port;
    private static ServerSocket socket;
    private static LRUCache lrucache;
    public static int cacheMiss = 0;
    public static int cacheHit = 0;
    public static int cacheAlgo = 0;
    public static LFUCache lfuCache;
    public static int capacity = 3;
    public static int totalRequests;
    public static float ratio;



    public static String getName(String s) {
        try {
            if (s!= null) {
                if (s.indexOf("?") > 0) {
                    s = s.replace(s.substring(s.indexOf("?")), "");
                }
                if (s.indexOf("/") > 0) {
                    s = s.substring(s.lastIndexOf("/"));
                }
                if (s.indexOf(".") > 0) {
                    s = s.replace(s.substring(s.lastIndexOf(".")), "").trim();
                }
                if (s.equals("/")) s = "_index";
                if (s.startsWith("/")) s = s.replace("/", "_");
            }
        } catch (Exception ex) {
            String x = "";
        }
        return s;
    }

    public static File writeFile(HttpResponse response, String URI, File f) {
        DataOutputStream outStream;
        try {
            File ourFile = new File(f, "cached" + getName(URI));
            outStream = new DataOutputStream(new FileOutputStream(ourFile));
            outStream.writeBytes(response.toString());
            outStream.write(response.body);
            outStream.close();
            return ourFile;
        } catch (Exception ex) {

        }
        return null;

    }


    public synchronized static String[] caching(HttpRequest request, HttpResponse response) throws IOException {
        File file;
        DataOutputStream outStream;
        String[] returnpair = new String[2];
        String URI = request.URI;
        try {
            if (request.requestHeaders.get("Referer") != null) {
                String referer = request.requestHeaders.get("Referer");
                File referedFolder = new File("cache/" + referer.replace(referer.substring(0,7), ""));
                if (!referedFolder.exists()) referedFolder.mkdir();
                file = writeFile(response, URI, referedFolder);
                returnpair[0] = file.getPath();
            } else {
                String c = "";
                    File f = new File("cache/" + request.getHost());
                    if (!f.exists()) f.mkdir();
                    file = writeFile(response, URI, f);
                    returnpair[0] = file.getPath();
                    if (file != null) {
                        if (cacheAlgo == 1) {
                            if ((returnpair[1] = lrucache.set(request.getHost(), f.getAbsolutePath())) != "") {
                                File folderToDelete = new File(returnpair[1]);
                                String[] entries = folderToDelete.list();
                                for (String s : entries) {
                                    File fileToDelete = new File(folderToDelete, s);
                                    fileToDelete.delete();
                                    //System.out.println("Deleting " + s);
                                }
                                folderToDelete.delete();
                                //System.out.println("deleting " + returnpair[1]);
                            }
                        } else {
                            if ((returnpair[1] = lfuCache.addCacheEntry(request.getHost(), f.getAbsolutePath())) != "") {
                                File folderToDelete = new File(returnpair[1]);
                                String[] entries = folderToDelete.list();
                                for (String s : entries) {
                                    File fileToDelete = new File(folderToDelete, s);
                                    fileToDelete.delete();
                                    //System.out.println("Deleting " + s);
                                }
                                folderToDelete.delete();
                                //System.out.println("deleting " + returnpair[1]);
                            }
                        }
                    }

            }
            //returnpair[0] = file.getPath();
        } catch (Exception ex) {
        }

        return returnpair;
    }



    public static class ReportThread implements Runnable {
        public void run() {
            try {
                System.out.println("Cache Miss: " + cacheMiss + " Cache Hit:" + cacheHit);
                File index = new File("cache/");
                String[] entries = index.list();
                /*for (String s : entries) {
                    File currentfile = new File(index.getPath(), s);
                    String[] otherentries = currentfile.list();
                    for (String x : otherentries) {
                        File thisfile = new File(currentfile.getPath(), x);
                        thisfile.delete();
                    }
                    currentfile.delete();
                }*/
                totalRequests = cacheHit + cacheMiss;
                ratio = ((float)cacheHit/totalRequests)*100;
                String algo = (cacheAlgo == 1) ? "LRU" : "LFU";
                FileWriter fWriter;
                BufferedWriter bwriter;
                File f = new File("report.txt");
                fWriter = new FileWriter(f, true);
                bwriter = new BufferedWriter(fWriter);
                bwriter.write(System.lineSeparator());
                StringBuilder sb = new StringBuilder();
                if (totalRequests != 0) {
                    sb.append("Replacement algorithm: " + algo);
                    sb.append("  Capacity: " + capacity);
                    sb.append("  Total Requests: " + totalRequests);
                    sb.append("  Misses: " + cacheMiss);
                    sb.append("  Hits: " + cacheHit);
                    sb.append("  Ratio: " + ratio);
                }
                bwriter.write(sb.toString());
                bwriter.flush();
            }catch (Exception ex) {
                String s = "";
            }
        }
    }

    public synchronized static CachedFile uncaching(String URI, HttpRequest req) throws IOException {
        File parentFolder;
        File cachedFile;
        FileInputStream fileIn;
        String hashfile;
        CachedFile cf;
        String parent;
        try {
        if (req.requestHeaders.get("Referer") != null) {
            parent = req.requestHeaders.get("Referer");
        } else {
            parent = req.getHost();
        }

        parent = parent.replace(parent.substring(0, 7), "");
        parent = parent.replace("/", "").trim();


        if ((hashfile = get(parent)) != "") {
            cf = new CachedFile();
            parentFolder = new File(hashfile);
            String[] entries = parentFolder.list();
            URI = "cached" + getName(req.URI);
            for (String s : entries) {
                if (s.equals(URI)) {
                    cacheHit++;
                    cachedFile = new File(parentFolder, URI);
                    fileIn = new FileInputStream(cachedFile);
                    cf.content = new byte[(int) cachedFile.length()];
                    fileIn.read(cf.content);
                    cf.lastModified = cachedFile.lastModified();
                    cf.fileName = cachedFile.getPath().substring(cachedFile.getPath().lastIndexOf("/"));
                    return cf;
                }
            }
            return null;
        } else {
            cacheMiss++;
            return null;
        }
    } catch (Exception ex)
    {
        String s = "";
    }
    return null;
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
