import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SICXE {
    record Op(int opcode, int format) {}

    record IntermediateLine(int address, Line line) {}
    static final Map<String, Op> OPTAB = Map.ofEntries(
            Map.entry("ADD", new Op(0x18, 3)),
            Map.entry("ADDF", new Op(0x58, 3)),
            Map.entry("ADDR", new Op(0x90, 2)),
            Map.entry("AND", new Op(0x40, 3)),
            Map.entry("CLEAR", new Op(0xB4, 2)),
            Map.entry("COMP", new Op(0x28, 3)),
            Map.entry("COMPF", new Op(0x88, 3)),
            Map.entry("COMPR", new Op(0xA0, 2)),
            Map.entry("DIV", new Op(0x24, 3)),
            Map.entry("DIVF", new Op(0x64, 3)),
            Map.entry("DIVR", new Op(0x9C, 2)),
            Map.entry("FIX", new Op(0xC4, 1)),
            Map.entry("FLOAT", new Op(0xC0, 1)),
            Map.entry("HIO", new Op(0xF4, 1)),
            Map.entry("J", new Op(0x3C, 3)),
            Map.entry("JEQ", new Op(0x30, 3)),
            Map.entry("JGT", new Op(0x34, 3)),
            Map.entry("JLT", new Op(0x38, 3)),
            Map.entry("JSUB", new Op(0x48, 3)),
            Map.entry("LDA", new Op(0x00, 3)),
            Map.entry("LDB", new Op(0x68, 3)),
            Map.entry("LDCH", new Op(0x50, 3)),
            Map.entry("LDF", new Op(0x70, 3)),
            Map.entry("LDL", new Op(0x08, 3)),
            Map.entry("LDS", new Op(0x6C, 3)),
            Map.entry("LDT", new Op(0x74, 3)),
            Map.entry("LDX", new Op(0x04, 3)),
            Map.entry("LPS", new Op(0xD0, 3)),
            Map.entry("MUL", new Op(0x20, 3)),
            Map.entry("MULF", new Op(0x60, 3)),
            Map.entry("MULR", new Op(0x98, 2)),
            Map.entry("NORM", new Op(0xC8, 1)),
            Map.entry("OR", new Op(0x44, 3)),
            Map.entry("RD", new Op(0xD8, 3)),
            Map.entry("RMO", new Op(0xAC, 2)),
            Map.entry("RSUB", new Op(0x4C, 3)),
            Map.entry("SHIFTL", new Op(0xA4, 2)),
            Map.entry("SHIFTR", new Op(0xA8, 2)),
            Map.entry("SIO", new Op(0xF0, 1)),
            Map.entry("SSK", new Op(0xEC, 3)),
            Map.entry("STA", new Op(0x0C, 3)),
            Map.entry("STB", new Op(0x78, 3)),
            Map.entry("STCH", new Op(0x54, 3)),
            Map.entry("STF", new Op(0x80, 3)),
            Map.entry("STI", new Op(0xD4, 3)),
            Map.entry("STL", new Op(0x14, 3)),
            Map.entry("STS", new Op(0x7C, 3)),
            Map.entry("STSW", new Op(0xE8, 3)),
            Map.entry("STT", new Op(0x84, 3)),
            Map.entry("STX", new Op(0x10, 3)),
            Map.entry("SUB", new Op(0x1C, 3)),
            Map.entry("SUBF", new Op(0x5C, 3)),
            Map.entry("SUBR", new Op(0x94, 2)),
            Map.entry("SVC", new Op(0xB0, 2)),
            Map.entry("TD", new Op(0xE0, 3)),
            Map.entry("TIO", new Op(0xF8, 1)),
            Map.entry("TIX", new Op(0x2C, 3)),
            Map.entry("TIXR", new Op(0xB8, 2)),
            Map.entry("WD", new Op(0xDC, 3))
    );

    static final Set<String> NO_OPERAND = Set.of("RSUB", "FIX", "FLOAT", "HIO", "NORM", "SIO", "TIO");

    record Line(String label, String opcode, String operand, boolean isComment) {}

    static Line parseLine(String raw) {
        String trimmed = raw.strip();
        if (trimmed.isEmpty() || trimmed.startsWith(".")) {
            return new Line(null, null, null, true);
        }

        String rest = raw.stripTrailing();
        String label = null;
        if (!Character.isWhitespace(rest.charAt(0))) {
            String[] parts = rest.split("\\s+", 2);
            label = parts[0];
            rest = parts.length > 1 ? parts[1] : "";
        }

        String[] parts = rest.strip().split("\\s+", 2);
        String opcode = parts[0];
        rest = parts.length > 1 ? parts[1].strip() : "";

        if (rest.isEmpty() || NO_OPERAND.contains(opcode.toUpperCase())) {
            return new Line(label, opcode, null, false);
        }

        String operand;
        if (rest.length() > 2 && (rest.charAt(0) == 'C' || rest.charAt(0) == 'X') && rest.charAt(1) == '\'') {
            int close = rest.indexOf('\'', 2);
            operand = close == -1 ? rest : rest.substring(0, close + 1);
        } else {
            operand = rest.split("\\s+", 2)[0];
        }
        return new Line(label, opcode, operand, false);
    }
    public static void main(String[] args) {
        if(args.length != 1) {
            error("Usage: java SICXE <file.asm>");
        }
        try {
            List<String> lines = Files.readAllLines(Paths.get(args[0]));
            List<SectionData> sections = new ArrayList<>();
            SectionData section = new SectionData(true);
            int pointer = readStart(section, lines);
            String endOperand = null;
            try {
                while(pointer < lines.size()) {
                    pointer = pass1(section, pointer,lines);
                    sections.add(section);

                    var line = parseLine(lines.get(pointer++));

                    if(line.opcode().equals("CSECT")){
                        section = new SectionData(false);
                        section.programName = line.label() == null ? "" : line.label();
                    } else if(line.opcode().equals("END")) {
                        endOperand = line.operand();
                    }
                }
                new FileWriter("output").close();
                for(SectionData s : sections) {
                    pass2(s, s.INTERMEDIATE, endOperand);
                }
            } catch (Exception e) {
                error(e.getMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int pass1(SectionData section, int pointer, List<String> lines) {

        lineLoop:
        while(pointer < lines.size()) {
            var line = parseLine(lines.get(pointer++));
            if(line.isComment()) continue;
            boolean csect = line.opcode().equals("CSECT");
            if(!csect) section.INTERMEDIATE.add(new IntermediateLine(section.LOCCTR, line));
            if(line.label() != null && !line.opcode().equals("EQU") && !csect) {
                defineSymbol(section, line.label(), section.LOCCTR);
            }

            var operand = line.operand();
            switch(line.opcode()) {
                case "EXTDEF" -> {
                    if(line.operand() == null) {
                        error("No labels in external defition directive");
                    }
                    var split = line.operand().split(",");
                    Collections.addAll(section.defList, split);
                }
                case "EXTREF" -> {
                    if(line.operand() == null) {
                        error("No labels in external reference directive");
                    }
                    var split = line.operand.split(",");
                    Collections.addAll(section.refList, split);
                }
                case "USE" -> switchBlock(section, operand == null ? "" : operand);
                case "WORD" -> section.LOCCTR += 3;
                case "BYTE" -> section.LOCCTR += constLen(operand);
                case "RESB" -> section.LOCCTR += reserveSize(section, operand);
                case "RESW" -> section.LOCCTR += 3 * reserveSize(section, operand);
                case "BASE", "NOBASE" -> {}
                case "EQU" -> defineEqu(section, line.label(), operand);
                case "ORG" -> section.LOCCTR = Expression.evaluate(operand, section).value();
                case "LTORG" -> flushLiterals(section);
                case "END" , "CSECT"  -> {
                    flushLiterals(section);
                    section.programBlocks.save(section.LOCCTR);
                    section.programLength = section.programBlocks.programLength();
                    break lineLoop;
                }
                default -> {
                    section.LOCCTR += instructionLength(line.opcode());
                    collectLiteral(section, operand);
                }
            }
        }
        for(String ref : section.defList) {
            if(!section.SYMTAB.containsKey(ref)) error("Unknown name " + ref + " in EXTDEF directive");
        }
        return pointer - 1;
    }

    private static int readStart(SectionData section, List<String> lines) {
        int pointer = 0;
        Line firstLine;
        while((firstLine = parseLine(lines.get(pointer))).isComment) pointer++;
        if(!firstLine.opcode().equals("START")) return pointer;

        section.LOCCTR = Integer.parseInt(firstLine.operand(),16);
        section.programStartAdd = section.LOCCTR;
        section.programName = firstLine.label() == null ? "" : firstLine.label();
        if(firstLine.label() != null) section.SYMTAB.put(firstLine.label(), section.LOCCTR);
        return pointer + 1;
    }

    private static void defineSymbol(SectionData section, String label, int value) {
        if(section.SYMTAB.containsKey(label)) {
            error("Duplicate labels " + label);
        }
        section.SYMTAB.put(label, value);
        section.SYMBLK.put(label, section.programBlocks.currentBlock());
    }

    private static void defineEqu(SectionData section, String label, String operand) {
        if(label == null || label.isEmpty()) error("No label for EQU directive");
        var term = Expression.evaluate(operand, section);
        defineSymbol(section, label, term.value());
        if(term.relative() == 0) section.ABSSET.add(label);
        else section.SYMBLK.put(label,term.block());
    }

    private static void switchBlock(SectionData section, String name) {
        section.programBlocks.save(section.LOCCTR);
        section.programBlocks.use(name);
        section.LOCCTR = section.programBlocks.get();
    }

    private static int reserveSize(SectionData section, String operand) {
        var term = Expression.evaluate(operand, section);
        if(term.relative() == 1 || term.externals() != null) error("Relative term in RESB/RESW");
        return term.value();
    }

    private static int instructionLength(String opcode) {
        boolean extended = opcode.startsWith("+");
        String code = extended ? opcode.substring(1) : opcode;
        if(!OPTAB.containsKey(code)) {
            error("Unknown opcode " + opcode);
        }
        return extended ? 4 : OPTAB.get(code).format();
    }

    private static void collectLiteral(SectionData section, String operand) {
        if(operand != null && operand.startsWith("=")) {
            //PENDING_LIT.add(operand);
            section.literalTable.add(operand);
        }
    }

    private static void flushLiterals(SectionData section) {
        var literalList = section.literalTable.flush();
        for(String literal : literalList) {
            //if(SYMTAB.containsKey(literal)) continue;
            //SYMTAB.put(literal, LOCCTR);
            section.literalTable.assignAddr(literal,section.LOCCTR,section.programBlocks.currentBlock());
            section.INTERMEDIATE.add(new IntermediateLine(section.LOCCTR, new Line("*","BYTE",literal.substring(1),false)));
            section.LOCCTR += constLen(literal.substring(1));
        }
    }

    private static int constLen(String operand) {
        if(operand.charAt(0) == 'C') {
            return (operand.length() - 3);
        }else if(operand.charAt(0) == 'X') {
            return (operand.length() - 3) / 2;
        }
        error("Unkown constant prefix " + operand.charAt(0));
        return 0;
    }

    private static void pass2(SectionData section,List<IntermediateLine> lines, String endOperand) {
        section.inPass2 = true;
        section.programBlocks.use("");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output", true))) {

            writer.write(String.format("H%-6s%06X%06X", section.programName, section.programStartAdd, section.programLength));
            writer.newLine();
            section.writeDRecords(writer);
            section.writeRRecords(writer);
            var textRecord = new TextRecord(writer);
            int endAddress = section.programStartAdd;

            for(var line : lines) {

                section.LOCCTR = line.address() + section.programBlocks.disp();
                var operand = line.line().operand();

                switch(line.line().opcode()) {
                    case "CSECT" -> error("How did we get here ? ");
                    case "EXTDEF" ->{}
                    case "EXTREF" ->{}
                    case "USE" -> {
                        textRecord.flush();
                        section.programBlocks.use(operand == null ? "" : operand);
                    }
                    case "BASE" -> section.base = Expression.evaluate(operand, section).value();
                    case "NOBASE" -> section.base = -1;
                    case "WORD" -> textRecord.add(section.LOCCTR, encodeWord(section, operand));
                    case "BYTE" -> textRecord.add(section.LOCCTR, encodeByte(operand));
                    case "RESW", "RESB" -> textRecord.flush();
                    case "LTORG", "EQU", "END" -> {}
                    default -> textRecord.add(section.LOCCTR, assembleInstruction(section, line));
                }
            }

            textRecord.flush();
            for(String M : section.MRecords) {
                writer.write(M);
                writer.newLine();
            }
            if(section.firstSection && endOperand != null) endAddress = Expression.evaluate(endOperand, section).value();
            if(section.firstSection)writer.write(String.format("E%06X", endAddress));
            else writer.write("E");
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String encodeWord(SectionData section, String operand) {
        var term = Expression.evaluate(operand, section);
        //if(term.relative() == 1) section.MRecords.add(String.format("M%06X06+%-6s", section.LOCCTR,section.programName));
        if(term.relative() == 1) addMRecord(section, section.LOCCTR,6,'+',section.programName);
        addExternalRecords(section, section.LOCCTR,6,term);
        return String.format("%06X", term.value() & 0xFFFFFF);
    }
    private static String encodeByte(String operand) {
        StringBuilder data = new StringBuilder();
        boolean isChar = operand.startsWith("C");
        operand = operand.substring(2, operand.length() - 1);

        if(isChar)  {
            for(char c : operand.toCharArray()) {
                data.append(String.format("%02X", (int) c));
            }
        }else {
            data.append(operand);
        }
        return data.toString();
    }

    private static String assembleInstruction(SectionData section, IntermediateLine line) {
        var opcode = line.line().opcode();
        boolean extended = opcode.startsWith("+");
        var info = OPTAB.get(extended ? opcode.substring(1) : opcode);
        if(info == null) {
            error("Unhandled opcode in pass 2: " + opcode);
        }
        return assembleInstruction(section, line, info);
    }

    enum AddressingMode {
        SIMPLE(0b11), IMMEDIATE(0b01), INDIRECT(0b10);
        final int ni;
        AddressingMode(int ni) {
            this.ni = ni;
        }
    }

    private static String assembleInstruction(SectionData section, IntermediateLine line, Op op) {
        int format = op.format();
        String opcodeText = line.line().opcode();
        var operand = line.line().operand();
        if(format == 3) {
            AddressingMode addressingMode = AddressingMode.SIMPLE;
            int b = 0;
            int p = 0;
            int x = 0;

            int address = 0;
            boolean extended =  opcodeText.startsWith("+");
            int pc = line.address + (extended ? 4 : 3) + section.programBlocks.disp();
            int e = extended ? 1 : 0;

            if(operand == null) {
                int ni = AddressingMode.SIMPLE.ni;
                return extended ? String.format("%08X", (op.opcode() << 24) | ni << 24 | e << 20)
                                : String.format("%06X", (op.opcode() << 16) | ni << 16);
            }

            var left = operand.substring(1);
            boolean isConstant = false;

            if(operand.contains(",X")) {
                x = 1;
                operand = operand.substring(0, operand.length() - 2);
                if(operand.startsWith("#") || operand.startsWith("@")) {
                    error("Can't use indexed addressing with inderect or immediate addressing modes");
                }
            }

            if(operand.startsWith("#")) {
                addressingMode = AddressingMode.IMMEDIATE;
                var term = Expression.evaluate(left, section);
                address = term.value();
                isConstant = term.relative() == 0;
                if(!extended && term.externals() != null) error("3 byte format using an exteranl label");
                addExternalRecords(section, section.LOCCTR + 1, 5, term);
            }else if(operand.startsWith("@")) {
                addressingMode = AddressingMode.INDIRECT;
                var term = Expression.evaluate(left, section);
                address = term.value();
                isConstant = term.relative() == 0;
                if(!extended && term.externals() != null) error("3 byte format using an exteranl label");
                addExternalRecords(section, section.LOCCTR + 1, 5, term);
            } else if (operand.startsWith("=")) {
                address = section.literalTable.getAddr(operand);
                var block = section.literalTable.getBlock(operand);
                address += section.programBlocks.disp(block);
                //address = getLabelAdd(section, operand);
            } else {
                var term = Expression.evaluate(operand, section);
                address = term.value();
                isConstant = term.relative() == 0;
                if(!extended && term.externals() != null) error("3 byte format using an exteranl label");
                addExternalRecords(section, section.LOCCTR + 1, 5, term);
            }

            if(!extended && !isConstant)  {
                if(address - pc < Math.pow(2,11) && address - pc >= -Math.pow(2,11)) {
                    address = address - pc;
                    if(address < 0) address &= 0xFFF;
                    p = 1;
                }else if(section.base != -1 && address - section.base < Math.pow(2,12) && address - section.base >= 0) {
                    address = address - section.base;
                    b = 1;
                }else{
                    error("Can't reach target address. Consider using the 4 bytes format");
                }
            }

            if(extended && address >= Math.pow(2,20)) {
                error("Number " + address + " is too big for 4 byte addressing format");
            }else if(!extended && address >= Math.pow(2,12)){
                error("Number " + address + " is too big for 3 byte addressing format");
            }

            if(extended) {
                if(!isConstant) {
                    //MRecords.add(String.format("M%06X05", line.address() + 1));
                    //section.MRecords.add(String.format("M%06X05", section.LOCCTR + 1));
                    addMRecord(section, section.LOCCTR + 1, 5, '+',section.programName);
                }
                return String.format("%08X", (op.opcode() << 24) | addressingMode.ni << 24 | x << 23 | e << 20 | address);
            }else {
                return String.format("%06X", (op.opcode()) << 16 | addressingMode.ni << 16 | x << 15 | b << 14 | p << 13 | address);
            }
        }else if(format == 2) {
            if(opcodeText.startsWith("+")) {
                error("Unknown opcode " + opcodeText);
            }
            var registers = operand.split(",");
            int r1 = getLabelAdd(section, registers[0]);
            int r2;
            if(registers.length == 2)  r2 = getLabelAdd(section, registers[1]);
            else r2 = 0;
            return String.format("%04X", (op.opcode() << 8) | r1 << 4 | r2);
        }else {
            if(opcodeText.startsWith("+")) {
                error("Unknown opcode " + opcodeText);
            }
            return String.format("%02X", op.opcode());
        }
    }

    private static int getLabelAdd(SectionData section, String label) {
        if(!section.SYMTAB.containsKey(label)) {
            error("Unknown label " + label);
        }
        //return SYMTAB.get(label) + programBlocks.disp(SYMBLK.get(label));
        return section.SYMTAB.get(label);
    }

    private static void addExternalRecords(SectionData section, int add, int length,Expression.Term term) {
        if(term.externals() == null) return;
        for(String s : term.externals()) {
            addMRecord(section, add,length,s.charAt(0),s.substring(1));
        }
    }
    private static void addMRecord(SectionData section, int add, int length, char sign, String name) {
        section.MRecords.add(String.format("M%06X%02X%c%s",add,length,sign,name));

    }
    private static void error(String reason) {
        System.err.println("Error: " + reason);
        System.exit(1);
    }
    private static boolean isNumeric(String s) {
        try {
            int val = Integer.parseInt(s);
            return true;
        }catch(NumberFormatException e) {
            return false;
        }
    }
}
