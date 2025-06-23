package lib;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

public class CompilationEngine {
    private JackTokenizer tokenizer;
    private VMWriter vmWriter;

    public SymbolTable symTblClass;
    public SymbolTable symTblSubroutine;

    private String currentClassName = "";
    private String currentSubroutineName = "";
    private KeyWord currentSubroutineType;
    private int runningIndex = 0;

    public CompilationEngine(Reader input, Writer output) throws IOException {
        tokenizer = new JackTokenizer(input);
        tokenizer.advance();
        symTblClass = new SymbolTable();
        symTblSubroutine = new SymbolTable();
        vmWriter = new VMWriter(output);
    }

    private static class SymbolTableResult {
        Segment segment;
        String type;
        int index;

        SymbolTableResult(Segment segment, int index, String type) {
            this.segment = segment;
            this.index = index;
            this.type = type;
        }
    }

    private SymbolTableResult lookupSymbolTables(String name) {
        if (symTblSubroutine.kindOf(name) != Kind.NONE) {
            int index = symTblSubroutine.indexOf(name);

            if (symTblSubroutine.kindOf(name) == Kind.VAR) {
                return new SymbolTableResult(Segment.LOCAL, index, symTblSubroutine.typeOf(name));
            } else {
                return new SymbolTableResult(Segment.ARGUMENT, index, symTblSubroutine.typeOf(name));
            }

        } else if (symTblClass.kindOf(name) != Kind.NONE) {
            int index = symTblClass.indexOf(name);

            if (symTblClass.kindOf(name) == Kind.FIELD) {
                return new SymbolTableResult(Segment.THIS, index, symTblClass.typeOf(name));
            } else {
                return new SymbolTableResult(Segment.STATIC, index, symTblClass.typeOf(name));
            }
        }

        // assume it's a class name if not found in the tables
        return null;
    }

    private KeyWord processKeyword(KeyWord keyWords[]) throws IOException {
        if (tokenizer.tokenType() != TokenType.KEYWORD) {
            throw new Error("Keyword expected");
        }

        if (!Arrays.asList(keyWords).contains(tokenizer.keyWord())) {
            throw new Error("Invalid keyword");
        }

        KeyWord keyword = tokenizer.keyWord();
        tokenizer.advance();

        return keyword;
    }

    private String processIdentifier() throws IOException {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            throw new Error("Identifier expected");
        }

        String identifier = tokenizer.currentToken.toString();
        tokenizer.advance();

