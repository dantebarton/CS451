package iota;

import java.util.ArrayList;
import java.util.HashMap;

import static iota.CLConstants.*;
import static iota.NPhysicalRegister.*;

/**
 * An abstract high-level intermediate representation (HIR) of a JVM instruction.
 */
abstract class NHIRInstruction {
    /**
     * Maps JVM opcode to the corresponding HIR mnemonic. For example, maps IMUL to "*".
     */
    protected static HashMap<Integer, String> jvm2HIR;

    // Create and populate jvm2HIR.
    static {
        jvm2HIR = new HashMap<>();
        jvm2HIR.put(LDC, "ldc");
        jvm2HIR.put(IADD, "+");
        jvm2HIR.put(IDIV, "/");
        jvm2HIR.put(IMUL, "*");
        jvm2HIR.put(IREM, "%");
        jvm2HIR.put(ISUB, "-");
        jvm2HIR.put(GOTO, "goto");
        jvm2HIR.put(IF_ICMPEQ, "==");
        jvm2HIR.put(IF_ICMPGE, ">=");
        jvm2HIR.put(IF_ICMPGT, ">");
        jvm2HIR.put(IF_ICMPLE, "<=");
        jvm2HIR.put(IF_ICMPLT, "<");
        jvm2HIR.put(IF_ICMPNE, "!=");
        jvm2HIR.put(INVOKESTATIC, "invoke");
        jvm2HIR.put(RETURN, "return");
        jvm2HIR.put(IRETURN, "ireturn");
    }

    /**
     * Maps HIR mnemonic to the corresponding LIR mnemonic. For example, maps "*" to "mul".
     */
    protected static HashMap<String, String> hir2lir;

    // Create and populate hir2lir.
    static {
        hir2lir = new HashMap<>();
        hir2lir.put("phi", "phi");
        hir2lir.put("ldc", "setn");
        hir2lir.put("+", "add");
        hir2lir.put("/", "div");
        hir2lir.put("*", "mul");
        hir2lir.put("%", "mod");
        hir2lir.put("-", "sub");
        hir2lir.put("goto", "jump");
        hir2lir.put("==", "jeq");
        hir2lir.put(">=", "jge");
        hir2lir.put(">", "jgt");
        hir2lir.put("<=", "jle");
        hir2lir.put("<", "jlt");
        hir2lir.put("!=", "jne");
        hir2lir.put("invoke", "call");
        hir2lir.put("return", "return");
        hir2lir.put("ireturn", "return");
    }

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
     * Type of this instruction ("I" for int and boolean, "V" for void, and "" for no type).
     */
    public String type;

    /**
     * The corresponding LIR instruction.
     */
    public NLIRInstruction lir;

    /**
     * Constructs an NHIRInstruction object.
     *
     * @param block    enclosing basic block.
     * @param id       id of the instruction.
     * @param mnemonic mnemonic of the instruction.
     */
    protected NHIRInstruction(NBasicBlock block, int id, String mnemonic) {
        this(block, id, mnemonic, "");
    }

    /**
     * Constructs an NHIRInstruction object.
     *
     * @param block    enclosing basic block.
     * @param id       id of the instruction.
     * @param mnemonic mnemonic of the instruction.
     * @param type     type of the instruction.
     */
    protected NHIRInstruction(NBasicBlock block, int id, String mnemonic, String type) {
        this.block = block;
        this.id = id;
        this.mnemonic = mnemonic;
        this.type = type;
        lir = null;
    }

    /**
     * Returns the id of this instruction as a string.
     *
     * @return the id of this instruction as a string.
     */
    public String id() {
        return type + id;
    }

    /**
     * Converts and returns a low-level representation (LIR) of this instruction.
     *
     * @return the LIR instruction corresponding to this instruction.
     */
    public abstract NLIRInstruction toLir();

