package lib;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

public class CompilationEngine {
    private JackTokenizer tokenizer;
    private Writer output;

    public CompilationEngine(Reader input, Writer output) throws IOException {
        tokenizer = new JackTokenizer(input);
        tokenizer.advance();
        this.output = output;
    }

    private String escapeXML(String str) {
        if (str == null) {
            return null;
        }

        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private void print(String str) throws IOException {
        output.append(str + "\n");
    }

    private void printXMLToken() throws IOException {
        String tag = null;

        switch (tokenizer.tokenType()) {
            case IDENTIFIER:
                tag = "identifier";
                break;

            case KEYWORD:
                tag = "keyword";
                break;

            case SYMBOL:
                tag = "symbol";
                break;

            case INT_CONST:
                tag = "integerConstant";
                break;

            case STRING_CONST:
                tag = "stringConstant";
                break;
        }

        if (tag != null) {
            print("<" + tag + "> " + escapeXML(tokenizer.currentToken.toString()) + " </" + tag + ">");
        }
    }

    private void processKeyword(KeyWord keyWords[]) throws IOException {
        if (tokenizer.tokenType() != TokenType.KEYWORD) {
            throw new Error("Keyword expected");
        }

        if (!Arrays.asList(keyWords).contains(tokenizer.keyWord())) {
            throw new Error("Invalid keyword");
        }

        printXMLToken();
        tokenizer.advance();
    }

    private void processIdentifier() throws IOException {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            throw new Error("Identifier expected");
        }

        printXMLToken();
        tokenizer.advance();
    }

    private void processIntegerConstant(int intConst) throws IOException {
        if (!(intConst >= 0 && intConst <= 32767)) {
            throw new Error("Out of range");
        }

        printXMLToken();
        tokenizer.advance();
    }

    private void processStringConstant(String strConst) throws IOException {
        printXMLToken();
        tokenizer.advance();
    }

    private void processKeywordConstant(KeyWord keywordConst) throws IOException {
        if (keywordConst != KeyWord.TRUE
                && keywordConst != KeyWord.FALSE
                && keywordConst != KeyWord.NULL
                && keywordConst != KeyWord.THIS) {
            throw new Error("Keyword constant expected");
        }

        printXMLToken();
        tokenizer.advance();
    }

    private void processType() throws IOException {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER
                && tokenizer.keyWord() != KeyWord.INT
                && tokenizer.keyWord() != KeyWord.CHAR
                && tokenizer.keyWord() != KeyWord.BOOLEAN) {

            throw new Error("Invalid type");
        }

