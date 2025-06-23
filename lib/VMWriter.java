package lib;

import java.io.IOException;
import java.io.Writer;

enum Segment {
    CONSTANT,
    ARGUMENT,
    LOCAL,
    STATIC,
    THIS,
    THAT,
    POINTER,
    TEMP;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

enum Command {
    ADD,
    SUB,
    NEG,
    EQ,
    GT,
    LT,
    AND,
    OR,
    NOT;

    public static Command fromBinaryOperator(Character binOp) {
        switch (binOp) {
            case '+':
                return Command.ADD;
            case '-':
                return Command.SUB;
            case '=':
                return Command.EQ;
            case '>':
                return Command.GT;
            case '<':
                return Command.LT;
            case '&':
                return Command.AND;
            case '|':
                return Command.OR;
            default:
                return null;
        }
    }
    
    public static Command fromUnaryOperator(Character unaryOp) {
        switch (unaryOp) {
            case '-':
                return Command.NEG;
            case '~':
                return Command.NOT;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

public class VMWriter {
    private final Writer output;
    private int identationSize = 0;

    public VMWriter(Writer output) {
        this.output = output;
    }

    private void write(String str) throws IOException {
        for (int i = 0; i < identationSize; i++) {
            output.append(" ");
        }
        output.append(str + "\n");
    }

    void setIndentationSize(int identationSize) {
        this.identationSize = identationSize;
    }

    void writePush(Segment segment, int index) throws IOException {
        write("push " + segment + " " + index);
    }

    void writePop(Segment segment, int index) throws IOException {
        write("pop " + segment + " " + index);
    }

    void writeArithmetic(Command command) throws IOException {
        write(command.toString());
    }

    void writeLabel(String label) throws IOException {
        int n = identationSize;
        setIndentationSize(0);
        write("label " + label);
        setIndentationSize(n);
    }

    void writeGoto(String label) throws IOException {
        write("goto " + label);
    }

    void writeIf(String label) throws IOException {
        write("if-goto " + label);
    }

    void writeCall(String name, int nArgs) throws IOException {
        write("call " + name + " " + nArgs);
    }

    void writeFunction(String name, int nVars) throws IOException {
        write("function " + name + " " + nVars);
    }

    void writeReturn() throws IOException {
        write("return");
    }

    void close() throws IOException {
        output.close();
    }
}
