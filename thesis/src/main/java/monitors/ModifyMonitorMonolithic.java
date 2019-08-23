package monitors;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.javatuples.Pair;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.commons.util.mappings.Mapping;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.fsa.NFAs;
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
    public ModifyMonitorMonolithic(FastNFA<String> monitor2,
            Word<String> transition) {
        copyMonitorInit(monitor2);
        deleteTransition(transition);
    }

    /**
     * Copy the monitor into the object.
     * 
     * @param monitor2
     *            the monitor to be copied
     */
    private void copyMonitorInit(FastNFA<String> monitor2) {
        this.monitor = new FastNFA<String>(monitor2.getInputAlphabet());
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE, monitor2,
                monitor2.getInputAlphabet(), this.monitor);
        // for (Integer st : this.monitor) {
        // if (!x.get(0).equals(st)) {
        // this.monitor.setInitial(st, false);
        // } else {
        // this.monitor.setInitial(st, true);
        // }
        // }
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
        // System.out.println("Transition to remove: " + transitionToRemove);
        // // Identify the transition to delete
        // Integer sourceState = monitorGetState(transitionToRemove.subWord(0,
        // transitionToRemove.size() - 1), null);
        // Integer destinationState = monitorGetState(transitionToRemove, null);
        // if (sourceState.equals(destinationState)) {
        // // If it is a self-loop, what's the point of removing it?
        // // It already contains no state information!
        // determinizeAndMinimize(this.monitor);
        // return;
        // }
        // // Integer destinationState = (Integer)
        // this.monitor.getStates(transitionToRemove).toArray()[0];
        // String action = transitionToRemove.lastSymbol();
        //
        // // Check if it is the only transition between the source and
        // destination states
        // if (!checkIfOnlyTransition(sourceState, action, destinationState)) {
        // // If it is not the only transition, then remove it directly
        // this.monitor.removeTransition(sourceState, action, destinationState);
        // } else {
        // // If it is the only transition, then remove and merge the source and
        // destination states
        // for (Integer x : this.monitor.getTransitions(sourceState, action)) {
        // this.monitor.removeTransition(sourceState, action, x);
        // }
        // // Transition correctly removed!
        Integer sourceState = (int) (Math.random() * this.monitor.size());
        Integer destinationState = (int) (Math.random() * this.monitor.size());
        while (destinationState.equals(sourceState)) {
            destinationState = (int) (Math.random() * this.monitor.size());
        }
        mergeStates(sourceState, destinationState);
        // }

        // Finally, determinize and minimize the resulting monitor
        // in order to return it
        // determinizeAndMinimize(this.monitor);
    }

    /**
     * Check if the transition specified by the parameters is the only
     * transition between the source and destination states. If it is, then we
     * will need to merge the source and destination states into a single state,
     * as otherwise the destination state will be unreachable!
     * 
     * @param sourceState
     * @param action
     * @param destinationState
     * @return boolean whether there is a single transition (true) or not
     *         (false).
     */
    // private boolean checkIfOnlyTransition(Integer sourceState, String action,
    // Integer destinationState) {
    // for (String input : this.monitor.getInputAlphabet()) {
    // if (!input.equalsIgnoreCase(action)) {
    // if (null != (monitorGetState(input, sourceState))) {
    // if (monitorGetState(input, sourceState)
    // .equals(destinationState)) {
    // return false;
    // }
    // }
    // }
    // }
    // return true;
    // }

    /**
     * Special implementation to get the destination state in order to make the
     * NFA monitor object behave like a CIF monitor. Put null as sourceState if
     * starting from initial state
     * 
     * @param actionSequence
     * @return the destination state of the <i>actionSequnce</i>
     */
    // public Integer monitorGetState(Word<String> actionSequence,
    // Integer sourceState) {
    // Integer state = (Integer) this.monitor.getInitialStates().toArray()[0];
    // assert this.monitor.getInitialStates().toArray().length == 1;
    // if (null != sourceState) {
    // state = sourceState;
    // }
    // Integer nextState = 0;
    // for (String inputAction : actionSequence) {
    // if (0 != this.monitor.getSuccessors(state, inputAction)
    // .toArray().length) {
    // nextState = (Integer) this.monitor
    // .getSuccessors(state, inputAction).toArray()[0];
    // state = nextState;
    // }
    // }
    // return nextState;
    // }

    /**
     * Get state function for a single symbol "sequence".
     * 
     * @param actionSequence
     * @param sourceState
     * @return destination state
     */
    // public Integer monitorGetState(String actionSequence, Integer
    // sourceState) {
    // List<String> input = new LinkedList<String>();
    // input.add(actionSequence);
    // return monitorGetState(Word.fromList(input), sourceState);
    // }

    /**
     * Determinize and minimize the NFA monitor into a deterministic NFA. Thus,
     * even though the monitor is stored as an NFA, it is still deterministic.
     * 
     * @param nfa
     */
    // private void determinizeAndMinimize(CompactNFA<String> nfa) {
    // FastDFA<String> mid = new FastDFA<String>(
    // this.monitor.getInputAlphabet());
    // NFAs.determinize(nfa, this.monitor.getInputAlphabet(), mid, true,
    // false);
    // // Determinization and minimization complete, and the result is stored
    // // in "mid".
    // this.monitor.clear();
    // this.monitor = new CompactNFA<String>(this.monitor.getInputAlphabet());
    // AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, mid,
    // this.monitor.getInputAlphabet(), this.monitor);
    // // "mid" is copied to the "monitor" field.
    // }

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

        // removeAllSelfLoops(this.monitor);
        return;
    }

    // private void removeAllSelfLoops(CompactNFA<String> monitor2) {
    // for (Integer srcState : this.monitor.getStates()) {
    // for (String input : this.monitor.getInputAlphabet()) {
    // Integer destState = 0;
    // if (null != this.monitor.getSuccessors(srcState, input)) {
    // if (this.monitor.getSuccessors(srcState, input)
    // .size() > 0) {
    // destState = (Integer) this.monitor
    // .getSuccessors(srcState, input).toArray()[0];
    //
    // if (destState.equals(srcState)) {
    // removeTransitionNFA(srcState, input, destState);
    // }
    // }
    // }
    // }
    // }
    // return;
    // }

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
        // Even though there is a for-loop here, we always have a deterministic
        // NFA, so it does not
        // matter.
        // assert this.monitor.getTransitions(sourceState, input).size() == 1;
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
