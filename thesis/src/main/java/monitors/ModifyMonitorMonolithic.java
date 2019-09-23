package monitors;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javatuples.Pair;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.words.Word;
import net.automatalib.words.impl.SimpleAlphabet;
import simulation.Neighbors;

/**
 * Class to remove the provided transition from the provided monitor.
 * 
 * @see #ModifyMonitor(CompactDFA, Word)
 * @author Bharat Garhewal
 *
 */
public class ModifyMonitorMonolithic {

    FastNFA<String> monitor = new FastNFA<String>(new SimpleAlphabet<String>());

    /**
     * 
     * Constructor which basically does two things: copies the incoming
     * monitor inside and then deletes the passed transition from the copy of
     * the monitor. Transition here meaning the final action of the transition
     * sequence, since the earlier symbols comprise the sequence used to
     * "access" the final action. It is not meant to call any other method.
     * Simply call the constructor and then call the {@link #getMonitor()}
     * method. This could possibly be re-factored as a single method and then
     * simply have an anonymous object of this class.
     * 
     * @param monitor2
     *            the monitor
     * @param transition
     *            the transition
     */
    public ModifyMonitorMonolithic(FastNFA<String> automaton,
            Pair<FastNFAState, FastNFAState> pairStates) {
        copyMonitorInit(automaton);
        mergeStates(pairStates);
    }

    public ModifyMonitorMonolithic(FastNFA<String> automaton,
            List<Pair<FastNFAState, FastNFAState>> collectionPairs) {
        copyMonitorInit(automaton);
        int initialDestinationState = collectionPairs.get(0).getValue1()
                .getId();
        int initialSourceState = collectionPairs.get(0).getValue0().getId();
        mergeStates(collectionPairs.remove(0));
        for (Pair<FastNFAState, FastNFAState> pair : collectionPairs) {
            FastNFAState sourceState = pair.getValue0();
            FastNFAState destinationState = pair.getValue1();
            if (destinationState.getId() != initialDestinationState
                    && sourceState.getId() != initialDestinationState
                    && sourceState.getId() != initialSourceState
                    && destinationState.getId() != initialSourceState) {
                mergeStates(pair);
            }
        }
    }

    /**
     * Copy the monitor into the object.
     * 
     * @param monitor2
     *            the monitor to be copied
     */
    private void copyMonitorInit(FastNFA<String> automaton) {
        this.monitor = new FastNFA<String>(automaton.getInputAlphabet());
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                automaton, automaton.getInputAlphabet(), this.monitor);
    }

    /**
     * Deletes the specified transition, more specifically, the action at the
     * end of the specified transition.
     * 
     * @param transitionToRemove
     *            the transition to remove
     */
    private void mergeStates(Pair<FastNFAState, FastNFAState> pairStates) {
        FastNFAState state1 = pairStates.getValue0();
        FastNFAState state2 = pairStates.getValue1();
        if (this.monitor.getInitialStates().contains(state2)) {
            mergeStatesCorrect(state2, state1);
        } else {
            mergeStatesCorrect(state1, state2);
        }
    }

    private void mergeStatesCorrect(FastNFAState stateKeep,
            FastNFAState stateRemove) {
        Map<String, Set<FastNFAState>> incomingTransitions = Neighbors
                .getPredecessors(monitor, stateRemove);
        Map<String, Set<FastNFAState>> outgoingTransitons = Neighbors
                .getSuccessors(monitor, stateRemove);

        // Redirect all transitions going to the stateRemove state to the
        // stateKeep state
        for (String input : incomingTransitions.keySet()) {
            Set<FastNFAState> states = incomingTransitions.get(input);
            for (FastNFAState s : states) {
                removeTransition(s, input, stateRemove);
                addTransition(s, input, stateKeep);
            }
        }

        // Redirect all transitions leaving from the stateRemoveState to the
        // stateKeep state
        for (String input : outgoingTransitons.keySet()) {
            for (FastNFAState s : outgoingTransitons.get(input)) {
                removeTransition(stateRemove, input, s);
                addTransition(stateKeep, input, s);
            }
        }

        // Remove all self-loops
        for (FastNFAState currState : this.monitor.getStates()) {
            for (String input : this.monitor.getInputAlphabet()) {
                Set<FastNFAState> succStates = Neighbors
                        .getSuccessors(monitor, currState).get(input);
                if (null != succStates && !succStates.isEmpty()) {
                    for (FastNFAState s : succStates) {
                        if (s.getId() == currState.getId()) {
                            removeTransition(currState, input, s);
                        }
                    }
                }
            }
        }

        int initialSize = this.monitor.size();
        this.monitor.removeState(this.monitor.getState(stateRemove.getId()));
        if (initialSize == this.monitor.size()) {
            System.err.println(
                    "Something is wrong with the modification algorithm.");
        }
    }

    private void addTransition(FastNFAState s, String input,
            FastNFAState stateKeep) {
        this.monitor.addTransition(this.monitor.getState(s.getId()), input,
                this.monitor.getState(stateKeep.getId()), null);
    }

    private void removeTransition(FastNFAState startState, String input,
            FastNFAState stateRemove) {
        this.monitor.removeTransition(this.monitor.getState(startState.getId()),
                input, this.monitor.getState(stateRemove.getId()));
    }

    /**
     * Getter method for the monitor, alongside some sanity checks.
     * 
     * @return monitor
     */
    public FastNFA<String> getMonitor() {
        return this.monitor;
    }
}