        printXMLToken();
        tokenizer.advance();
    }

    private void processSymbol(Character symbols[]) throws IOException {
        if (!Arrays.asList(symbols).contains(tokenizer.symbol())) {
            String[] symbolStrings = Arrays.stream(symbols).map(String::valueOf).toArray(String[]::new);
            throw new Error(String.join(" or ", symbolStrings) + " expected" + ", got " + tokenizer.currentToken);
        }

        printXMLToken();
        tokenizer.advance();
    }

    public void compileClass() throws IOException {
        print("<class>");
        processKeyword(new KeyWord[] { KeyWord.CLASS });
        processIdentifier();
        processSymbol(new Character[] { '{' });

        while (tokenizer.keyWord() == KeyWord.STATIC || tokenizer.keyWord() == KeyWord.FIELD) {
            compileClassVarDec();
        }

        while (tokenizer.keyWord() == KeyWord.CONSTRUCTOR
                || tokenizer.keyWord() == KeyWord.FUNCTION
                || tokenizer.keyWord() == KeyWord.METHOD) {
            compileSubroutineDec();
        }

        processSymbol(new Character[] { '}' });
        print("</class>");
    }

    public void compileClassVarDec() throws IOException {
        print("<classVarDec>");
        processKeyword(new KeyWord[] { KeyWord.STATIC, KeyWord.FIELD });
        processType();
        processIdentifier();

        while (tokenizer.symbol() == Character.valueOf(',')) {
            processSymbol(new Character[] { ',' });
            processIdentifier();
        }
        processSymbol(new Character[] { ';' });
        print("</classVarDec>");
    }

    public void compileSubroutineDec() throws IOException {
        print("<subroutineDec>");
        processKeyword(new KeyWord[] {
                KeyWord.CONSTRUCTOR,
                KeyWord.FUNCTION,
                KeyWord.METHOD,
        });

        // process "void" or type
        if (tokenizer.keyWord() == KeyWord.VOID) {
            processKeyword(new KeyWord[] { KeyWord.VOID });
        } else {
            processType();
        }

        processIdentifier();
        processSymbol(new Character[] { '(' });
        print("<parameterList>");
        compileParameterList();
        print("</parameterList>");
        processSymbol(new Character[] { ')' });
        compileSubroutineBody();
        print("</subroutineDec>");
    }

    public void compileParameterList() throws IOException {
        if (tokenizer.symbol() != Character.valueOf(')')) {
            processType();
            processIdentifier();

            if (tokenizer.symbol() == Character.valueOf(',')) {
                processSymbol(new Character[] { ',' });
                compileParameterList();
            }
        }
    }

    public void compileSubroutineBody() throws IOException {
        print("<subroutineBody>");
        processSymbol(new Character[] { '{' });

        while (tokenizer.keyWord() == KeyWord.VAR) {
            compileVarDec();
        }

        compileStatements();
        processSymbol(new Character[] { '}' });
        print("</subroutineBody>");
    }

    public void compileVarDec() throws IOException {
        print("<varDec>");
        processKeyword(new KeyWord[] { KeyWord.VAR });
        processType();
        processIdentifier();

        while (tokenizer.symbol() == Character.valueOf(',')) {
            processSymbol(new Character[] { ',' });

            processIdentifier();
        }

        processSymbol(new Character[] { ';' });
        print("</varDec>");
    }

    public void compileStatements() throws IOException {
        print("<statements>");

        while (true) {
            if (tokenizer.keyWord() == KeyWord.LET) {
                compileLet();
            } else if (tokenizer.keyWord() == KeyWord.IF) {
                compileIf();
            } else if (tokenizer.keyWord() == KeyWord.WHILE) {
                compileWhile();
            } else if (tokenizer.keyWord() == KeyWord.DO) {
                compileDo();
            } else if (tokenizer.keyWord() == KeyWord.RETURN) {
                compileReturn();
            } else {
                break;
            }
        }

        print("</statements>");
    }

    public void compileLet() throws IOException {
        print("<letStatement>");

        processKeyword(new KeyWord[] { KeyWord.LET });
        processIdentifier();

        if (tokenizer.symbol() == Character.valueOf('[')) {
            processSymbol(new Character[] { '[' });
            compileExpression();
            processSymbol(new Character[] { ']' });
        }
        processSymbol(new Character[] { '=' });
        compileExpression();
        processSymbol(new Character[] { ';' });

        print("</letStatement>");
    }

    public void compileIf() throws IOException {
        print("<ifStatement>");

        processKeyword(new KeyWord[] { KeyWord.IF });
        processSymbol(new Character[] { '(' });
        compileExpression();
        processSymbol(new Character[] { ')' });
        processSymbol(new Character[] { '{' });
        compileStatements();
        processSymbol(new Character[] { '}' });

        if (tokenizer.keyWord() == KeyWord.ELSE) {
            processKeyword(new KeyWord[] { KeyWord.ELSE });
            processSymbol(new Character[] { '{' });
            compileStatements();
            processSymbol(new Character[] { '}' });
        }

        print("</ifStatement>");
    }

    public void compileWhile() throws IOException {
        print("<whileStatement>");

        processKeyword(new KeyWord[] { KeyWord.WHILE });
        processSymbol(new Character[] { '(' });
        compileExpression();
        processSymbol(new Character[] { ')' });
        processSymbol(new Character[] { '{' });
        compileStatements();
        processSymbol(new Character[] { '}' });

        print("</whileStatement>");
    }

    public void compileDo() throws IOException {
        print("<doStatement>");

        processKeyword(new KeyWord[] { KeyWord.DO });
        compileSubroutineCall();
        processSymbol(new Character[] { ';' });

        print("</doStatement>");
    }

    // no wrapping tag
    public void compileSubroutineCall() throws IOException {
        if (tokenizer.peekNextToken(1).equals(".")) {
            processIdentifier();
            processSymbol(new Character[] { '.' });
            processIdentifier();
        } else {
            processIdentifier();
        }

        processSymbol(new Character[] { '(' });
        compileExpressionList();
        processSymbol(new Character[] { ')' });
    }

    public void compileReturn() throws IOException {
        print("<returnStatement>");

        processKeyword(new KeyWord[] { KeyWord.RETURN });

        if (tokenizer.symbol() != Character.valueOf(';')) {
            compileExpression();
        }

        processSymbol(new Character[] { ';' });

        print("</returnStatement>");
    }

    public void compileExpression() throws IOException {
        print("<expression>");

        compileTerm();

        List<Character> opList = Arrays.asList('+', '-', '*', '/', '&', '|', '<', '>', '=');

        while (opList.contains(tokenizer.symbol())) {
            processSymbol(opList.toArray(new Character[0]));
            compileTerm();
        }

        print("</expression>");
    }

    public void compileTerm() throws IOException {
        print("<term>");

        String nextToken = tokenizer.peekNextToken(1);

        if (nextToken.equals("[")) {
            // -> varName'['expression']'
            processIdentifier();
            processSymbol(new Character[] { '[' });
            compileExpression();
            processSymbol(new Character[] { ']' });
        } else if (tokenizer.symbol() == Character.valueOf('(')) {
            // -> '('expression')'
            processSymbol(new Character[] { '(' });
            compileExpression();
            processSymbol(new Character[] { ')' });
        } else if (tokenizer.symbol() == Character.valueOf('-') || tokenizer.symbol() == Character.valueOf('~')) {
            // -> unaryOp term
            processSymbol(new Character[] { '-', '~' });
            compileTerm();
        } else if (nextToken.equals("(") || nextToken.equals(".")) {
            compileSubroutineCall();
        } else if (tokenizer.tokenType() == TokenType.INT_CONST) {
            processIntegerConstant(tokenizer.intVal());
        } else if (tokenizer.tokenType() == TokenType.STRING_CONST) {
            processStringConstant(tokenizer.stringVal());
        } else if (tokenizer.tokenType() == TokenType.KEYWORD) {
            processKeywordConstant(tokenizer.keyWord());
        } else {
            processIdentifier();
        }

        print("</term>");
    }

    public int compileExpressionList() throws IOException {
        int n = 0;

        print("<expressionList>");

        if (tokenizer.symbol() != Character.valueOf(')')) {
            compileExpression();
            n += 1;
            while (tokenizer.symbol() == Character.valueOf(',')) {
                processSymbol(new Character[] { ',' });
                compileExpression();
                n += 1;
            }
        }

        print("</expressionList>");

        return n;
    }
}
