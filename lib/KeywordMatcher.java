package lib;

import java.util.HashMap;
import java.util.Map;

public class KeywordMatcher {
    private static final String[] keywords = {
            "class", "constructor", "function", "method", "field", "static",
            "var", "int", "char", "boolean", "void", "true", "false", "null",
            "this", "let", "do", "if", "else", "while", "return"
    };

    private static final Trie keywordTrie = new Trie();

    static {
        for (String keyword : keywords) {
            keywordTrie.insert(keyword);
        }
    }

    public static boolean match(String word) {
        return keywordTrie.has(word);
    }

    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord = false;
    }

    private static class Trie {
        TrieNode root = new TrieNode();

        void insert(String word) {
            TrieNode current = root;
            for (char ch : word.toCharArray()) {
                current = current.children.computeIfAbsent(ch, key -> new TrieNode());
            }
            current.isEndOfWord = true;
        }

        boolean has(String word) {
            TrieNode current = root;
            for (char ch : word.toCharArray()) {
                current = current.children.get(ch);
                if (current == null) {
                    return false;
                }
            }
            return current.isEndOfWord;
        }
    }
}
