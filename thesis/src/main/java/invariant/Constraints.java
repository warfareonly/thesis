/**
 * 
 */
package invariant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;

/**
 * @author Bharat Garhewal
 *
 */
public class Constraints {

	private Map<String, Map<Integer, Map<String, Set<Integer>>>> constraints;
	private Map<String, String> actionToSubSpecNames;
	private Map<String, Integer> subSpecificationSizeMap;
	private Set<String> unusedTransitionsInvariantStatements;

	/**
	 * Constructor for the constraints class
	 * 
	 * @param constraints
	 * @param actionToSubSpecNames
	 * @param subSpecificationsMap
	 */
	public Constraints(Map<String, Map<Integer, Map<String, Set<Integer>>>> constraints,
			Map<String, String> actionToSubSpecNames, Map<String, FastDFA<String>> subSpecificationsMap) {
		this.constraints = constraints;
		this.actionToSubSpecNames = actionToSubSpecNames;
		this.subSpecificationSizeMap = new HashMap<String, Integer>();
		for (String name : subSpecificationsMap.keySet()) {
			this.subSpecificationSizeMap.put(name, subSpecificationsMap.get(name).size());
		}
		this.unusedTransitionsInvariantStatements = constructUnusedTransitionsBlocker(constraints, subSpecificationsMap);
	}

	/**
	 * Transforms the constraints into a set of invariant strings. Also simplifies the constraints to remove
	 * redundancies. Since the order of the invariants does not matter, we can use an unordered container (as opposed to
	 * using an ordered one).
	 * 
	 * @return set of invariant strings
	 */
	public Set<String> constructInvariantStatements() {
		Set<String> ret = new HashSet<String>();
		for (String action : constraints.keySet()) {
			Map<Integer, Map<String, Set<Integer>>> actionConstraints = constraints.get(action);
			for (Integer state : actionConstraints.keySet()) {
				/*
				 * Simplifies the invariant. For example, if an action "a" of sub-specification "I1" is valid in ALL
				 * states of sub-specification "I2" there is no point in writing "a needs I2.s1 or I2.s2 ..." So, we
				 * remove the redundancy by simply checking if the number of states in which "a" is valid is equal to
				 * the number of states in "I2".
				 * 
				 * Of course, if "a" is valid in every single state of "I1", then that is also a kind of redundancy, in
				 * a manner of speaking, but we currently do not do anything with that.
				 */
				Map<String, Set<Integer>> actualActionStateConstraints = actionConstraints.get(state).entrySet()
						.stream().filter(x -> x.getValue().size() != subSpecificationSizeMap.get(x.getKey()))
						.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
//				System.err.println(actualActionStateConstraints);
				// Convert the constraints data structure to a set of strings.
				ret.add((new InvariantStatement(action, state, null, actionToSubSpecNames.get(action),
						actualActionStateConstraints)).buildInvariantStatement());
			}
		}
		// Add all the "unused transition" invariants as well.
		ret.addAll(unusedTransitionsInvariantStatements);
		return ret;
	}

	/**
	 * Given a map of the constraints and the subSpecName map, constructs invariant statements for the transitions which
	 * were not present in the specification. <b>E.g.</b> if an action-state combo (a,i) was never used in the
	 * specification, the method generates an invariant whereby action "a" at state "i" of the relevant
	 * sub-specification is blocked. <b> No need to call it after merging the monitor constraints to the "normal"
	 * constraints </b> Why? Left as an exercise to the reader (who is <i>clearly</i> not a reader of the report!)
	 * 
	 * @param constraints
	 *            the constraints map
	 * @param subSpecificationsMap
	 *            the sub-specification name to sub-specification map
	 * @return set of invariant statements blocking unused transitions
	 */
	private Set<String> constructUnusedTransitionsBlocker(
			Map<String, Map<Integer, Map<String, Set<Integer>>>> constraints,
			Map<String, FastDFA<String>> subSpecificationsMap) {
		/*
		 * The gist of this function is as follows: construct a set of tuples of <action, state>, that is, which action
		 * is valid at which state in a sub-specification for all actions and states. We call this the
		 * "candidate unused transitions". If the <action,state> pair is used in a constraint somewhere, then that means
		 * that the pair is actually used in the specification. In that scenario, remove the pair from the candidate
		 * set. Finally, for all remaining pairs in the set, write the unused transition invariant statement and just
		 * put them all together in a set of strings.
		 */
		Set<Pair<String, Integer>> actionStateTuples = new HashSet<>();
		for (String subSpecificationName : subSpecificationsMap.keySet()) {
			if (!subSpecificationName.equalsIgnoreCase("globalMonitor")) {
				FastDFA<String> subSpecification = subSpecificationsMap.get(subSpecificationName);
				for (FastDFAState state : subSpecification.getStates()) {
					for (String action : subSpecification.getInputAlphabet()) {
						if (null != subSpecification.getSuccessor(state, action)) {
							actionStateTuples.add(new Pair<String, Integer>(action, state.getId()));
						}
					}
				}
			}
		}
		// Construction of candidate <action,pair> set is complete
		for (String action : constraints.keySet()) {
			for (Integer state : constraints.get(action).keySet()) {
				actionStateTuples.remove(new Pair<String, Integer>(action, state));
			}
		}
		// We've removed all "used" <action, state> pairs.
		// Now, simply construct the invariants for the rest.
		Set<String> ret = new HashSet<String>();
		for (Pair<String, Integer> actionStatePair : actionStateTuples) {
			ret.add(InvariantStatement.InvariantStatementUnusedBuild(actionStatePair.getValue0(),
					actionStatePair.getValue1(), actionToSubSpecNames.get(actionStatePair.getValue0())));
		}
		return ret;
	}

	/**
	 * Getter for constraints
	 * 
	 * @return the map of constraints
	 */
	public Map<String, Map<Integer, Map<String, Set<Integer>>>> getConstraints() {
		return this.constraints;
	}

	public Constraints(Constraints monitorConstraints, Constraints memorylessConstraints,
			Map<Integer, Map<String, Integer>> equivalentStates) {

	}
}
