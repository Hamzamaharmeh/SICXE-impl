public class Expression {
    private final String text;
    private int pos = 0;

    record Term(int value, int relative) {
    }
    private Expression(String text) {
        this.text = text;
    }

    public static Term evaluate(String text) {
        Expression e = new Expression(text.strip());
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
            left = new Term(val, relCount);
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
            left = new Term(op == '*' ? left.value() * right.value() : left.value() / right.value()
            ,0);
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
            return new Term(SICXE.LOCCTR,1);
        }
        if(c == '-') {
            pos++;
            var t = parseFactor();
            return new Term(-t.value, -t.relative);
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
            return new Term(Integer.parseInt(text.substring(start,pos)), 0);
        }

        if(Character.isAlphabetic(c)) {
            int start = pos;
            while(pos  < text.length() && isAlphaNumeric(peek())) pos++;
            var s = text.substring(start, pos);
            if(!SICXE.SYMTAB.containsKey(s)) {
                throw new IllegalStateException("Undefiend Label " + s);
            }
            int disp = SICXE.programBlocks.disp(SICXE.SYMBLK.get(s));
            int val = SICXE.SYMTAB.get(s) + disp;
            boolean abs = SICXE.ABSSET.contains(s);
            return new Term(val, abs ? 0 : 1);
        }
        throw new IllegalArgumentException("Unexpected '" + c + "' in expression " + text);
    }

    private char peek() {
        return text.charAt(pos);
    }
    private boolean isAlphaNumeric(char c) {
        return Character.isAlphabetic(c) || Character.isDigit(c);
    }
}
