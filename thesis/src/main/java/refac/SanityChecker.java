package refac;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.words.Word;
import utils.Misc;

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
        // System.err.println(this.nonInjectiveWordsSet);
    }

    /*
     * /** Checks if the final states of the confused words are indeed
     * different.
     *
     * @param nfa the monitor
     * 
     * @return true if disjoint, otherwise false
     * 
     * @throws IOException
     */
    public boolean checkMonitor(FastNFA<String> nfa) throws IOException {
        assert 1 == nfa.getInitialStates().size();

        this.nonInjectiveWordsSet.forEach(x -> System.out.println(x));
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
            List<Set<Integer>> confusedStateSetList = new LinkedList<>();
            for (Set<Word<String>> confusedWords : confusedWordsSet) {
                Set<Integer> confusedWordsEndStates = confusedWords.stream()
                        .map(word -> MonitorTool.getSuccessors(nfa, null, word))
                        .flatMap(x -> x.stream()).collect(Collectors.toSet());
                confusedStateSetList.add(confusedWordsEndStates);
            }
            System.out.println(confusedStateSetList);
            for (int i = 0; i < confusedStateSetList.size(); i++) {
                for (int j = i + 1; j < confusedStateSetList.size(); j++) {
                    if (!Collections.disjoint(confusedStateSetList.get(i),
                            confusedStateSetList.get(j))) {
                        System.out.println("returning false!");
                        Misc.printMonitor(nfa);
                        return false;
                    }
                }
            }
        }
        System.out.println("Returning true!");
        Misc.printMonitor(nfa);
        return true;
    }

    // public boolean checkMonitor(FastNFA<String> nfa) throws IOException {
    // Set<FastNFAState> setInitialStates = nfa.getInitialStates();
    // assert setInitialStates.size() == 1;
    //
    // // Loop through all sets of sets of confusedWords sets
    // // Basically, we now have a 3-level deep set
    // // Level 1 : Set of all confused states (L1)
    // // Level 2: Sets of Words identifying each state (L2)
    // // Level 3: Sets of words for one particular state (L3)
    //
    // // The requirement now is that all words in the L3 set must end at the
    // // same state
    //
    // // From the L3 level, we will "obtain" a set of states. The L2
    // // requirement is that every state at the L2 level must be distinct.
    //
    // // There is no requirement at the L1 level: this is, thankfully, just a
    // // normal container! (Naturally, "normal" in our context: it contains
    // // all the sets of states which are confused and as such, has no
    // // requirements amongst its own elements).
    //
    // for (Set<Set<Word<String>>> confusedWordsSet : this.nonInjectiveWordsSet)
    // {
    // Set<FastNFAState> confusedStateSetList = new HashSet<>();
    // Integer confusedStateSetListSize = 0;
    // for (Set<Word<String>> confusedWords : confusedWordsSet) {
    // confusedStateSetListSize++;
    // Set<FastNFAState> confusedWordsEndStates = confusedWords
    // .stream()
    // .map(word -> MonitorTool.getSuccessors(nfa, null, word))
    // .flatMap(c -> c.stream()).map(x -> nfa.getState(x))
    // .collect(Collectors.toSet());
    // // if (!(confusedWordsEndStates.size() == 1)) {
    // // return false;
    // // } else {
    // confusedStateSetList.add(confusedWordsEndStates.stream()
    // .collect(Collectors.toList()).get(0));
    // // }
    // }
    // if (confusedStateSetListSize != confusedStateSetList.size()) {
    // return false;
    // } else {
    // confusedStateSetList.clear();
    // confusedStateSetListSize = 0;
    // }
    // }
    // return true;
    // }
}
