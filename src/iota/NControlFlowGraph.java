package iota;


import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import static iota.CLConstants.*;
import static iota.NPhysicalRegister.*;

/**
 * Representation of a basic block within a control-flow graph (cfg).
 */
class NBasicBlock {
    /**
     * Unique identifier of this block.
     */
    public int id;

    /**
     * The cfg that this block belongs to.
     */
    public NControlFlowGraph cfg;

    /**
     * List of tuples in this block.
     */
    public ArrayList<NTuple> tuples;

    /**
     * Has this block been visited?
     */
    public boolean isVisited;

    /**
     * Is this block active?
     */
    public boolean isActive;

    /**
     * List of predecessors of this block.
     */
    public ArrayList<NBasicBlock> predecessors;

    /**
     * List of successors of this block.
     */
    public ArrayList<NBasicBlock> successors;

    /**
     * Is this block a loop head?
     */
    public boolean isLoopHead;

    /**
     * Is this block a loop tail?
     */
    public boolean isLoopTail;

    /**
     * State vector of this block.
     */
    public NHirInstruction[] locals;

    /**
     * List of high-level (HIR) instructions in this block.
     */
    public ArrayList<NHirInstruction> hir;

    /**
     * List of low-level (LIR) instructions in this block.
     */
    public ArrayList<NLirInstruction> lir;

    /**
     * List of Marvin (ie, target machine) instructions in this block.
     */
    public ArrayList<NMarvinInstruction> marvin;

    /**
     * The local liveUse set (registers that are used before they are defined in this block).
     */
    public BitSet liveUse;

    /**
     * The local liveDef set (registers that are written to in this block).
     */
    public BitSet liveDef;

    /**
     * The global liveIn set (this.liveOut - this.liveDef + this.liveUse).
     */
    public BitSet liveIn;

    /**
     * The global liveOut set (union of s.liveIn for each successor s of this block).
     */
    public BitSet liveOut;

    /**
     * Constructs an NBasicBlock object.
     *
     * @param cfg the cfg containing the block.
     * @param id  id of the block.
     */
    public NBasicBlock(NControlFlowGraph cfg, int id) {
        this.cfg = cfg;
        this.id = id;
        tuples = new ArrayList<>();
        isVisited = false;
        isActive = false;
        predecessors = new ArrayList<>();
        successors = new ArrayList<>();
        isLoopHead = false;
        isLoopTail = false;
        hir = new ArrayList<>();
        lir = new ArrayList<>();
        marvin = new ArrayList<>();
    }

    /**
     * Returns the id of this block as a string.
     *
     * @return the id of this block as a string.
     */
    public String id() {
        return "B" + id;
    }

    /**
     * Returns true if this block and other have the same id, and false otherwise.
     *
     * @param other the other block.
     * @return true if this block and other have the same id, and false otherwise.
     */
    public boolean equals(NBasicBlock other) {
        return this.id == other.id;
    }

    /**
     * Returns a string representation of this block.
     *
     * @return string representation of this block.
     */
    public String toString() {
        return id();
    }

    /**
     * Writes the tuples in this block to standard output.
     *
     * @param p for pretty printing with indentation.
     */
    public void writeTuplesToStdOut(PrettyPrinter p) {
        String lh = isLoopHead ? ", LH" : "";
        String lt = isLoopTail ? ", LT" : "";
        p.printf("%s (pred: %s, succ: %s%s%s):\n", id(), predecessors.toString(), successors.toString(), lh, lt);
        for (NTuple tuple : tuples) {
            tuple.writeToStdOut(p);
        }
        p.println();
    }

    /**
     * Writes the HIR instructions in this block to standard output.
     *
     * @param p for pretty printing with indentation.
     */
    public void writeHirToStdOut(PrettyPrinter p) {
        String localsStr = "";
        String lh = isLoopHead ? ", LH" : "";
        String lt = isLoopTail ? ", LT" : "";
        for (int i = 0; i < locals.length; i++) {
            NHirInstruction ins = locals[i];
            localsStr += ins != null ? cfg.hirMap.get(ins.id).id() + ", " : "?, ";
        }
        localsStr = localsStr.isEmpty() ? "[]" : "[" + localsStr.substring(0, localsStr.length() - 2) + "]";
        p.printf("%s (pred: %s, succ: %s%s%s, locals: %s):\n", id(), predecessors.toString(), successors.toString(),
                lh, lt, localsStr);
        for (NHirInstruction instruction : hir) {
            p.printf("%s\n", cfg.hirMap.get(instruction.id));
        }
        p.println();
    }

