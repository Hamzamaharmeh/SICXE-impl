import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgramBlocks {

    private final Map<String,Integer> LOCTAB = new HashMap<>();
    private final Map<Integer,String> ORDER = new HashMap<>();
    private final Map<String,Integer> ORDERREV = new HashMap<>();
    private int[] dispArr;
    int count = 1;
    String current = "";
    int currOrder;
    public ProgramBlocks() {
        LOCTAB.put("", 0);
        ORDER.put(0,"");
        ORDERREV.put("",0);
        currOrder = 1;
    }
    public void use(String name) {
        if(!LOCTAB.containsKey(name)) {
            LOCTAB.put(name, 0);
            ORDERREV.put(name,currOrder);
            ORDER.put(currOrder,name);
            currOrder++;
            count++;
        }
        current = name;
    }

    public String currentBlock() {
        return this.current;
    }
    public int get() {
        return LOCTAB.get(current);
    }
    public void save(int val) {
        LOCTAB.put(current,val);
    }
    public void add(int val) {
        LOCTAB.put(current,LOCTAB.get(current)+val);
    }

    /**
     * Should only be called from pass2
     * @return the displacement from the start of the program
     */
    public int disp() {
        if(dispArr == null) {
            dispArr = new int[LOCTAB.size()];
            Arrays.fill(dispArr,-1);
            dispArr[0] = 0;
        }
        int order = ORDERREV.get(current);
        return dispHelper(order);
    }
    public int disp(String name) {
        var curr = this.current;
        use(name);
        int ans = disp();
        use(curr);
        return ans;
    }
    public int programLength() {
        var curr = this.current;
        var name = ORDER.get(count - 1);
        use(name);
        int disp = disp();
        use(curr);
        return disp + LOCTAB.get(name);
    }
    private int dispHelper(int order) {
        if(dispArr[order] != -1) return  dispArr[order];
        return dispArr[order] = dispHelper(order - 1) + LOCTAB.get(ORDER.get(order-1));
    }
}
