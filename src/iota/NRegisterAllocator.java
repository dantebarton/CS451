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
                // Store a spilled write operand in memory.
                if (lir.write != null && lir.write instanceof NVirtualRegister) {
                    NVirtualRegister write = (NVirtualRegister) lir.write;
                    if (write.spill) {
                        NLirStore store = new NLirStore(block, lir.id + 1, "store", write.pReg, regInfo[SP],
                                write.offset);
                        newLir.add(newLir.indexOf(lir) + 1, store);
                    }
                }

                // Load a spilled read operand from memory.
                for (int i = 0; i < lir.reads.size(); i++) {
                    NRegister reg = lir.reads.get(i);
                    if (reg instanceof NVirtualRegister) {
                        NVirtualRegister read = (NVirtualRegister) reg;
                        if (read.spill) {
                            NLirLoad load = new NLirLoad(block, lir.id - (lir.reads.size() - i), "load",
                                    read.pReg, regInfo[SP], read.offset);
                            newLir.add(newLir.indexOf(lir), load);
                        }
                    }
                }
            }
            block.lir = newLir;
        }
    }
}
