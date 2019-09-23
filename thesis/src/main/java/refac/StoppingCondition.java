/**
 * 
 */
package refac;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.util.automata.fsa.NFAs;

/**
 * Provides a stopping criteria for the monitor state-merging procedure and also
 * the next pair of states to be merged.
 * 
 * @author Bharat Garhewal
 *
 */
public class StoppingCondition {

    private List<Pair<FastNFAState, FastNFAState>> listMergePairs = new LinkedList<>();

    public StoppingCondition(FastNFA<String> monitor) {
        List<FastNFAState> listOfStates = monitor.getStates().stream()
                .collect(Collectors.toList());
        for (int i = 0; i < listOfStates.size(); i++) {
            for (int j = i + 1; j < listOfStates.size(); j++) {
                // listMergePairs.add(
                // new Pair<>(listOfStates.get(i), listOfStates.get(j)));
                // if
                // (!monitor.getInitialStates().contains(monitor.getState(i))) {
                listMergePairs.add(
                        new Pair<>(listOfStates.get(i), listOfStates.get(j)));
                // } else {
                // listMergePairs.add(new Pair<>(listOfStates.get(i),
                // listOfStates.get(j)));
                // }
            }
        }
        
        // Random seed 12 was the reason for the failing case. 
        Collections.shuffle(listMergePairs, (new Random())); // 33 gives a 3-state monitor

        // System.out.println("New stopping conditions created!");
        // System.out.println("Combinations are: " + listMergePairs);
    }

    public Pair<FastNFAState, FastNFAState> getNextPairOfStates() {
        // return listMergePairs.isEmpty() ? null : listMergePairs.get(0);
        if (listMergePairs.isEmpty()) {
            return null;
        } else {
            Pair<FastNFAState, FastNFAState> ret = listMergePairs.remove(0);
            return ret;
        }
    }

    /**
     * Get all the state-merge pairs in one go!
     * 
     * @return
     */
    public List<Pair<FastNFAState, FastNFAState>> getAllPairs() {
        return this.listMergePairs;
    }
}
