package monitors;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javatuples.Pair;

import net.automatalib.automata.fsa.MutableDFA;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.words.Word;
import net.automatalib.words.impl.SimpleAlphabet;

public class UniversalMonitor {

	private CompactDFA<String> specification;
	private Map<String, Map<Integer, Set<Integer>>> specificiationToSubSpecStateMap = new HashMap<String, Map<Integer, Set<Integer>>>();
	private CompactNFA<String> monitor;
	private CompactDFA<String> backupMonitor;
	private Iterator<Word<String>> transitionCoverIterator;
	private int numberOfTransitions;

	public UniversalMonitor(CompactDFA<String> spec,
			Map<String, Map<Integer, Set<Integer>>> specificiationToSubSpecStateMap) {
		this.specification = new CompactDFA<String>(spec.getInputAlphabet());
		if (null != specificiationToSubSpecStateMap) {
			this.specificiationToSubSpecStateMap.putAll(specificiationToSubSpecStateMap);
		}
		this.monitor = new CompactNFA<String>(spec.getInputAlphabet());
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, spec, spec.getInputAlphabet(), this.specification);
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, this.specification, this.specification.getInputAlphabet(),
				this.monitor);
		this.backupMonitor = new CompactDFA<String>(spec.getInputAlphabet());
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, this.specification, this.specification.getInputAlphabet(),
				this.backupMonitor);
		this.transitionCoverIterator = Covers.transitionCoverIterator(this.specification,
				this.specification.getInputAlphabet());
		List<Word<String>> tc = new LinkedList<>();
		Covers.transitionCover(this.specification, this.specification.getInputAlphabet(), tc);
