package iota;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Used to mark how an LIR instruction affects the register bound to a liveness interval.
 */
enum UseType {
    /**
     * This marker is used if the instruction reads from the register.
     */
    READ,

    /**
     * This marker is used if the instruction writes to the register.
     */
    WRITE
}

/**
 * Representation of a liveness interval.
 */
class NInterval {
    /**
     * ID of the register (virtual or physical) that is attached to this interval.
     */
    public int registerId;

    /**
     * The sequence of ranges for this interval.
     */
    public ArrayList<NRange> ranges;

    /**
     * Maps LIR instruction ID to use type (READ or WRITE).
     */
    public HashMap<Integer, UseType> usePositions;

    /**
     * Constructs an NInterval object.
     *
     * @param registerId ID of the register (virtual or physical) that is attached to this interval.
     */
    public NInterval(int registerId) {
        this.registerId = registerId;
        ranges = new ArrayList<>();
        usePositions = new HashMap<>();
    }

    /**
     * Changes the start value of the first range in this interval (if it is not empty) to the given value.
     *
     * @param newStart new start value for the first range.
     */
    public void firstRangeFrom(int newStart) {
        if (!ranges.isEmpty()) {
            ranges.get(0).start = newStart;
        }
    }

    /**
     * Adds the given range to this interval. If this interval is empty, the given range is simply added to it.
     * Otherwise, if range ends right where the first range begins or if the range intersects with the first range,
     * then the first range's start value is set to that of the given range. Otherwise, the range is added as the new
     * first range of this interval.
     *
     * @param range the range.
     */
    public void addRange(NRange range) {
        if (!ranges.isEmpty()) {
            if (range.stop + 5 == ranges.get(0).start || range.intersects(ranges.get(0))) {
                ranges.get(0).start = range.start;
            } else {
                ranges.add(0, range);
            }
        } else {
            ranges.add(range);
        }
    }

    /**
     * Records whether the LIR instruction with the given ID reads or writes to the register bound to this interval.
     *
     * @param lirID   LIR instruction ID.
     * @param useType READ or WRITE.
     */
    public void addUsePosition(int lirID, UseType useType) {
        usePositions.put(lirID, useType);
    }

    /**
     * Returns true if this interval intersects the other interval, and false otherwise.
     *
     * @param other the other interval.
     * @return true if this interval intersects the other interval, and false otherwise.
     */
    public boolean intersects(NInterval other) {
        for (NRange r : this.ranges) {
            for (NRange s : other.ranges) {
                if (r.intersects(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a string representation of this interval.
     *
     * @return a string representation of this interval.
     */
    public String toString() {
        String s = "";
        for (NRange range : ranges) {
            String t = "[";
            if (usePositions.containsKey(range.start)) {
                t += usePositions.get(range.start) == UseType.READ ? "R " : "W ";
            } else {
                t += "- ";
            }
            t += range;
            if (usePositions.containsKey(range.stop)) {
                t += usePositions.get(range.stop) == UseType.READ ? " R" : " W";
            } else {
                t += " -";
            }
            t += "]";
            s += t + ", ";
        }
        return s.isEmpty() ? "" : s.substring(0, s.length() - 2);
    }
}

/**
 * Representation of a range within an interval.
 */
class NRange {
    /**
     * Start position of the range.
     */
    public int start;

    /**
     * End position of the range.
     */
    public int stop;

    /**
     * Constructs an NRange given its start and stop positions.
     *
     * @param start start position.
     * @param stop  stop position.
     */
    public NRange(int start, int stop) {
        this.start = start;
        this.stop = stop;
    }

    /**
     * Returns true if this range intersects the other range, and false otherwise.
     *
     * @param other the other range.
     * @return true if this range intersects the other range, and false otherwise.
     */
    public boolean intersects(NRange other) {
        return !(this.stop < other.start || other.stop < this.start);
    }

    /**
     * Returns a string representation of this range.
     *
     * @return a string representation of this range.
     */
    public String toString() {
        return start + ", " + stop;
    }
}
