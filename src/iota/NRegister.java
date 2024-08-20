// Copyright 2024- Swami Iyer

package iota;

/**
 * Abstract representation of a register.
 */
abstract class NRegister {
    /**
     * Register number.
     */
    public int number;

    /**
     * Register name.
     */
    public String name;

    /**
     * Constructs an NRegister object.
     *
     * @param number register number.
     * @param name   register name.
     */
    protected NRegister(int number, String name) {
        this.number = number;
        this.name = name;
    }

    /**
     * Returns a string representation of this register.
     *
     * @return a string representation of this register.
     */
    public String toString() {
        return name;
    }
}

/**
 * Representation of a virtual register.
 */
class NVirtualRegister extends NRegister {
    /**
     * Constructs an NVirtualRegister object.
     *
     * @param number register number.
     */
    public NVirtualRegister(int number) {
        super(number, "v" + number);
    }
}

/**
 * Representation of a physical register in Marvin.
 */
class NPhysicalRegister extends NRegister {
    /**
     * Constant 0 register, zero.
     */
    public static final int ZERO = 0;

    /**
     * Temporary register, R1.
     */
    public static final int R1 = 1;

    /**
     * Temporary register, R2.
     */
    public static final int R2 = 2;

    /**
     * Temporary register, R3.
     */
    public static final int R3 = 3;

    /**
     * Temporary register, R4.
     */
    public static final int R4 = 4;

    /**
     * Temporary register, R5.
     */
    public static final int R5 = 5;

    /**
     * Temporary register, R6.
     */
    public static final int R6 = 6;

    /**
     * Temporary register, R7.
     */
    public static final int R7 = 7;

    /**
     * Temporary register, R8.
     */
    public static final int R8 = 8;

    /**
     * Temporary register, R9.
     */
    public static final int R9 = 9;

    /**
     * Temporary register, R10.
     */
    public static final int R10 = 10;

    /**
     * Temporary register, R11.
     */
    public static final int R11 = 11;

    /**
     * Temporary register, R12.
     */
    public static final int R12 = 12;

    /**
     * Return value register, rv.
     */
    public static final int RV = 13;

    /**
     * Return address register, ra.
     */
    public static final int RA = 14;

    /**
     * Stack pointer register, sp.
     */
    public static final int SP = 15;

    /**
     * Maps register number to the register's representation.
     */
    public static final NPhysicalRegister[] regInfo = {new NPhysicalRegister(0, "r0"), new NPhysicalRegister(1, "r1"),
            new NPhysicalRegister(2, "r2"), new NPhysicalRegister(3, "r3"), new NPhysicalRegister(4, "r4"),
            new NPhysicalRegister(5, "r5"), new NPhysicalRegister(6, "r6"), new NPhysicalRegister(7, "r7"),
            new NPhysicalRegister(8, "r8"), new NPhysicalRegister(9, "r9"), new NPhysicalRegister(10, "r10"),
            new NPhysicalRegister(11, "r11"), new NPhysicalRegister(12, "r12"), new NPhysicalRegister(13, "r13"),
            new NPhysicalRegister(14, "r14"), new NPhysicalRegister(15, "r15")};

    /**
     * Constructs an NPhysicalRegister object.
     *
     * @param number register number.
     * @param name   register name.
     */
    public NPhysicalRegister(int number, String name) {
        super(number, name);
    }
}
