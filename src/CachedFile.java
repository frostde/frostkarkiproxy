public class CachedFile {
    public boolean hit;
    public boolean valid;
    public String fileName;
    public long lastModified;
    public byte[] content;

    CachedFile() {
        hit = true;
        valid = false;
        fileName = "";
        lastModified = 0;
        content = new byte[0];
    }
}
