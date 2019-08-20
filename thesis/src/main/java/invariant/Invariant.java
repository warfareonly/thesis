package invariant;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.javatuples.Pair;
import org.javatuples.Quintet;

import monitors.UniversalMonitor;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.SimpleAlphabet;
import nl.tue.cif.v3x0x0.common.CifEvalException;

public class Invariant {
	public Set<String> unusedTransitionInvarsSet = new HashSet<String>();
	public Set<Quintet<String, Integer, List<String>, String, Map<String, Set<Integer>>>> blockRequirements = new HashSet<>();
	protected CompactDFA<String> specification;
	private UniversalMonitor universalMonitor;
	public CompactDFA<String> monitor = new CompactDFA<String>(new SimpleAlphabet<String>());
	List<String> outNames = new LinkedList<String>();
	public Map<Pair<String, Integer>, Map<String, Set<Integer>>> invariantsMap = new HashMap<>();
	Map<String, CompactDFA<String>> subSpecificationsMap = new HashMap<String, CompactDFA<String>>();
	Map<String, Map<Integer, Set<Integer>>> specificiationToSubSpecStateMap = new HashMap<String, Map<Integer, Set<Integer>>>();
	public Set<String> invars = new HashSet<String>();
	private boolean needMonitor = false;

	public Map<Pair<String, Integer>, Map<String, Set<Integer>>> monitorInvariantsMap = new HashMap<>();

	public Invariant(CompactDFA<String> specification, Map<String, CompactDFA<String>> specificationsMap)
			throws CifEvalException, IOException {
		// A map of the sub-specs, connecting their names to their DFAs
		this.subSpecificationsMap.putAll(specificationsMap);

		// The specification DFA
		this.specification = specification;

		this.universalMonitor = new UniversalMonitor(specification, null);
		// this.monitor = this.universalMonitor.getCandidateMonitor(true);

		// Empty set-up mapping the states of the specification, to the states of the
		// sub-spec
		this.specificiationToSubSpecStateMap = new HashMap<String, Map<Integer, Set<Integer>>>();
		for (String subSpecName : specificationsMap.keySet()) {
			Map<Integer, Set<Integer>> emptyMap = new HashMap<Integer, Set<Integer>>();
			for (int s = 0; s < this.specification.size(); s++) {
				Set<Integer> emptySet = new HashSet<Integer>();
				emptyMap.put(s, emptySet);
			}
			this.specificiationToSubSpecStateMap.put(subSpecName, emptyMap);
		}
	}

	public void computeInvariants() throws Exception {

		// Computes the state mapping from the specification to the sub-specification for
		// each sub-specification
		this.subSpecificationsMap.forEach((subSpecName, subSpecification) -> {
			Iterator<Word<String>> transitionCoverIterator = Covers.transitionCoverIterator(this.specification,
					this.specification.getInputAlphabet());
			Alphabet<String> subSpecAlphabet = subSpecification.getInputAlphabet();

			while (transitionCoverIterator.hasNext()) {
				Word<String> input = transitionCoverIterator.next();
				Integer specState = this.specification.getState(input);
				if (null != specState) {
					List<String> inputList = projectToAlphabet(input, subSpecAlphabet);
					Integer subspecState = subSpecification.getState(inputList);
					Set<Integer> oldStates = this.specificiationToSubSpecStateMap.get(subSpecName).get(specState);
					oldStates.add(subspecState);
				}
			}
		});

		// Generate non-combined map of state-guards
		computeStateGuards();
		System.err.println(this.specificiationToSubSpecStateMap);

		// Combined the state guards from the map created above
		constructInvariantsFromMap(false);
		// Handle all the actions from the sub-specifications
		// which were not used in the specification
		handleRemainingInvariants();
		return;
	}

