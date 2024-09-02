package iota;

import static iota.CLConstants.*;

/**
 * Abstract representation of a JVM instruction as a tuple.
 */
abstract class NTuple {
    /**
     * Program counter of the instruction.
     */
    public int pc;

    /**
     * Opcode of the instruction.
     */
    public int opcode;

    /**
     * Mnemonic of the instruction.
     */
    public String mnemonic;

    /**
     * Whether the tuple is the leader of the basic block containing it.
     */
    public boolean isLeader;

    /**
     * Constructs an NTuple object.
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
 * Representation of a branch instruction.
 */
class NBranchTuple extends NTuple {
    /**
     * Branch location.
     */
    public short location;

    /**
     * Constructs an NBranchTuple object.
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
 * Representation of an instruction for loading an integer constant.
 */
class NIConstTuple extends NTuple {
    /**
     * The integer constant.
     */
    public int N;

    /**
     * Constructs an NIConstTuple object.
     *
     * @param pc program counter of the instruction.
     * @param N  the integer constant.
     */
    public NIConstTuple(int pc, int N) {
        super(pc, LDC);
        this.N = N;
    }

    /**
     * {@inheritDoc}
     */
    public void writeToStdOut(PrettyPrinter p) {
        p.printf("%s: %s %s\n", pc, mnemonic, N);
    }
}

/**
 * Representation of an instruction for calling static methods.
 */
class NInvokestaticTuple extends NTuple {
    /**
     * Name of the method.
     */
    public String name;

    /**
     * Descriptor of the method.
     */
    public String descriptor;

    /**
     * Constructs an NInvokestaticTuple object.
     *
     * @param pc         program counter of the instruction.
     * @param name       name of the method.
     * @param descriptor descriptor of the method.
     */
    public NInvokestaticTuple(int pc, String name, String descriptor) {
        super(pc, INVOKESTATIC);
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
 * Representation of a load-store instruction.
 */
class NLoadStoreTuple extends NTuple {
    /**
     * Index of the variable to load from or store to.
     */
    public byte index;

    /**
     * Constructs an NLoadStoreTuple object.
     *
     * @param pc     program counter of the instruction.
     * @param opcode opcode of the instruction.
     * @param index  variable index.
     */
    public NLoadStoreTuple(int pc, int opcode, byte index) {
        super(pc, opcode);
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */
    public void writeToStdOut(PrettyPrinter p) {
        p.printf("%s: %s %s\n", pc, mnemonic, index);
    }
}

/**
 * Representation of a no-argument instruction.
 */
class NNoArgTuple extends NTuple {
    /**
     * Constructs an NNoArgTuple object.
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
