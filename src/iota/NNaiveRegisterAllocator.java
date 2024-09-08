package iota;

import java.util.LinkedList;

/**
 * A register allocator that uses a naive round-robin approach to allocate physical registers to virtual registers.
 */
class NNaiveRegisterAllocator extends NRegisterAllocator {
    /**
     * Constructs an NNaiveRegisterAllocator object.
     *
     * @param cfg control flow graph for the method.
     */
    public NNaiveRegisterAllocator(NControlFlowGraph cfg) {
        super(cfg);
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        int pRegId = 0;
        int offset = 0;

        // Store the already allocated virtual registers into a queue.
        LinkedList<NVirtualRegister> allocated = new LinkedList<>();

        for (NBasicBlock block : cfg.basicBlocks) {
            for (NLirInstruction ins : block.lir) {
                if (ins.write instanceof NVirtualRegister) {
                    NVirtualRegister write = (NVirtualRegister) ins.write;
                    if (write.pReg == null) {
                        if (pRegId < NPhysicalRegister.MAX_COUNT) {
                            // Free register available, so allocate one to write.
                            write.pReg = NPhysicalRegister.regInfo[pRegId++];
                        } else {
                            // Free register not available, so pull one out from allocated, spill it if it's not
                            // spilled already, allocate it to write, and spill write.
                            NVirtualRegister reg = allocated.removeFirst();
                            if (!reg.spill) {
                                reg.spill = true;
                                reg.offset = offset++;
                            }
                            write.pReg = reg.pReg;
                            write.spill = true;
                            write.offset = offset++;
                        }
                        if (!cfg.pRegisters.contains(write.pReg)) {
                            cfg.pRegisters.add(write.pReg);
                        }
                        allocated.addLast(write);
                    }
                }

                // If an instruction has two read operands, both, if different from one another, must not be allocated
                // the same register.
                if (ins.reads.size() == 2 && ins.reads.get(0) instanceof NVirtualRegister
                        && ins.reads.get(1) instanceof NVirtualRegister) {
                    NVirtualRegister read1 = (NVirtualRegister) ins.reads.get(0);
                    NVirtualRegister read2 = (NVirtualRegister) ins.reads.get(1);
                    if (!read1.equals(read2) && read1.pReg.equals(read2.pReg)) {
                        read2.pReg = NPhysicalRegister.regInfo[(read2.pReg.number + 1) % NPhysicalRegister.MAX_COUNT];
                        if (!cfg.pRegisters.contains(read2.pReg)) {
                            cfg.pRegisters.add(read2.pReg);
                        }
                    }
                }
            }
        }
    }
}