	private void handleRemainingInvariants() {
		for (String name : this.subSpecificationsMap.keySet()) {
			CompactDFA<String> dfa = this.subSpecificationsMap.get(name);
			for (int i = 0; i < dfa.size(); i++) {
				List<String> possibleInputs = getPossibleInputs(dfa, i);
				for (String actionName : possibleInputs) {
					Pair<String, Integer> actionCombo = new Pair<>(actionName, i);
					if (!this.invariantsMap.keySet().contains(actionCombo)) {
						this.unusedTransitionInvarsSet
								.add(InvariantStatement.InvariantStatementUnusedBuild(actionCombo.getValue0(),
										actionCombo.getValue1(), getNameOfSubSpecFromAction(actionCombo.getValue0())));
					}
				}
			}
		}
	}

	private void computeStateGuards() throws IOException {
		for (String subSpecName : this.subSpecificationsMap.keySet()) {
			CompactDFA<String> subSpecification = this.subSpecificationsMap.get(subSpecName);
			Iterator<Word<String>> transitionCoverIterator = Covers.transitionCoverIterator(this.specification,
					this.specification.getInputAlphabet());
			while (transitionCoverIterator.hasNext()) {
				Word<String> input = transitionCoverIterator.next();
				Integer specState = this.specification.getState(input);
				if (null != specState) {
					List<String> possibleInputs = projectToAlphabet(getPossibleInputs(this.specification, specState),
							subSpecification.getInputAlphabet());
					if (possibleInputs.isEmpty()) {
						continue;
					}
					Integer subSpecState = subSpecification
							.getState(projectToAlphabet(input, subSpecification.getInputAlphabet()));
					writeGuards(input, subSpecName, specState, possibleInputs, subSpecState);
				}
			}
		}
		return;
	}

	private void writeGuards(Word<String> input, String subSpecName, Integer specState, List<String> possibleInputs,
			Integer subSpecState) throws IOException {
		Map<String, Set<Integer>> eqStates = new HashMap<String, Set<Integer>>();
		for (String x : this.specificiationToSubSpecStateMap.keySet()) {
			Set<Integer> setStates = new HashSet<Integer>();
			this.specificiationToSubSpecStateMap.get(x).get(specState).forEach(y -> setStates.add(y));
			eqStates.put(x, setStates);
		}
		for (String x : possibleInputs) {
			Map<String, Set<Integer>> subSpecCombo = this.invariantsMap
					.getOrDefault(new Pair<String, Integer>(x, subSpecState), new HashMap<String, Set<Integer>>());
			for (String name : eqStates.keySet()) {
				Set<Integer> setStates = subSpecCombo.getOrDefault(name, new HashSet<Integer>());
				setStates.addAll(eqStates.getOrDefault(name, new HashSet<Integer>()));
				subSpecCombo.put(name, setStates);
			}

			/*
			 * Code for monitor starts here!!!!
			 * 
			 */
			// addToMonitorInvariantsMap(input, specState, subSpecState, x);
			/*
			 * Code for monitor ends here!!!
			 * 
			 */
			this.invariantsMap.put(new Pair<String, Integer>(x, subSpecState), subSpecCombo);

		}
		return;
	}

	private void constructInvariantsFromMap(boolean needMonitor) {
		this.invars.clear();
		for (Pair<String, Integer> actionCombo : this.invariantsMap.keySet()) {
			String actionName = actionCombo.getValue0();
			String subSpecName = getNameOfSubSpecFromAction(actionName);
			Map<String, Set<Integer>> eqStateMap = this.invariantsMap.get(actionCombo);
			/*
			 * Code for monitor starts here!!!!
			 * 
			 */
			System.out.println(actionCombo);
			if (needMonitor) {
				eqStateMap.put("globalMonitor", this.monitorInvariantsMap.get(actionCombo).get("globalMonitor"));
			}
			/*
			 * Code for monitor ends here!!!
			 * 
			 */
			this.invars.add(new InvariantStatement(actionName, actionCombo.getValue1(), null, subSpecName, eqStateMap)
					.buildInvariantStatement());
		}
	}

	private List<String> getPossibleInputs(CompactDFA<String> dfa, Integer state) {
		List<String> ret = new LinkedList<String>();
		for (String in : dfa.getInputAlphabet()) {
			if (dfa.getSuccessor(state, in) != (null)) {
				ret.add(in);
			}
		}
		return ret;
	}