    /**
     * Writes the LIR instructions in this block to standard output.
     *
     * @param p for pretty printing with indentation.
     */
    public void writeLirToStdOut(PrettyPrinter p) {
        String lh = isLoopHead ? ", LH" : "";
        String lt = isLoopTail ? ", LT" : "";
        p.printf("%s (pred: %s, succ: %s%s%s):\n", id(), predecessors.toString(), successors.toString(), lh, lt);
        for (NLirInstruction instruction : lir) {
            p.printf("%s\n", instruction);
        }
        p.println();
    }

    public void writeLivenessSetsToStdOut(PrettyPrinter p) {
        p.printf("%s:\n", id());
        String s = "";
        for (int i = liveUse.nextSetBit(0); i >= 0; i = liveUse.nextSetBit(i + 1)) {
            s += i < 16 ? regInfo[i] + ", " : "v" + i + ", ";
        }
        s = s.isEmpty() ? "" : s.substring(0, s.length() - 2);
        p.printf("liveUse: {%s}\n", s);
        s = "";
        for (int i = liveDef.nextSetBit(0); i >= 0; i = liveDef.nextSetBit(i + 1)) {
            s += i < 16 ? regInfo[i] + ", " : "v" + i + ", ";
        }
        s = s.isEmpty() ? "" : s.substring(0, s.length() - 2);
        p.printf("liveDef: {%s}\n", s);
        s = "";
        for (int i = liveIn.nextSetBit(0); i >= 0; i = liveIn.nextSetBit(i + 1)) {
            s += i < 16 ? regInfo[i] + ", " : "v" + i + ", ";
        }
        s = s.isEmpty() ? "" : s.substring(0, s.length() - 2);
        p.printf("liveIn: {%s}\n", s);
        s = "";
        for (int i = liveOut.nextSetBit(0); i >= 0; i = liveOut.nextSetBit(i + 1)) {
            s += i < 16 ? regInfo[i] + ", " : "v" + i + ", ";
        }
        s = s.isEmpty() ? "" : s.substring(0, s.length() - 2);
        p.printf("liveOut: {%s}\n\n", s);
    }
}

/**
 * Representation of a control flow graph (cfg) for a method.
 */
class NControlFlowGraph {
    // Constant pool for the class containing the method.
    private CLConstantPool cp;

    // Contains information about the method.
    private CLMethodInfo m;

    // Name of the method this cfg corresponds to.
    public String name;

    // Descriptor of the method this cfg corresponds to.
    public String descriptor;

    // List of basic blocks forming the cfg for the method.
    public ArrayList<NBasicBlock> basicBlocks;

    // Maps the pc of a JVM instruction to the block it is in.
    private HashMap<Integer, NBasicBlock> pcToBasicBlock;

    public int numLocals;

    // HIR instruction identifier.
    private static int hirId;

    // Maps HIR instruction ids in this cfg to HIR instructions.
    public HashMap<Integer, NHirInstruction> hirMap;

    // LIR instruction identifier.
    public static int lirId;

    /**
     * Virtual register identifier.
     */
    public static int regId;

    /**
     * Liveness intervals of this cfg.
     */
    public ArrayList<NInterval> intervals;

    /**
     * Virtual and physical registers used in this cfg.
     */
    public ArrayList<NRegister> registers;

    /**
     * Physical registers used in this cfg.
     */
    public ArrayList<NPhysicalRegister> pRegisters;

