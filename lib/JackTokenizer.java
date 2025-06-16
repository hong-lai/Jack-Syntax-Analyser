package lib;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;

public class JackTokenizer {
    private Reader reader;
    private int currentChar;
    public Token currentToken = new Token();
    private Deque<Integer> charBuffer = new ArrayDeque<>();
    private Deque<Token> tokenBuffer = new ArrayDeque<>();

    public JackTokenizer(Reader reader) throws IOException {
        this.reader = reader;
        this.getChar();
    }

    public boolean hasMoreTokens() {
        return currentChar != -1;
    }

    public String peekNextToken(int lookahead) throws IOException {
        Token savedCurrentToken = (Token) currentToken.clone();

        for (int i = 0; i < lookahead; i++) {
            advance();
            tokenBuffer.add((Token) currentToken.clone());
        }

        currentToken = savedCurrentToken;

        return tokenBuffer.getFirst().toString();
    }

    private void getChar() throws IOException {
        if (charBuffer.size() > 0) {
            currentChar = charBuffer.poll();
        } else {
            currentChar = reader.read();
        }
    }

    private void ungetChar() {
        charBuffer.add(currentChar);
    }

    public void advance() throws IOException {
        if (tokenBuffer.size() > 0) {
            currentToken = tokenBuffer.pop();
        } else if (hasMoreTokens()) {
            // Skip white spaces and bunch of specified characters
            while (Character.isWhitespace(currentChar)) {
                getChar();
            }

            // Skip comment
            if (currentChar == '/') {
                getChar();
                if (currentChar == '/') {
                    // Skip // style comment
                    do {
                        getChar();
                    } while (hasMoreTokens() && currentChar != '\r' && currentChar != '\n');

                    advance();
                    return;
                } else if (currentChar == '*') {
                    // Skip /* */ style comment
                    getChar();
                    int prevChar = currentChar;
                    while (hasMoreTokens()) {
                        getChar();
                        if (prevChar == '*' && currentChar == '/') {
                            getChar();
                            break;
                        }
                        prevChar = currentChar;
                    }

                    advance();
                    return;
                } else {
                    ungetChar();
                    currentChar = '/';
                }
            }

            // Symbol
            if ("{}()[].,;+-_*/&|<>=~".contains(String.valueOf((char) currentChar))) {
                currentToken.setSymbol((char) currentChar);
            }

            // Integer Constant
            else if (Character.isDigit(currentChar)) {
                StringBuilder sb = new StringBuilder();
                while (Character.isDigit(currentChar)) {
                    sb.append((char) currentChar);
                    getChar();
                }
                ungetChar();
                currentToken.setIntVal(Integer.parseInt(sb.toString()));
            }

            // String Constant
            else if (currentChar == '"') {
                getChar();
                if (!hasMoreTokens()) {
                    throw new Error("Double quote expected");
                }

                StringBuilder sb = new StringBuilder();

                while (currentChar != '"') {
                    sb.append((char) currentChar);
                    getChar();
                    if (!hasMoreTokens()) {
                        throw new Error("Double quote expected");
                    }
                }
                currentToken.setStringVal(sb.toString());
            }

            // Keyword / identifier
            else if (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
                StringBuilder sb = new StringBuilder();

                while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
                    sb.append((char) currentChar);
                    getChar();
                }
                ungetChar();

                if (KeywordMatcher.match(sb.toString())) {
                    currentToken.setKeyWord(KeyWord.valueOf(sb.toString().toUpperCase()));
                } else {
                    currentToken.setIdentifier(sb.toString());
                }
            }

            getChar();
        }
    }

    public TokenType tokenType() {
        return currentToken.type;
    }

    public KeyWord keyWord() {
        return currentToken.keyWord;
    }

    public Character symbol() {
        return currentToken.symbol;
    }

    public String identifier() {
        return currentToken.identifier;
    }

    public Integer intVal() {
        return currentToken.intVal;
    }

    public String stringVal() {
        return currentToken.stringVal;
    }
}
