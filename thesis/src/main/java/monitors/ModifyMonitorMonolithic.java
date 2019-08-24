package monitors;

import java.util.HashSet;
import java.util.Set;

import org.javatuples.Pair;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.commons.util.mappings.Mapping;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.words.Word;
import net.automatalib.words.impl.SimpleAlphabet;

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
     * FIXME
     * Constructor which basically does two things: copies the incoming monitor
     * inside and then deletes the passed transition from the copy of the
     * monitor. Transition here meaning the final action of the transition
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
            Word<String> transition) {
        copyMonitorInit(automaton);
        deleteTransition(transition);
    }

    /**
     * Copy the monitor into the object.
     * 
     * @param monitor2
     *            the monitor to be copied
     */
    private void copyMonitorInit(FastNFA<String> automaton) {
        this.monitor = new FastNFA<String>(automaton.getInputAlphabet());
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE, automaton,
                automaton.getInputAlphabet(), this.monitor);
    }

    /**
     * Deletes the specified transition, more specifically, the action at the
     * end of the specified transition. Also merges states and determinizes and
     * minimizes the resulting automaton.
     * 
     * @param transitionToRemove
     *            the transition to remove
     */
    private void deleteTransition(Word<String> transitionToRemove) {
        Integer sourceState = (int) (Math.random() * this.monitor.size());
        Integer destinationState = (int) (Math.random() * this.monitor.size());
        while (destinationState.equals(sourceState)) {
            destinationState = (int) (Math.random() * this.monitor.size());
        }
        System.out.println(
                "Merging states " + sourceState + " and " + destinationState);
        mergeStates(sourceState, destinationState);
    }

    /**
     * Merges the source and destination states by basically redirecting going
     * in/out of one state to the other and finally deleting the former.
     * 
     * @param sourceState
     *            The source state
     * @param destinationState
     *            The destination state
     */
    private void mergeStates(Integer sourceState, Integer destinationState) {
        Set<Pair<Integer, String>> incomingTransitions = new HashSet<>();
        Set<Pair<Integer, String>> outgoingTransitions = new HashSet<>();
        incomingTransitions.addAll(StateInformation
                .getPredecessors(this.monitor, destinationState));
        outgoingTransitions.addAll(
                StateInformation.getSuccessors(this.monitor, destinationState));

        // Redirect all incoming transitions for the destinationState to the
        // sourceState
        for (Pair<Integer, String> incomingPair : incomingTransitions) {
            this.removeTransitionNFA(incomingPair.getValue0(),
                    incomingPair.getValue1(), destinationState);
            this.addTransitionNFA(incomingPair.getValue0(),
                    incomingPair.getValue1(), sourceState);
        }

        // Redirect all outgoing transitions for the destinationState to the
        // sourceState
        for (Pair<Integer, String> outgoingPair : outgoingTransitions) {
            this.removeTransitionNFA(destinationState, outgoingPair.getValue1(),
                    outgoingPair.getValue0());
            this.addTransitionNFA(sourceState, outgoingPair.getValue1(),
                    outgoingPair.getValue0());
        }
        if (this.monitor.getInitialStates()
                .contains(this.monitor.getState(destinationState))) {
            this.monitor.setInitial(this.monitor.getState(sourceState), true);
        }
        this.monitor.removeState(this.monitor.getState(destinationState));

        return;
    }

    /**
     * Removes a transition from the <b>sourceState</b> via <b>input</b> going
     * to <b>destinationState</b>
     * 
     * @param sourceState
     * @param input
     * @param destinationState
     */
    private void removeTransitionNFA(Integer sourceState, String input,
            Integer destinationState) {
        for (FastNFAState x : this.monitor
                .getTransitions(this.monitor.getState(sourceState), input)) {
            this.monitor.removeTransition(this.monitor.getState(sourceState),
                    input, x);
        }
        return;
    }

    /**
     * Adds a transition from the <b>sourceState</b> via <b>input</b> going to
     * <b>destinationState</b>
     * 
     * @param sourceState
     * @param input
     * @param destinationState
     */
    private void addTransitionNFA(Integer sourceState, String input,
            Integer destinationState) {
        this.monitor.addTransition(this.monitor.getState(sourceState), input,
                this.monitor.createTransition(
                        this.monitor.getState(destinationState), null));
    }

    /**
     * Getter method for the monitor, alongside some sanity checks.
     * 
     * @return monitor
     */
    public FastNFA<String> getMonitor() {
        FastNFA<String> ret = new FastNFA<String>(
                this.monitor.getInputAlphabet());
        Mapping<FastNFAState, FastNFAState> x = AutomatonLowLevelCopy.copy(
                AutomatonCopyMethod.BFS, this.monitor,
                this.monitor.getInputAlphabet(), ret);
        assert null != x;
        assert null != ret;
        return ret;
    }
}
