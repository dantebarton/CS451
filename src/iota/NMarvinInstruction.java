package iota;

import java.io.PrintWriter;
import java.util.HashMap;

/**
 * An abstract representation of a Marvin instruction.
 */
abstract class NMarvinInstruction {
    /**
     * Maps Marvin operator mnemonic to the corresponding operator text. For example, maps "mul" to "*".
     */
    protected static HashMap<String, String> mnemonic2Op;

    // Create and populate mnemonic2Op.
    static {
        mnemonic2Op = new HashMap<>();
        mnemonic2Op.put("add", "+");
        mnemonic2Op.put("div", "/");
        mnemonic2Op.put("mul", "*");
        mnemonic2Op.put("mod", "%");
        mnemonic2Op.put("sub", "-");
        mnemonic2Op.put("jeqn", "==");
        mnemonic2Op.put("jgen", ">=");
        mnemonic2Op.put("jgtn", ">");
        mnemonic2Op.put("jlen", "<=");
        mnemonic2Op.put("jltn", "<");
        mnemonic2Op.put("jnen", "!=");
    }

    /**
     * Program counter.
     */
    public int pc;

    /**
     * Instruction mnemonic.
     */
    public String mnemonic;

    /**
     * Constructs an NMarvinInstruction object.
     *
     * @param mnemonic instruction mnemonic.
     */
    protected NMarvinInstruction(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    /**
     * Writes this instruction to the given output stream.
     *
     * @param out output stream.
     */
    abstract void write(PrintWriter out);
}

/**
 * Representation of an arithmetic instruction of the form "x = y operation z".
 */
class NMarvinArithmetic extends NMarvinInstruction {
    /**
     * Result.
     */
    public NPhysicalRegister rX;

    /**
     * Operand 1.
     */
    public NPhysicalRegister rY;

    /**
     * Operand 2.
     */
    public NPhysicalRegister rZ;

    /**
     * Constructs an NMarvinArithmetic object.
     *
     * @param mnemonic instruction mnemonic.
     * @param rX       result.
     * @param rY       operand 1.
     * @param rZ       operand 2.
     */
    public NMarvinArithmetic(String mnemonic, NPhysicalRegister rX, NPhysicalRegister rY, NPhysicalRegister rZ) {
        super(mnemonic);
        this.rX = rX;
        this.rY = rY;
        this.rZ = rZ;
    }

    /**
     * {@inheritDoc}
     */
    public void write(PrintWriter out) {
        String comment = rX + " = " + rY + " " + mnemonic2Op.get(mnemonic) + " " + rZ;
        out.printf("%-6s%-8s%-8s%-8s%-8s# %s\n", pc, mnemonic, rX, rY, rZ, comment);
    }
}

/**
 * Representation of an instruction for calling a method.
 */
class NMarvinCall extends NMarvinInstruction {
    /**
     * Return address.
     */
    public NPhysicalRegister rX;

    /**
     * Address where the method is defined.
     */
    public int N;

    /**
     * Constructs an NMarvinCall object.
     *
     * @param rX return address.
     * @param N  address where the method is defined.
     */
    public NMarvinCall(NPhysicalRegister rX, int N) {
        super("calln");
        this.rX = rX;
        this.N = N;
    }

    /**
     * {@inheritDoc}
     */
    public void write(PrintWriter out) {
        String comment = "call method @" + N;
        out.printf("%-6s%-8s%-8s%-8s%-8s# %s\n", pc, mnemonic, rX, N, "", comment);
    }
}

/**
 * Representation of an instruction for copying one register (source) into another (target) register.
 */
class NMarvinCopy extends NMarvinInstruction {
    /**
     * Target register.
     */
    public NPhysicalRegister rX;

    /**
     * Source register.
     */
    public NPhysicalRegister rY;

    /**
     * Constructs an NMarvinCopy object.
     *
     * @param rX target register.
     * @param rY source register.
     */
    public NMarvinCopy(NPhysicalRegister rX, NPhysicalRegister rY) {
        super("copy");
        this.rX = rX;
        this.rY = rY;
    }

    /**
     * {@inheritDoc}
     */
    public void write(PrintWriter out) {
        String comment = rX + " = " + rY;
        out.printf("%-6s%-8s%-8s%-8s%-8s# %s\n", pc, mnemonic, rX, rY, "", comment);
    }
}

/**
 * Representation of an instruction for assigning an integer to a register (target).
 */
class NMarvinIConst extends NMarvinInstruction {
    /**
     * Target register.
     */
    public NPhysicalRegister rX;

    /**
     * The integer.
     */
    public int N;

    /**
     * Constructs an NMarvinIConst object.
     *
     * @param rX target register.
     * @param N  the integer.
     */
    public NMarvinIConst(NPhysicalRegister rX, int N) {
        super(N == 0 ? "set0" : N == 1 ? "set1" : "setn");
        this.rX = rX;
        this.N = N;
    }

    /**
     * {@inheritDoc}
     */
    public void write(PrintWriter out) {
        String comment = rX + " = " + N;
        out.printf("%-6s%-8s%-8s%-8s%-8s# %s\n", pc, mnemonic, rX, N, "", comment);
    }
}

/**
 * Representation of an instruction for incrementing (or decrementing) a register (target) by a constant value.
 */
class NMarvinInc extends NMarvinInstruction {
    /**
     * Target register.
     */
    public NPhysicalRegister rX;

    /**
     * Increment (or decrement) value.
     */
    public int N;

    /**
     * Constructs an NMarvinInc object.
     *
     * @param rX target register.
     * @param N  increment (or decrement) value.
     */
    public NMarvinInc(NPhysicalRegister rX, int N) {
        super("addn");
        this.rX = rX;
        this.N = N;
    }

