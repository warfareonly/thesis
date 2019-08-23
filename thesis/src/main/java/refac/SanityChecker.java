package refac;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    private final Set<Set<Word<String>>> nonInjectiveWordsSet;

    public SanityChecker(FastDFA<String> specification,
            List<String> subSpecificationsList) throws Exception {
        this.nonInjectiveWordsSet = Injection.nonInjectiveAccessSequences(
                specification, subSpecificationsList);
        assert this.nonInjectiveWordsSet != null;
    }

    /**
     * Checks if the final states of the confused words are indeed different.
     * 
     * @param nfa
     *            the monitor
     * @return true if disjoint, otherwise false
     */
    public boolean checkMonitor(FastNFA<String> nfa) {
        Set<FastNFAState> setInitialStates = nfa.getInitialStates();
        assert setInitialStates.size() == 1;
        Integer initialState = setInitialStates.stream()
                .collect(Collectors.toList()).get(0).getId();
        // Loop through all sets of confusedWords sets
        for (Set<Word<String>> confusedWords : this.nonInjectiveWordsSet) {
            System.out.println(confusedWords);
            List<Set<Integer>> endStates = confusedWords.stream()
                    .map(x -> MonitorTool.getSuccessors(nfa, initialState, x))
                    .collect(Collectors.toList());
            System.out.println(endStates);
            for (int i = 0; i < endStates.size(); i++) {
                for (int j = i + 1; j < endStates.size(); j++) {
                    if (!Collections.disjoint(endStates.get(i),
                            endStates.get(j))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
