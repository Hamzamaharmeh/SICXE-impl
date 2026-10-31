import java.util.ArrayList;
import java.util.List;

/**
 * This class assumes that you add or subtract relative terms from the same block only
 * Because it's not mentioned in the book
 */
public class Expression {
    private final String text;
    private final SectionData section;
    private int pos = 0;

    record Term(int value, int relative, String block, List<String> externals) {
    }
    private Expression(String text, SectionData section) {
        this.text = text;
        this.section = section;
    }

    public static Term evaluate(String text, SectionData section) {
        Expression e = new Expression(text.strip(), section);
        Term result = e.parseExpression();
        if(e.pos != e.text.length()) {
            throw new IllegalArgumentException("Unexpected '" + e.text.charAt(e.pos) + "' in expression " + text);
        }
        if(result.relative() > 1 || result.relative() < 0) {
            throw new IllegalStateException("More than one relative term in expression " + text);
        }
        return result;
    }

    private Term parseExpression() {
        Term left = parseTerm();
        while(pos < text.length() && (peek() == '+' || peek() == '-')) {
            char op = text.charAt(pos++);
            Term right = parseTerm();
            int val = op == '+' ? left.value + right.value : left.value - right.value;
            int relCount = op == '+' ? left.relative() + right.relative() : left.relative() - right.relative();
            String block = left.block() != null ? left.block() : right.block();

            List<String> list= new ArrayList<>();
            if(left.externals != null)list.addAll(left.externals);
            if(right.externals != null)list.addAll(op == '+' ? right.externals : flipSign(right.externals));

            left = new Term(val, relCount,block,list.isEmpty() ? null : list);
        }
        return left;
    }

    private Term parseTerm() {
        Term left = parseFactor();
        while(pos < text.length() && (peek() == '*' || peek() == '/')) {
            char op = text.charAt(pos++);
            Term right = parseFactor();
            if(op == '/' && right.value() == 0) {
                throw new IllegalArgumentException("Division by zero in expression " + text);
            }
            if(right.relative() != 0 || left.relative() != 0) {
                throw new IllegalStateException("relative terms can't be multiplied or divided");
            }
            if(hasExternals(left) || hasExternals(right)) {
                throw new IllegalStateException("external references can't be multiplied or divided");
            }
            String block = left.block() != null ? left.block() : right.block();

            left = new Term(op == '*' ? left.value() * right.value() : left.value() / right.value()
            ,0,block,null);
        }
        return left;
    }

    private Term parseFactor() {
        if(pos >= text.length()) {
            throw new IllegalArgumentException("Expression ends too early: " + text);
        }
        char c = peek();
        if(c == '*') {
            pos++;
            return new Term(section.LOCCTR,1,section.programBlocks.currentBlock(), null);
        }
        if(c == '-') {
            pos++;
            var t = parseFactor();
            return new Term(-t.value, -t.relative,t.block,flipSign(t.externals));
        }
        if(c == '(') {
            pos++;
            Term value = parseExpression();
            if(pos >= text.length() || peek() != ')') {
                throw new IllegalArgumentException("Missing ')' in expression " + text);
            }
            pos++;
            return value;
        }
        if(Character.isDigit(c)) {
            int start = pos;
            while(pos < text.length() && Character.isDigit(peek())) pos++;
            return new Term(Integer.parseInt(text.substring(start,pos)), 0, null,null);
        }

        if(Character.isAlphabetic(c)) {
            int start = pos;
            while(pos  < text.length() && isAlphaNumeric(peek())) pos++;
            var s = text.substring(start, pos);
            if(section.refList.contains(s)) {
                var list = new ArrayList<String>();
                list.add("+" + s);
                return new Term(0,0,null, list);
            }
            if(!section.SYMTAB.containsKey(s)) {
                throw new IllegalStateException("Undefiend Label " + s);
            }
            boolean abs = section.ABSSET.contains(s);
            int disp = 0;
            if(section.inPass2 && !abs) disp = section.programBlocks.disp(section.SYMBLK.get(s));
            int val = section.SYMTAB.get(s) + disp;
            String block = null;
            if(!abs) block = section.SYMBLK.get(s);
            return new Term(val, abs ? 0 : 1,block,null);
        }
        throw new IllegalArgumentException("Unexpected '" + c + "' in expression " + text);
    }

    private char peek() {
        return text.charAt(pos);
    }
    private boolean isAlphaNumeric(char c) {
        return Character.isAlphabetic(c) || Character.isDigit(c);
    }
    private boolean hasExternals(Term t) {
        return t.externals() != null && !t.externals().isEmpty();
    }
    private List<String> flipSign(List<String> list) {
        if(list == null) return null;
        List<String> ans = new ArrayList<>();
        for(String s : list) {
            char sign = ' ';
            if(s.charAt(0) == '+') sign = '-';
            else sign = '+';
            ans.add(sign + s.substring(1));
        }
        return ans;
    }
}
