// Copyright 2024- Swami Iyer

package iota;

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

import java.util.ArrayList;
import java.util.Hashtable;

import static iota.NPhysicalRegister.*;

/**
 * An abstract high-level intermediate representation (HIR) of a JVM instruction.
 */
abstract class NHIRInstruction {
    /**
     * Maps JVM opcode to a string mnemonic for HIR instructions. For example, the opcode IMUL is mapped to the
     * string "*".
     */
    protected static Hashtable<Integer, String> hirMnemonic;

    // Create and populate hirMnemonic.
    static {
        hirMnemonic = new Hashtable<>();
        hirMnemonic.put(IADD, "+");
        hirMnemonic.put(IDIV, "/");
        hirMnemonic.put(IMUL, "*");
        hirMnemonic.put(IREM, "%");
        hirMnemonic.put(ISUB, "-");
        hirMnemonic.put(RETURN, "return");
        hirMnemonic.put(IRETURN, "return");
        hirMnemonic.put(INVOKESTATIC, "call");
        hirMnemonic.put(IF_ICMPEQ, "==");
        hirMnemonic.put(IF_ICMPGE, ">=");
        hirMnemonic.put(IF_ICMPGT, ">");
        hirMnemonic.put(IF_ICMPLE, "<=");
        hirMnemonic.put(IF_ICMPLT, "<");
        hirMnemonic.put(IF_ICMPNE, "!=");
        hirMnemonic.put(GOTO, "goto");
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
     * Short type name for this instruction.
     */
    public String type;

    /**
     * The LIR instruction corresponding to this HIR instruction.
     */
    public NLIRInstruction lir;

    /**
     * Constructs an NHIRInstruction object.
     *
     * @param block enclosing block.
     * @param id    identifier of the instruction.
     */
    protected NHIRInstruction(NBasicBlock block, int id) {
        this(block, id, "");
    }

    /**
     * Constructs an NHIRInstruction object.
     *
     * @param block enclosing block.
     * @param id    identifier of the instruction.
     * @param type  type name of the instruction.
     */
    protected NHIRInstruction(NBasicBlock block, int id, String type) {
        this.block = block;
        this.id = id;
        this.type = type;
    }

    /**
     * Converts and returns a low-level representation (LIR) of this HIR instruction.
     *
     * @return the LIR instruction corresponding to this HIR instruction.
     */
    public abstract NLIRInstruction toLir();

    /**
     * Returns the id of this instruction as a string.
     *
     * @return the id of this instruction as a string.
     */
    public String id() {
        return type + id;
    }

    /**
     * Returns true if this instruction is the same (ie, has the same id) as the other, and false otherwise.
     *
     * @param other the instruction to compare to.
     * @return true if this instruction is the same (ie, has the same id) as the other, and false otherwise.
     */
    public boolean equals(NHIRInstruction other) {
        return this.id == other.id;
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
 * HIR instruction representing a formal parameter.
 */
class NHIRLoadParam extends NHIRInstruction {
    /**
     * Formal parameter index.
     */
    public int index;

    /**
     * Constructs an NHIRLoadParam object.
     *
     * @param block enclosing block.
     * @param id    identifier of the instruction.
     * @param index formal parameter index.
     */
    public NHIRLoadParam(NBasicBlock block, int id, int index) {
        super(block, id, "I");
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */
    public NLIRInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        lir = new NLIRLoadParam(block, NControlFlowGraph.lirId++, index);
        block.lir.add(lir);
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": ldparam " + index;
    }
}

/**
 * HIR instruction representing a phi function.
 */
class NHIRPhiFunction extends NHIRInstruction {
    /**
     * Arguments of the function.
     */
    public ArrayList<NHIRInstruction> args;

    /**
     * Index of the variable to which the function is bound.
     */
    public int index;

    /**
     * Constructs an NHIRPhiFunction object.
     *
     * @param block enclosing block.
     * @param id    identifier of the instruction.
     * @param args  arguments of the function.
     * @param index index of the variable to which the function is bound.
     */
    public NHIRPhiFunction(NBasicBlock block, int id, ArrayList<NHIRInstruction> args, int index) {
        super(block, id, "");
        this.args = args;
        this.index = index;
        type = "I";
    }

    /**
     * {@inheritDoc}
     */
    public NLIRInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        lir = new NLIRPhiFunction(block, NControlFlowGraph.lirId++);
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        String s = id() + ": phi(";
        for (NHIRInstruction ins : args) {
            s += ins == null ? "?, " : ins.id() + ", ";
        }
        return s.endsWith(", ") ? s.substring(0, s.length() - 2) + ")" : s + ")";
    }
}

/**
 * HIR instruction corresponding to JVM arithmetic instructions.
 */
class NHIRArithmetic extends NHIRInstruction {
    /**
     * Opcode of the arithmetic instruction.
     */
    public int opcode;

    /**
     * Lhs id.
     */
    public int lhs;

    /**
     * Rhs id.
     */
    public int rhs;

    /**
     * Constructs an NHIRArithmetic object.
     *
     * @param block  enclosing block.
     * @param id     identifier of the instruction.
     * @param opcode opcode of the arithmetic instruction.
     * @param lhs    lhs id.
     * @param rhs    rhs id.
     */
    public NHIRArithmetic(NBasicBlock block, int id, int opcode, int lhs, int rhs) {
        super(block, id, "I");
        this.opcode = opcode;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    /**
     * {@inheritDoc}
     */
    public NLIRInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        NLIRInstruction ins1 = block.cfg.hirMap.get(lhs).toLir();
        NLIRInstruction ins2 = block.cfg.hirMap.get(rhs).toLir();
        lir = new NLIRArithmetic(block, NControlFlowGraph.lirId++, opcode, ins1, ins2);
        block.lir.add(lir);
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + block.cfg.hirMap.get(lhs).id() + " " + hirMnemonic.get(opcode) + " " +
                block.cfg.hirMap.get(rhs).id();
    }
}

/**
 * HIR instruction corresponding to the JVM instructions representing integer constants.
 */
class NHIRIntConstant extends NHIRInstruction {
    /**
     * The constant int value.
     */
    public int value;

    /**
     * Constructs an NHIRIntConstant object.
     *
     * @param block enclosing block.
     * @param id    identifier of the instruction.
     * @param value the constant int value.
     */
    public NHIRIntConstant(NBasicBlock block, int id, int value) {
        super(block, id, "I");
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public NLIRInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        lir = new NLIRIntConstant(block, NControlFlowGraph.lirId++, value);
        block.lir.add(lir);
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + value;
    }
}

/**
 * HIR instruction representing a JVM return instruction.
 */
class NHIRReturn extends NHIRInstruction {
    /**
     * Opcode of the return instruction.
     */
    public int opcode;

    /**
     * Return value id.
     */
    public int value;

    /**
     * Constructs an NHIRReturn object.
     *
     * @param block  enclosing block.
     * @param id     identifier of the instruction.
     * @param opcode opcode for the return instruction.
     * @param value  return value id.
     */
    public NHIRReturn(NBasicBlock block, int id, int opcode, int value) {
        super(block, id, value == -1 ? "" : "I");
        this.opcode = opcode;
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public NLIRInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        NLIRInstruction result = null;
        if (value != -1) {
            result = block.cfg.hirMap.get(value).toLir();
            NLIRCopy copy = new NLIRCopy(block, NControlFlowGraph.lirId++, result.write,
                    NPhysicalRegister.regInfo[RV]);
            block.lir.add(copy);
            block.cfg.registers.set(RV, NPhysicalRegister.regInfo[RV]);
        }
        lir = new NLIRReturn(block, NControlFlowGraph.lirId++, opcode, result == null ? null :
                NPhysicalRegister.regInfo[RV]);
        block.lir.add(lir);
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        if (value == -1) {
            return id() + ": " + hirMnemonic.get(opcode);
        }
        return id() + ": " + hirMnemonic.get(opcode) + " " + block.cfg.hirMap.get(value).id();
    }
}

/**
 * HIR instruction representing method invocation (INVOKESTATIC) instruction in JVM.
 */
class NHIRMethodCall extends NHIRInstruction {
    /**
     * Name of the method being invoked.
     */
    public String name;

    /**
     * Arguments to the method.
     */
    public ArrayList<Integer> args;

    /**
     * Constructs an NHIRInvoke object.
     *
     * @param block enclosing block.
     * @param id    identifier of the instruction.
     * @param name  name of the method.
     * @param args  arguments to the method.
     * @param type  return type.
     */
    public NHIRMethodCall(NBasicBlock block, int id, String name, ArrayList<Integer> args, String type) {
        super(block, id, type);
        this.name = name;
        this.args = args;
    }

    /**
     * {@inheritDoc}
     */
    public NLIRInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        ArrayList<NRegister> arguments = new ArrayList<>();
        for (int i = 0; i < this.args.size(); i++) {
            int arg = args.get(i);
            NLIRInstruction ins = block.cfg.hirMap.get(arg).toLir();
            arguments.add(ins.write);
        }
        lir = new NLIRMethodCall(block, NControlFlowGraph.lirId++, name, arguments, type);
        block.lir.add(lir);

        // If the function returns a value, generate an LIR move instruction to save away the value in the physical
        // register v0 into a virtual register.
        if (lir.write != null) {
            NVirtualRegister to = new NVirtualRegister(NControlFlowGraph.regId++);
            NLIRCopy copy = new NLIRCopy(block, NControlFlowGraph.lirId++, NPhysicalRegister.regInfo[RV], to);
            block.cfg.registers.add(to);
            block.lir.add(copy);
            lir = copy;
        }

        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        String s = id() + ": " + hirMnemonic.get(INVOKESTATIC) + " " + name + "(";
        for (int i = 0; i < args.size(); i++) {
            int arg = args.get(i);
            s += block.cfg.hirMap.get(arg).id() + ", ";
        }
        return s.endsWith(", ") ? s.substring(0, s.length() - 2) + ")" : s + ")";
    }
}

/**
 * HIR instruction representing conditional jump instructions in JVM.
 */
class NHIRJump extends NHIRInstruction {
    /**
     * Opcode of the instruction.
     */
    public int opcode;