    /**
     * Constructs a ControlFlowGraph object for a method.
     *
     * @param cp constant pool of the class containing the method.
     * @param m  contains information about the method.
     */
    public NControlFlowGraph(CLConstantPool cp, CLMethodInfo m, String name, String descriptor) {
        this.cp = cp;
        this.m = m;
        this.name = name;
        this.descriptor = descriptor;

        hirId = 0;
        hirMap = new HashMap<Integer, NHirInstruction>();

        // Get the bytecode for the method.
        ArrayList<Integer> code = getByteCode();

        // Convert the bytecode to tuples.
        ArrayList<NTuple> tuples = bytecodeToTuples(code);

        if (tuples.isEmpty()) {
            return;
        }

        // Map bytecode instruction pc to the tuple representing the instruction.
        HashMap<Integer, NTuple> pcToTuple = new HashMap<>();
        for (NTuple tuple : tuples) {
            pcToTuple.put(tuple.pc, tuple);
        }

        // Identify the basic block leaders.
        findLeaders(tuples, pcToTuple);

        // Build the basic blocks.
        pcToBasicBlock = new HashMap<>();
        basicBlocks = buildBasicBlocks(tuples, pcToBasicBlock);

        // Connect up the basic blocks, ie, build the control flow graph.
        basicBlocks.get(0).successors.add(basicBlocks.get(1));
        basicBlocks.get(1).predecessors.add(basicBlocks.get(0));
        for (NBasicBlock block : basicBlocks) {
            if (block.tuples.isEmpty()) {
                continue;
            }
            pcToBasicBlock.put(block.tuples.get(0).pc, block);
        }
        for (int i = 0; i < basicBlocks.size(); i++) {
            NBasicBlock block = basicBlocks.get(i);
            if (block.tuples.isEmpty()) {
                continue;
            }
            NTuple tuple = block.tuples.get(block.tuples.size() - 1);
            if (tuple instanceof NBranchTuple) {
                NBranchTuple branchTuple = (NBranchTuple) tuple;
                NBasicBlock target = pcToBasicBlock.get((int) branchTuple.location);
                if (tuple.opcode != GOTO && i < basicBlocks.size() - 1) {
                    // Fall through block.
                    block.successors.add(basicBlocks.get(i + 1));
                    basicBlocks.get(i + 1).predecessors.add(block);
                }
                block.successors.add(target);
                target.predecessors.add(block);
            } else {
                if (i < basicBlocks.size() - 1) {
                    block.successors.add(basicBlocks.get(i + 1));
                    basicBlocks.get(i + 1).predecessors.add(block);
                }
            }
        }
    }

    /**
     * Recursively identifies loop header and loop tail blocks starting from the given block.
     *
     * @param block a block.
     * @param pred  block's predecessor or null.
     */
    public void detectLoops(NBasicBlock block, NBasicBlock pred) {
        if (!block.isVisited) {
            block.isVisited = true;
            block.isActive = true;
            for (NBasicBlock successor : block.successors) {
                detectLoops(successor, block);
            }
            block.isActive = false;
        } else if (block.isActive) {
            block.isLoopHead = true;
            pred.isLoopTail = true;
        }
    }

    /**
     * Removes basic blocks that cannot be reached from B0.
     */
    public void removeUnreachableBlocks() {
        // Create a list of blocks that cannot be reached.
        ArrayList<NBasicBlock> toRemove = new ArrayList<NBasicBlock>();
        for (NBasicBlock block : basicBlocks) {
            if (!block.isVisited) {
                toRemove.add(block);
            }
        }

        // From the predecessor list for each block, remove the ones that are in toRemove list.
        for (NBasicBlock block : basicBlocks) {
            for (NBasicBlock pred : toRemove) {
                block.predecessors.remove(pred);
            }
        }

        // From the list of all blocks, remove the ones that are in toRemove list.
        for (NBasicBlock block : toRemove) {
            basicBlocks.remove(block);
        }
    }