    /**
     * Returns true if this instruction is the same (ie, has the same id) as the other, and false otherwise.
     *
     * @param other the instruction to compare to.
     * @return true if this instruction is the same (ie, has the same id) as the other, and false otherwise.
     */
    public boolean equals(NHIRInstruction other) {
        return id == other.id;
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
 * HIR instruction representing a phi function.
 */
class NHIRPhiFunction extends NHIRInstruction {
    /**
     * Arguments to the function.
     */
    public ArrayList<NHIRInstruction> args;

    /**
     * Index of the variable to which the function is bound.
     */
    public int index;

    /**
     * Constructs an NHIRPhiFunction object.
     *
     * @param block enclosing basic block.
     * @param id    id of the instruction.
     * @param args  arguments to the function.
     * @param index index of the variable to which the function is bound.
     */
    public NHIRPhiFunction(NBasicBlock block, int id, ArrayList<NHIRInstruction> args, int index) {
        super(block, id, "phi", "I");
        this.args = args;
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */
    public NLIRInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        lir = new NLIRPhiFunction(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic));
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        String s = id() + ": " + mnemonic + "(";
        for (NHIRInstruction ins : args) {
            s += ins == null ? "?, " : ins.id() + ", ";
        }
        return !s.endsWith(", ") ? s + ")" : s.substring(0, s.length() - 2) + ")";
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
     * @param block enclosing basic block.
     * @param id    id of the instruction.
     * @param index index of the parameter.
     */
    public NHIRLoadParam(NBasicBlock block, int id, int index) {
        super(block, id, "ldparam", "I");
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */
    public NLIRInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        NLIRIntConstant offset = new NLIRIntConstant(block, NControlFlowGraph.lirId++, hir2lir.get("ldc"),
                -(index + 3));
        NLIRCopy fp = new NLIRCopy(block, NControlFlowGraph.lirId++, new NVirtualRegister(NControlFlowGraph.regId++),
                NPhysicalRegister.regInfo[FP]);
        NLIRArithmetic address = new NLIRArithmetic(block, NControlFlowGraph.lirId++, hir2lir.get("+"), fp, offset);
        lir = new NLIRLoad(block, NControlFlowGraph.lirId++, new NVirtualRegister(NControlFlowGraph.regId++),
                address.write);
        block.lir.add(offset);
        block.lir.add(fp);
        block.lir.add(address);
        block.lir.add(lir);
        block.cfg.registers.set(FP, NPhysicalRegister.regInfo[FP]);
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + index;
    }
}

/**
 * HIR instruction representing an integer constant.
 */
class NHIRIntConstant extends NHIRInstruction {
    /**
     * The constant int value.
     */
    public int value;

    /**
     * Constructs an NHIRIntConstant object.
     *
     * @param block enclosing basic block.
     * @param id    id of the instruction.
     * @param value the constant int value.
     */
    public NHIRIntConstant(NBasicBlock block, int id, int value) {
        super(block, id, jvm2HIR.get(LDC), "I");
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public NLIRInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        lir = new NLIRIntConstant(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic), value);
        block.lir.add(lir);
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + value;
    }
}

/**
 * HIR instruction representing an arithmetic instruction.
 */
class NHIRArithmetic extends NHIRInstruction {
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
     * @param id     id of the instruction.
     * @param block  enclosing basic block.
     * @param opcode opcode of the instruction.
     * @param lhs    lhs id.
     * @param rhs    rhs id.
     */
    public NHIRArithmetic(NBasicBlock block, int id, int opcode, int lhs, int rhs) {
        super(block, id, jvm2HIR.get(opcode), "I");
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
        NLIRInstruction lhsLIR = block.cfg.hirMap.get(lhs).toLir();
        NLIRInstruction rhsLIR = block.cfg.hirMap.get(rhs).toLir();
        lir = new NLIRArithmetic(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic), lhsLIR, rhsLIR);
        block.lir.add(lir);
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + block.cfg.hirMap.get(lhs).id() + " " + mnemonic + " " + block.cfg.hirMap.get(rhs).id();
    }
}

/**
 * HIR instruction representing conditional or unconditional jump instruction.
 */
class NHIRJump extends NHIRInstruction {
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
     * Block to jump to on false (null for an unconditional jump).
     */
    public NBasicBlock onFalseBlock;

    /**
     * Constructs an NHIRJump object for an unconditional jump.
     *
     * @param block       enclosing basic block.
     * @param id          id of the instruction.
     * @param onTrueBlock block to jump to.
     */
    public NHIRJump(NBasicBlock block, int id, NBasicBlock onTrueBlock) {
        super(block, id, jvm2HIR.get(GOTO));
        lhs = -1;
        rhs = -1;
        this.onTrueBlock = onTrueBlock;
    }

