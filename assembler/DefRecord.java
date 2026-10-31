import java.io.BufferedWriter;
import java.io.IOException;

public class DefRecord {

    private static final int MAX_ENTRIES = 6;

    private int entries = 0;
    private final BufferedWriter writer;
    private final StringBuilder data = new StringBuilder();

    public DefRecord(BufferedWriter writer) {
        this.writer = writer;
    }

    public void add(int address, String name) throws  IOException {
        if(entries == MAX_ENTRIES) {
            flush();
            entries = 0;
        }
        data.append(String.format("%-6s%06X",name,address));
        entries++;
    }
    public void flush() throws IOException {
        if(data.isEmpty()) return;
        writer.write("D" + data.toString());
        writer.newLine();
        data.setLength(0);
    }

}