    /**
     * Converts tuples in each block to their high-level (HIR) representations.
     */
    public void tuplesToHir() {
        numLocals = numLocals();
        NHirInstruction[] locals = new NHirInstruction[numLocals];
        ArrayList<String> argTypes = argumentTypes(descriptor);

        // The source block B0 and its state vector.
        NBasicBlock beginBlock = basicBlocks.get(0);
        for (int i = 0; i < locals.length; i++) {
            NHirInstruction ins;
            if (i < argTypes.size()) {
                ins = new NHirLoadParam(beginBlock, hirId++, i);
                beginBlock.hir.add(ins);
                hirMap.put(ins.id, ins);
                locals[i] = ins;
            }
        }
        beginBlock.locals = locals;

        for (NBasicBlock block : basicBlocks) {
            block.isVisited = false;
        }

        // Do a BFS of this cfg starting at block B0, converting the tuples in each block to their corresponding
        // HIR instructions.
        Stack<Integer> operandStack = new Stack<>();
        Queue<NBasicBlock> q = new LinkedList<>();
        beginBlock.isVisited = true;
        q.add(beginBlock);
        while (!q.isEmpty()) {
            NBasicBlock block = q.remove();
            for (NBasicBlock succ : block.successors) {
                if (!succ.isVisited) {
                    succ.isVisited = true;
                    q.add(succ);
                }
            }

            if (block.predecessors.size() == 1) {
                // Block inherits its predecessor's state vector
                block.locals = block.predecessors.get(0).locals.clone();
            } else if (block.predecessors.size() > 1) {
                // The state vectors of the block's predecessors are merged to form the state vector of the block,
                // which is a list of phi functions.
                block.locals = mergeLocals(block);
            }

            int index = -1;
            int lhs = -1, rhs = -1;
            NBasicBlock onTrueBlock, onFalseBlock;
            NHirInstruction instruction = null;
            for (NTuple tuple : block.tuples) {
                switch (tuple.opcode) {
                    case ICONST_0:
                    case ICONST_1:
                    case LDC:
                        int value = tuple.opcode == ICONST_0 ? 0 : (tuple.opcode == ICONST_1 ? 1 :
                                ((NIConstTuple) tuple).N);
                        instruction = new NHirIConst(block, hirId++, value);
                        hirMap.put(instruction.id, instruction);
                        block.hir.add(instruction);
                        operandStack.push(instruction.id);
                        break;
                    case ILOAD:
                        index = ((NLoadStoreTuple) tuple).index;
                        operandStack.push(block.locals[index].id);
                        break;
                    case ISTORE:
                        index = ((NLoadStoreTuple) tuple).index;
                        block.locals[index] = hirMap.get(operandStack.pop());
                        break;
                    case DUP:
                        operandStack.push(operandStack.peek());
                        break;
                    case POP:
                        operandStack.pop();
                        break;
                    case INEG:
                        // We rewrite -x as -1 * x.
                        NHirIConst m1 = new NHirIConst(block, hirId++, -1);
                        lhs = m1.id;
                        rhs = operandStack.pop();
                        instruction = new NHirArithmetic(block, hirId++, IMUL, lhs, rhs);
                        hirMap.put(m1.id, m1);
                        block.hir.add(m1);
                        hirMap.put(instruction.id, instruction);
                        block.hir.add(instruction);
                        operandStack.push(instruction.id);
                        break;
                    case IADD:
                    case IDIV:
                    case IMUL:
                    case IREM:
                    case ISUB:
                        rhs = operandStack.pop();
                        lhs = operandStack.pop();
                        instruction = new NHirArithmetic(block, hirId++, tuple.opcode, lhs, rhs);
                        hirMap.put(instruction.id, instruction);
                        block.hir.add(instruction);
                        operandStack.push(instruction.id);
                        break;
                    case GOTO:
                        NBranchTuple branchTuple = ((NBranchTuple) tuple);
                        onTrueBlock = pcToBasicBlock.get((int) branchTuple.location);
                        instruction = new NHirJump(block, hirId++, onTrueBlock);
                        hirMap.put(instruction.id, instruction);
                        block.hir.add(instruction);
                        break;
                    case IFEQ:
                    case IFNE:
                        // We rewrite these using IF_ICMPEQ and IF_ICMPNE respectively.
                        NHirIConst zero = new NHirIConst(block, hirId++, 0);
                        lhs = operandStack.pop();
                        rhs = zero.id;
                        branchTuple = ((NBranchTuple) tuple);
                        onTrueBlock = pcToBasicBlock.get((int) branchTuple.location);
                        onFalseBlock = pcToBasicBlock.get((int) (branchTuple.pc + 3));
                        instruction = new NHirJump(block, hirId++, tuple.opcode == IFEQ ? IF_ICMPEQ : IF_ICMPNE,
                                lhs, rhs, onTrueBlock, onFalseBlock);
                        hirMap.put(zero.id, zero);
                        block.hir.add(zero);
                        hirMap.put(instruction.id, instruction);
                        block.hir.add(instruction);
                        break;
                    case IF_ICMPEQ:
                    case IF_ICMPGE:
                    case IF_ICMPGT:
                    case IF_ICMPLE:
                    case IF_ICMPLT:
                    case IF_ICMPNE:
                        branchTuple = ((NBranchTuple) tuple);
                        rhs = operandStack.pop();
                        lhs = operandStack.pop();
                        onTrueBlock = pcToBasicBlock.get((int) branchTuple.location);
                        onFalseBlock = pcToBasicBlock.get((int) (branchTuple.pc + 3));
                        instruction = new NHirJump(block, hirId++, tuple.opcode, lhs, rhs, onTrueBlock, onFalseBlock);
                        block.cfg.hirMap.put(instruction.id, instruction);
                        block.hir.add(instruction);
                        break;
                    case INVOKESTATIC:
                        NInvokestaticTuple methodCall = ((NInvokestaticTuple) tuple);
                        ArrayList<Integer> args = new ArrayList<>();
                        int numArgs = argumentCount(methodCall.descriptor);
                        for (int i = 0; i < numArgs; i++) {
                            int arg = operandStack.pop();
                            args.add(0, arg);
                        }
                        String returnType = returnType(methodCall.descriptor);
                        boolean isIOMethod = methodCall.name.equals("read") && methodCall.descriptor.equals("()I") ||
                                methodCall.name.equals("write") && methodCall.descriptor.equals("(I)V");
                        instruction = new NHirCall(block, hirId++, methodCall.name, args, returnType, isIOMethod);
                        if (!returnType.equals("V")) {
                            operandStack.push(instruction.id);
                        }
                        hirMap.put(instruction.id, instruction);
                        block.hir.add(instruction);
                        break;
                    case RETURN:
                        instruction = new NHirReturn(block, hirId++);
                        block.cfg.hirMap.put(instruction.id, instruction);
                        block.hir.add(instruction);
                        break;
                    case IRETURN:
                        instruction = new NHirReturn(block, hirId++, operandStack.pop());
                        block.cfg.hirMap.put(instruction.id, instruction);
                        block.hir.add(instruction);
                        break;
                }
            }
        }
    }

