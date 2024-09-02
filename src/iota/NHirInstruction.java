package iota;

import java.util.ArrayList;
import java.util.HashMap;

import static iota.CLConstants.*;
import static iota.NPhysicalRegister.*;

/**
 * An abstract high-level intermediate representation (HIR) of a JVM instruction.
 */
abstract class NHirInstruction {
    /**
     * Maps JVM opcode to the corresponding HIR mnemonic. For example, maps IMUL to "*".
     */
    protected static HashMap<Integer, String> jvm2Hir;

    // Create and populate jvm2HIR.
    static {
        jvm2Hir = new HashMap<>();
        jvm2Hir.put(LDC, "ldc");
        jvm2Hir.put(IADD, "+");
        jvm2Hir.put(IDIV, "/");
        jvm2Hir.put(IMUL, "*");
        jvm2Hir.put(IREM, "%");
        jvm2Hir.put(ISUB, "-");
        jvm2Hir.put(GOTO, "goto");
        jvm2Hir.put(IF_ICMPEQ, "==");
        jvm2Hir.put(IF_ICMPGE, ">=");
        jvm2Hir.put(IF_ICMPGT, ">");
        jvm2Hir.put(IF_ICMPLE, "<=");
        jvm2Hir.put(IF_ICMPLT, "<");
        jvm2Hir.put(IF_ICMPNE, "!=");
        jvm2Hir.put(INVOKESTATIC, "invoke");
        jvm2Hir.put(RETURN, "return");
        jvm2Hir.put(IRETURN, "ireturn");
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
     * Enclosing basic block.
     */
    public NBasicBlock block;

    /**
     * Instruction id.
     */
    public int id;

    /**
     * Instruction mnemonic.
     */
    public String mnemonic;

    /**
     * Instruction type ("I" for int and boolean, "V" for void, and "" for no type).
     */
    public String type;

    /**
     * The corresponding LIR instruction.
     */
    public NLirInstruction lir;

    /**
     * Constructs an NHirInstruction object.
     *
     * @param block    enclosing basic block.
     * @param id       instruction id.
     * @param mnemonic instruction mnemonic.
     */
    protected NHirInstruction(NBasicBlock block, int id, String mnemonic) {
        this(block, id, mnemonic, "");
    }

    /**
     * Constructs an NHirInstruction object.
     *
     * @param block    enclosing basic block.
     * @param id       instruction id.
     * @param mnemonic instruction mnemonic.
     * @param type     instruction type.
     */
    protected NHirInstruction(NBasicBlock block, int id, String mnemonic, String type) {
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
    public abstract NLirInstruction toLir();

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
 * Representation of an arithmetic instruction.
 */
class NHirArithmetic extends NHirInstruction {
    /**
     * Lhs instruction id.
     */
    public int lhs;

    /**
     * Rhs instruction id.
     */
    public int rhs;

    /**
     * Constructs an NHirArithmetic object.
     *
     * @param id     instruction id.
     * @param block  enclosing basic block.
     * @param opcode instruction opcode.
     * @param lhs    lhs instruction id.
     * @param rhs    rhs instruction id.
     */
    public NHirArithmetic(NBasicBlock block, int id, int opcode, int lhs, int rhs) {
        super(block, id, jvm2Hir.get(opcode), "I");
        this.lhs = lhs;
        this.rhs = rhs;
    }

    /**
     * {@inheritDoc}
     */
    public NLirInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        NLirInstruction lhsIns = block.cfg.hirMap.get(lhs).toLir();
        NLirInstruction rhsIns = block.cfg.hirMap.get(rhs).toLir();
        lir = new NLirArithmetic(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic), lhsIns, rhsIns);
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
 * Representation of a method call instruction.
 */
class NHirCall extends NHirInstruction {
    /**
     * Name of the method.
     */
    public String name;

    /**
     * Arguments to the method.
     */
    public ArrayList<Integer> args;

    /**
     * Whether this method is an input (read()I) or output (write(I)V) method.
     */
    public boolean isIOMethod;

    /**
     * Constructs an NHirCall object.
     *
     * @param block      enclosing basic block.
     * @param id         instruction id.
     * @param name       name of the method.
     * @param args       arguments to the method.
     * @param type       return type.
     * @param isIOMethod whether this method is an input (read()I) or output (write(I)V) method.
     */
    public NHirCall(NBasicBlock block, int id, String name, ArrayList<Integer> args, String type, boolean isIOMethod) {
        super(block, id, jvm2Hir.get(INVOKESTATIC), type);
        this.name = name;
        this.args = args;
        this.isIOMethod = isIOMethod;
    }

    /**
     * {@inheritDoc}
     */
    public NLirInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        if (isIOMethod && name.equals("read")) {
            // read()I maps to read instruction.
            NVirtualRegister to = new NVirtualRegister(NControlFlowGraph.regId++);
            lir = new NLirRead(block, NControlFlowGraph.lirId++, to);
            block.lir.add(lir);
        } else if (isIOMethod && name.equals("write")) {
            // write(I)V maps to write instruction.
            NLirInstruction arg = block.cfg.hirMap.get(args.get(0)).toLir();
            lir = new NLirWrite(block, NControlFlowGraph.lirId++, arg.write);
            block.lir.add(lir);
        } else {
            ArrayList<NRegister> arguments = new ArrayList<>();

            // Arguments are passed by storing them in memory.
            for (int i = args.size() - 1; i >= 0; i--) {
                NLirInstruction argIns = block.cfg.hirMap.get(args.get(i)).toLir();
                arguments.add(0, argIns.write);
                NLirPush arg = new NLirPush(block, NControlFlowGraph.lirId++, argIns.write, regInfo[SP]);
                block.lir.add(arg);
            }

            NLirCall call = new NLirCall(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic), name, arguments,
                    type);
            block.lir.add(call);

            // Pop the stack frame that was set up for and by the method call.
            lir = new NLirInc(block, NControlFlowGraph.lirId++, regInfo[SP], -args.size());
            block.lir.add(lir);

            // Return value from the method is stored in RV. We copy it into a virtual register that we create
            // for the return value.
            if (lir.write != null) {
                NVirtualRegister to = new NVirtualRegister(NControlFlowGraph.regId++);
                lir = new NLirCopy(block, NControlFlowGraph.lirId++, to, regInfo[RV]);
                block.lir.add(lir);
            }
        }
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

/**
 * Representation of an instruction for loading an integer.
 */
class NHirIConst extends NHirInstruction {
    /**
     * The integer.
     */
    public int N;

    /**
     * Constructs an NHirIConst object.
     *
     * @param block enclosing basic block.
     * @param id    instruction id.
     * @param N     the integer.
     */
    public NHirIConst(NBasicBlock block, int id, int N) {
        super(block, id, jvm2Hir.get(LDC), "I");
        this.N = N;
    }

    /**
     * {@inheritDoc}
     */
    public NLirInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        lir = new NLirIConst(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic), N);
        block.lir.add(lir);
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return id() + ": " + mnemonic + " " + N;
    }
}

/**
 * Representation of a jump (conditional or unconditional) jump instruction.
 */
class NHirJump extends NHirInstruction {
    /**
     * Lhs instruction id.
     */
    public int lhs;

