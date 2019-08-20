/**
 * 
 */
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

import invariant.Constraints;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import utils.Args;
import utils.Misc;

/**
 * Class to hold information about the global monitor. Also generates the global monitor for the chosen transition
 * iteration order.
 * 
 * @see #computeMonitor(Set)
 * @author Bharat Garhewal
 *
 */
public class GlobalMonitor {

	Args options;
	CompactDFA<String> specification;
	Iterator<Word<String>> transitionCoverIterator;
	private CompactDFA<String> monitor;
	private CompactDFA<String> monitorSafe;
	private Map<String, CompactDFA<String>> mapSubSpecifications = new HashMap<>();
	private Map<String, Map<Integer, Map<String, Set<Integer>>>> memorylessConstraints = new HashMap<>();
	private Map<Integer, Integer> specificationToMonitorMap = new HashMap<Integer, Integer>();
	private Map<Pair<String, Integer>, Set<Integer>> subSpecficationActionComboToSpecificationMap = new HashMap<>();

	public enum IterationOrder {
		FWD, BWD, RND;
	}

	/**
	 * Class for the global monitor
	 * 
	 * @param options
	 *            the same options variable from the "Main" class
	 * @param specification
	 *            the specification
	 * @param cons
	 *            the memoryless constraints map
	 * @param mapSubSpecifications
	 *            the string to sub-specifications map
	 * @param iterationOrder
	 *            iteration order of the transition cover iterator
	 */
	public GlobalMonitor(Args options, CompactDFA<String> specification, Constraints cons,
			Map<String, CompactDFA<String>> mapSubSpecifications, IterationOrder iterationOrder,
			Set<String> preferredActions) {
		this.options = options;
		this.memorylessConstraints = cons.getConstraints();
		this.specification = new CompactDFA<String>(specification.getInputAlphabet());
		this.monitor = new CompactDFA<String>(specification.getInputAlphabet());
		this.mapSubSpecifications = mapSubSpecifications;
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, specification, specification.getInputAlphabet(),
				this.specification);
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, specification, specification.getInputAlphabet(),
				this.monitor);
		transitionCoverIterator = prepareTransitionCover(specification, iterationOrder, preferredActions);
		this.subSpecficationActionComboToSpecificationMap = generateActionComboMap(mapSubSpecifications);
	}

	/**
	 * Always guaranteed to produce a correct monitor, as long as it is possible.
	 * 
	 * @param preferredActions
	 *            the set of actions which you want to <b>remain</b> in the monitor.
	 */
	public void computeMonitor() {
		Set<String> invariants = computeMonitorConstraints();
		try {
			boolean specificationAsMonitor = Misc.writeToOutput(options, invariants, this.monitor);
			if (specificationAsMonitor) {
				System.out.println("Specification works as monitor, trying to reduce!");
			} else {
				System.out.println("Specification is not working as a monitor, quitting!");
				return;
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		Integer count = 0;
		do {
			System.out.println("Monitor #" + count++ + " of 1446");
			Word<String> transition = transitionCoverIterator.next();
			this.monitorSafe = new CompactDFA<String>(this.monitor.getInputAlphabet());
			AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, this.monitor, this.monitor.getInputAlphabet(),
					this.monitorSafe);
			ModifyMonitor mod = new ModifyMonitor(this.monitor, transition);
			this.monitor = new CompactDFA<String>(this.specification.getInputAlphabet());
			AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, mod.getMonitor(), this.specification.getInputAlphabet(),
					this.monitor);
			invariants = computeMonitorConstraints();
			/*
			 * Check and restore starts here!
			 */
			try {
				if (!Misc.writeToOutput(options, invariants, this.monitor)) {
					this.monitor = new CompactDFA<String>(this.monitorSafe.getInputAlphabet());
					AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, this.monitorSafe,
							this.monitorSafe.getInputAlphabet(), this.monitor);
					invariants = computeMonitorConstraints();
					Misc.writeToOutput(options, invariants, this.monitor);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			/*
			 * 
			 * Check and restore ends here!
			 */
			// try {
			// System.in.read();
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
		} while (transitionCoverIterator.hasNext());
		// Hammer-time!
	}

	/**
	 * Just a helper, prints out the entire transition function to (System.out). Even the undefined transitions, so use
	 * with caution on automata with a large number of states.
	 * 
	 * @param monitor2
	 */
	@SuppressWarnings("unused")
	private void printMonitor(CompactDFA<String> monitor2) {
		for (Integer st : monitor2.getStates()) {
			for (String in : monitor2.getInputAlphabet()) {
				System.out.println(st + " + " + in + " = " + monitor2.getSuccessor(st, in));
			}
		}
	}

	/**
	 * Generate the set of <i>complete</i> constraints, combining the monitor and the memoryless constraints into a
	 * single set of strings. Also simplifies them.
	 * 
	 * @param preferredActions
	 *            A set of actions which are preferably in the final monitor
	 * @return set of invariant strings
	 */
	public Set<String> computeMonitorConstraints() {
		generateSpecificationToMonitorMap();
		Map<String, Map<Integer, Map<String, Set<Integer>>>> constraintsMonitor = generateConstraintsMonitorMap();
		Map<String, Map<Integer, Map<String, Set<Integer>>>> completeConstraints = new HashMap<>();
		Misc.deepMerge(completeConstraints, constraintsMonitor);
		Misc.deepMerge(completeConstraints, memorylessConstraints);

		Map<String, CompactDFA<String>> subSpecwithMonitorMap = new HashMap<>();
		subSpecwithMonitorMap.putAll(mapSubSpecifications);
		subSpecwithMonitorMap.put("globalMonitor", this.monitor);
		Constraints cons = new Constraints(completeConstraints, Misc.computeActionToSubSpecNames(mapSubSpecifications),
				subSpecwithMonitorMap);
		return cons.constructInvariantStatements();
	}

	/**
	 * Generate the constraints of the monitor, in the {@literal <action, state>} format map of the other memoryless
	 * constraints. The returned constraint map will be merged with the memoryless constraint map.
	 * 
	 * @return monitor constraints map
	 */
	private Map<String, Map<Integer, Map<String, Set<Integer>>>> generateConstraintsMonitorMap() {
		Map<String, Map<Integer, Map<String, Set<Integer>>>> ret = new HashMap<>();
		Set<String> actionNames = new HashSet<>();
		// Initialization of the map, (slightly) complicated!
		for (Pair<String, Integer> x : this.subSpecficationActionComboToSpecificationMap.keySet()) {
			actionNames.add(x.getValue0());
			if (ret.containsKey(x.getValue0())) {
				assert x.getValue1() != null;
				assert x.getValue0() != null;
				Map<Integer, Map<String, Set<Integer>>> intMap = ret.get(x.getValue0());
				Map<String, Set<Integer>> monitorMap = new HashMap<String, Set<Integer>>();
				monitorMap.put("globalMonitor", new HashSet<Integer>());
				intMap.put(x.getValue1(), monitorMap);
			} else {
				Map<Integer, Map<String, Set<Integer>>> intMap = new HashMap<>();
				assert x.getValue1() != null;
				assert x.getValue0() != null;
				Map<String, Set<Integer>> monitorMap = new HashMap<String, Set<Integer>>();
				monitorMap.put("globalMonitor", new HashSet<Integer>());
				intMap.put(x.getValue1(), monitorMap);
				ret.put(x.getValue0(), intMap);
			}
		}
		// Initialization of the map is finished.
		// Load up the values!
		for (Pair<String, Integer> actionCombo : this.subSpecficationActionComboToSpecificationMap.keySet()) {
			Set<Integer> specStates = this.subSpecficationActionComboToSpecificationMap.get(actionCombo);
			Set<Integer> subSpecificationStates = new HashSet<>();
			for (Integer x : specStates) {
				// Gives the equivalent monitor states for the states of the specification
				assert this.specificationToMonitorMap.get(x) != null;
				subSpecificationStates.add(this.specificationToMonitorMap.get(x));
			}
			for (Integer s : subSpecificationStates) {
				assert s != null;
			}
			ret.get(actionCombo.getValue0()).get(actionCombo.getValue1()).get("globalMonitor")
					.addAll(subSpecificationStates);
		}
		return ret;
	}

	/**
	 * Generates a map of the {@literal <action, state>} tuples of all the sub-specifications to their set of states in
	 * the specification.
	 * 
	 * @param specification
	 * @param mapSubSpecifications
	 *            map of string to sub-specification
	 * @return
	 */
	private Map<Pair<String, Integer>, Set<Integer>> generateActionComboMap(
			Map<String, CompactDFA<String>> mapSubSpecifications) {
		Map<Pair<String, Integer>, Set<Integer>> ret = new HashMap<>();
		Iterator<Word<String>> tcIterator = Covers.transitionCoverIterator(specification,
				specification.getInputAlphabet()); // Compute transition cover of the specification
		Map<String, String> actionToSubSpecificationNameMap = Misc.computeActionToSubSpecNames(mapSubSpecifications);
		// Got the map of action -> sub-specification names
		while (tcIterator.hasNext()) {
			Word<String> input = tcIterator.next();
			Integer specificationState = specification.getState(input);
			if (null != specificationState) {
				// If the word "input" is valid
				List<String> possibleInputs = getPossibleInputs(specification, specificationState);
				// Get all the outgoing actions, we don't care where they lead
				for (String pInput : possibleInputs) {
					CompactDFA<String> subSpec = mapSubSpecifications.get(actionToSubSpecificationNameMap.get(pInput));
					List<String> subSpecificationInput = Misc.projectToAlphabet(input, subSpec.getInputAlphabet());
					if (ret.containsKey(new Pair<String, Integer>(pInput, subSpec.getState(subSpecificationInput)))) {
						ret.get(new Pair<String, Integer>(pInput, subSpec.getState(subSpecificationInput)))
								.add(specificationState);
					} else {
						Set<Integer> x = new HashSet<>();
						x.add(specificationState);
						ret.put(new Pair<String, Integer>(pInput, subSpec.getState(subSpecificationInput)), x);
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Generates a map from the states of the specification to the states of the monitor.
	 */
	private void generateSpecificationToMonitorMap() {
		this.specificationToMonitorMap.clear();
		Iterator<Word<String>> tc = Covers.transitionCoverIterator(this.specification,
				this.specification.getInputAlphabet());
		while (tc.hasNext()) {
			Word<String> input = tc.next();
			Integer specState = this.specification.getState(input);
			if (null != specState) {
				Integer monitorState = monitorGetState(this.monitor, input);
				this.specificationToMonitorMap.put(specState, monitorState);
			}
		}
	}

	private Integer monitorGetState(CompactDFA<String> monitor2, Word<String> input) {
		Integer state = monitor2.getInitialState();
		Integer destState = state;
		for (String in : input) {
			destState = monitor2.getSuccessor(state, in);
			if (null != destState) {
				state = destState;
			}
		}
		return state;
	}

	private List<String> getPossibleInputs(CompactDFA<String> dfa, Integer state) {
		List<String> ret = new LinkedList<String>();
		for (String in : dfa.getInputAlphabet()) {
			if (dfa.getSuccessor(state, in) != null) {
				ret.add(in);
			}
		}
		return ret;
	}

	/**
	 * Prepare the transition cover of the DFA according to our specified order and set of preferred actions.
	 * 
	 * @param dfa
	 *            the monitor DFA
	 * @param ord
	 *            the order in which to iterate through the transition cover
	 * @param preferredActions
	 *            set of actions that should be preserved in the monitor
	 * @return iterator for the transition cover
	 */
	private Iterator<Word<String>> prepareTransitionCover(CompactDFA<String> dfa, IterationOrder ord,
			Set<String> preferredActions) {
		Set<String> removableActions = new HashSet<>();
		removableActions.addAll(dfa.getInputAlphabet());
		if (null != preferredActions) {
			removableActions.removeAll(preferredActions);
		}
		Iterator<Word<String>> tc = Covers.transitionCoverIterator(dfa, dfa.getInputAlphabet());
		List<Word<String>> ret = new LinkedList<>();
		if (null != preferredActions) {
			while (tc.hasNext()) {
				Word<String> input = tc.next();
				// System.out.println(input);
				// System.out.println(input.lastSymbol());
				if (null != dfa.getState(input) && removableActions.contains(input.lastSymbol())) {
					ret.add(input);
				}
			}
		} else {
			while (tc.hasNext()) {
				Word<String> input = tc.next();
				if (null != dfa.getState(input))
					ret.add(input);

			}
		}
		@SuppressWarnings("unused")
		Iterator<Word<String>> retIterator;
		switch (ord) {
		case BWD: {
			Collections.reverse(ret);
			retIterator = ret.iterator();
		}
			break;
		case FWD:
			retIterator = ret.iterator();
			break;
		case RND: {
			Collections.shuffle(ret);
			retIterator = ret.iterator();
		}
			break;
		default:
			retIterator = ret.iterator();
			break;

		}
		// System.out.println(ret);
		return ret.iterator();
	}
}
