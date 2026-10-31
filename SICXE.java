import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SICXE {
    static int base = -1;
    static int LOCCTR = 0;
    static int programLength = 0;
    static int programStartAdd = 0;
    static boolean inPass2 = false;
    record Op(int opcode, int format) {}

    static LiteralTable literalTable = new LiteralTable();
    static String programName = "";
    static final List<String> MRecords = new ArrayList<>();
    static final Set<String> ABSSET = new HashSet<>();
    static final Map<String, Integer> SYMTAB = new HashMap<>();
    static final Map<String,String> SYMBLK = new HashMap<>();
    static final ProgramBlocks programBlocks = new ProgramBlocks();
    static {
        SYMTAB.putAll(Map.of(
                "A", 0, "X", 1, "L", 2, "B", 3, "S", 4,
                "T", 5, "F", 6, "PC", 8, "SW", 9));
        ABSSET.add("S");
        ABSSET.add("SW");
        ABSSET.add("PC");
        ABSSET.add("F");
        ABSSET.add("T");
        ABSSET.add("B");
        ABSSET.add("L");
        ABSSET.add("X");
        ABSSET.add("A");
    }

    record IntermediateLine(int address, Line line) {}
    static final List<IntermediateLine> INTERMEDIATE = new ArrayList<>();
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
            try {
                pass1(lines);
                pass2(INTERMEDIATE);
            } catch (Exception e) {
                error(e.getMessage());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void pass1(List<String> lines) {
        int pointer = readStart(lines);

        lineLoop:
        while(pointer < lines.size()) {
            var line = parseLine(lines.get(pointer++));
            if(line.isComment()) continue;
            INTERMEDIATE.add(new IntermediateLine(LOCCTR, line));
            if(line.label() != null && !line.opcode().equals("EQU")) {
                defineSymbol(line.label(), LOCCTR);
            }

            var operand = line.operand();
            switch(line.opcode()) {
                case "USE" -> switchBlock(operand == null ? "" : operand);
                case "WORD" -> LOCCTR += 3;
                case "BYTE" -> LOCCTR += constLen(operand);
                case "RESB" -> LOCCTR += reserveSize(operand);
                case "RESW" -> LOCCTR += 3 * reserveSize(operand);
                case "BASE", "NOBASE" -> {}
                case "EQU" -> defineEqu(line.label(), operand);
                case "ORG" -> LOCCTR = Expression.evaluate(operand).value();
                case "LTORG" -> flushLiterals();
                case "END" ->  {
                    flushLiterals();
                    programBlocks.save(LOCCTR);
                    programLength = programBlocks.programLength();
                    break lineLoop;
                }
                default -> {
                    LOCCTR += instructionLength(line.opcode());
                    collectLiteral(operand);
                }
            }
        }
    }

    private static int readStart(List<String> lines) {
        int pointer = 0;
        Line firstLine;
        while((firstLine = parseLine(lines.get(pointer))).isComment) pointer++;
        if(!firstLine.opcode().equals("START")) return pointer;

        LOCCTR = Integer.parseInt(firstLine.operand(),16);
        programStartAdd = LOCCTR;
        programName = firstLine.label() == null ? "" : firstLine.label();
        if(firstLine.label() != null) SYMTAB.put(firstLine.label(), LOCCTR);
        return pointer + 1;
    }

    private static void defineSymbol(String label, int value) {
        if(SYMTAB.containsKey(label)) {
            error("Duplicate labels " + label);
        }
        SYMTAB.put(label, value);
        SYMBLK.put(label, programBlocks.currentBlock());
    }

    private static void defineEqu(String label, String operand) {
        if(label == null || label.isEmpty()) error("No label for EQU directive");
        var term = Expression.evaluate(operand);
        defineSymbol(label, term.value());
        if(term.relative() == 0) ABSSET.add(label);
        else SYMBLK.put(label,term.block());
    }

    private static void switchBlock(String name) {
        programBlocks.save(LOCCTR);
        programBlocks.use(name);
        LOCCTR = programBlocks.get();
    }

    private static int reserveSize(String operand) {
        var term = Expression.evaluate(operand);
        if(term.relative() == 1) error("Relative term in RESB/RESW");
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

    private static void collectLiteral(String operand) {
        if(operand != null && operand.startsWith("=")) {
            //PENDING_LIT.add(operand);
            literalTable.add(operand);
        }
    }

    private static void flushLiterals() {
        var literalList = literalTable.flush();
        for(String literal : literalList) {
            //if(SYMTAB.containsKey(literal)) continue;
            //SYMTAB.put(literal, LOCCTR);
            literalTable.assignAddr(literal,LOCCTR,programBlocks.currentBlock());
            INTERMEDIATE.add(new IntermediateLine(LOCCTR, new Line("*","BYTE",literal.substring(1),false)));
            LOCCTR += constLen(literal.substring(1));
        }
        //PENDING_LIT.clear();
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

    private static void pass2(List<IntermediateLine> lines) {
        inPass2 = true;
        programBlocks.use("");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output"))) {

            writer.write(String.format("H%-6s%06X%06X", programName, programStartAdd, programLength));
            writer.newLine();
            var textRecord = new TextRecord(writer);
            int endAddress = programStartAdd;

            for(var line : lines) {

                LOCCTR = line.address() + programBlocks.disp();
                var operand = line.line().operand();

                switch(line.line().opcode()) {
                    case "USE" -> {
                        textRecord.flush();
                        programBlocks.use(operand == null ? "" : operand);
                    }
                    case "BASE" -> base = Expression.evaluate(operand).value();
                    case "NOBASE" -> base = -1;
                    case "WORD" -> textRecord.add(LOCCTR, encodeWord(operand));
                    case "BYTE" -> textRecord.add(LOCCTR, encodeByte(operand));
                    case "RESW", "RESB" -> textRecord.flush();
                    case "END" -> {
                        if(operand != null) endAddress = Expression.evaluate(operand).value();
                    }
                    case "LTORG", "EQU" -> {}
                    default -> textRecord.add(LOCCTR, assembleInstruction(line));
                }
            }

            textRecord.flush();
            for(String M : MRecords) {
                writer.write(M);
                writer.newLine();
            }
            writer.write(String.format("E%06X", endAddress));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String encodeWord(String operand) {
        var term = Expression.evaluate(operand);
        if(term.relative() == 1) MRecords.add(String.format("M%06X06", LOCCTR));
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

    private static String assembleInstruction(IntermediateLine line) {
        var opcode = line.line().opcode();
        boolean extended = opcode.startsWith("+");
        var info = OPTAB.get(extended ? opcode.substring(1) : opcode);
        if(info == null) {
            error("Unhandled opcode in pass 2: " + opcode);
        }
        return assembleInstruction(line, info);
    }

    enum AddressingMode {
        SIMPLE(0b11), IMMEDIATE(0b01), INDIRECT(0b10);
        final int ni;
        AddressingMode(int ni) {
            this.ni = ni;
        }
    }

    private static String assembleInstruction(IntermediateLine line, Op op) {
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
            int pc = line.address + (extended ? 4 : 3) + programBlocks.disp();
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
                var term = Expression.evaluate(left);
                address = term.value();
                isConstant = term.relative() == 0;
            }else if(operand.startsWith("@")) {
                addressingMode = AddressingMode.INDIRECT;
                var term = Expression.evaluate(left);
                address = term.value();
                isConstant = term.relative() == 0;
            } else if (operand.startsWith("=")) {
                address = literalTable.getAddr(operand);
                var block = literalTable.getBlock(operand);
                address += programBlocks.disp(block);
                //address = getLabelAdd(operand);
            } else {
                var term = Expression.evaluate(operand);
                address = term.value();
                isConstant = term.relative() == 0;
            }

            if(!extended && !isConstant)  {
                if(address - pc < Math.pow(2,11) && address - pc >= -Math.pow(2,11)) {
                    address = address - pc;
                    if(address < 0) address &= 0xFFF;
                    p = 1;
                }else if(base != -1 && address - base < Math.pow(2,12) && address - base >= 0) {
                    address = address - base;
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
                    MRecords.add(String.format("M%06X05", LOCCTR + 1));
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
            int r1 = getLabelAdd(registers[0]);
            int r2;
            if(registers.length == 2)  r2 = getLabelAdd(registers[1]);
            else r2 = 0;
            return String.format("%04X", (op.opcode() << 8) | r1 << 4 | r2);
        }else {
            if(opcodeText.startsWith("+")) {
                error("Unknown opcode " + opcodeText);
            }
            return String.format("%02X", op.opcode());
        }
    }

    private static int getLabelAdd(String label) {
        if(!SYMTAB.containsKey(label)) {
            error("Unknown label " + label);
        }
        //return SYMTAB.get(label) + programBlocks.disp(SYMBLK.get(label));
        return SYMTAB.get(label);
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