    /**
     * Rhs instruction id.
     */
    public int rhs;

    /**
     * Block to jump to on true.
     */
    public NBasicBlock trueBlock;

    /**
     * Block to jump to on false (null for an unconditional jump).
     */
    public NBasicBlock falseBlock;

    /**
     * Constructs an NHirJump object for an unconditional jump.
     *
     * @param block     enclosing basic block.
     * @param id        instruction id.
     * @param trueBlock block to jump to.
     */
    public NHirJump(NBasicBlock block, int id, NBasicBlock trueBlock) {
        super(block, id, jvm2Hir.get(GOTO));
        lhs = -1;
        rhs = -1;
        this.trueBlock = trueBlock;
        falseBlock = null;
    }

    /**
     * Constructs an NHirJump object for a conditional jump.
     *
     * @param block      enclosing basic block.
     * @param id         instruction id.
     * @param opcode     instruction opcode.
     * @param lhs        Lhs instruction id.
     * @param rhs        Rhs instruction id.
     * @param trueBlock  block to jump to on true.
     * @param falseBlock block to jump to on false.
     */
    public NHirJump(NBasicBlock block, int id, int opcode, int lhs, int rhs, NBasicBlock trueBlock,
                    NBasicBlock falseBlock) {
        super(block, id, jvm2Hir.get(opcode));
        this.lhs = lhs;
        this.rhs = rhs;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
    }