    /**
     * Constructs an NHIRJump object for a conditional jump.
     *
     * @param block        enclosing basic block.
     * @param id           id of the instruction.
     * @param opcode       opcode of the instruction.
     * @param lhs          Lhs id.
     * @param rhs          Rhs id.
     * @param onTrueBlock  block to jump to on true.
     * @param onFalseBlock block to jump to on false.
     */
    public NHIRJump(NBasicBlock block, int id, int opcode, int lhs, int rhs, NBasicBlock onTrueBlock,
                    NBasicBlock onFalseBlock) {
        super(block, id, jvm2HIR.get(opcode));
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
        if (onFalseBlock == null) {
            // Unconditional jump.
            lir = new NLIRJump(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic), onTrueBlock);
        } else {
            // Conditional jump.
            NLIRInstruction lhsLIR = block.cfg.hirMap.get(lhs).toLir();
            NLIRInstruction rhsLIR = block.cfg.hirMap.get(rhs).toLir();
            lir = new NLIRJump(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic), lhsLIR, rhsLIR, onTrueBlock,
                    onFalseBlock);
        }
        block.lir.add(lir);
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        if (onFalseBlock == null) {
            return id() + ": " + mnemonic + " " + onTrueBlock.id();
        }
        return id() + ": if " + block.cfg.hirMap.get(lhs).id() + " " + mnemonic + " " +
                block.cfg.hirMap.get(rhs).id() + " then " + onTrueBlock.id() + " else " + onFalseBlock.id();
    }
}

/**
 * HIR instruction representing a return instruction.
 */
class NHIRReturn extends NHIRInstruction {
    /**
     * Return value id.
     */
    public int value;

    /**
     * Constructs an NHIRReturn object for return instruction without a value.
     *
     * @param block enclosing basic block.
     * @param id    id of the instruction.
     */
    public NHIRReturn(NBasicBlock block, int id) {
        super(block, id, jvm2HIR.get(RETURN), "");
        this.value = -1;
    }

    /**
     * Constructs an NHIRReturn object for return instruction with a value.
     *
     * @param block enclosing basic block.
     * @param id    id of the instruction.
     * @param value return value id.
     */
    public NHIRReturn(NBasicBlock block, int id, int value) {
        super(block, id, jvm2HIR.get(IRETURN), "I");
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
        if (value == -1) {
            lir = new NLIRReturn(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic));
        } else {
            result = block.cfg.hirMap.get(value).toLir();
            NLIRCopy copy = new NLIRCopy(block, NControlFlowGraph.lirId++, NPhysicalRegister.regInfo[RV], result.write);
            block.lir.add(copy);
            block.cfg.registers.set(RV, NPhysicalRegister.regInfo[RV]);
            lir = new NLIRReturn(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic),
                    NPhysicalRegister.regInfo[RV]);
        }
        block.lir.add(lir);
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + (value == -1 ? mnemonic : mnemonic + " " + block.cfg.hirMap.get(value).id());
    }
}

/**
 * HIR instruction representing a method call instruction.
 */
class NHIRMethodCall extends NHIRInstruction {
    /**
     * Name of the method.
     */
    public String name;

    /**
     * Arguments to the method.
     */
    public ArrayList<Integer> args;

    /**
     * Constructs an NHIRMethodCall object.
     *
     * @param block enclosing basic block.
     * @param id    id of the instruction.
     * @param name  name of the method.
     * @param args  arguments to the method.
     * @param type  return type.
     */
    public NHIRMethodCall(NBasicBlock block, int id, String name, ArrayList<Integer> args, String type) {
        super(block, id, jvm2HIR.get(INVOKESTATIC), type);
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

        for (int i = args.size() - 1; i >= 0; i--) {
            int arg = args.get(i);
            NLIRInstruction ins = block.cfg.hirMap.get(arg).toLir();
            arguments.add(0, ins.write);
            NLIRPush push = new NLIRPush(block, NControlFlowGraph.lirId++, ins.write, NPhysicalRegister.regInfo[SP]);
            block.lir.add(push);
        }

        lir = new NLIRMethodCall(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic), name, arguments, type);
        block.lir.add(lir);
        if (lir.write != null) {
            NVirtualRegister to = new NVirtualRegister(NControlFlowGraph.regId++);
            NLIRCopy copy = new NLIRCopy(block, NControlFlowGraph.lirId++, to, NPhysicalRegister.regInfo[RV]);
            block.cfg.registers.add(to);
            block.lir.add(copy);
            lir = copy;
        }

        NLIRIncrement add = new NLIRIncrement(block, NControlFlowGraph.lirId++, NPhysicalRegister.regInfo[SP],
                -args.size());
        block.lir.add(add);

        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        String s = id() + ": " + mnemonic + " " + name + "(";
        for (int i = 0; i < args.size(); i++) {
            int arg = args.get(i);
            s += block.cfg.hirMap.get(arg).id() + ", ";
        }
        return !s.endsWith(", ") ? s + ")" : s.substring(0, s.length() - 2) + ")";
    }
}
