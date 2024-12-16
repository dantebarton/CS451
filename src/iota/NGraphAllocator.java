package iota;

import java.util.*;

/**
 * A register allocator that uses the graph coloring algorithm to allocate
 * physical registers to virtual registers.
 */
class NGraphRegisterAllocator extends NRegisterAllocator {
   private Map<Integer, Set<Integer>> interferenceGraph ; // Create an interference graph 
   // Queues for managing degrees of nodes during allocation process
   private PriorityQueue<Integer> lowDegreeQueue;
   private PriorityQueue<Integer> highDegreeQueue;
   // cache to store the degree of each register for easier lookup
   private Map<Integer, Integer> degreeCache;


    /**
     * Constructs an NGraphRegisterAllocator object.
     *
     * @param cfg control flow graph for the method.
     */
    public NGraphRegisterAllocator(NControlFlowGraph cfg) {
        super(cfg);
    }

    /*
     * Builds the interference graph by analyzing the liveness intervals of the registers
     */
    private void buildInterferenceGraph() {
        cfg.computeLivenessIntervals();
        interferenceGraph = new HashMap<>();

        cfg.intervals.stream()
            .filter(interval -> interval != null && cfg.registers.get(interval.regId) instanceof NVirtualRegister)
            .forEach(interval -> interferenceGraph.put(interval.regId, new HashSet<>()));

        // The code below builds the graph by checking for intersecting register(if they do intersect add an edge between the two registers in the graph)    
        for (int i = 0; i < cfg.intervals.size(); i++) {
            NInterval interval1 = cfg.intervals.get(i);
            if (interval1 == null || !(cfg.registers.get(i) instanceof NVirtualRegister))
                continue;
            for (int j = i + 1; j < cfg.intervals.size(); j++) {
                NInterval interval2 = cfg.intervals.get(j);
                if (interval2 == null || !(cfg.registers.get(j) instanceof NVirtualRegister))
                    continue;
                
                    if (interval1.intersects(interval2)) {
                        interferenceGraph.get(interval1.regId).add(interval2.regId);
                        interferenceGraph.get(interval2.regId).add(interval1.regId);
                    }
            }
        }
        System.out.println("Interference Graph: " + interferenceGraph);
    }

    /*
     * Validates that all nodes in the graph has a corresponding neighbor to mitigate earlier null register issues I encountered
     */
    private void validateInterferenceGraph() {
        for (Map.Entry<Integer, Set<Integer>> entry : interferenceGraph.entrySet()) {
            for (int neighbor : entry.getValue()) {
                if (!interferenceGraph.containsKey(neighbor)) {
                    throw new IllegalStateException("Invalid interference graph: Node " + neighbor + " missing.");
                }
            }
        }
    }

    /**
     * Determines which register to spill by selecting the one with the highest degree and lowest usage.
     */
    private int determineSpill(Set<Integer> current) {
        int maxDegree = -1;
        int candidate = -1;
        int minWeight = Integer.MAX_VALUE;

        for (int regID : current) {
            if (!(cfg.registers.get(regID) instanceof NVirtualRegister))
                continue;
            
            NInterval interval = cfg.intervals.get(regID);
            int degree = getDegree(regID);
            int weight = interval.usePositions.size();

            if ((degree > maxDegree) || degree == maxDegree && weight < minWeight) {
                maxDegree = degree;
                candidate = regID;
                minWeight = weight;
            }
        }
        return candidate;
    }

    /*
     * Gets the degree of a given register
     */
    private int getDegree(int regId) {
         // If degreeCache is already initialized, fetch the degree directly.
        if (degreeCache.containsKey(regId)) {
            return degreeCache.get(regId);
        }

        // Fallback to compute degree if not present in the cache.
        int degree = interferenceGraph.getOrDefault(regId, Collections.emptySet()).size();
        degreeCache.put(regId, degree); // Cache it for future use.
        return degree;
    }