    /**
     * Converts the hir instructions in this cfg to lir instructions.
     */
    public void hirToLir() {
        lirId = 0;
        regId = 16;
        registers = new ArrayList<>();
        pRegisters = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            registers.add(null);
        }
        for (NBasicBlock block : basicBlocks) {
            for (NHirInstruction instruction : block.hir) {
                instruction.toLir();
            }
        }
    }

    /**
     *
     */
    public void cleanupPhiFunctions() {
        for (int ins : hirMap.keySet()) {
            NHirInstruction hir = hirMap.get(ins);
            if (hir instanceof NHirPhiFunction) {
                NHirPhiFunction phi = (NHirPhiFunction) hir;
                int index = phi.index;
                for (int i = 0; i < phi.args.size(); i++) {
                    NHirInstruction arg = phi.args.get(i);
                    NBasicBlock pred = phi.block.predecessors.get(i);
                    phi.args.set(i, pred.locals[index]);
                }

                boolean redundant = true;
                if (phi.block.isLoopHead) {
                    if (phi.id != phi.args.get(1).id) {
                        redundant = false;
                    }
                } else {
                    NHirInstruction firstArg = phi.args.get(0);
                    for (int i = 1; i < phi.args.size(); i++) {
                        if (phi.args.get(i).id != firstArg.id) {
                            redundant = false;
                        }
                    }
                }
                if (redundant) {
                    phi.block.hir.remove(phi);
                    hirMap.put(phi.id, phi.args.get(0));
                }
            }
        }
    }

    /**
     *
     */
    public void resolvePhiFunctions() {
        for (int ins1 : hirMap.keySet()) {
            NHirInstruction hir = hirMap.get(ins1);
            if (hir instanceof NHirPhiFunction) {
                NHirPhiFunction phi = (NHirPhiFunction) hir;
                NBasicBlock block = phi.block;
                for (int i = 0; i < phi.args.size(); i++) {
                    if (phi.args.get(i) == null) {
                        continue;
                    }
                    NHirInstruction arg = hirMap.get(phi.args.get(i).id);
                    NBasicBlock targetBlock = block.predecessors.get(i);
                    NLirCopy copy = new NLirCopy(arg.block, lirId++, phi.lir.write, arg.lir.write);
                    NHirInstruction targetIns = hirMap.get(targetBlock.hir.get(targetBlock.hir.size() - 1).id);
                    if (targetIns instanceof NHirJump) {
                        targetBlock.lir.add(targetBlock.lir.size() - 1, copy);
                    } else {
                        targetBlock.lir.add(copy);
                    }
                }
            }
        }
    }

    /**
     * Assigns new ids (0, 5, 10, and so on) to the LIR instructions in this cfg. The gaps allow us to insert
     * spill/restore instructions if needed during register allocation.
     */
    public void renumberLir() {
        int nextId = 0;
        for (NBasicBlock block : basicBlocks) {
            ArrayList<NLirInstruction> newLir = new ArrayList<>();
            for (NLirInstruction lir : block.lir) {
                lir.id = nextId;
                nextId += 5;
                newLir.add(lir);
            }
            block.lir = newLir;
        }
    }

    /**
     *
     */
    public void lirToMarvin() {
        for (NBasicBlock block : basicBlocks) {
            for (NLirInstruction ins : block.lir) {
                ins.toMarvin();
            }
        }
    }

    /**
     *
     */
    public void prepareMethodEntryAndExit() {
        NBasicBlock entry = basicBlocks.get(0);
        NMarvinInstruction instruction = new NMarvinStore("pushr", regInfo[RA], regInfo[SP]);
        int i = 0;
        entry.marvin.add(i++, instruction);
        instruction = new NMarvinStore("pushr", regInfo[FP], regInfo[SP]);
        entry.marvin.add(i++, instruction);
        instruction = new NMarvinCopy(regInfo[FP], regInfo[SP]);
        entry.marvin.add(i++, instruction);
        for (int j = 0; j < pRegisters.size(); j++) {
            NPhysicalRegister pRegister = pRegisters.get(j);
            instruction = new NMarvinStore("pushr", pRegister, regInfo[SP]);
            entry.marvin.add(i++, instruction);
        }

        NBasicBlock exit = new NBasicBlock(this, basicBlocks.size());
        for (int j = pRegisters.size() - 1; j >= 0; j--) {
            NPhysicalRegister pRegister = pRegisters.get(j);
            instruction = new NMarvinLoad("popr", pRegister, regInfo[SP]);
            exit.marvin.add(instruction);
        }
        instruction = new NMarvinLoad("popr", regInfo[FP], regInfo[SP]);
        exit.marvin.add(instruction);
        instruction = new NMarvinLoad("popr", regInfo[RA], regInfo[SP]);
        exit.marvin.add(instruction);
        instruction = new NMarvinJump("jumpr", regInfo[RA], null, 0, false);
        exit.marvin.add(instruction);
        basicBlocks.add(exit);
    }

    /**
     *
     */
    public void resolveJumps(HashMap<String, Integer> methodAddresses) {

    }

    /**
     * Computes the liveness intervals for the registers (virtual and physical) used by this cfg.
     */
    public void computeLivenessIntervals() {
        computeLocalLivenessSets();
        computeGlobalLivenessSets();
        intervals = new ArrayList<>();
        for (int i = 0; i < registers.size(); i++) {
            intervals.add(new NInterval(i));
        }
        for (int i = basicBlocks.size() - 1; i >= 0; i--) {
            NBasicBlock b = basicBlocks.get(i);
            if (b.lir.isEmpty()) {
                continue;
            }
            int bStart = b.lir.get(0).id;
            int bEnd = b.lir.get(b.lir.size() - 1).id;
            BitSet liveOut = b.liveOut;
            for (int j = liveOut.nextSetBit(0); j >= 0; j = liveOut.nextSetBit(j + 1)) {
                intervals.get(j).addRange(new NRange(bStart, bEnd));
            }
            for (int j = b.lir.size() - 1; j >= 0; j--) {
                int lirID = b.lir.get(j).id;
                NRegister output = b.lir.get(j).write;
                if (output != null) {
                    intervals.get(output.number).firstRangeFrom(lirID);
                    intervals.get(output.number).addUsePosition(lirID, UseType.WRITE);
                }
                ArrayList<NRegister> inputs = b.lir.get(j).reads;
                for (NRegister input : inputs) {
                    intervals.get(input.number).addRange(new NRange(bStart, lirID));
                    intervals.get(input.number).addUsePosition(lirID, UseType.READ);
                }
            }
        }
    }

    /**
     * Writes the tuples in this cfg to standard output.
     *
     * @param p for pretty printing with indentation.
     */
    public void writeTuplesToStdOut(PrettyPrinter p) {
        p.indentRight();
        p.printf("[[ TUPLES ]]\n\n");
        for (NBasicBlock block : basicBlocks) {
            block.writeTuplesToStdOut(p);
        }
        p.indentLeft();
    }

    /**
     * Writes the hir instructions in this cfg to standard output.
     *
     * @param p for pretty printing with indentation.
     */
    public void writeHirToStdOut(PrettyPrinter p) {
        p.indentRight();
        p.printf("[[ HIR ]]\n\n");
        for (NBasicBlock block : basicBlocks) {
            block.writeHirToStdOut(p);
        }
        p.indentLeft();
    }

    /**
     * Writes the lir instructions in this cfg to standard output.
     *
     * @param p for pretty printing with indentation.
     */
    public void writeLirToStdOut(PrettyPrinter p) {
        p.indentRight();
        p.printf("[[ LIR ]]\n\n");
        for (NBasicBlock block : basicBlocks) {
            block.writeLirToStdOut(p);
        }
        p.indentLeft();
    }

    /**
     * Writes the local and global liveness sets in this cfg to standard output.
     *
     * @param p for pretty printing with indentation.
     */
    public void writeLivenessSetsToStdOut(PrettyPrinter p) {
        p.indentRight();
        p.printf("[[ Liveness Sets ]]\n\n");
        for (NBasicBlock block : basicBlocks) {
            block.writeLivenessSetsToStdOut(p);
        }
        p.indentLeft();
    }

    /**
     * Writes the liveness intervals in this cfg to standard output.
     *
     * @param p for pretty printing with indentation.
     */
    public void writeLivenessIntervalsToStdOut(PrettyPrinter p) {
        p.indentRight();
        p.printf("[[ Liveness Intervals ]]\n\n");
        for (NInterval interval : intervals) {
            if (!interval.ranges.isEmpty()) {
                String id = "" + interval.registerId;
                if (registers.get(interval.registerId) instanceof NVirtualRegister) {
                    NVirtualRegister reg = (NVirtualRegister) registers.get(interval.registerId);
                    if (reg.spill) {
                        p.printf("v%s: %s -> %s:%s \n", id, interval, reg.pReg, reg.offset);
                    } else {
                        p.printf("v%s: %s -> %s\n", id, interval, reg.pReg);
                    }
                } else {
                    p.printf("r%s: %s\n", id, interval);
                }
            }
        }
        p.indentLeft();
    }

    /**
     * @param out
     */
    public void write(PrintWriter out) {
        out.printf("# %s%s\n", name, descriptor);
        for (NBasicBlock block : basicBlocks) {
            out.println();
            for (NMarvinInstruction ins : block.marvin) {
                ins.write(out);
            }
        }
        out.println();
    }

    // Extracts and returns the JVM bytecode for the method denoted by this cfg.
    private ArrayList<Integer> getByteCode() {
        for (CLAttributeInfo info : m.attributes) {
            if (info instanceof CLCodeAttribute) {
                return ((CLCodeAttribute) info).code;
            }
        }
        return null;
    }

    // Converts the given list of bytecode into a list of tuples and returns that list.
    private ArrayList<NTuple> bytecodeToTuples(ArrayList<Integer> code) {
        ArrayList<NTuple> tuples = new ArrayList<NTuple>();
        for (int i = 0; i < code.size(); i++) {
            int pc = i;
            int opcode = code.get(i);
            byte operand1, operand2;
            switch (opcode) {
                case DUP:
                case IADD:
                case ICONST_0:
                case ICONST_1:
                case IDIV:
                case IMUL:
                case INEG:
                case IREM:
                case IRETURN:
                case ISUB:
                case POP:
                case RETURN:
                    tuples.add(new NNoArgTuple(pc, opcode));
                    break;
                case LDC:
                    short operand = code.get(++i).shortValue();
                    int constant = ((CLConstantIntegerInfo) cp.cpItem(operand)).i;
                    tuples.add(new NIConstTuple(pc, constant));
                    break;
                case ILOAD:
                case ISTORE:
                    byte offset = code.get(++i).byteValue();
                    tuples.add(new NLoadStoreTuple(pc, opcode, offset));
                    break;
                case GOTO:
                case IFEQ:
                case IFNE:
                case IF_ICMPEQ:
                case IF_ICMPGE:
                case IF_ICMPGT:
                case IF_ICMPLE:
                case IF_ICMPLT:
                case IF_ICMPNE:
                    operand1 = code.get(++i).byteValue();
                    operand2 = code.get(++i).byteValue();
                    short location = (short) (pc + ((operand1 << 8) | operand2));
                    tuples.add(new NBranchTuple(pc, opcode, location));
                    break;
                case INVOKESTATIC:
                    operand1 = code.get(++i).byteValue();
                    operand2 = code.get(++i).byteValue();
                    short index = (short) (((operand1 << 8) | operand2));
                    CLConstantMemberRefInfo memberRefInfo = (CLConstantMethodRefInfo) cp.cpItem(index);
                    CLConstantNameAndTypeInfo nameAndTypeInfo =
                            (CLConstantNameAndTypeInfo) cp.cpItem(memberRefInfo.nameAndTypeIndex);
                    String name = new String(((CLConstantUtf8Info) cp.cpItem(nameAndTypeInfo.nameIndex)).b);
                    String descriptor = new String(((CLConstantUtf8Info) cp.cpItem(nameAndTypeInfo.descriptorIndex)).b);
                    tuples.add(new NInvokestaticTuple(pc, name, descriptor));
                    break;
            }
        }
        return tuples;
    }

    // Identifies the leaders (ie, first tuple and tuples that are branch targets) for this cfg.
    private void findLeaders(ArrayList<NTuple> tuples, HashMap<Integer, NTuple> pcToTuple) {
        for (int i = 0; i < tuples.size(); i++) {
            NTuple tuple = tuples.get(i);
            if (i == 0) {
                tuple.isLeader = true;
            }
            if (tuple instanceof NBranchTuple) {
                NBranchTuple branchTuple = (NBranchTuple) tuple;
                int location = branchTuple.location;
                pcToTuple.get(location).isLeader = true;
                if (i < tuples.size() - 1) {
                    tuples.get(i + 1).isLeader = true;
                }
            }
        }
    }

    // Builds a list of basic blocks for this cfg from the given list of tuples and returns that list. In the process,
    // populates pcToBasicBlock, which maps leader tuple's pc to the basic block the tuple belongs to.
    private ArrayList<NBasicBlock> buildBasicBlocks(ArrayList<NTuple> tuples, HashMap<Integer, NBasicBlock> pcToBasicBlock) {
        ArrayList<NBasicBlock> basicBlocks = new ArrayList<>();
        int blockId = 0;
        NBasicBlock block = new NBasicBlock(this, blockId++);
        for (NTuple tuple : tuples) {
            if (tuple.isLeader) {
                basicBlocks.add(block);
                block = new NBasicBlock(this, blockId++);
                if (!pcToBasicBlock.containsKey(tuple.pc)) {
                    pcToBasicBlock.put(tuple.pc, block);
                }
            }
            block.tuples.add(tuple);
        }
        basicBlocks.add(block);
        return basicBlocks;
    }

    // Returns the number of local variables in the method denoted by this cfg.
    private int numLocals() {
        for (CLAttributeInfo info : m.attributes) {
            if (info instanceof CLCodeAttribute) {
                return ((CLCodeAttribute) info).maxLocals;
            }
        }
        return 0;
    }

    // Merges the locals from each of the predecessors of the specified block into a list of phi functions and
    // returns that list.
    private NHirInstruction[] mergeLocals(NBasicBlock block) {
        NHirInstruction[] locals = new NHirInstruction[numLocals];
        for (int i = 0; i < numLocals; i++) {
            ArrayList<NHirInstruction> args = new ArrayList<>();
            for (NBasicBlock pred : block.predecessors) {
                args.add(pred.locals == null ? null : pred.locals[i]);
            }
            NHirInstruction ins = new NHirPhiFunction(block, hirId++, args, i);
            block.hir.add(ins);
            hirMap.put(ins.id, ins);
            locals[i] = ins;
        }
        return locals;
    }

    // Returns the number of formal parameters for a method with the given descriptor.
    private int argumentCount(String descriptor) {
        return descriptor.substring(1, descriptor.lastIndexOf(")")).length();
    }

    // Returns a list containing the argument types of a method with the given descriptor.
    private ArrayList<String> argumentTypes(String descriptor) {
        ArrayList<String> args = new ArrayList<String>();
        String arguments = descriptor.substring(1, descriptor.length() - 2);
        for (int i = 0; i < arguments.length(); i++) {
            args.add(arguments.charAt(0) + "");
        }
        return args;
    }

    // Computes the local liveness sets (ie, liveUse and liveDef) for this cfg.
    private void computeLocalLivenessSets() {
        for (NBasicBlock b : basicBlocks) {
            b.liveUse = new BitSet(registers.size());
            b.liveDef = new BitSet(registers.size());
            for (NLirInstruction lir : b.lir) {
                for (NRegister reg : lir.reads) {
                    if (!b.liveDef.get(reg.number)) {
                        b.liveUse.set(reg.number);
                    }
                }
                if (lir.write != null) {
                    b.liveDef.set(lir.write.number);
                }
            }
        }
    }

    // Computes the global liveness sets (ie, liveIn and liveOut) for this cfg.
    private void computeGlobalLivenessSets() {
        for (NBasicBlock b : basicBlocks) {
            b.liveIn = new BitSet(registers.size());
            b.liveOut = new BitSet(registers.size());
        }
        boolean changed;
        do {
            changed = false;
            for (int i = basicBlocks.size() - 1; i >= 0; i--) {
                NBasicBlock b = basicBlocks.get(i);
                BitSet newLiveOut = new BitSet(registers.size());
                for (NBasicBlock s : b.successors) {
                    newLiveOut.or(s.liveIn);
                }
                if (!b.liveOut.equals(newLiveOut)) {
                    b.liveOut = newLiveOut;
                    changed = true;
                }
                b.liveIn = (BitSet) b.liveOut.clone();
                b.liveIn.andNot(b.liveDef);
                b.liveIn.or(b.liveUse);
            }
        } while (changed);
    }

    // Returns the return type of a method with the given descriptor, or "V" (for void).
    private String returnType(String descriptor) {
        return descriptor.substring(descriptor.lastIndexOf(")") + 1);
    }
}
