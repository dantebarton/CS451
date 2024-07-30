// Copyright 2024- Swami Iyer

package iota;

import java.util.ArrayList;
import java.util.Hashtable;

import static iota.CLConstants.GOTO;
import static iota.CLConstants.IADD;
import static iota.CLConstants.IDIV;
import static iota.CLConstants.IF_ICMPEQ;
import static iota.CLConstants.IF_ICMPGE;
import static iota.CLConstants.IF_ICMPGT;
import static iota.CLConstants.IF_ICMPLE;
import static iota.CLConstants.IF_ICMPLT;
import static iota.CLConstants.IF_ICMPNE;
import static iota.CLConstants.IMUL;
import static iota.CLConstants.INVOKESTATIC;
import static iota.CLConstants.IREM;
import static iota.CLConstants.IRETURN;
import static iota.CLConstants.ISUB;
import static iota.CLConstants.RETURN;

import static iota.NPhysicalRegister.*;

/**
 * An abstract low-level intermediate representation (LIR) of a JVM instruction.
 */
abstract class NLIRInstruction {
    /**
     * Maps JVM opcode to a string mnemonic for LIR instructions. For example, the opcode IMUL is mapped to the
     * string "MUL".
     */
    protected static Hashtable<Integer, String> lirMnemonic;

    // Create and populate hirMnemonic.
    static {
        lirMnemonic = new Hashtable<>();
        lirMnemonic.put(IADD, "add");
        lirMnemonic.put(IDIV, "div");
        lirMnemonic.put(IMUL, "mul");
        lirMnemonic.put(IREM, "mod");
        lirMnemonic.put(ISUB, "sub");
        lirMnemonic.put(RETURN, "return");
        lirMnemonic.put(IRETURN, "return");
        lirMnemonic.put(INVOKESTATIC, "call");
        lirMnemonic.put(IF_ICMPEQ, "eq");
        lirMnemonic.put(IF_ICMPGE, "ge");
        lirMnemonic.put(IF_ICMPGT, "gt");
        lirMnemonic.put(IF_ICMPLE, "le");
        lirMnemonic.put(IF_ICMPLT, "lt");
        lirMnemonic.put(IF_ICMPNE, "ne");
        lirMnemonic.put(GOTO, "goto");
    }

    /**
     * The block containing this instruction.
     */
    public NBasicBlock block;

    /**
     * Unique identifier of this instruction.
     */
    public int id;

    /**
     * Registers that store the inputs (if any) of this instruction.
     */
    public ArrayList<NRegister> reads;

    /**
     * Register that stores the result (if any) of this instruction.
     */
    public NRegister write;

    /**
     * Constructs an NLIRInstruction object.
     *
     * @param block enclosing block.
     * @param id    identifier of the instruction.
     */
    protected NLIRInstruction(NBasicBlock block, int id) {
        this.block = block;
        this.id = id;
        reads = new ArrayList<NRegister>();
    }

    /**
     * Replace references to virtual registers in this LIR instruction with references to physical registers.
     */
    public void allocatePhysicalRegisters() {
        // Nothing here.
    }

    /**
     * Translates this LIR instruction into HMMM and writes it standard output.
     *
     * @param p for pretty printing with indentation.
     */
    public void toHMMM(PrettyPrinter p) {
        // Nothing here.
    }

    /**
     * Returns a string representation of this instruction.
     *
     * @return a string representation of this instruction.
     */
    public String toString() {
        return "" + id;
    }
}

/**
 * LIR instruction representing a formal parameter.
 */
class NLIRLoadParam extends NLIRInstruction {
    // Local variable index.
    private int index;

    /**
     * Constructs an NLIRLoadLocal object.
     *
     * @param block enclosing block.
     * @param id    identifier of the instruction.
     * @param index local variable index.
     */
    public NLIRLoadParam(NBasicBlock block, int id, int index) {
        super(block, id);
        this.index = index;
        write = new NVirtualRegister(NControlFlowGraph.regId++);
        block.cfg.registers.add(write);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id + ": ldparam " + index + " " + write;
    }
}

/**
 * LIR instruction representing phi functions.
 */
class NLIRPhiFunction extends NLIRInstruction {
    /**
     * Constructs an NLIRPhiFunction object.
     *
     * @param block enclosing block.
     * @param id    identifier of the instruction.
     */
    public NLIRPhiFunction(NBasicBlock block, int id) {
        super(block, id);
        write = new NVirtualRegister(NControlFlowGraph.regId++);
        block.cfg.registers.add(write);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id + ": phi() " + write;
    }
}

/**
 * LIR instruction corresponding to the JVM arithmetic instructions.
 */
class NLIRArithmetic extends NLIRInstruction {
    /**
     * Opcode of the arithmetic instruction.
     */
    public int opcode;

