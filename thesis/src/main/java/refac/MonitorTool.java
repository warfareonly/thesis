/**
 * 
 */
package refac;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.words.Word;

/**
 * @author Bharat Garhewal
 *
 */
public class MonitorTool {

    // public static Set<Integer> getState(FastNFA<String> nfa,
    // Word<String> input) {
    // return getSuccessors(nfa, nfa.getInitialStates(), input);
    // }
    //
    // public static Set<Integer> getSuccessors(FastNFA<String> nfa,
    // Collection<FastNFAState> initialStates, Word<String> input) {
    // Set<Integer> ret = new HashSet<Integer>();
    // assert null != initialStates;
    // FastNFAState state = initialStates
    // .toArray(new FastNFAState[nfa.getInitialStates().size()])[0];
    // FastNFAState nextState = state;
    // for (String inputAction : input) {
    // if (0 != nfa.getSuccessors(state, inputAction).toArray().length) {
    // nextState = nfa.getSuccessors(state, inputAction)
    // .toArray(new FastNFAState[nfa
    // .getSuccessors(state, inputAction).size()])[0];
    // state = nextState;
    // }
    // }
    //
    // nfa.removeState(state, state);
    // ret.add(nextState.getId());
    // return ret;
    // }

    /**
     * Compute the end states of the (possibly NFA!) monitor: This is a
     * recursive implementation, exploring each path as it occurs until the end
     * of the input word is reached and then combines all the results and
     * returns them as a single set of integers.
     * 
     * @param nfa
     *            the monitor automaton
     * @param state
     *            the initial state
     * @param inputs
     *            the input word
     * @return the set of destination states
     */
    public static Set<Integer> getSuccessors(FastNFA<String> nfa, Integer state,
            Word<String> inputs) {
        Set<Integer> ret = new HashSet<>();
        if (inputs.isEmpty()) {
            ret.add(state);
            return ret;
        }
        Set<FastNFAState> nextStates = nfa.getSuccessors(nfa.getState(state),
                inputs.firstSymbol());
        if (null == nextStates || nextStates.isEmpty()) {
            ret.addAll(getSuccessors(nfa, state, inputs.subWord(1)));
            return ret;
        } else {
            for (FastNFAState s : nextStates) {
                ret.addAll(getSuccessors(nfa, s.getId(), inputs.subWord(1)));
            }
            return ret;
        }
    }
}
