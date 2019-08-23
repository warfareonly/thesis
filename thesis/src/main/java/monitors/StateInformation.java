/**
 * 
 */
package monitors;

import java.util.HashSet;
import java.util.Set;

import org.javatuples.Pair;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;

/**
 * A simple, inefficient, helper class to obtain the successors and predecessors
 * of a state in an NFA
 * 
 * @author Bharat Garhewal
 *
 */
public class StateInformation {

    /**
     * Obtain the set of successors of the particular state
     * 
     * @param nfa
     * @param state
     * @return the set of successors
     */
    public static Set<Pair<Integer, String>> getSuccessors(
            CompactNFA<String> nfa, Integer state) {
        Set<Pair<Integer, String>> ret = new HashSet<>();
        for (String input : nfa.getInputAlphabet()) {
            if (0 != nfa.getSuccessors(state, input).toArray().length) {
                ret.add(new Pair<Integer, String>(
                        (Integer) nfa.getSuccessors(state, input).toArray()[0],
                        input));
            }
        }
        return ret;
    }

    public static Set<Pair<Integer, String>> getSuccessors(FastNFA<String> nfa,
            Integer state) {
        Set<Pair<Integer, String>> ret = new HashSet<>();
        for (String input : nfa.getInputAlphabet()) {
            if (0 != nfa.getSuccessors(nfa.getState(state), input)
                    .toArray().length) {
                for (FastNFAState s : nfa
                        .getSuccessors(nfa.getState(state), input)
                        .toArray(new FastNFAState[nfa
                                .getSuccessors(nfa.getState(state), input)
                                .size()])) {
                    ret.add(new Pair<Integer, String>(s.getId(), input));
                }
            }
        }
        return ret;
    }

    /**
     * Obtain the set of predecessors of the particular state
     * 
     * @param nfa
     * @param state
     * @return the set of predecessors
     */
    public static Set<Pair<Integer, String>> getPredecessors(
            CompactNFA<String> nfa, Integer state) {
        Set<Pair<Integer, String>> ret = new HashSet<>();
        for (Integer s : nfa.getStates()) {
            for (String input : nfa.getInputAlphabet()) {
                if (0 != nfa.getSuccessors(s, input).toArray().length) {
                    if (state.equals((Integer) nfa.getSuccessors(s, input)
                            .toArray()[0])) {
                        ret.add(new Pair<Integer, String>(s, input));
                    }
                }
            }
        }
        return ret;
    }

    public static Set<Pair<Integer, String>> getPredecessors(
            FastNFA<String> nfa, Integer state) {
        Set<Pair<Integer, String>> ret = new HashSet<>();
        for (FastNFAState s : nfa.getStates()) {
            for (String input : nfa.getInputAlphabet()) {
                if (0 != nfa.getSuccessors(s, input).toArray().length) {
                    for (FastNFAState d : nfa.getSuccessors(s, input).toArray(
                            new FastNFAState[nfa.getSuccessors(s, input)
                                    .toArray().length])) {
                        ret.add(new Pair<Integer, String>(d.getId(), input));
                    }
                }
            }
        }
        return ret;
    }
}
