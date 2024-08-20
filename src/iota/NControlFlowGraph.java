// Copyright 2024- Swami Iyer

package iota;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import static iota.CLConstants.DUP;
import static iota.CLConstants.GOTO;
import static iota.CLConstants.IADD;
import static iota.CLConstants.ICONST_0;
import static iota.CLConstants.ICONST_1;
import static iota.CLConstants.IDIV;
import static iota.CLConstants.IFEQ;
import static iota.CLConstants.IFNE;
import static iota.CLConstants.IF_ICMPEQ;
import static iota.CLConstants.IF_ICMPGE;
import static iota.CLConstants.IF_ICMPGT;
import static iota.CLConstants.IF_ICMPLE;
import static iota.CLConstants.IF_ICMPLT;
import static iota.CLConstants.IF_ICMPNE;
import static iota.CLConstants.ILOAD;
import static iota.CLConstants.IMUL;
import static iota.CLConstants.INEG;
import static iota.CLConstants.INVOKESTATIC;
import static iota.CLConstants.IREM;
import static iota.CLConstants.IRETURN;
import static iota.CLConstants.ISTORE;
import static iota.CLConstants.ISUB;
import static iota.CLConstants.LDC;
import static iota.CLConstants.POP;
import static iota.CLConstants.RETURN;

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
     * List of predecessors of this block.
     */
    public ArrayList<NBasicBlock> predecessors;

    /**
     * List of successors of this block.
     */
    public ArrayList<NBasicBlock> successors;

    /**
     * Is this block is a loop head?
     */
    public boolean isLoopHead;

    /**
     * Is this block a loop tail?
     */
    public boolean isLoopTail;

    /**
     * Has this block been visited?
     */
    public boolean isVisited;

    /**
     * Is this block active?
     */
    public boolean isActive;

    /**
     * State vector of this block.
     */
    public NHIRInstruction[] locals;

    /**
     * List of high-level (HIR) instructions in this block.
     */
    public ArrayList<NHIRInstruction> hir;

    /**
     * List of low-level (LIR) instructions in this block.
     */
    public ArrayList<NLIRInstruction> lir;

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
     * Constructs a basic block.
     *
     * @param cfg the cfg containing the block.
     * @param id  id of the block.
     */
    public NBasicBlock(NControlFlowGraph cfg, int id) {
        this.cfg = cfg;
        this.id = id;
        tuples = new ArrayList<>();
        predecessors = new ArrayList<>();
        successors = new ArrayList<>();
        isLoopHead = false;
        isLoopTail = false;
        isVisited = false;
        isActive = false;
        hir = new ArrayList<>();
        lir = new ArrayList<>();
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
            NHIRInstruction ins = locals[i];
            localsStr += ins != null ? cfg.hirMap.get(ins.id).id() + ", " : "?, ";
        }
        localsStr = localsStr.isEmpty() ? "[]" : "[" + localsStr.substring(0, localsStr.length() - 2) + "]";
        p.printf("%s (pred: %s, succ: %s%s%s, locals: %s):\n", id(), predecessors.toString(), successors.toString(),
                lh, lt, localsStr);
        for (NHIRInstruction instruction : hir) {
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
        for (NLIRInstruction instruction : lir) {
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

    public void writeLivenessIntervalsToStdOut(PrettyPrinter p) {

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
    public HashMap<Integer, NHIRInstruction> hirMap;

    // LIR instruction identifier.
    public static int lirId;

    /**
     * Virtual register identifier.
     */
    public static int regId;

    /**
     * Registers allocated for this cfg by the HIR to LIR conversion algorithm.
     */
    public ArrayList<NRegister> registers;

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
        basicBlocks = new ArrayList<>();
        pcToBasicBlock = new HashMap<>();

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

        // Form blocks.
        buildBasicBlocks(tuples);

        // Connect up the basic blocks, ie, build its control flow graph.
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
                    // Fall through block
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
     * Implements loop detection algorithm to figure out if the specified block is a loop head or a loop tail.
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
     * Removes blocks that cannot be reached from the begin block (B0). Also removes these blocks from the
     * predecessor lists.
     */
    public void removeUnreachableBlocks() {
        // Create a list of blocks that cannot be reached.
        ArrayList<NBasicBlock> toRemove = new ArrayList<NBasicBlock>();
        for (NBasicBlock block : basicBlocks) {
            if (!block.isVisited) {
                toRemove.add(block);
            }
        }

        // From the predecessor list for each blocks, remove the ones that are in toRemove list.
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
        hirId = 0;
        hirMap = new HashMap<Integer, NHIRInstruction>();
        numLocals = numLocals();
        NHIRInstruction[] locals = new NHIRInstruction[numLocals];
        ArrayList<String> argTypes = argumentTypes(descriptor);
        NBasicBlock beginBlock = basicBlocks.get(0);
        for (int i = 0; i < locals.length; i++) {
            NHIRInstruction ins = null;
            if (i < argTypes.size()) {
                ins = new NHIRLoadParam(beginBlock, hirId++, i);
                beginBlock.hir.add(ins);
                hirMap.put(ins.id, ins);
                locals[i] = ins;
            }
        }
        beginBlock.locals = locals;

        for (NBasicBlock block : basicBlocks) {
            block.isVisited = false;
        }
        Stack<Integer> operandStack = new Stack<Integer>();
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

            // Convert tuples in block to HIR instructions.
            if (block.predecessors.size() == 1) {
                block.locals = block.predecessors.get(0).locals.clone();
            } else if (block.predecessors.size() > 1) {
                block.locals = mergeLocals(block);
            }
            NHIRInstruction ins = null;
            int index = -1;
            int lhs = -1, rhs = -1;
            for (NTuple tuple : block.tuples) {
                switch (tuple.opcode) {
                    case GOTO:
                        NBranchTuple branchTuple = ((NBranchTuple) tuple);
                        NBasicBlock trueDestination = pcToBasicBlock.get((int) branchTuple.location);
                        NBasicBlock falseDestination = null;
                        ins = new NHIRJump(block, hirId++, tuple.opcode, -1, -1, trueDestination, falseDestination);
                        hirMap.put(ins.id, ins);
                        block.hir.add(ins);
                        operandStack.push(ins.id);
                        break;
                    case IFEQ:
                    case IFNE:
                        NHIRIntConstant zero = new NHIRIntConstant(block, hirId++, 0);
                        lhs = operandStack.pop();
                        rhs = zero.id;
                        branchTuple = ((NBranchTuple) tuple);
                        trueDestination = pcToBasicBlock.get((int) branchTuple.location);
                        falseDestination = pcToBasicBlock.get((int) (branchTuple.pc + 1));
                        ins = new NHIRJump(block, hirId++, tuple.opcode, lhs, rhs, trueDestination,
                                falseDestination);
                        hirMap.put(zero.id, zero);
                        block.hir.add(zero);
                        hirMap.put(ins.id, ins);
                        block.hir.add(ins);
                        operandStack.push(ins.id);
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
                        trueDestination = pcToBasicBlock.get((int) branchTuple.location);
                        falseDestination = pcToBasicBlock.get((int) (branchTuple.pc + 3));
                        ins = new NHIRJump(block, hirId++, tuple.opcode, lhs, rhs, trueDestination, falseDestination);
                        block.cfg.hirMap.put(ins.id, ins);
                        block.hir.add(ins);
                        break;
                    case DUP:
                        operandStack.push(operandStack.peek());
                        break;
                    case POP:
                        operandStack.pop();
                        break;
                    case ICONST_0:
                        ins = new NHIRIntConstant(block, hirId++, 0);
                        hirMap.put(ins.id, ins);
                        block.hir.add(ins);
                        operandStack.push(ins.id);
                        break;
                    case ICONST_1:
                        ins = new NHIRIntConstant(block, hirId++, 1);
                        hirMap.put(ins.id, ins);
                        block.hir.add(ins);
                        operandStack.push(ins.id);
                        break;
                    case LDC:
                        ins = new NHIRIntConstant(block, hirId++, ((NLDCTuple) tuple).value);
                        hirMap.put(ins.id, ins);
                        block.hir.add(ins);
                        operandStack.push(ins.id);
                        break;
                    case ILOAD:
                        index = ((NLoadStoreTuple) tuple).offset;
                        operandStack.push(block.locals[index].id);
                        break;
                    case ISTORE:
                        index = ((NLoadStoreTuple) tuple).offset;
                        block.locals[index] = hirMap.get(operandStack.pop());
                        break;
                    case INVOKESTATIC:
                        NMethodCallTuple methodCall = ((NMethodCallTuple) tuple);
                        String name = methodCall.name;
                        String desc = methodCall.descriptor;
                        ArrayList<Integer> args = new ArrayList<Integer>();
                        int numArgs = argumentCount(desc);
                        for (int i = 0; i < numArgs; i++) {
                            int arg = operandStack.pop();
                            args.add(0, arg);
                        }
                        String returnType = returnType(desc);
                        ins = new NHIRMethodCall(block, hirId++, methodCall.name, args, returnType);
                        if (!returnType.equals("V")) {
                            operandStack.push(ins.id);
                        }
                        hirMap.put(ins.id, ins);
                        block.hir.add(ins);
                        break;
                    case INEG:
                        NHIRIntConstant m1 = new NHIRIntConstant(block, hirId++, -1);
                        lhs = m1.id;
                        rhs = operandStack.pop();
                        ins = new NHIRArithmetic(block, hirId++, IMUL, lhs, rhs);
                        hirMap.put(m1.id, m1);
                        block.hir.add(m1);
                        hirMap.put(ins.id, ins);
                        block.hir.add(ins);
                        operandStack.push(ins.id);
                        break;
                    case IADD:
                    case IDIV:
                    case IMUL:
                    case IREM:
                    case ISUB:
                        rhs = operandStack.pop();
                        lhs = operandStack.pop();
                        ins = new NHIRArithmetic(block, hirId++, tuple.opcode, lhs, rhs);
                        hirMap.put(ins.id, ins);
                        block.hir.add(ins);
                        operandStack.push(ins.id);
                        break;
                    case IRETURN:
                        ins = new NHIRReturn(block, hirId++, tuple.opcode, operandStack.pop());
                        block.cfg.hirMap.put(ins.id, ins);
                        block.hir.add(ins);
                        break;
                    case RETURN:
                        ins = new NHIRReturn(block, hirId++, tuple.opcode, -1);
                        block.cfg.hirMap.put(ins.id, ins);
                        block.hir.add(ins);
                        break;
                }
            }
        }
    }

    public void cleanupPhiFunctions() {
        for (int ins : hirMap.keySet()) {
            NHIRInstruction hir = hirMap.get(ins);
            if (hir instanceof NHIRPhiFunction) {
                NHIRPhiFunction phi = (NHIRPhiFunction) hir;
                int index = phi.index;
                for (int i = 0; i < phi.args.size(); i++) {
                    NHIRInstruction arg = phi.args.get(i);
                    NBasicBlock pred = phi.block.predecessors.get(i);
                    phi.args.set(i, pred.locals[index]);
                }

                boolean redundant = true;
                if (phi.block.isLoopHead) {
                    if (phi.id != phi.args.get(1).id) {
                        redundant = false;
                    }
                } else {
                    NHIRInstruction firstArg = phi.args.get(0);
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
     * Converts the hir instructions in this cfg to lir instructions.
     */
    public void hirToLir() {
        lirId = 0;
        regId = 16;
        registers = new ArrayList<NRegister>();
        for (int i = 0; i < 16; i++) {
            registers.add(null);
        }
        for (int ins : hirMap.keySet()) {
            hirMap.get(ins).toLir();
        }
    }

    public void resolvePhiFunctions() {
        for (int ins1 : hirMap.keySet()) {
            NHIRInstruction hir = hirMap.get(ins1);
            if (hir instanceof NHIRPhiFunction) {
                NHIRPhiFunction phi = (NHIRPhiFunction) hir;
                NBasicBlock block = phi.block;
                for (int i = 0; i < phi.args.size(); i++) {
                    if (phi.args.get(i) == null) {
                        continue;
                    }
                    NHIRInstruction arg = hirMap.get(phi.args.get(i).id);
                    NBasicBlock targetBlock = block.predecessors.get(i);
                    NLIRCopy copy = new NLIRCopy(arg.block, lirId++, phi.lir, arg.lir);
                    int len = targetBlock.hir.size();
                    NHIRInstruction targetIns = hirMap.get(targetBlock.hir.get(len - 1).id);
                    if (targetIns instanceof NHIRJump) {
                        targetBlock.lir.add(len - 1, copy);
                    } else {
                        targetBlock.lir.add(copy);
                    }
                }
            }
        }
    }

    /**
     * Assigns new ids to the LIR instructions in this cfg.
     */
    public void renumberLirInstructions() {
        int nextId = 0;
        for (NBasicBlock block : basicBlocks) {
            ArrayList<NLIRInstruction> newLir = new ArrayList<>();
            for (NLIRInstruction lir : block.lir) {
                lir.id = nextId;
                nextId += 5; // Increment by 5 to accommodate spill instructions
                newLir.add(lir);
            }
            block.lir = newLir;
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

    public void writeLivenessSetsToStdOut(PrettyPrinter p) {
        p.indentRight();
        p.printf("[[ Liveness Sets ]]\n\n");
        for (NBasicBlock block : basicBlocks) {
            block.writeLivenessSetsToStdOut(p);
        }
        p.indentLeft();
    }

    public void writeLivenessIntervalsToStdOut(PrettyPrinter p) {
        p.indentRight();
        p.printf("[[ Liveness Intervals ]]\n\n");
        for (NBasicBlock block : basicBlocks) {
            block.writeLivenessIntervalsToStdOut(p);
        }
        p.indentLeft();
    }

    // Builds the basic blocks for this control flow graph.
    private void buildBasicBlocks(ArrayList<NTuple> tuples) {
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
    }

    // Finds the leaders for this control flow graph.
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

    // Converts the given list of bytecode into a list of tuples and returns that list.
    private ArrayList<NTuple> bytecodeToTuples(ArrayList<Integer> code) {
        ArrayList<NTuple> tuples = new ArrayList<NTuple>();
        for (int i = 0; i < code.size(); i++) {
            int pc = i;
            int opcode = code.get(i);
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
                    tuples.add(new NLDCTuple(pc, opcode, constant));
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
                    byte operand1 = code.get(++i).byteValue();
                    byte operand2 = code.get(++i).byteValue();
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
                    tuples.add(new NMethodCallTuple(pc, opcode, name, descriptor));
                    break;
            }
        }
        return tuples;
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

    // Returns the number of local variables in the method denoted by this cfg.
    private int numLocals() {
        int numLocals = 0;
        for (CLAttributeInfo info : m.attributes) {
            if (info instanceof CLCodeAttribute) {
                numLocals = ((CLCodeAttribute) info).maxLocals;
                break;
            }
        }
        return numLocals;
    }

    private ArrayList<String> argumentTypes(String descriptor) {
        ArrayList<String> args = new ArrayList<String>();
        String arguments = descriptor.substring(1, descriptor.length() - 2);
        for (int i = 0; i < arguments.length(); i++) {
            args.add(arguments.charAt(0) + "");
        }
        return args;
    }

    // Merges the locals from each of the predecessors of the specified block.
    private NHIRInstruction[] mergeLocals(NBasicBlock block) {
        NHIRInstruction[] locals = new NHIRInstruction[numLocals];
        for (int i = 0; i < numLocals; i++) {
            ArrayList<NHIRInstruction> args = new ArrayList<>();
            for (NBasicBlock pred : block.predecessors) {
                args.add(pred.locals == null ? null : pred.locals[i]);
            }
            NHIRInstruction ins = new NHIRPhiFunction(block, hirId++, args, i);
            block.hir.add(ins);
            hirMap.put(ins.id, ins);
            locals[i] = ins;
        }
        return locals;
    }

    private class BasicBlockComparator implements Comparator<NBasicBlock> {
        public int compare(NBasicBlock a, NBasicBlock b) {
            return Integer.compare(a.id, b.id);
        }
    }

    // Returns the return type of a method with the given descriptor, or "V" (for void).
    private String returnType(String descriptor) {
        return descriptor.substring(descriptor.lastIndexOf(")") + 1);
    }

    // Returns the argument count (number of formal parameters) for the method with the given descriptor.
    private int argumentCount(String descriptor) {
        int i = 0;
        String argTypes = descriptor.substring(1, descriptor.lastIndexOf(")"));
        return argTypes.length();
    }

    public void computeLocalLivenessSets() {
        for (NBasicBlock b : basicBlocks) {
            b.liveUse = new BitSet(registers.size());
            b.liveDef = new BitSet(registers.size());
            for (NLIRInstruction lir : b.lir) {
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

    public void computeGlobalLivenessSets() {
        for (NBasicBlock b : basicBlocks) {
            b.liveIn = new BitSet(registers.size());
            b.liveOut = new BitSet(registers.size());
        }
        boolean changed = false;
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

    public void computeLivenessIntervals() {

    }
}
