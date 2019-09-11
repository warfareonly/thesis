package monitors;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
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
import simulation.Neighbors;
import utils.Misc;

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
     * FIXME Constructor which basically does two things: copies the incoming
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
//        System.out.println("Incoming size of monitor: " + this.monitor.size());
//        Misc.printMonitor(monitor);
        deleteTransition(pairStates);
//        System.out.println("Outgoing size of monitor: " + this.monitor.size());
//        Misc.printMonitor(monitor);
//        try {
//            System.in.read();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        // System.exit(0);
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
     * end of the specified transition. Also merges states and determinizes and
     * minimizes the resulting automaton.
     * 
     * @param transitionToRemove
     *            the transition to remove
     */
    private void deleteTransition(Pair<FastNFAState, FastNFAState> pairStates) {
        // Integer sourceState = (int) (Math.random() * this.monitor.size());
        // Integer destinationState = (int) (Math.random() *
        // this.monitor.size());
        // while (destinationState.equals(sourceState)) {
        // destinationState = (int) (Math.random() * this.monitor.size());
        // }
        // System.out.println(
        // "Merging states " + sourceState + " and " + destinationState);
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
        // System.out.println(incomingTransitions);
        for (String input : incomingTransitions.keySet()) {
            Set<FastNFAState> states = incomingTransitions.get(input);
            for (FastNFAState s : states) {
                // s: source, input: event label, stateRemove : destination
//                System.out.println("Incoming transitions : " + input);
                removeTransition(s, input, stateRemove);
                addTransition(s, input, stateKeep);
            }
        }

        // Redirect all transitions leaving from the stateRemoveState to the
        // stateKeep state
        for (String input : outgoingTransitons.keySet()) {
            for (FastNFAState s : outgoingTransitons.get(input)) {
//                System.out.println("Outgoing transitions : " + input);
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
        // if (0 != Neighbors.getPredecessors(monitor, stateRemove).size()) {
        // System.out.flush();
        // System.err.flush();
        // System.err.println(
        // "something went wrong with removing the incoming transitions.
        // Exiting...");
        // System.exit(0);
        // }

        int initialSize = this.monitor.size();
        this.monitor.removeState(this.monitor.getState(stateRemove.getId()));
        if (initialSize == this.monitor.size()) {
            System.err.println(
                    "Something is wrong with the modification algorithm.");
            // System.exit(0);
        }
    }

    private void addTransition(FastNFAState s, String input,
            FastNFAState stateKeep) {
        FastNFAState x = this.monitor.addTransition(
                this.monitor.getState(s.getId()), input,
                this.monitor.getState(stateKeep.getId()), null);
    }

    private void removeTransition(FastNFAState startState, String input,
            FastNFAState stateRemove) {
        this.monitor.removeTransition(this.monitor.getState(startState.getId()),
                input, this.monitor.getState(stateRemove.getId()));
    }

    // /**
    // * Merges the source and destination states by basically redirecting going
    // * in/out of destination state to the source state and finally deleting
    // the
    // * destination state.
    // *
    // * @param sourceState
    // * The source state
    // * @param destinationState
    // * The destination state
    // */
    // private void mergeStates(Integer sourceState, Integer destinationState) {
    // Set<Pair<FastNFAState, String>> incomingTransitions = new HashSet<>();
    // Set<Pair<FastNFAState, String>> outgoingTransitions = new HashSet<>();
    // incomingTransitions.addAll(
    // Neighbors.getPredecessors(this.monitor, destinationState));
    // outgoingTransitions.addAll(
    // StateInformation.getSuccessors(this.monitor, destinationState));
    //
    // // Redirect all incoming transitions for the destinationState to the
    // // sourceState
    // for (Pair<Integer, String> incomingPair : incomingTransitions) {
    // this.removeTransitionNFA(incomingPair.getValue0(),
    // incomingPair.getValue1(), destinationState);
    // this.addTransitionNFA(incomingPair.getValue0(),
    // incomingPair.getValue1(), sourceState);
    // }
    //
    // // Redirect all outgoing transitions for the destinationState to the
    // // sourceState
    // for (Pair<Integer, String> outgoingPair : outgoingTransitions) {
    // this.removeTransitionNFA(destinationState, outgoingPair.getValue1(),
    // outgoingPair.getValue0());
    // this.addTransitionNFA(sourceState, outgoingPair.getValue1(),
    // outgoingPair.getValue0());
    // }
    // if (this.monitor.getInitialStates()
    // .contains(this.monitor.getState(destinationState))) {
    // this.monitor.setInitial(this.monitor.getState(sourceState), true);
    // }
    // System.out.println("Size of monitor: " + this.monitor.size());
    // this.monitor.removeState(this.monitor.getState(destinationState));
    // System.out.println(
    // "Size of monitor after state removal: " + this.monitor.size());
    //
    // return;
    // }
    //
    // /**
    // * Removes a transition from the <b>sourceState</b> via <b>input</b> going
    // * to <b>destinationState</b>
    // *
    // * @param sourceState
    // * @param input
    // * @param destinationState
    // */
    // private void removeTransitionNFA(Integer sourceState, String input,
    // Integer destinationState) {
    // for (FastNFAState x : this.monitor
    // .getTransitions(this.monitor.getState(sourceState), input)) {
    // this.monitor.removeTransition(this.monitor.getState(sourceState),
    // input, x);
    // }
    // return;
    // }
    //
    // /**
    // * Adds a transition from the <b>sourceState</b> via <b>input</b> going to
    // * <b>destinationState</b>
    // *
    // * @param sourceState
    // * @param input
    // * @param destinationState
    // */
    // private void addTransitionNFA(Integer sourceState, String input,
    // Integer destinationState) {
    // this.monitor.addTransition(this.monitor.getState(sourceState), input,
    // this.monitor.createTransition(
    // this.monitor.getState(destinationState), null));
    // }

    /**
     * Getter method for the monitor, alongside some sanity checks.
     * 
     * @return monitor
     */
    public FastNFA<String> getMonitor() {
        return this.monitor;
    }
}
