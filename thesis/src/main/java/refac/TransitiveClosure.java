/**
 * 
 */
package refac;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.words.Word;

/**
 * This class deals with transitive closures. Given an FastDFA and a set of
 * "confused states", it is aimed at generating the transitive closure of each
 * state which end up reaching the set of confused states in the FastDFA. This
 * is used to "emulate" the Sigma* set needed to define equivalent states, which
 * is patently impossible to finish in finite time.
 * 
 * @author Bharat Garhewal
 *
 */
public class TransitiveClosure {

    public static Map<FastDFAState, Map<FastDFAState, Set<Word<String>>>> generateEnhancedTransitiveClosure(
            FastDFA<String> dfa, Set<FastDFAState> confusedStates) {
        // Return type state -> state2 : set of words needed to reach state2
        Map<FastDFAState, Map<FastDFAState, Set<Word<String>>>> ret = new HashMap<>(
                confusedStates.size());

        // Initialize the map
        ret = initializeConfusedTransitionClosureMap(ret, confusedStates);

        FastDFAState zoriginalInitialState = dfa.getInitialState();
        FastDFAState originalInitialState = zoriginalInitialState;
        for (FastDFAState initialState : confusedStates) {

            // Change the initial state of the automaton
            dfa.setInitial(originalInitialState, false);
            dfa.setInitialState(initialState);

            // Compute the transition cover iterator of the automaton
            Iterator<Word<String>> transitionCoverIterator = Covers
                    .transitionCoverIterator(dfa, dfa.getInputAlphabet());

            while (transitionCoverIterator.hasNext()) {
                Word<String> input = transitionCoverIterator.next();
                FastDFAState destinationState = dfa.getState(input);

                if (null != destinationState) {
                    if (confusedStates.contains(destinationState)) {
                        ret.get(initialState).get(destinationState).add(input);
                    }
                }
            }

            originalInitialState = initialState;
        }

        // Restore the original initial state of the dfa (since we have a
        // reference to it, and not a copy of it!
        
//        dfa.
//        for (FastDFAState x : dfa.getStates()) {
//            dfa.setInitial(x, false);
//        }
        dfa.setInitialState(zoriginalInitialState);
        return ret;
    }

    private static Map<FastDFAState, Map<FastDFAState, Set<Word<String>>>> initializeConfusedTransitionClosureMap(
            Map<FastDFAState, Map<FastDFAState, Set<Word<String>>>> map,
            Set<FastDFAState> confusedStates) {
        for (FastDFAState state : confusedStates) {
            Map<FastDFAState, Set<Word<String>>> xyzMap = new HashMap<>();
            for (FastDFAState s : confusedStates) {
                Set<Word<String>> ws = new HashSet<>();
                xyzMap.put(s, ws);
            }
            map.put(state, xyzMap);
        }
        return map;
    }

}
