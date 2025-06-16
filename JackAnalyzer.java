import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

import lib.*;

public class JackAnalyzer {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: JackAnalyzer <input file or directory>");
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
            String outputFile = jackFile.getAbsolutePath().replaceAll("\\.jack$", ".xml");
            try (
                    Reader input = new FileReader(jackFile);
                    Writer output = new FileWriter(outputFile)) {
                CompilationEngine compilationEngine = new CompilationEngine(input, output);
                compilationEngine.compileClass();
            }
        }
    }
}