package iota;

import java.util.ArrayList;

import static iota.NPhysicalRegister.*;

/**
 * An abstract low-level intermediate representation (LIR) of an HIR instruction.
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
     * Converts this instruction to the corresponding Marvin instruction.
     */
    public void toMarvin() {
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
}

/**
 * LIR instruction representing an integer constant.
 */
class NLIRIntConstant extends NLIRInstruction {
    /**
     * The constant int value.
     */
    public int value;

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
    public void toMarvin() {
        NMarvinInstruction ins = null;
        block.marvin.add(ins);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + write + " " + value;
    }
}

/**
 * LIR instruction representing an arithmetic instruction.
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
 * LIR instruction representing conditional or unconditional jump instruction.
 */
class NLIRJump extends NLIRInstruction {
    /**
     * Block to jump to on true.
     */
    public NBasicBlock onTrueBlock;

    /**
     * Block to jump to on false (null for an unconditional jump).
     */
    public NBasicBlock onFalseBlock;

    /**
     * Constructs an NLIRJump object for an unconditional jump.
     *
     * @param block       enclosing block.
     * @param id          id of the instruction.
     * @param mnemonic    mnemonic of the jump instruction.
     * @param onTrueBlock block to jump to.
     */
    public NLIRJump(NBasicBlock block, int id, String mnemonic, NBasicBlock onTrueBlock) {
        super(block, id, mnemonic);
        this.onTrueBlock = onTrueBlock;
        onFalseBlock = null;
    }

    /**
     * Constructs an NLIRJump object for a conditional jump.
     *
     * @param block        enclosing block.
     * @param id           id of the instruction.
     * @param mnemonic     mnemonic of the jump instruction.
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
 * LIR instruction representing a return instruction.
 */
class NLIRReturn extends NLIRInstruction {
    /**
     * Constructs an NLIRReturn object for return instruction without a value.
     *
     * @param block    enclosing block.
     * @param id       id of the instruction.
     * @param mnemonic mnemonic of the instruction.
     */
    public NLIRReturn(NBasicBlock block, int id, String mnemonic) {
        super(block, id, mnemonic);
    }

    /**
     * Constructs an NLIRReturn object for return instruction with a value.
     *
     * @param block    enclosing block.
     * @param id       id of the instruction.
     * @param mnemonic mnemonic of the instruction.
     * @param result   physical register storing  the return value.
     */
    public NLIRReturn(NBasicBlock block, int id, String mnemonic, NPhysicalRegister result) {
        super(block, id, mnemonic);
        reads.add(result);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + (reads.isEmpty() ? mnemonic : mnemonic + " " + reads.get(0));
    }
}

/**
 * LIR instruction representing a method call instruction.
 */
class NLIRMethodCall extends NLIRInstruction {
    /**
     * Name of the method.
     */
    private String name;

    /**
     * Constructs an NLIRMethodCall object.
     *
     * @param block     enclosing basic block.
     * @param id        id of the instruction.
     * @param mnemonic  mnemonic of the instruction.
     * @param name      name of the method.
     * @param arguments arguments to the method.
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
        return !s.endsWith(", ") ? s + ")" : s.substring(0, s.length() - 2) + ")";
    }
}

/**
 * LIR instruction representing a copy instruction.
 */
class NLIRCopy extends NLIRInstruction {
    /**
     * Constructs an NLIRCopy object.
     *
     * @param block enclosing block.
     * @param id    id of the instruction.
     * @param to    register (virtual or physical) to copy to.
     * @param from  register (virtual or physical) to copy from.
     */
    public NLIRCopy(NBasicBlock block, int id, NRegister to, NRegister from) {
        super(block, id, "copy");
        write = to;
        reads.add(from);
        block.cfg.registers.add(write);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + write + " " + reads.get(0);
    }
}

/**
 * LIR instruction representing a load (from memory to register) instruction.
 */
class NLIRLoad extends NLIRInstruction {
    /**
     * Memory address to load from.
     */
    public NRegister from;

    /**
     * Constructs an NLIRLoad object.
     *
     * @param block enclosing block.
     * @param id    id of the instruction.
     * @param to    register (virtual or physical) to store into.
     * @param from  memory from to load from.
     */
    public NLIRLoad(NBasicBlock block, int id, NRegister to, NRegister from) {
        super(block, id, "load");
        write = to;
        reads.add(from);
        this.from = from;
        block.cfg.registers.add(write);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + write + " " + reads.get(0);
    }
}

/**
 * LIR instruction representing a store (from register to memory) instruction.
 */
class NLIRStore extends NLIRInstruction {
    /**
     * Memory address to store at.
     */
    public NRegister to;

    /**
     * Constructs an NLIRStore object.
     *
     * @param block enclosing block.
     * @param id    id of the instruction.
     * @param from  register (virtual or physical) to store from.
     * @param to    memory address to store at.
     */
    public NLIRStore(NBasicBlock block, int id, NRegister from, NRegister to) {
        super(block, id, "store");
        reads.add(from);
        reads.add(to);
        this.to = to;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + reads.get(0) + " " + reads.get(1);
    }
}

/**
 *
 */
class NLIRPush extends NLIRInstruction {
    /**
     * Constructs an NLIRPush object.
     *
     * @param block enclosing block.
     * @param id    id of the instruction.
     * @param from  register (virtual or physical) to store from.
     * @param to    memory address to store at.
     */
    public NLIRPush(NBasicBlock block, int id, NRegister from, NRegister to) {
        super(block, id, "push");
        reads.add(from);
        reads.add(to);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + reads.get(0) + " " + reads.get(1);
    }
}

class NLIRIncrement extends NLIRInstruction {
    public int value;

    public NLIRIncrement(NBasicBlock block, int id, NRegister register, int value) {
        super(block, id, "addn");
        write = register;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + reads.get(0) + " " + reads.get(1);
    }
}