    /**
     * {@inheritDoc}
     */
    public NLirInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        if (falseBlock == null) {
            // Unconditional jump.
            lir = new NLirJump(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic), trueBlock);
        } else {
            // Conditional jump.
            NLirInstruction lhsIns = block.cfg.hirMap.get(lhs).toLir();
            NLirInstruction rhsIns = block.cfg.hirMap.get(rhs).toLir();
            lir = new NLirJump(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic), lhsIns, rhsIns, trueBlock,
                    falseBlock);
        }
        block.lir.add(lir);
        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        if (falseBlock == null) {
            return id() + ": " + mnemonic + " " + trueBlock.id();
        }
        return id() + ": if " + block.cfg.hirMap.get(lhs).id() + " " + mnemonic + " " +
                block.cfg.hirMap.get(rhs).id() + " then " + trueBlock.id() + " else " + falseBlock.id();
    }
}

/**
 * Representation of an instruction for loading a formal parameter value.
 */
class NHirLoadParam extends NHirInstruction {
    /**
     * Formal parameter index.
     */
    public int index;

    /**
     * Constructs an NHirLoadParam object.
     *
     * @param block enclosing basic block.
     * @param id    instruction id.
     * @param index formal parameter index.
     */
    public NHirLoadParam(NBasicBlock block, int id, int index) {
        super(block, id, "ldparam", "I");
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */
    public NLirInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        NLirCopy fp = new NLirCopy(block, NControlFlowGraph.lirId++, regInfo[R11], regInfo[FP]);
        NLirInc address = new NLirInc(block, NControlFlowGraph.lirId++, fp.write, -(index + 3));
        NVirtualRegister param = new NVirtualRegister(NControlFlowGraph.regId++);
        lir = new NLirLoad(block, NControlFlowGraph.lirId++, "load", param, address.write);
        block.lir.add(fp);
        block.lir.add(address);
        block.lir.add(lir);
        block.cfg.registers.set(R11, regInfo[R11]);
        block.cfg.registers.set(FP, regInfo[FP]);
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
 * Representation of a phi function.
 */
class NHirPhiFunction extends NHirInstruction {
    /**
     * Function arguments.
     */
    public ArrayList<NHirInstruction> args;

    /**
     * Index of the variable to which the function is bound.
     */
    public int index;

    /**
     * Constructs an NHirPhiFunction object.
     *
     * @param block enclosing basic block.
     * @param id    instruction id.
     * @param args  function arguments.
     * @param index index of the variable to which the function is bound.
     */
    public NHirPhiFunction(NBasicBlock block, int id, ArrayList<NHirInstruction> args, int index) {
        super(block, id, "phi", "I");
        this.args = args;
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */
    public NLirInstruction toLir() {
        if (lir != null) {
            return lir;
        }

        // We create an LIR instruction for the phi function so a virtual register is created for it. We do not add
        // the instruction to the block.cfg.lir list.
        lir = new NLirPhiFunction(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic));

        return lir;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        String s = id() + ": " + mnemonic + "(";
        for (NHirInstruction ins : args) {
            s += ins == null ? "?, " : ins.id() + ", ";
        }
        return !s.endsWith(", ") ? s + ")" : s.substring(0, s.length() - 2) + ")";
    }
}

/**
 * Representation of a return instruction.
 */
class NHirReturn extends NHirInstruction {
    /**
     * Instruction id of the return value.
     */
    public int value;

    /**
     * Constructs an NHirReturn object for return instruction without a value.
     *
     * @param block enclosing basic block.
     * @param id    instruction id.
     */
    public NHirReturn(NBasicBlock block, int id) {
        super(block, id, jvm2Hir.get(RETURN), "");
        this.value = -1;
    }

    /**
     * Constructs an NHirReturn object for return instruction with a value.
     *
     * @param block enclosing basic block.
     * @param id    instruction id.
     * @param value instruction id of the return value.
     */
    public NHirReturn(NBasicBlock block, int id, int value) {
        super(block, id, jvm2Hir.get(IRETURN), "I");
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public NLirInstruction toLir() {
        if (lir != null) {
            return lir;
        }
        NLirInstruction result = null;
        if (value == -1) {
            lir = new NLirReturn(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic));
        } else {
            result = block.cfg.hirMap.get(value).toLir();
            NLirCopy rv = new NLirCopy(block, NControlFlowGraph.lirId++, regInfo[RV], result.write);
            block.lir.add(rv);
            block.cfg.registers.set(RV, regInfo[RV]);
            lir = new NLirReturn(block, NControlFlowGraph.lirId++, hir2lir.get(mnemonic), regInfo[RV]);
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
