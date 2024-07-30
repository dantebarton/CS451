// Copyright 2024- Swami Iyer

package iota;

import java.io.File;
import java.util.HashMap;

/**
 * A class for generating HMMM code.
 */
public class NEmitter {
    // Source program file name.
    private String sourceFile;

    // Maps a method to its control flow graph.
    private HashMap<CLMethodInfo, NControlFlowGraph> methods;

    // Destination directory for the HMMM code (ie, .hmmm file).
    private String destDir;

    // Whether an error occurred while creating/writing HMMM code.
    private boolean errorHasOccurred;

    /**
     * Constructs an HMMMEmitter object.
     *
     * @param sourceFile the source iota program file name.
     * @param clFile     JVM bytecode representation of the program.
     * @param ra         register allocation scheme (naive or graph) to use.
     * @param verbose    whether to produce verbose output or not.
     */
    public NEmitter(String sourceFile, CLFile clFile, String ra, boolean verbose) {
        this.sourceFile = sourceFile.substring(sourceFile.lastIndexOf(File.separator) + 1);
        methods = new HashMap<CLMethodInfo, NControlFlowGraph>();
        CLConstantPool cp = clFile.constantPool;
        for (int i = 0; i < clFile.methodsCount; i++) {
            CLMethodInfo m = clFile.methods.get(i);

            String name = new String(((CLConstantUtf8Info) cp.cpItem(m.nameIndex)).b);
            String desc = new String(((CLConstantUtf8Info) cp.cpItem(m.descriptorIndex)).b);

            if (name.equals("read") && desc.equals("()I") || name.equals("write") && desc.equals("(I)V") ||
                name.equals("write") && desc.equals("(Z)V")) {
                continue;
            }

            // Build a control flow graph (cfg) for this method. Each block in the cfg, at the end of this step, has
            // the JVM bytecode translated into tuple representation.
            NControlFlowGraph cfg = new NControlFlowGraph(cp, m, name, desc);

            cfg.detectLoops(cfg.basicBlocks.get(0), null);

            cfg.removeUnreachableBlocks();

            cfg.tuplesToHir();

            cfg.cleanupPhiFunctions();

            cfg.hirToLir();

            cfg.resolvePhiFunctions();

            cfg.renumberLirInstructions();

            methods.put(m, cfg);

            if (verbose) {
                // Write the tuples in cfg to standard output.
                PrettyPrinter p = new PrettyPrinter();
                p.printf(">>> %s%s\n", cfg.name, cfg.descriptor);
                cfg.writeTuplesToStdOut(p);
                cfg.writeHirToStdOut(p);
                cfg.writeLirToStdOut(p);
            }
        }
    }

    /**
     * Sets the destination directory for the .hmmm file.
     *
     * @param destDir the destination directory.
     */
    public void destinationDir(String destDir) {
        this.destDir = destDir;
    }

    /**
     * Returns true if an emitter error has occurred up to now, and false otherwise.
     *
     * @return true if an emitter error has occurred up to now, and false otherwise.
     */
    public boolean errorHasOccurred() {
        return errorHasOccurred;
    }

    /**
     * Writes out .hmmm file to the file system. The destination directory for the files can be set using the
     * destinationDir() method.
     */
    public void write() {
    }

    // Reports any error that occurs while creating/writing the spim file, to standard error.
    private void reportEmitterError(String message, Object... args) {
        System.err.printf("Error: " + message, args);
        System.err.println();
        errorHasOccurred = true;
    }
}

/**
 * A utility class that allows pretty (indented) printing to standard output.
 */
class PrettyPrinter {
    // Width of an indentation.
    private int indentWidth;

    // Current indentation (number of blank spaces).
    private int indent;

    /**
     * Constructs a pretty printer with an indentation width of 2.
     */
    public PrettyPrinter() {
        this(2);
    }

    /**
     * Constructs a pretty printer.
     *
     * @param indentWidth number of blank spaces for an indent.
     */
    public PrettyPrinter(int indentWidth) {
        this.indentWidth = indentWidth;
        indent = 0;
    }

    /**
     * Indents right.
     */
    public void indentRight() {
        indent += indentWidth;
    }

    /**
     * Indents left.
     */
    public void indentLeft() {
        if (indent > 0) {
            indent -= indentWidth;
        }
    }

    /**
     * Prints an empty line to standard output.
     */
    public void println() {
        doIndent();
        System.out.println();
    }

    /**
     * Prints the specified string (followed by a newline) to standard output.
     *
     * @param s string to print.
     */

    public void println(String s) {
        doIndent();
        System.out.println(s);
    }

    /**
     * Prints the specified string to standard output.
     *
     * @param s string to print.
     */
    public void print(String s) {
        doIndent();
        System.out.print(s);
    }

    /**
     * Prints args to standard output according to the specified format.
     *
     * @param format format specifier.
     * @param args   values to print.
     */
    public void printf(String format, Object... args) {
        doIndent();
        System.out.printf(format, args);
    }

    // Indents by printing spaces to standard output.
    private void doIndent() {
        for (int i = 0; i < indent; i++) {
            System.out.print(" ");
        }
    }
}
