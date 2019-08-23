package refac;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.words.Word;

/**
 * Sanity checker class to check if we have made correct modifications to a
 * monitor or not
 * 
 * @author Bharat Garhewal
 *
 */
public class SanityChecker {

    private final Set<Set<Set<Word<String>>>> nonInjectiveWordsSet;

    public SanityChecker(FastDFA<String> specification,
            List<String> subSpecificationsList) throws Exception {
        this.nonInjectiveWordsSet = Injection.nonInjectiveAccessSequences(
                specification, subSpecificationsList);
        assert this.nonInjectiveWordsSet != null;
        System.err.println(this.nonInjectiveWordsSet);
    }

    /**
     * Checks if the final states of the confused words are indeed different.
     * 
     * @param nfa
     *            the monitor
     * @return true if disjoint, otherwise false
     * @throws IOException
     */
    public boolean checkMonitor(FastNFA<String> nfa) throws IOException {
        Set<FastNFAState> setInitialStates = nfa.getInitialStates();
        assert setInitialStates.size() == 1;
        Integer initialState = setInitialStates.stream()
                .collect(Collectors.toList()).get(0).getId();
        // Loop through all sets of sets of confusedWords sets
        // Basically, we now have a 3-level deep set
        // Level 1 : Set of all confused states (L1)
        // Level 2: Sets of Words identifying each state (L2)
        // Level 3: Sets of words for one particular state (L3)

        // The requirement now is that all words in the L3 set must end at the
        // same state

        // From the L3 level, we will "obtain" a set of states. The L2
        // requirement is that every state at the L2 level must be distinct.

        // There is no requirement at the L1 level: this is, thankfully, just a
        // normal container! (Naturally, "normal" in our context: it contains
        // all the sets of states which are confused and as such, has no
        // requirements amongst its own elements).

        for (Set<Set<Word<String>>> confusedWordsSet : this.nonInjectiveWordsSet) {
            Set<FastNFAState> confusedStateSetList = new HashSet<>();
            Integer confusedStateSetListSize = 0;
            for (Set<Word<String>> confusedWords : confusedWordsSet) {
                confusedStateSetListSize++;
                Set<FastNFAState> confusedWordsEndStates = confusedWords
                        .stream()
                        .map(word -> MonitorTool.getSuccessors(nfa, null, word))
                        .flatMap(c -> c.stream()).map(x -> nfa.getState(x))
                        .collect(Collectors.toSet());
                if (!(confusedWordsEndStates.size() == 1)) {
                    for (FastNFAState x : nfa.getStates()) {
                        for (String i : nfa.getInputAlphabet()) {
                            if (null != nfa.getSuccessors(x, i)
                                    && !nfa.getSuccessors(x, i).isEmpty()) {
                                System.err.println(x + " + " + i + " = "
                                        + nfa.getSuccessors(x, i));
                            }
                        }
                    }
//                    System.err.println(confusedWords);
//                    System.err.println(confusedWordsEndStates);
//                    System.in.read();
                    return false;
                } else {
                    confusedStateSetList.add(confusedWordsEndStates.stream()
                            .collect(Collectors.toList()).get(0));
                }
            }
            if (confusedStateSetListSize != confusedStateSetList.size()) {
//                System.err.println(confusedStateSetListSize);
//                System.err.println(confusedStateSetList);
//                System.in.read();
                return false;
            } else {
                confusedStateSetList.clear();
                confusedStateSetListSize = 0;
            }
            // System.out.println(confusedWordsSet);
            // System.exit(0);
            // System.out.println(endStates);
            // for (int i = 0; i < endStates.size(); i++) {
            // for (int j = i + 1; j < endStates.size(); j++) {
            // if (!Collections.disjoint(endStates.get(i),
            // endStates.get(j))) {
            // return false;
            // }
            // }
            // }
        }
        return true;
    }
}