	private String getNameOfSubSpecFromAction(String actionName) {
		for (String subSpecName : this.subSpecificationsMap.keySet()) {
			if (this.subSpecificationsMap.get(subSpecName).getInputAlphabet().contains(actionName)) {
				return subSpecName;
			}
		}
		return null;
	}

	public static List<String> projectToAlphabet(Word<String> input, Alphabet<String> alphabet) {
		return projectToAlphabet(input.asList(), alphabet);
	}

	public static List<String> projectToAlphabet(List<String> input, Alphabet<String> alphabet) {
		List<String> projectedInputs = new LinkedList<>();
		projectedInputs.addAll(input);
		projectedInputs.retainAll(alphabet);
		return projectedInputs;
	}

	public void generateInitialMonitor() {
		this.universalMonitor = new UniversalMonitor(this.specification, null);
		this.generateMonitorGuards();
		this.monitor = null;
		this.needMonitor = true;
	}

	private void generateMonitorGuards() {
		this.monitorInvariantsMap.clear();
		for (String subSpecName : this.subSpecificationsMap.keySet()) {
			CompactDFA<String> subSpecification = this.subSpecificationsMap.get(subSpecName);
			Iterator<Word<String>> transitionCoverIterator = Covers.transitionCoverIterator(this.specification,
					this.specification.getInputAlphabet());
			while (transitionCoverIterator.hasNext()) {
				Word<String> input = transitionCoverIterator.next();
				Integer specState = this.specification.getState(input);
				if (null != specState) {
					List<String> possibleInputs = projectToAlphabet(getPossibleInputs(this.specification, specState),
							subSpecification.getInputAlphabet());
					if (possibleInputs.isEmpty()) {
						continue;
					}
					Integer subSpecState = subSpecification
							.getState(projectToAlphabet(input, subSpecification.getInputAlphabet()));
					for (String x : possibleInputs) {
						/*
						 * Code for monitor starts here!!!!
						 * 
						 */
						addToMonitorInvariantsMap(input, specState, subSpecState, x);
						/*
						 * Code for monitor ends here!!!
						 * 
						 */
					}
				}
			}
		}
		return;
	}

	/**
	 * @param input
	 * @param specState
	 * @param subSpecState
	 * @param x
	 */
	private void addToMonitorInvariantsMap(Word<String> input, Integer specState, Integer subSpecState, String x) {
		Map<String, Set<Integer>> monitorEqMap = monitorInvariantsMap
				.getOrDefault(new Pair<String, Integer>(x, subSpecState), new HashMap<String, Set<Integer>>());
		Set<Integer> monitorEqStates = monitorEqMap.getOrDefault("globalMonitor", new HashSet<Integer>());
		monitorEqStates.add(this.universalMonitor.monitorGetState(input, null));
		monitorEqMap.put("globalMonitor", monitorEqStates);
		this.monitorInvariantsMap.put(new Pair<String, Integer>(x, subSpecState), monitorEqMap);
	}

	public boolean generateNewMonitor(boolean correct) {
		this.monitor = this.universalMonitor.getNewCandidateMonitor(correct);
		if(null==this.monitor) {
			return false;
		}
//		System.out.println(this.monitor.getInitialState());
//		for (Integer x : this.monitor.getStates()) {
//			for (String y : this.monitor.getInputAlphabet()) {
//				System.out.println(x + " + " + y + " = " + this.monitor.getSuccessor(x, y));
//			}
//		}
		generateMonitorGuards();
		return true;
	}

	public Set<String> getConstraints() {
		if (!needMonitor) {
			Set<String> ret = new HashSet<String>();
			ret.addAll(invars);
			ret.addAll(unusedTransitionInvarsSet);
			return ret;
		} else {
			constructInvariantsFromMap(needMonitor);
			Set<String> ret = new HashSet<String>();
			ret.addAll(invars);
			ret.addAll(unusedTransitionInvarsSet);
			return ret;
		}
	}

	public CompactDFA<String> getMonitor() {
		return this.universalMonitor.getInitialMonitor();
	}
}
