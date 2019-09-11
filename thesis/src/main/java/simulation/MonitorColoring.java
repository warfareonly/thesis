package simulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;

/**
 * Class to color the states of P || M depending on the states of the monitor M.
 * 
 * @author bgarhewa
 *
 */
public class MonitorColoring {

    private FastNFA<String> productMonitorComposed;
    private FastNFA<String> monitor;
    private Map<FastNFAState, Set<FastNFAState>> mapColor;

    public MonitorColoring(FastNFA<String> productMonitorComposed,
            FastNFA<String> monitor) {
        this.productMonitorComposed = productMonitorComposed;
        this.monitor = monitor;

        // Every state of P || M is 'colored' with a (possibly set of) state in
        // the monitor M
        computeColoring();
    }

    private void computeColoring() {
        Map<FastNFAState, Set<FastNFAState>> ret = new HashMap<>();

        // Put in the initial states to have the same color.
        for (FastNFAState initState : this.productMonitorComposed
                .getInitialStates()) {
            for (FastNFAState initMonitorState : this.monitor
                    .getInitialStates()) {
                if (ret.containsKey(initState)) {
                    ret.get(initState).add(initMonitorState);
                } else {
                    ret.put(initState, new HashSet<>());
                    ret.get(initState).add(initMonitorState);
                }
            }
        }

        // Now, explore the state-space of P||M and then add in the colors.
        // First, we need to write the steps to explore the state-space of P||M.

        Set<FastNFAState> exploredStates = new HashSet<>();
        Queue<FastNFAState> statesToExplore = new LinkedList<>();

        // Add the initial states to the set of explored states.
        exploredStates.addAll(this.productMonitorComposed.getInitialStates());

        while (!exploredStates
                .equals(this.productMonitorComposed.getStates())) {

        }
        this.mapColor = ret;
        return;
    }

    void exploreStateSpace(Set<FastNFAState> currentStatesSet) {

        Map<FastNFAState, Set<FastNFAState>> ret = new HashMap<>();

        // Put in the initial states to have the same color.
        for (FastNFAState initState : this.productMonitorComposed
                .getInitialStates()) {
            for (FastNFAState initMonitorState : this.monitor
                    .getInitialStates()) {
                if (ret.containsKey(initState)) {
                    ret.get(initState).add(initMonitorState);
                } else {
                    ret.put(initState, new HashSet<>());
                    ret.get(initState).add(initMonitorState);
                }
            }
        }

        Set<FastNFAState> visited = new HashSet<>();
        Queue<FastNFAState> queue = new LinkedList<>();

        for (FastNFAState s : currentStatesSet) {
            visited.add(s);
            queue.add(s);
        }

        while (queue.size() != 0) {
            FastNFAState stateToExplore = queue.poll();

            Map<String, Set<FastNFAState>> successors = Neighbors
                    .getSuccessors(productMonitorComposed, stateToExplore);

            for (String input : successors.keySet()) {
                Set<FastNFAState> succSet = successors.get(input);
                for (FastNFAState x : succSet) {
                    for (FastNFAState monState : ret.get(x)) {
                        Set<FastNFAState> monSuccSet = this.monitor
                                .getSuccessors(monState, input);
                        for (FastNFAState moniState : monSuccSet) {
                            
                        }
                    }
                    if (!visited.contains(x)) {
                        queue.add(x);
                    }
                }
            }
        }
        return;
    }

    public Map<FastNFAState, Set<FastNFAState>> getColoring() {
        return this.mapColor;
    }
}
