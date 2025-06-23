package lib;

import java.util.HashMap;
import java.util.Map;

enum Kind {
    STATIC,
    FIELD,
    ARG,
    VAR,
    NONE;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

public class SymbolTable {
    private static class SymbolTableEntry {
        String type;
        Kind kind;
        int index;

        SymbolTableEntry(String type, Kind kind, int index) {
            this.type = type;
            this.kind = kind;
            this.index = index;
        }
    }

    private Map<Kind, Integer> indices = new HashMap<>();
    private Map<String, SymbolTableEntry> table = new HashMap<>();

    public SymbolTable() {
        reset();
    }

    public void reset() {
        indices.clear();
        table.clear();

        for (Kind kind : new Kind[] { Kind.STATIC, Kind.FIELD, Kind.VAR, Kind.ARG }) {
            indices.put(kind, 0);
        }
    }

    public void define(String name, String type, Kind kind) {
        Integer index = indices.get(kind);

        if (table.get(name) != null) {
            throw new Error("Identifier '" + name + "' already defined");
        }

        table.put(name, new SymbolTableEntry(type, kind, index));
        indices.put(kind, index + 1);
    }

    public int varCount(Kind kind) {
        return (int) table.values().stream().filter(entry -> entry.kind == kind).count();
    }

    public Kind kindOf(String name) {
        SymbolTableEntry entry = table.get(name);
        return entry != null ? entry.kind : Kind.NONE;
    }

    public String typeOf(String name) {
        SymbolTableEntry entry = table.get(name);
        return entry != null ? entry.type : null;
    }

    public int indexOf(String name) {
        SymbolTableEntry entry = table.get(name);
        return entry != null ? entry.index : -1;
    }

    public void print() {
        System.out.println("      Name |     Type |     Kind |   #");
        System.out.println("--------------------------------------");

        for (String key : table.keySet()) {
            SymbolTableEntry entry = table.get(key);
            System.out.printf("%10s | %8s | %8s | %3d\n",
                    key,
                    entry.type.toString(),
                    entry.kind.toString(),
                    entry.index);
        }
    }
}
