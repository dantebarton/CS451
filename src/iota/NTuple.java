// Copyright 2024- Swami Iyer

package iota;

/**
 * An abstract representation of a JVM instruction as a tuple.
 */
abstract class NTuple {
    /**
     * Program counter of the JVM instruction.
     */
    public int pc;

    /**
     * Opcode of the JVM instruction.
     */
    public int opcode;

    /**
     * Mnemonic of the JVM instruction.
     */
    public String mnemonic;

    /**
     * Is this tuple the leader of the basic block containing it.
     */
    public boolean isLeader;

    /**
     * Constructs an NTuple representing a JVM instruction.
     *
     * @param pc     program counter of the instruction.
     * @param opcode opcode of the instruction.
     */
    protected NTuple(int pc, int opcode) {
        this.pc = pc;
        this.opcode = opcode;
        this.mnemonic = CLInstruction.instructionInfo[opcode].mnemonic;
        this.isLeader = false;
    }

    /**
     * Writes the information pertaining to this tuple to standard output.
     *
     * @param p for pretty printing with indentation.
     */
    public abstract void writeToStdOut(PrettyPrinter p);
}

/**
 * Tuple representation of a branch instruction.
 */
class NBranchTuple extends NTuple {
    /**
     * Branch location.
     */
    public short location;

    /**
     * Constructs a BranchTuple representing a branch instruction.
     *
     * @param pc       program counter of the instruction.
     * @param opcode   opcode of the instruction.
     * @param location branch location.
     */
    public NBranchTuple(int pc, int opcode, short location) {
        super(pc, opcode);
        this.location = location;
    }

    /**
     * {@inheritDoc}
     */
    public void writeToStdOut(PrettyPrinter p) {
        p.printf("%s: %s %s\n", pc, mnemonic, location);
    }
}

/**
 * Tuple representation of LDC instruction.
 */
class NLDCTuple extends NTuple {
    /**
     * The integer constant corresponding to the LDC instruction.
     */
    public int value;

    /**
     * Constructs an LDCTuple representing LDC instruction.
     *
     * @param pc     program counter of the instruction.
     * @param opcode opcode of the instruction.
     * @param value  integer constant corresponding to the instruction.
     */
    public NLDCTuple(int pc, int opcode, int value) {
        super(pc, opcode);
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public void writeToStdOut(PrettyPrinter p) {
        p.printf("%s: %s %s\n", pc, mnemonic, value);
    }
}

/**
 * Tuple representation of load-store instructions.
 */
class NLoadStoreTuple extends NTuple {
    /**
     * Variable offset.
     */
    public byte offset;

    /**
     * Constructs a LoadStoreTuple representing a load-store instruction.
     *
     * @param pc     program counter of the instruction.
     * @param opcode opcode of the instruction.
     * @param offset variable offset.
     */
    public NLoadStoreTuple(int pc, int opcode, byte offset) {
        super(pc, opcode);
        this.offset = offset;
    }

    /**
     * {@inheritDoc}
     */
    public void writeToStdOut(PrettyPrinter p) {
        p.printf("%s: %s %s\n", pc, mnemonic, offset);
    }
}

/**
 * Tuple representation of method call (INVOKESTATIC) instruction.
 */
class NMethodCallTuple extends NTuple {
    /**
     * Name of the method.
     */
    public String name;

    /**
     * Descriptor of the method.
     */
    public String descriptor;

    /**
     * Constructs a MethodCallTuple representing a method call instruction.
     *
     * @param pc         program counter of the instruction.
     * @param opcode     opcode of the instruction.
     * @param name       name of the method.
     * @param descriptor descriptor of the method.
     */
    public NMethodCallTuple(int pc, int opcode, String name, String descriptor) {
        super(pc, opcode);
        this.name = name;
        this.descriptor = descriptor;
    }

    /**
     * {@inheritDoc}
     */
    public void writeToStdOut(PrettyPrinter p) {
        p.printf("%s: %s %s%s\n", pc, mnemonic, name, descriptor);
    }
}

/**
 * Tuple representation of a no-argument instruction.
 */
class NNoArgTuple extends NTuple {
    /**
     * Constructs a NoArgTuple representing a no-argument instruction.
     *
     * @param pc     program counter of the instruction.
     * @param opcode opcode of the instruction.
     */
    public NNoArgTuple(int pc, int opcode) {
        super(pc, opcode);
    }

    /**
     * {@inheritDoc}
     */
    public void writeToStdOut(PrettyPrinter p) {
        p.printf("%s: %s\n", pc, mnemonic);
    }
}
