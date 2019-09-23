/**
 * 
 */
package simulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;

/**
 * Helper class to generate predecessors and successors
 * 
 * @author Bharat Garhewal
 * @param <S>
 * @param <TP>
 * @param <SP>
 * @param <T>
 *
 */
public class Neighbors {

    public static Map<String, FastDFAState> getPredecessors(FastDFA<String> dfa,
            FastDFAState currState) {
        Map<String, FastDFAState> ret = new HashMap<>();
        for (FastDFAState state : dfa.getStates()) {
            for (String input : dfa.getInputAlphabet()) {
                if (null != dfa.getSuccessor(state, input)) {
                    if (currState.equals(dfa.getSuccessor(state, input))) {
                        ret.put(input, state);
                    }
                }
            }
        }
        return ret;
    }

    public static Map<String, Set<FastDFAState>> getSuccessors(
            FastDFA<String> dfa, FastDFAState currState) {
        Map<String, Set<FastDFAState>> ret = new HashMap<>();
        for (String input : dfa.getInputAlphabet()) {
            if (null != dfa.getSuccessor(currState, input)) {
                Set<FastDFAState> x = new HashSet<>();
                x.add(dfa.getSuccessor(currState, input));
                ret.put(input, x);
            }
        }
        return ret;
    }

    public static Map<String, Set<FastNFAState>> getPredecessors(
            FastNFA<String> nfa, FastNFAState currState) {
        Map<String, Set<FastNFAState>> ret = new HashMap<>();
        for (FastNFAState state : nfa.getStates()) {

            for (String input : nfa.getInputAlphabet()) {

                Set<Integer> successors = nfa.getSuccessors(state, input)
                        .stream().map(x -> x.getId())
                        .collect(Collectors.toSet());

                if (null != successors && !successors.isEmpty()
                        && successors.size() != 0) {
                    if (successors.contains(currState.getId())) {
                        if (!ret.containsKey(input)) {
                            Set<FastNFAState> x = new HashSet<FastNFAState>();
                            x.add(state);
                            ret.put(input, x);
                        }
                        ret.get(input).add(state);
                    }
                }
            }
        }

        Map<String, Set<FastNFAState>> realRet = new HashMap<>();
        for (String k : ret.keySet()) {
            if (!ret.get(k).isEmpty()) {
                realRet.put(k, ret.get(k));
            }
        }

        return realRet;
    }

    public static Map<String, Set<FastNFAState>> getSuccessors(
            FastNFA<String> nfa, FastNFAState currState) {
        Map<String, Set<FastNFAState>> ret = new HashMap<>();
        for (String input : nfa.getInputAlphabet()) {
            Set<Integer> successors = nfa.getSuccessors(currState, input)
                    .stream().map(x -> x.getId()).collect(Collectors.toSet());
            if (null != successors && !successors.isEmpty()
                    && successors.size() != 0) {
                ret.put(input, successors.stream().map(x -> nfa.getState(x))
                        .collect(Collectors.toSet()));
            }
        }
        return ret;
    }
}
