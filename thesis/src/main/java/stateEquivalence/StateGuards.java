/**
 * 
 */
package stateEquivalence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import utils.Misc;

/**
 * Class for the computation of state guards.
 * 
 * @author Bharat Garhewal
 *
 */
public class StateGuards {

	/**
	 * Generate state-guards for the given specification and sub-specifications.
	 * 
	 * @param specification
	 * @param subSpecificationNamesMap
	 * @return
	 */
	public static Map<String, Map<Integer, Map<String, Set<Integer>>>> execStateGuards(CompactDFA<String> specification,
			Map<String, CompactDFA<String>> subSpecificationNamesMap) {
		Map<Integer, Map<String, Integer>> equivalentStateMap = StateEquivalence
				.calculateEquivalentStates(specification, subSpecificationNamesMap);
		return computeStateGuards(specification, subSpecificationNamesMap, equivalentStateMap);
	}

	/**
	 * Constructs the state-guards of the specification, from the perspective of an action. Basically, the result
	 * answers the question: which states from which sub-specifications does any arbitrary combination of an action and
	 * a state need? (Within the limits as allowed by the (sub-)specification(s))
	 * 
	 * @param specification
	 *            the specification automaton, as a {@literal CompactDFA<String>}
	 * @param subSpecificationsMap
	 *            the map of name to sub-specification automaton
	 * @param equivalentStateMaps
	 *            map from state of specification to map of states of the sub-specifications.
	 * @return
	 */
	public static Map<String, Map<Integer, Map<String, Set<Integer>>>> computeStateGuards(
			CompactDFA<String> specification, Map<String, CompactDFA<String>> subSpecificationsMap,
			Map<Integer, Map<String, Integer>> equivalentStateMaps) {
		Map<String, String> actionToSubSpecificationNameMap = Misc.computeActionToSubSpecNames(subSpecificationsMap);
		Map<String, Map<Integer, Map<String, Set<Integer>>>> ret = new HashMap<String, Map<Integer, Map<String, Set<Integer>>>>();

		// Initializes the entire map, as Java insists on it...
		// Makes sense, but still!
		ret = initializeConstraintsMap(ret, specification, subSpecificationsMap, equivalentStateMaps,
				actionToSubSpecificationNameMap);
		// After we initialize the entire data-structure, we need not "put" back in a modification ever,
		// as the "getOrDefault" method does not return a view if the value is present.

		for (Integer state : specification.getStates()) {
			for (String action : specification.getInputAlphabet()) {
				if (null != specification.getSuccessor(state, action)) {
					String subSpecificationName = actionToSubSpecificationNameMap.get(action);
					Map<Integer, Map<String, Set<Integer>>> constraintsMap = ret.getOrDefault(action, new HashMap<>());
					Map<String, Integer> equivalentStates = equivalentStateMaps.get(state);
					Map<String, Set<Integer>> cons = constraintsMap.getOrDefault(
							equivalentStates.get(subSpecificationName), new HashMap<String, Set<Integer>>());
					for (String subSpecName : subSpecificationsMap.keySet()) {
						if (!subSpecName.contentEquals(subSpecificationName)) {
							Set<Integer> equiSet = cons.getOrDefault(subSpecName, new HashSet<>());
							equiSet.add(equivalentStates.get(subSpecName));
						}
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Initializes the entire data structure, as I do not like putting in a bunch of if-statements.
	 * 
	 * @param ret
	 * @param specification
	 * @param subSpecificationsMap
	 * @param equivalentStateMaps
	 * @param actionToSubSpecificationNameMap
	 * @return
	 */
	private static Map<String, Map<Integer, Map<String, Set<Integer>>>> initializeConstraintsMap(
			Map<String, Map<Integer, Map<String, Set<Integer>>>> ret, CompactDFA<String> specification,
			Map<String, CompactDFA<String>> subSpecificationsMap,
			Map<Integer, Map<String, Integer>> equivalentStateMaps,
			Map<String, String> actionToSubSpecificationNameMap) {
//		System.out.println(actionToSubSpecificationNameMap);
		for (Integer state : specification.getStates()) {
			for (String action : specification.getInputAlphabet()) {
				if (null != specification.getSuccessor(state, action)) {
//					System.out.println(action);
					String subSpecName = actionToSubSpecificationNameMap.get(action);
//					System.out.println(equivalentStateMaps + " " + state + " " + subSpecName);
					Integer subSpecState = equivalentStateMaps.get(state).get(subSpecName);
					Map<Integer, Map<String, Set<Integer>>> actionConstraints = ret.getOrDefault(action,
							new HashMap<Integer, Map<String, Set<Integer>>>());
					Map<String, Set<Integer>> actionStateConstraints = actionConstraints.getOrDefault(subSpecState,
							new HashMap<String, Set<Integer>>());
					for (String x : subSpecificationsMap.keySet()) {
//						System.out.println(x + " " + subSpecName);
						if (!x.contentEquals(subSpecName)) {
							actionStateConstraints.put(x, new HashSet<Integer>());
						}
					}
					actionConstraints.put(subSpecState, actionStateConstraints);
					ret.put(action, actionConstraints);
				}
			}
		}
		return ret;
	}

}
