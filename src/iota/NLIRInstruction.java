// Copyright 2024- Swami Iyer

package iota;

import java.util.ArrayList;

import static iota.NPhysicalRegister.*;

/**
 * An abstract low-level intermediate representation (LIR) of a JVM instruction.
 */
abstract class NLIRInstruction {
    /**
     * The enclosing basic block.
     */
    public NBasicBlock block;

    /**
     * Unique id of this instruction.
     */
    public int id;

    /**
     * Mnemonic of this instruction.
     */
    public String mnemonic;

    /**
     * Registers that store the inputs (if any) of this instruction.
     */
    public ArrayList<NRegister> reads;

    /**
     * Register that stores the output (if any) of this instruction.
     */
    public NRegister write;

    /**
     * Constructs an NLIRInstruction object.
     *
     * @param block    enclosing block.
     * @param id       id of the instruction.
     * @param mnemonic mnemonic of the instruction.
     */
    protected NLIRInstruction(NBasicBlock block, int id, String mnemonic) {
        this.block = block;
        this.id = id;
        this.mnemonic = mnemonic;
        reads = new ArrayList<>();
        write = null;
    }

    /**
     * Returns the id of this instruction as a string.
     *
     * @return the id of this instruction as a string.
     */
    public String id() {
        return id + "";
    }

    /**
     * Replace references to virtual registers in this instruction with references to physical registers.
     */
    public void allocatePhysicalRegisters() {
        // Nothing here.
    }

    /**
     * Translates this instruction into Marvin instruction and writes it standard output.
     *
     * @param p for pretty printing with indentation.
     */
    public void toMarvin(PrettyPrinter p) {
        // Nothing here.
    }

    /**
     * Returns a string representation of this instruction.
     *
     * @return a string representation of this instruction.
     */
    public String toString() {
        return id();
    }
}

/**
 * LIR instruction representing a formal parameter.
 */
class NLIRLoadParam extends NLIRInstruction {
    /**
     * Formal parameter index.
     */
    public int index;

    /**
     * Constructs an NLIRLoadParam object.
     *
     * @param block    enclosing basic block.
     * @param id       id of the instruction.
     * @param mnemonic mnemonic of the instruction.
     * @param index    formal parameter index.
     */
    public NLIRLoadParam(NBasicBlock block, int id, String mnemonic, int index) {
        super(block, id, mnemonic);
        this.index = index;
        write = new NVirtualRegister(NControlFlowGraph.regId++);
        block.cfg.registers.add(write);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + write + " " + index;
    }
}

/**
 * LIR instruction representing a phi function.
 */
class NLIRPhiFunction extends NLIRInstruction {
    /**
     * Constructs an NLIRPhiFunction object.
     *
     * @param block    enclosing basic block.
     * @param id       id of the instruction.
     * @param mnemonic mnemonic of the instruction.
     */
    public NLIRPhiFunction(NBasicBlock block, int id, String mnemonic) {
        super(block, id, mnemonic);
        write = new NVirtualRegister(NControlFlowGraph.regId++);
        block.cfg.registers.add(write);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": phi() " + write;
    }
}

/**
 * LIR instruction corresponding to the JVM arithmetic instructions.
 */
class NLIRArithmetic extends NLIRInstruction {
    /**
     * Constructs an NLIRArithmetic object.
     *
     * @param block    enclosing basic block.
     * @param id       id of the instruction.
     * @param mnemonic mnemonic of the instruction.
     * @param lhs      lhs instruction.
     * @param rhs      rhs instruction.
     */
    public NLIRArithmetic(NBasicBlock block, int id, String mnemonic, NLIRInstruction lhs, NLIRInstruction rhs) {
        super(block, id, mnemonic);
        reads.add(lhs.write);
        reads.add(rhs.write);
        write = new NVirtualRegister(NControlFlowGraph.regId++);
        block.cfg.registers.add(write);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + write + " " + reads.get(0) + " " + reads.get(1);
    }
}

/**
 * LIR instruction corresponding to the JVM instructions representing integer constants.
 */
class NLIRIntConstant extends NLIRInstruction {
    // The constant int value.
    private int value;

    /**
     * Constructs an NLIRIntConstant object.
     *
     * @param block    enclosing basic block.
     * @param id       id of the instruction.
     * @param mnemonic mnemonic of the instruction.
     * @param value    the constant int value.
     */
    public NLIRIntConstant(NBasicBlock block, int id, String mnemonic, int value) {
        super(block, id, mnemonic);
        this.value = value;
        write = new NVirtualRegister(NControlFlowGraph.regId++);
        block.cfg.registers.add(write);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + write + " " + value;
    }
}