    /**
     * Lhs id.
     */
    public int lhs;

    /**
     * Rhs id.
     */
    public int rhs;

    /**
     * Block to jump to on true.
     */
    public NBasicBlock onTrueBlock;

    /**
     * Block to jump to on false.
     */
    public NBasicBlock onFalseBlock;

    /**
     * Constructs an NHIRConditionalJump object.
     *
     * @param block        enclosing block.
     * @param id           identifier of the instruction.
     * @param opcode       opcode of the JVM instruction.
     * @param lhs          Lhs id.
     * @param rhs          Rhs id.
     * @param onTrueBlock  block to jump to on true.
     * @param onFalseBlock block to jump to on false.
     */
    public NHIRJump(NBasicBlock block, int id, int opcode, int lhs, int rhs, NBasicBlock onTrueBlock,
                    NBasicBlock onFalseBlock) {
        super(block, id, "");
        this.opcode = opcode;
        this.lhs = lhs;
        this.rhs = rhs;
        this.onTrueBlock = onTrueBlock;
        this.onFalseBlock = onFalseBlock;
    }

    /**
     * {@inheritDoc}
     */
    public NLIRInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        if (opcode != GOTO) {
            // Conditional jump.
            NLIRInstruction ins1 = block.cfg.hirMap.get(lhs).toLir();
            NLIRInstruction ins2 = block.cfg.hirMap.get(rhs).toLir();
            NLIRInstruction sub = new NLIRArithmetic(block, NControlFlowGraph.lirId++, ISUB, ins1, ins2);
            block.lir.add(sub);
            lir = new NLIRJump(block, NControlFlowGraph.lirId++, opcode, sub, onTrueBlock, onFalseBlock);
            block.lir.add(lir);
        } else {
            // Unconditional jump.
            lir = new NLIRJump(block, NControlFlowGraph.lirId++, onTrueBlock);
            block.lir.add(lir);
        }
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        if (onFalseBlock == null) {
            return id() + ": " + hirMnemonic.get(opcode) + " " + onTrueBlock.id();
        }
        return id() + ": if " + block.cfg.hirMap.get(lhs).id() + " " + hirMnemonic.get(opcode) + " " +
                block.cfg.hirMap.get(rhs).id() + " then " + onTrueBlock.id() + " else " + onFalseBlock.id();
    }
}
