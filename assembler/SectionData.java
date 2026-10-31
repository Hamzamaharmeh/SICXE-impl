import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

public class SectionData {
    boolean firstSection;
    int base = -1;
    int LOCCTR = 0;
    int programLength = 0;
    int programStartAdd = 0;
    boolean inPass2 = false;
    LiteralTable literalTable = new LiteralTable();
    String programName = "";
    final List<String> MRecords = new ArrayList<>();
    final Set<String> ABSSET = new HashSet<>();
    final Map<String, Integer> SYMTAB = new HashMap<>();
    final Map<String,String> SYMBLK = new HashMap<>();
    final ProgramBlocks programBlocks = new ProgramBlocks();
    final List<SICXE.IntermediateLine> INTERMEDIATE = new ArrayList<>();
    final List<String> refList = new ArrayList<>();
    final List<String> defList = new ArrayList<>();

    public SectionData(boolean firstSection) {
        SYMTAB.putAll(Map.of(
                "A", 0, "X", 1, "L", 2, "B", 3, "S", 4,
                "T", 5, "F", 6, "PC", 8, "SW", 9));
        ABSSET.addAll(List.of("A", "X", "L", "B", "S", "T", "F", "PC", "SW"));
        this.firstSection = firstSection;
    }

    public void writeDRecords(BufferedWriter writer) {
        var dRecord = new DefRecord(writer);
        try {
            for (String ref : defList) {
                int address = SYMTAB.get(ref);
                if(!ABSSET.contains(ref)) address += programBlocks.disp(SYMBLK.get(ref));
                dRecord.add(address, ref);
            }
            dRecord.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void writeRRecords(BufferedWriter writer) {
        var rRecord = new RefRecord(writer);
        try {
            for (String ref : refList) {
                rRecord.add(ref);
            }
            rRecord.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
