import java.io.BufferedWriter;
import java.io.IOException;

public class RefRecord {
    private static final int MAX_ENTRIES = 12;

    private int entries = 0;
    private final BufferedWriter writer;
    private final StringBuilder data = new StringBuilder();

    public RefRecord(BufferedWriter writer) {
        this.writer = writer;
    }

    public void add(String name) throws IOException {
        if(entries == MAX_ENTRIES) {
            flush();
            entries = 0;
        }
        data.append(String.format("%-6s", name));
        entries++;
    }
    public void flush() throws IOException {
        if(data.isEmpty()) return;
        writer.write("R" + data.toString());
        writer.newLine();
        data.setLength(0);
    }
}
