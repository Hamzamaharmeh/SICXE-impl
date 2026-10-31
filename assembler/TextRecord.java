import java.io.BufferedWriter;
import java.io.IOException;

public class TextRecord {
    private static final int MAX_LENGTH = 30;

    private final BufferedWriter writer;
    private int startAdd;
    private final StringBuilder data = new StringBuilder();

    public TextRecord(BufferedWriter writer) {
        this.writer = writer;
    }

    public void add(int address, String objectCode) throws IOException {
        int length = objectCode.length() / 2;
        if(data.length() / 2 + length > MAX_LENGTH) {
            flush();
        }
        if(data.isEmpty()) startAdd = address;
        data.append(objectCode);
    }

    public void flush() throws IOException {
        if(data.isEmpty()) return;
        writer.write(String.format("T%06X%02X%s", startAdd, data.length() / 2, data));
        writer.newLine();
        data.setLength(0);
    }
}