    /**
     * {@inheritDoc}
     */
    public void write(PrintWriter out) {
        String comment = rX + " += " + N;
        out.printf("%-6s%-8s%-8s%-8s%-8s# %s\n", pc, mnemonic, rX, N, "", comment);
    }
}

/**
 * Representation of a jump (conditional or unconditional) instruction.
 */
class NMarvinJump extends NMarvinInstruction {
    /**
     * Lhs of the condition (null for an unconditional jump).
     */
    public NPhysicalRegister rX;

    /**
     * Rhs of the condition (null for an unconditional jump).
     */
    public NPhysicalRegister rY;

    /**
     * Jump address.
     */
    public int N;

    /**
     * Whether the jump (if unconditional) is to return from a method.
     */
    public boolean returnFromMethod;

    /**
     * Constructs an NMarvinJump object for an unconditional jump.
     *
     * @param mnemonic         instruction mnemonic.
     * @param N                jump address.
     * @param returnFromMethod whether the jump is to return from a method.
     */
    public NMarvinJump(String mnemonic, int N, boolean returnFromMethod) {
        super(mnemonic);
        this.N = N;
        this.returnFromMethod = returnFromMethod;
    }

    /**
     * Constructs an NMarvinJump object for a conditional jump.
     *
     * @param mnemonic instruction mnemonic.
     * @param rX       lhs of the condition.
     * @param rY       rhs of the condition.
     * @param N        jump address.
     */
    public NMarvinJump(String mnemonic, NPhysicalRegister rX, NPhysicalRegister rY, int N) {
        super(mnemonic);
        this.rX = rX;
        this.rY = rY;
        this.N = N;
    }

    /**
     * {@inheritDoc}
     */
    public void write(PrintWriter out) {
        String comment;
        if (rX == null) {
            comment = "jump to " + N;
            out.printf("%-6s%-8s%-8s%-8s%-8s# %s\n", pc, mnemonic, N, "", "", comment);
        } else {
            comment = "if " + rX + " " + mnemonic2Op.get(mnemonic) + " " + rY + " jump to " + N;
            out.printf("%-6s%-8s%-8s%-8s%-8s# %s\n", pc, mnemonic, rX, rY, N, comment);
        }
    }
}

/**
 * Representation of an instruction for loading a value from memory into a register.
 */
class NMarvinLoad extends NMarvinInstruction {
    /**
     * Where to load.
     */
    public NPhysicalRegister rX;

    /**
     * What to load.
     */
    public NPhysicalRegister rY;

    /**
     * Constructs an NMarvinLoad object.
     *
     * @param mnemonic instruction mnemonic.
     * @param rX       where to load.
     * @param rY       what to load.
     */
    public NMarvinLoad(String mnemonic, NPhysicalRegister rX, NPhysicalRegister rY) {
        super(mnemonic);
        this.rX = rX;
        this.rY = rY;
    }

    /**
     * {@inheritDoc}
     */
    public void write(PrintWriter out) {
        String comment;
        if (mnemonic.equals("loadr")) {
            comment = rX + " = mem[" + rY + "]";
        } else {
            // Must be "popr".
            comment = rX + " = mem[--" + rY + "]";
        }
        out.printf("%-6s%-8s%-8s%-8s%-8s# %s\n", pc, mnemonic, rX, rY, "", comment);
    }
}

/**
 * Representation of an instruction for storing a value from register into memory.
 */
class NMarvinStore extends NMarvinInstruction {
    /**
     * What to store.
     */
    public NPhysicalRegister rX;

    /**
     * Where to store.
     */
    public NPhysicalRegister rY;

    /**
     * Constructs an NMarvinStore object.
     *
     * @param mnemonic instruction mnemonic.
     * @param rX       what to store.
     * @param rY       where to store.
     */
    public NMarvinStore(String mnemonic, NPhysicalRegister rX, NPhysicalRegister rY) {
        super(mnemonic);
        this.rX = rX;
        this.rY = rY;
    }

    /**
     * {@inheritDoc}
     */
    public void write(PrintWriter out) {
        String comment;
        if (mnemonic.equals("storer")) {
            comment = "mem[" + rY + "]" + " = " + rX;
        } else {
            // Must be "pushr".
            comment = "mem[" + rY + "++]" + " = " + rX;
        }
        out.printf("%-6s%-8s%-8s%-8s%-8s# %s\n", pc, mnemonic, rX, rY, "", comment);
    }
}

/**
 * Representation of an instruction for reading an integer from standard input into a register (target).
 */
class NMarvinRead extends NMarvinInstruction {
    /**
     * Target register.
     */
    public NPhysicalRegister rX;

    /**
     * Constructs an NMarvinRead object.
     *
     * @param rX target register.
     */
    public NMarvinRead(NPhysicalRegister rX) {
        super("read");
        this.rX = rX;
    }

    /**
     * {@inheritDoc}
     */
    public void write(PrintWriter out) {
        String comment = rX + " = read()";
        out.printf("%-6s%-8s%-8s%-8s%-8s# %s\n", pc, mnemonic, rX, "", "", comment);
    }
}

/**
 * Representation of an instruction for writing an integer in a register (source) to standard output.
 */
class NMarvinWrite extends NMarvinInstruction {
    /**
     * Source register.
     */
    public NPhysicalRegister rX;

    /**
     * Constructs an NMarvinWrite object.
     *
     * @param rX source register.
     */
    public NMarvinWrite(NPhysicalRegister rX) {
        super("write");
        this.rX = rX;
    }

    /**
     * {@inheritDoc}
     */
    public void write(PrintWriter out) {
        String comment = "write(" + rX + ")";
        out.printf("%-6s%-8s%-8s%-8s%-8s# %s\n", pc, mnemonic, rX, "", "", comment);
    }
}