/**
 * HIR instruction representing a JVM return instruction.
 */
class NLIRReturn extends NLIRInstruction {
    /**
     * Constructs an NLIRReturn object.
     *
     * @param block    enclosing block.
     * @param id       id of the instruction.
     * @param mnemonic mnemonic of the instruction.
     * @param result   physical register storing return value, or null.
     */
    public NLIRReturn(NBasicBlock block, int id, String mnemonic, NPhysicalRegister result) {
        super(block, id, mnemonic);
        if (result != null) {
            reads.add(result);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + (reads.isEmpty() ? mnemonic : mnemonic + " " + reads.get(0));
    }
}

/**
 * LIR instruction representing method invocation instructions in JVM.
 */
class NLIRMethodCall extends NLIRInstruction {
    // Name of the method being invoked.
    private String name;

    private String type;

    /**
     * Constructs an NHIRInvoke object.
     *
     * @param block     enclosing basic block.
     * @param id        id of the instruction.
     * @param mnemonic  mnemonic of the instruction.
     * @param name      name of the method.
     * @param arguments list of register storing the of arguments for the method.
     * @param type      return type.
     */
    public NLIRMethodCall(NBasicBlock block, int id, String mnemonic, String name, ArrayList<NRegister> arguments,
                          String type) {
        super(block, id, mnemonic);
        this.name = name;
        for (NRegister arg : arguments) {
            reads.add(arg);
        }
        if (!type.equals("V")) {
            write = NPhysicalRegister.regInfo[RV];
            block.cfg.registers.set(RV, write);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        String s = id + ": " + mnemonic + " " + (write != null ? write + " " : "") + name + "(";
        for (NRegister input : reads) {
            s += input + ", ";
        }
        return s.endsWith(", ") ? s.substring(0, s.length() - 2) + ")" : s + ")";
    }
}

/**
 * LIR instruction representing an conditional jump instructions in JVM.
 */
class NLIRJump extends NLIRInstruction {
    // Block to jump to on true.
    private NBasicBlock onTrueBlock;

    // Block to jump to on false.
    private NBasicBlock onFalseBlock;

    public NLIRJump(NBasicBlock block, int id, String mnemonic, NBasicBlock onTrueBlock) {
        super(block, id, mnemonic);
        this.onTrueBlock = onTrueBlock;
        onFalseBlock = null;
    }

    /**
     * Constructs an NLIRJump object.
     *
     * @param block        enclosing block.
     * @param id           id of the instruction.
     * @param mnemonic     mnemonic of the instruction.
     * @param lhs          lhs LIR.
     * @param rhs          rhs LIR.
     * @param onTrueBlock  block to jump to on true.
     * @param onFalseBlock block to jump to on false.
     */
    public NLIRJump(NBasicBlock block, int id, String mnemonic, NLIRInstruction lhs, NLIRInstruction rhs,
                    NBasicBlock onTrueBlock, NBasicBlock onFalseBlock) {
        super(block, id, mnemonic);
        reads.add(lhs.write);
        reads.add(rhs.write);
        this.onTrueBlock = onTrueBlock;
        this.onFalseBlock = onFalseBlock;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        if (onFalseBlock == null) {
            return id() + ": " + mnemonic + " " + onTrueBlock.id();
        }
        return id() + ": " + mnemonic + " " + reads.get(0) + " " + reads.get(1) + " " + onTrueBlock.id() +
                " " + onFalseBlock.id();
    }
}

/**
 * LIR copy instruction.
 */
class NLIRCopy extends NLIRInstruction {
    /**
     * Constructs an NLIRCopy object.
     *
     * @param block    enclosing block.
     * @param id       id of the instruction.
     * @param to       LIR to move to.
     * @param from     LIR to move from.
     */
    public NLIRCopy(NBasicBlock block, int id, NLIRInstruction to, NLIRInstruction from) {
        super(block, id, "copy");
        reads.add(from.write);
        write = to.write;
    }

    /**
     * Constructs an NLIRCopy object.
     *
     * @param block enclosing block.
     * @param id    id of the instruction.
     * @param to    register (virtual or physical) to move to.
     * @param from  register (virtual or physical) to move from.
     */
    public NLIRCopy(NBasicBlock block, int id, NRegister to, NRegister from) {
        super(block, id, "copy");
        reads.add(from);
        write = to;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + write + " " + reads.get(0);
    }
}