//		Collections.reverse(tc);
//		Collections.shuffle(tc);
		this.numberOfTransitions = tc.size();
		this.transitionCoverIterator = tc.iterator();
	}

	private CompactDFA<String> modify(boolean correct) {
		if (!correct) {
			this.monitor.clear();
			AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, this.backupMonitor,
					this.specification.getInputAlphabet(), this.monitor);
		}
		Word<String> transitionToRemove = getNextTransition();
		if (null == transitionToRemove) {
			return null;
		}
		deleteTransition(transitionToRemove);
		System.out.println("size on monitor = " + this.monitor.size());
		System.err.println("Done!");
		return new CompactDFA<String>(this.specification.getInputAlphabet());
	}

	/**
	 * Deletes the specified transition, more specifically, the action at the end of the specified transition. Also
	 * merges states and determinizes and minimizes the resulting automaton.
	 * 
	 * @param transitionToRemove
	 */
	private void deleteTransition(Word<String> transitionToRemove) {
		System.out.println("Transition to remove: " + transitionToRemove);
		// Save the current monitor
		this.backupMonitor.clear();
		this.backupMonitor = new CompactDFA<String>(this.specification.getInputAlphabet());
		for (Integer x : this.monitor.getInitialStates()) {
			if (x != 0) {
				this.monitor.setInitial(x, false);
			}
		}
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, this.monitor, this.specification.getInputAlphabet(),
				this.backupMonitor);

		// Identify the transition to delete
		// Integer sourceState = (Integer) this.monitor.getStates(transitionToRemove.subWord(0,
		// transitionToRemove.size()))
		// .toArray()[0];
		Integer sourceState = monitorGetState(transitionToRemove.subWord(0, transitionToRemove.size() - 1), null);
		Integer destinationState = monitorGetState(transitionToRemove, null);
		if (sourceState.equals(destinationState)) {
			System.err.println("You dumb mf...");
		}
		// Integer destinationState = (Integer) this.monitor.getStates(transitionToRemove).toArray()[0];
		String action = transitionToRemove.lastSymbol();

		// Check if it is the only transition between the source and destination states
		if (!checkIfOnlyTransition(sourceState, action, destinationState)) {
			// If it is not the only transition, then remove it directly
			System.out.println("INSANITY: not the only transition " + transitionToRemove);
			this.monitor.removeTransition(sourceState, action, destinationState);
		} else {
			// If it is the only transition, then remove and merge the source and destination states
			System.out.println("INSANITY: only transition " + transitionToRemove + " source: " + sourceState
					+ " destination: " + destinationState);
			for (Integer x : this.monitor.getTransitions(sourceState, action)) {
				this.monitor.removeTransition(sourceState, action, x);
			}
			// Transition correctly removed!
			mergeStates(sourceState, destinationState);
		}

		// Finally, determinize and minimize the resulting monitor
		// in order to return it
		determinizeAndMinimize(this.monitor);
	}

	/**
	 * Merges the source and destination states by basically redirecting going in/out of one state to the other and
	 * finally deleting the former.
	 * 
	 * @param sourceState
	 *            The source state
	 * @param destinationState
	 *            The destination state
	 */
	private void mergeStates(Integer sourceState, Integer destinationState) {
		Set<Pair<Integer, String>> incomingTransitions = new HashSet<>();
		Set<Pair<Integer, String>> outgoingTransitions = new HashSet<>();
		incomingTransitions.addAll(getPredecessors(this.monitor, destinationState));
		outgoingTransitions.addAll(getSuccessors(this.monitor, destinationState));
		// System.out.println("Incoming transitions to destinationState: " + incomingTransitions);
		// System.out.println("Outgoing transitions from destinationState: " + outgoingTransitions);
		// System.out.println("Pre merging...\n\n");
		// for (Integer x : this.monitor.getStates()) {
		// for (String y : this.monitor.getInputAlphabet()) {
		// System.out.println(x + " + " + y + " = " + this.monitor.getSuccessors(x, y));
		// }
		// }

		// Redirect all incoming transitions for the destinationState to the sourceState
		for (Pair<Integer, String> incomingPair : incomingTransitions) {
			// this.monitor.removeTransition(incomingPair.getValue0(), incomingPair.getValue1(), destinationState);
			this.removeTransitionNFA(incomingPair.getValue0(), incomingPair.getValue1(), destinationState);
			this.addTransitionNFA(incomingPair.getValue0(), incomingPair.getValue1(), sourceState);
			// this.monitor.addTransition(incomingPair.getValue0(), incomingPair.getValue1(), sourceState);
		}

		// Redirect all outgoing transitions for the destinationState to the sourceState
		for (Pair<Integer, String> outgoingPair : outgoingTransitions) {
			this.removeTransitionNFA(destinationState, outgoingPair.getValue1(), outgoingPair.getValue0());
			this.addTransitionNFA(sourceState, outgoingPair.getValue1(), outgoingPair.getValue0());

			// this.monitor.removeTransition(destinationState, outgoingPair.getValue1(), outgoingPair.getValue0());
			// this.monitor.addTransition(sourceState, outgoingPair.getValue1(), outgoingPair.getValue0());
		}
		// System.out.println("\n\nPost merging...");
		// for (Integer x : this.monitor.getStates()) {
		// for (String y : this.monitor.getInputAlphabet()) {
		// System.out.println(x + " + " + y + " = " + this.monitor.getSuccessors(x, y));
		// }
		// }
		return;
	}

	private boolean checkIfOnlyTransition(Integer sourceState, String action, Integer destinationState) {
		for (String input : this.monitor.getInputAlphabet()) {
			if (!input.equalsIgnoreCase(action)) {
				if (null != (monitorGetState(input, sourceState))) {
					if (monitorGetState(input, sourceState).equals(destinationState)) {
						System.out.println(sourceState + " + " + action + " = " + destinationState);
						return false;
					}
				}
			}
		}
		return true;
	}

	private void determinizeAndMinimize(CompactNFA<String> nfa) {
		System.out.println("Size before det and min: " + this.monitor.size());
		FastDFA<String> mid = new FastDFA<String>(this.specification.getInputAlphabet());
		NFAs.determinize(nfa, this.specification.getInputAlphabet(), mid, true, true);
		System.out.println("Size after det and min: " + mid.size());
		this.monitor.clear();
		this.monitor = new CompactNFA<String>(this.specification.getInputAlphabet());
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, mid, this.specification.getInputAlphabet(), this.monitor);
		System.out.println("Size after det and min and copy: " + this.monitor.size());
	}

	/**
	 * Return the next word in the transition cover, after checking if it is a legitimate transition in the
	 * specification field. Returns <b>null</b> if there are no more words in the transition cover.
	 * 
	 * @return Word<String> : the next input word in the transition cover
	 */
	private Word<String> getNextTransition() {
		while (this.transitionCoverIterator.hasNext()) {
			Word<String> transition = this.transitionCoverIterator.next();
			System.err.println("\n\n" + this.numberOfTransitions-- + " left");
			if (null != this.specification.getState(transition)) {
				return transition;
			}
		}
		return null;
	}

	/**
	 * Special implementation to get the destination state in order to make the NFA monitor object behave like a CIF
	 * monitor. Put null as sourceState if starting from initial state
	 * 
	 * @param actionSequence
	 * @return the destination state of the <i>actionSequnce</i>
	 */
	public Integer monitorGetState(Word<String> actionSequence, Integer sourceState) {
		Integer state = (Integer) this.monitor.getInitialStates().toArray()[0];
		// System.err.println("Initial state " + state);
		if (null != sourceState) {
			state = sourceState;
		}
		Integer nextState = 0;
		for (String inputAction : actionSequence) {
			// System.err.println(actionSequence + " with current action: " + inputAction);
			// System.err.println(this.monitor.getSuccessors(state, inputAction));
			// for (String x : this.monitor.getInputAlphabet()) {
			// System.err.println("Action " + x + " " + this.monitor.getSuccessors(state, x));
			// }
			if (0 != this.monitor.getSuccessors(state, inputAction).toArray().length) {
				nextState = (Integer) this.monitor.getSuccessors(state, inputAction).toArray()[0];
				state = nextState;
			}
			// nextState = (Integer) this.monitor.getSuccessors(state, inputAction).toArray()[0];
			// if (null != nextState) {
			// state = nextState;
			// }
		}
		return nextState;
	}

	public Integer monitorGetState(String actionSequence, Integer sourceState) {
		List<String> input = new LinkedList<String>();
		input.add(actionSequence);
		return monitorGetState(Word.fromList(input), sourceState);
	}

	public static Set<Pair<Integer, String>> getSuccessors(CompactNFA<String> nfa, Integer state) {
		Set<Pair<Integer, String>> ret = new HashSet<>();
		for (String input : nfa.getInputAlphabet()) {
			if (0 != nfa.getSuccessors(state, input).toArray().length) {
				ret.add(new Pair<Integer, String>((Integer) nfa.getSuccessors(state, input).toArray()[0], input));
			}
		}
		return ret;
	}

	public static Set<Pair<Integer, String>> getPredecessors(CompactNFA<String> nfa, Integer state) {
		// CompactDFA<String> dfa = new CompactDFA<String>(nfa.getInputAlphabet());
		// AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, nfa, nfa.getInputAlphabet(), dfa);
		Set<Pair<Integer, String>> ret = new HashSet<>();
		for (Integer s : nfa.getStates()) {
			for (String input : nfa.getInputAlphabet()) {
				if (0 != nfa.getSuccessors(s, input).toArray().length) {
					if (state.equals((Integer) nfa.getSuccessors(s, input).toArray()[0])) {
						ret.add(new Pair<Integer, String>(s, input));
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Constructs a new candidate monitor for the specification. Begins with the entire specification as an initial
	 * global monitor.
	 * 
	 * @return new candidate monitor
	 */
	public CompactDFA<String> getNewCandidateMonitor(boolean correct) {
		CompactDFA<String> out = modify(correct);
		if (null == out) {
			return null;
		}
		CompactDFA<String> ret = new CompactDFA<String>(this.specification.getInputAlphabet());
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, this.monitor, this.specification.getInputAlphabet(), ret);
		// System.out.println(ret.getInitialState());
		// for (Integer x : ret.getStates()) {
		// for (String y : ret.getInputAlphabet()) {
		// System.out.println(x + " + " + y + " = " + ret.getSuccessor(x, y));
		// }
		// }
		return ret;
	}

	public CompactDFA<String> getInitialMonitor() {
		CompactDFA<String> ret = new CompactDFA<String>(this.specification.getInputAlphabet());
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, this.monitor, this.specification.getInputAlphabet(), ret);
		return ret;
	}

	private void removeTransitionNFA(Integer sourceState, String input, Integer destinationState) {
		for (Integer x : this.monitor.getTransitions(sourceState, input)) {
			this.monitor.removeTransition(sourceState, input, x);
		}
		return;
	}

	private void addTransitionNFA(Integer sourceState, String input, Integer destinationState) {
		this.monitor.addTransition(sourceState, input, this.monitor.createTransition(destinationState, null));
	}

	private CompactDFA<String> convertFastDFAToCompactDFA(FastDFA<String> fast) {
		CompactDFA<String> dfa = new CompactDFA<String>(fast.getInputAlphabet());
		dfa.addInitialState(true);
		for (int i = 1; i < fast.size(); i++) {
			dfa.addState(true);
		}

		for (FastDFAState x : fast.getStates()) {
			for (String y : fast.getInputAlphabet()) {
				if (null != fast.getSuccessor(x, y)) {
					dfa.addTransition(x.getId(), y, fast.getSuccessor(x, y).getId(), null);
				}
			}
		}
		return dfa;
	}

}