    /*
     * Helper function used to initialize the degree cache to help make the lookup process more efficient
     */
    private void initializeDegreeCache() {
        degreeCache = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : interferenceGraph.entrySet()) {
            degreeCache.put(entry.getKey(), entry.getValue().size());
        }
        for (Set<Integer> neighbors : interferenceGraph.values()) {
            for (int neighbor : neighbors) {
                degreeCache.putIfAbsent(neighbor, 0); // Note: Default to 0 if not already present
            }
        }
        System.out.println("Degree Cache: " + degreeCache);
    }

    /**
     * Updates the degree of neighboring nodes when a node is removed or processed.
     * To-do: This function works in hand with simplify but there is a bug that causes null registers to appear
     * This bug needs to be resolved!!!
     */
    private void updateDegree(int node, boolean decrement) {
        for (int neighbor : interferenceGraph.get(node)) {
            if (degreeCache.containsKey(neighbor)) {
                degreeCache.put(neighbor, degreeCache.get(neighbor) + (decrement ? -1 : 1));
            }
        }
        degreeCache.remove(node);
    }

    /**
     * Initializes the low and high degree priority queues for the register allocation process.
     */
    private void initializeQueues(Set<Integer> remaining) {
        lowDegreeQueue = new PriorityQueue<>(Comparator.comparingInt(degreeCache::get));
        highDegreeQueue = new PriorityQueue<>((a, b) -> degreeCache.get(b) - degreeCache.get(a));
        lowDegreeQueue.addAll(remaining);
        highDegreeQueue.addAll(remaining);

        System.out.println("Low Degree Queue: " + lowDegreeQueue);
        System.out.println("High Degree Queue: " + highDegreeQueue);
    }

    /**
     * Finds the register with the lowest degree (fewest neighbors) from the remaining set.
     */
    private int lowDegreeNode(Set<Integer> remaining) {
        while (!lowDegreeQueue.isEmpty() && !remaining.contains(lowDegreeQueue.peek())) {
            lowDegreeQueue.poll();
        }
        return lowDegreeQueue.isEmpty() ? -1 : lowDegreeQueue.peek();
    }

    /**
     * Allocates a color (physical register) for a given virtual register.
     * Note: If there is no color availabe then it should proceed to spill
     */
    private int colorGraph (int regId, Map<Integer, Integer> coloredMap) {
        Set<Integer> usedColorSet = new HashSet<>();
        NInterval interval = cfg.intervals.get(regId);

        for (int nextNode : interferenceGraph.get(regId)) {
            NInterval neighborInterval = cfg.intervals.get(nextNode);
            if (neighborInterval != null && interval.intersects(neighborInterval) && coloredMap.containsKey(nextNode)) {
                usedColorSet.add(coloredMap.get(nextNode));
            }
        }

        for (int i = 1; i < NPhysicalRegister.MAX_COUNT; i++) {
            if (!usedColorSet.contains(i)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Simplifies the graph by removing nodes with low degree and updating the degree cache.
     * To-do: This function works in hand with updateDegree but there is a bug that causes null registers to appear
     * This bug needs to be resolved!!!
     */
    private void simplify(Set<Integer> remaining) {
        while (!remaining.isEmpty()) {
            int node = lowDegreeNode(remaining);
    
            if (node == -1) break; // Handle spills or other logic
            
            // Remove the node and update degree cache
            remaining.remove(node);
            updateDegree(node, true); // Decrement degrees for neighbors
        }
    }

    /**
     * {@inheritDoc}
     */
     public void run() {
        buildInterferenceGraph();
        validateInterferenceGraph();
        initializeDegreeCache();

    Stack<Integer> stack = new Stack<>();
    Set<Integer> remaining = new HashSet<>(interferenceGraph.keySet());
    initializeQueues(remaining);
    //simplify(remaining); ///TODO
    Set<Integer> spills = new HashSet<>();

    while (!remaining.isEmpty()) {
        int node = lowDegreeNode(remaining);

        if (node == -1) {
            node = determineSpill(remaining);
            if (node == -1) break;
            spills.add(node);
        }

        remaining.remove(node);
        stack.push(node);
    }

    Map<Integer, Integer> registerMapping = new HashMap<>();
    int spillOffset = 4;

    while (!stack.isEmpty()) {
        int node = stack.pop();
        NVirtualRegister vReg = (NVirtualRegister) cfg.registers.get(node);

        if (spills.contains(node)) {
            vReg.spill = true;
            vReg.offset = spillOffset;
            vReg.pReg = NPhysicalRegister.regInfo[0];
            spillOffset += 4;
            System.out.println("Spilled node: " + node + " to offset: " + vReg.offset);
        } else {
            int color = colorGraph(node, registerMapping);
            if (color != -1) {
                registerMapping.put(node, color);
                vReg.pReg = NPhysicalRegister.regInfo[color];
                System.out.println("Assigned color " + color + " to node: " + node);
            } else {
                vReg.spill = true;
                vReg.offset = spillOffset;
                vReg.pReg = NPhysicalRegister.regInfo[0];
                spillOffset += 4;
                System.out.println("Spilled node: " + node + " due to lack of colors");
            }
        }
    }
    System.out.println("Final Register Allocations: " + registerMapping);

   
    handleSpills();
    initializeQueues(remaining);
     }
}