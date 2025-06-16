package lib;

enum TokenType {
    KEYWORD,
    SYMBOL,
    IDENTIFIER,
    INT_CONST,
    STRING_CONST
}

enum KeyWord {
    CLASS,
    METHOD,
    FUNCTION,
    CONSTRUCTOR,
    INT,
    BOOLEAN,
    CHAR,
    VOID,
    VAR,
    STATIC,
    FIELD,
    LET,
    DO,
    IF,
    ELSE,
    WHILE,
    RETURN,
    TRUE,
    FALSE,
    NULL,
    THIS
}

public class Token implements Cloneable {
    TokenType type;
    KeyWord keyWord;
    Character symbol;
    String identifier;
    Integer intVal;
    String stringVal;

    public void setKeyWord(KeyWord keyWord) {
        this.type = TokenType.KEYWORD;
        this.keyWord = keyWord;
        this.symbol = null;
        this.identifier = null;
        this.intVal = null;
        this.stringVal = null;
    }

    public void setSymbol(char symbol) {
        this.type = TokenType.SYMBOL;
        this.keyWord = null;
        this.symbol = symbol;
        this.identifier = null;
        this.intVal = null;
        this.stringVal = null;
    }

    public void setIdentifier(String identifier) {
        this.type = TokenType.IDENTIFIER;
        this.keyWord = null;
        this.symbol = null;
        this.identifier = identifier;
        this.intVal = null;
        this.stringVal = null;
    }

    public void setIntVal(int intVal) {
        this.type = TokenType.INT_CONST;
        this.keyWord = null;
        this.symbol = null;
        this.identifier = null;
        this.intVal = intVal;
        this.stringVal = null;
    }

    public void setStringVal(String stringVal) {
        this.type = TokenType.STRING_CONST;
        this.keyWord = null;
        this.symbol = null;
        this.identifier = null;
        this.intVal = null;
        this.stringVal = stringVal;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public String toString() {
        switch (type) {
            case KEYWORD:
                return keyWord.toString().toLowerCase();
            case SYMBOL:
                return String.valueOf(symbol);
            case IDENTIFIER:
                return identifier;
            case INT_CONST:
                return String.valueOf(intVal);
            case STRING_CONST:
                return stringVal;
            default:
                return null;
        }
    }
}