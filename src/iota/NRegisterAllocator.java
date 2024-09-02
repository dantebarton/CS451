package iota;

import java.util.ArrayList;

import static iota.NPhysicalRegister.*;

/**
 * An abstract representation of a register allocator.
 */
abstract class NRegisterAllocator {
    /**
     * The control flow graph for a method.
     */
    protected NControlFlowGraph cfg;

    /**
     * Constructs an NRegisterAllocator object.
     *
     * @param cfg control flow graph for the method.
     */
    protected NRegisterAllocator(NControlFlowGraph cfg) {
        this.cfg = cfg;
        cfg.computeLivenessIntervals();
    }

    /**
     * Allocates physical registers to virtual registers.
     */
    public abstract void run();

    /**
     * Handles spills by inserting (into LIR code) load/store instructions for registers that are marked for spill.
     * <p>
     * If an instruction i writes to a spilled virtual register v, a store instruction is inserted right after i to
     * store v in memory at the address SP + v.offset.
     * <p>
     * If an instruction i reads from a spilled register v, a load instruction is inserted right before i to load
     * into v the value in memory at the address SP + v.offset.
     */
    public void handleSpills() {
        for (NBasicBlock block : cfg.basicBlocks) {
            ArrayList<NLirInstruction> newLir = new ArrayList<>();
            for (NLirInstruction lir : block.lir) {
                newLir.add(lir);
            }
            for (NLirInstruction lir : block.lir) {
                //
                if (lir.write != null && lir.write instanceof NVirtualRegister) {
                    NVirtualRegister write = (NVirtualRegister) lir.write;
                    if (write.spill) {
                        NLirCopy sp = new NLirCopy(block, lir.id + 1, regInfo[R11], regInfo[SP]);
                        NLirInc address = new NLirInc(block, lir.id + 2, sp.write, write.offset);
                        NLirStore store = new NLirStore(block, lir.id + 3, write.pReg, address.write);
                        newLir.add(newLir.indexOf(lir) + 1, sp);
                        newLir.add(newLir.indexOf(lir) + 2, address);
                        newLir.add(newLir.indexOf(lir) + 3, store);
                    }
                }

                // Ignore method calls.
                if (lir instanceof NLirCall) {
                    continue;
                }

                //
                if (lir.reads.size() == 1) {
                    NRegister reg = lir.reads.get(0);
                    if (reg instanceof NVirtualRegister) {
                        NVirtualRegister read = (NVirtualRegister) reg;
                        if (read.spill) {
                            NLirCopy sp = new NLirCopy(block, lir.id - 3, regInfo[R11], regInfo[SP]);
                            NLirInc address = new NLirInc(block, lir.id - 2, sp.write, read.offset);
                            NLirLoad load = new NLirLoad(block, lir.id - 1, "load", read.pReg, address.write);
                            newLir.add(newLir.indexOf(lir) - 3, sp);
                            newLir.add(newLir.indexOf(lir) - 3, address);
                            newLir.add(newLir.indexOf(lir) - 3, load);
                        }
                    }
                } else if (lir.reads.size() == 2) {

                }
            }
            block.lir = newLir;
        }
    }
}