    /**
     * Constructs an NLIRArithmetic object.
     *
     * @param block  enclosing block.
     * @param id     identifier of the instruction.
     * @param opcode opcode for the arithmetic operator.
     * @param lhs    LIR for lhs.
     * @param rhs    LIR for rhs.
     */
    public NLIRArithmetic(NBasicBlock block, int id, int opcode, NLIRInstruction lhs, NLIRInstruction rhs) {
        super(block, id);
        this.opcode = opcode;
        reads.add(lhs.write);
        reads.add(rhs.write);
        write = new NVirtualRegister(NControlFlowGraph.regId++);
        block.cfg.registers.add(write);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id + ": " + lirMnemonic.get(opcode) + " " + write + " " + reads.get(0) + " " + reads.get(1);
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
     * @param block enclosing block.
     * @param id    identifier of the instruction.
     * @param value the constant int value.
     */
    public NLIRIntConstant(NBasicBlock block, int id, int value) {
        super(block, id);
        this.value = value;
        write = new NVirtualRegister(NControlFlowGraph.regId++);
        block.cfg.registers.add(write);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id + ": set " + write + " " + value;
    }
}

/**
 * HIR instruction representing a JVM return instruction.
 */
class NLIRReturn extends NLIRInstruction {
    // JVM opcode for the return instruction.
    private int opcode;

    /**
     * Constructs an NLIRReturn object.
     *
     * @param block  enclosing block.
     * @param id     identifier of the instruction.
     * @param opcode JVM opcode for the return instruction.
     * @param result physical register storing return value, or null.
     */
    public NLIRReturn(NBasicBlock block, int id, int opcode, NPhysicalRegister result) {
        super(block, id);
        this.opcode = opcode;
        if (result != null) {
            reads.add(result);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        if (reads.size() == 0) {
            return id + ": " + lirMnemonic.get(opcode);
        }
        return id + ": " + lirMnemonic.get(opcode) + " " + reads.get(0);
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
     * @param block     enclosing block.
     * @param id        identifier of the instruction.
     * @param name      name of the method.
     * @param arguments list of register storing the of arguments for the method.
     * @param type      return type.
     */
    public NLIRMethodCall(NBasicBlock block, int id, String name, ArrayList<NRegister> arguments, String type) {
        super(block, id);
        this.name = name;
        for (NRegister arg : arguments) {
            reads.add(arg);
        }
        if (!type.equals("V")) {
            write = NPhysicalRegister.regInfo[RV];
            block.cfg.registers.set(RV, write); // Swami: what's this for?
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        String s = id + ": call " + (write != null ? write + " " : "") + name + "(";
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
    // Test expression opcode.
    private int opcode;

    // Block to jump to on true.
    private NBasicBlock onTrueBlock;

    // Block to jump to on false.
    private NBasicBlock onFalseBlock;

    public NLIRJump(NBasicBlock block, int id, NBasicBlock onTrueBlock) {
        super(block, id);
        this.opcode = GOTO;
        this.onTrueBlock = onTrueBlock;
    }

    /**
     * Constructs an NLIRJump object.
     *
     * @param block        enclosing block.
     * @param id           identifier of the instruction.
     * @param arg          lhs LIR.
     * @param opcode       opcode in the test.
     * @param onTrueBlock  block to jump to on true.
     * @param onFalseBlock block to jump to on false.
     */
    public NLIRJump(NBasicBlock block, int id, int opcode, NLIRInstruction arg,
                    NBasicBlock onTrueBlock, NBasicBlock onFalseBlock) {
        super(block, id);
        this.opcode = opcode;
        reads.add(arg.write);
        this.onTrueBlock = onTrueBlock;
        this.onFalseBlock = onFalseBlock;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        if (opcode == GOTO) {
            return id + ": " + lirMnemonic.get(opcode) + " " + onTrueBlock.id();
        }
        return id + ": if " + reads.get(0) + " " + lirMnemonic.get(opcode) + " 0 then " + onTrueBlock.id() + " else " +
                onFalseBlock.id();
    }
}

/**
 * LIR copy instruction.
 */
class NLIRCopy extends NLIRInstruction {
    /**
     * Constructs an NLIRCopy object.
     *
     * @param block enclosing block.
     * @param id    identifier of the instruction.
     * @param from  LIR to move from.
     * @param to    LIR to move to.
     */
    public NLIRCopy(NBasicBlock block, int id, NLIRInstruction from, NLIRInstruction to) {
        super(block, id);
        reads.add(from.write);
        write = to.write;
    }

    /**
     * Constructs an NLIRCopy object.
     *
     * @param block enclosing block.
     * @param id    identifier of the instruction.
     * @param from  register (virtual or physical) to move from.
     * @param to    register (virtual or physical) to move to.
     */
    public NLIRCopy(NBasicBlock block, int id, NRegister from, NRegister to) {
        super(block, id);
        reads.add(from);
        write = to;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id + ": copy " + write + " " + reads.get(0);
    }
}
