import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

import lib.*;

public class JackCompiler {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: JackCompiler <input file or directory>");
            return;
        }

        File inputPath = new File(args[0]);
        File[] jackFiles;

        if (inputPath.isDirectory()) {
            jackFiles = inputPath.listFiles((dir, name) -> name.endsWith(".jack"));
        } else if (inputPath.isFile() && inputPath.getName().endsWith(".jack")) {
            jackFiles = new File[] { inputPath };
        } else {
            System.out.println("Input must be a jack file or a directory containing jack files.");
            System.exit(1);
            return;
        }

        for (File jackFile : jackFiles) {
            String outputFile = jackFile.getAbsolutePath().replaceAll("\\.jack$", ".vm");
            try (
                    Reader input = new BufferedReader(new FileReader(jackFile));
                    Writer output = new BufferedWriter(new FileWriter(outputFile))) {

                CompilationEngine compilationEngine = new CompilationEngine(input, output);
                compilationEngine.compileClass();
            }
        }
    }
}