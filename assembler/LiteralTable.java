import java.util.*;

public class LiteralTable {

    private final Set<String> flushedLiterals = new HashSet<>();
    private final List<String> unFlushedLiterals = new ArrayList<>();
    private final Map<String,Integer> literalAddress = new HashMap<>();
    private final Map<String,String> literalBlocks = new HashMap<>();
    public void add(String literal) {
        if(flushedLiterals.contains(literal)) return;
        unFlushedLiterals.add(literal);
    }

    public List<String> flush() {
        Iterator<String> it = unFlushedLiterals.iterator();
        var ans = new ArrayList<String>();
        while(it.hasNext()) {
            var lit = it.next();
            it.remove();
            if(flushedLiterals.contains(lit)) continue;
            ans.add(lit);
            flushedLiterals.add(lit);
        }
        return ans;
    }

    public void assignAddr(String literal, int add,String block) {
        if(!flushedLiterals.contains(literal)) {
            throw new IllegalArgumentException("Lieral " + literal + " is not defined");
        }
        literalAddress.put(literal,add);
        literalBlocks.put(literal,block);
    }
    public String getBlock(String literal) {
        return literalBlocks.get(literal);
    }
    public int getAddr(String literal) {
        return literalAddress.get(literal);
    }
}