        return identifier;
    }

    private Integer processIntegerConstant(int intConst) throws IOException {
        if (!(intConst >= 0 && intConst <= 32767)) {
            throw new Error("Out of range");
        }

        Integer intVal = tokenizer.intVal();
        tokenizer.advance();

        return intVal;
    }

    private String processStringConstant(String strConst) throws IOException {
        String stringVal = tokenizer.stringVal();
        tokenizer.advance();

        return stringVal;
    }

    private KeyWord processKeywordConstant(KeyWord keywordConst) throws IOException {
        if (keywordConst != KeyWord.TRUE
                && keywordConst != KeyWord.FALSE
                && keywordConst != KeyWord.NULL
                && keywordConst != KeyWord.THIS) {
            throw new Error("Keyword constant expected");
        }

        KeyWord keyword = keywordConst;
        tokenizer.advance();

        return keyword;
    }

    private String processType() throws IOException {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER
                && tokenizer.keyWord() != KeyWord.INT
                && tokenizer.keyWord() != KeyWord.CHAR
                && tokenizer.keyWord() != KeyWord.BOOLEAN) {

            throw new Error("Invalid type");
        }

        String token = tokenizer.currentToken.toString();
        tokenizer.advance();

        return token;
    }

    private Character processSymbol(Character symbols[]) throws IOException {
        if (!Arrays.asList(symbols).contains(tokenizer.symbol())) {
            String[] symbolStrings = Arrays.stream(symbols).map(String::valueOf).toArray(String[]::new);
            throw new Error(String.join(" or ", symbolStrings) + " expected" + ", got " + tokenizer.currentToken);
        }

        Character symbol = tokenizer.symbol();
        tokenizer.advance();

        return symbol;
    }

    public void compileClass() throws IOException {
        processKeyword(new KeyWord[] { KeyWord.CLASS });

        currentClassName = processIdentifier();
        symTblClass.reset();

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
    }

    public void compileClassVarDec() throws IOException {
        KeyWord kind = processKeyword(new KeyWord[] { KeyWord.STATIC, KeyWord.FIELD });
        String type = processType();
        String name = processIdentifier();

        symTblClass.define(name, type, Kind.valueOf(kind.toString().toUpperCase()));

        while (tokenizer.symbol() == Character.valueOf(',')) {
            processSymbol(new Character[] { ',' });
            name = processIdentifier();

            symTblClass.define(name, type, Kind.valueOf(kind.toString().toUpperCase()));
        }
        processSymbol(new Character[] { ';' });
    }

    public void compileSubroutineDec() throws IOException {
        symTblSubroutine.reset();

        currentSubroutineType = processKeyword(new KeyWord[] {
                KeyWord.CONSTRUCTOR,
                KeyWord.FUNCTION,
                KeyWord.METHOD,
        });

        if (currentSubroutineType == KeyWord.METHOD) {
            symTblSubroutine.define("this", currentClassName, Kind.ARG);
        }

        // process "void" or type
        if (tokenizer.keyWord() == KeyWord.VOID) {
            processKeyword(new KeyWord[] { KeyWord.VOID });
        } else {
            processType();
        }

        currentSubroutineName = processIdentifier();
        processSymbol(new Character[] { '(' });
        compileParameterList();
        processSymbol(new Character[] { ')' });
        compileSubroutineBody();
    }

    public void compileParameterList() throws IOException {
        if (tokenizer.symbol() != Character.valueOf(')')) {
            String type = processType();
            String name = processIdentifier();

            symTblSubroutine.define(name, type, Kind.ARG);

            if (tokenizer.symbol() == Character.valueOf(',')) {
                processSymbol(new Character[] { ',' });
                compileParameterList();
            }
        }
    }

    public void compileSubroutineBody() throws IOException {
        processSymbol(new Character[] { '{' });

        while (tokenizer.keyWord() == KeyWord.VAR) {
            compileVarDec();
        }

        // must first evaluate the symbol table to get the number of vars
        vmWriter.writeFunction(currentClassName + "." + currentSubroutineName, symTblSubroutine.varCount(Kind.VAR));
        vmWriter.setIndentationSize(4);

        if (currentSubroutineType == KeyWord.METHOD) {
            vmWriter.writePush(Segment.ARGUMENT, 0);
            vmWriter.writePop(Segment.POINTER, 0);

        } else if (currentSubroutineType == KeyWord.CONSTRUCTOR) {
            vmWriter.writePush(Segment.CONSTANT, symTblClass.varCount(Kind.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(Segment.POINTER, 0);
        }

        compileStatements();
        processSymbol(new Character[] { '}' });

        vmWriter.setIndentationSize(0);
    }

    public void compileVarDec() throws IOException {
        processKeyword(new KeyWord[] { KeyWord.VAR });
        String type = processType();
        String name = processIdentifier();

        symTblSubroutine.define(name, type, Kind.VAR);

        while (tokenizer.symbol() == Character.valueOf(',')) {
            processSymbol(new Character[] { ',' });

            name = processIdentifier();
            symTblSubroutine.define(name, type, Kind.VAR);
        }

        processSymbol(new Character[] { ';' });
    }

    public void compileStatements() throws IOException {
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
    }

    public void compileLet() throws IOException {
        processKeyword(new KeyWord[] { KeyWord.LET });
        String name = processIdentifier();

        SymbolTableResult symbolTableResult = lookupSymbolTables(name);
        boolean isArray = false;

        if (tokenizer.symbol() == Character.valueOf('[')) {
            processSymbol(new Character[] { '[' });
            compileExpression();

            // add the array base address
            vmWriter.writePush(symbolTableResult.segment, symbolTableResult.index);
            vmWriter.writeArithmetic(Command.ADD);

            isArray = true;

            processSymbol(new Character[] { ']' });
        }
        processSymbol(new Character[] { '=' });
        compileExpression();

        if (isArray) {
            vmWriter.writePop(Segment.TEMP, 0);
            vmWriter.writePop(Segment.POINTER, 1);
            vmWriter.writePush(Segment.TEMP, 0);
            vmWriter.writePop(Segment.THAT, 0);
        } else {
            vmWriter.writePop(symbolTableResult.segment, symbolTableResult.index);
        }

        processSymbol(new Character[] { ';' });
    }

    public void compileIf() throws IOException {
        processKeyword(new KeyWord[] { KeyWord.IF });
        processSymbol(new Character[] { '(' });

        String L1 = currentClassName + "_" + runningIndex++;
        String L2 = currentClassName + "_" + runningIndex++;

        compileExpression();
        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(L2);

        processSymbol(new Character[] { ')' });
        processSymbol(new Character[] { '{' });
        compileStatements();
        processSymbol(new Character[] { '}' });

        vmWriter.writeGoto(L1);
        vmWriter.writeLabel(L2);

        if (tokenizer.keyWord() == KeyWord.ELSE) {
            processKeyword(new KeyWord[] { KeyWord.ELSE });
            processSymbol(new Character[] { '{' });
            compileStatements();
            processSymbol(new Character[] { '}' });
        }

        vmWriter.writeLabel(L1);
    }

    public void compileWhile() throws IOException {
        processKeyword(new KeyWord[] { KeyWord.WHILE });
        processSymbol(new Character[] { '(' });

        String L1 = currentClassName + "_" + runningIndex++;
        String L2 = currentClassName + "_" + runningIndex++;

        vmWriter.writeLabel(L1);

        compileExpression();
        // if the expression is not true then exit (L2)
        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(L2);

        processSymbol(new Character[] { ')' });
        processSymbol(new Character[] { '{' });
        compileStatements();

        vmWriter.writeGoto(L1);
        processSymbol(new Character[] { '}' });

        vmWriter.writeLabel(L2);
    }

    public void compileDo() throws IOException {
        processKeyword(new KeyWord[] { KeyWord.DO });
        compileSubroutineCall();
        // dump the top stack value
        vmWriter.writePop(Segment.TEMP, 0);
        processSymbol(new Character[] { ';' });
    }

    // no wrapping tag
    public void compileSubroutineCall() throws IOException {
        String name = "";
        int thisArg = 0;

        if (tokenizer.peekNextToken(1).equals(".")) {
            name = processIdentifier();

            SymbolTableResult symbolTableResult = lookupSymbolTables(name);

            // instance var name changes to class name, push the corresponding 'this'
            if (symbolTableResult != null) {
                name = symbolTableResult.type;
                vmWriter.writePush(symbolTableResult.segment, symbolTableResult.index);
                thisArg = 1;
            }

            processSymbol(new Character[] { '.' });
            name += "." + processIdentifier();
        } else {
            // for constructor and method subroutine, function call without '.' must be
            // refer to the current class so the prefix is needed (e.g. methodA =>
            // ClassA.methodA)
            if (currentSubroutineType == KeyWord.CONSTRUCTOR || currentSubroutineType == KeyWord.METHOD) {
                name = currentClassName + ".";
                vmWriter.writePush(Segment.POINTER, 0);
                thisArg = 1;
            }
            name += processIdentifier();
        }

        processSymbol(new Character[] { '(' });
        int nArgs = compileExpressionList();
        processSymbol(new Character[] { ')' });

        vmWriter.writeCall(name, nArgs + thisArg);
    }

    public void compileReturn() throws IOException {
        processKeyword(new KeyWord[] { KeyWord.RETURN });

        if (tokenizer.symbol() != Character.valueOf(';')) {
            compileExpression();
            vmWriter.writeReturn();
        } else {
            // add a dummy 0
            vmWriter.writePush(Segment.CONSTANT, 0);
            vmWriter.writeReturn();
        }

        processSymbol(new Character[] { ';' });
    }

    public void compileExpression() throws IOException {
        compileTerm();

        List<Character> binOpList = Arrays.asList('+', '-', '*', '/', '&', '|', '<', '>', '=');

        while (binOpList.contains(tokenizer.symbol())) {
            Character binOp = processSymbol(binOpList.toArray(new Character[0]));
            compileTerm();

            if (binOp.equals('*')) {
                vmWriter.writeCall("Math.multiply", 2);
            } else if (binOp.equals('/')) {
                vmWriter.writeCall("Math.divide", 2);
            } else {
                vmWriter.writeArithmetic(Command.fromBinaryOperator(binOp));
            }
        }
    }

    public void compileTerm() throws IOException {
        String nextToken = tokenizer.peekNextToken(1);

        if (nextToken.equals("[")) {
            // -> varName'['expression']'
            String name = processIdentifier();
            processSymbol(new Character[] { '[' });
            compileExpression();

            // add the array base address and make the target value on the top stack
            SymbolTableResult symbolTableResult = lookupSymbolTables(name);
            vmWriter.writePush(symbolTableResult.segment, symbolTableResult.index);
            vmWriter.writeArithmetic(Command.ADD);
            vmWriter.writePop(Segment.POINTER, 1);
            vmWriter.writePush(Segment.THAT, 0);

            processSymbol(new Character[] { ']' });
        } else if (tokenizer.symbol() == Character.valueOf('(')) {
            // -> '('expression')'
            processSymbol(new Character[] { '(' });
            compileExpression();
            processSymbol(new Character[] { ')' });
        } else if (tokenizer.symbol() == Character.valueOf('-') || tokenizer.symbol() == Character.valueOf('~')) {
            // -> unaryOp term
            Character unaryOp = processSymbol(new Character[] { '-', '~' });
            compileTerm();

            vmWriter.writeArithmetic(Command.fromUnaryOperator(unaryOp));
        } else if (nextToken.equals("(") || nextToken.equals(".")) {
            compileSubroutineCall();
        } else if (tokenizer.tokenType() == TokenType.INT_CONST) {
            int intVal = processIntegerConstant(tokenizer.intVal());

            vmWriter.writePush(Segment.CONSTANT, intVal);
        } else if (tokenizer.tokenType() == TokenType.STRING_CONST) {
            String stringVal = processStringConstant(tokenizer.stringVal());
            // push the string len and call 'String.new' constructor, and then push the char
            // code using 'append' method
            vmWriter.writePush(Segment.CONSTANT, stringVal.length());
            vmWriter.writeCall("String.new", 1);

            for (char ch : stringVal.toCharArray()) {
                vmWriter.writePush(Segment.CONSTANT, ch); // char code
                vmWriter.writeCall("String.appendChar", 2); // seems to return the address same as String.new
            }

        } else if (tokenizer.tokenType() == TokenType.KEYWORD) {
            KeyWord keyword = processKeywordConstant(tokenizer.keyWord());

            switch (keyword) {
                case THIS:
                    vmWriter.writePush(Segment.POINTER, 0);
                    break;
                case TRUE: // -1
                    vmWriter.writePush(Segment.CONSTANT, 1);
                    vmWriter.writeArithmetic(Command.NEG);
                    break;
                case FALSE: // 0
                case NULL:
                    vmWriter.writePush(Segment.CONSTANT, 0);
                    break;
                default:
                    break;
            }
        } else {
            String name = processIdentifier();
            SymbolTableResult symbolTableResult = lookupSymbolTables(name);

            vmWriter.writePush(symbolTableResult.segment, symbolTableResult.index);
        }
    }

    public int compileExpressionList() throws IOException {
        int n = 0;

        if (tokenizer.symbol() != Character.valueOf(')')) {
            compileExpression();
            n += 1;
            while (tokenizer.symbol() == Character.valueOf(',')) {
                processSymbol(new Character[] { ',' });
                compileExpression();
                n += 1;
            }
        }

        return n;
    }
}
