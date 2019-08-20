/**
 * 
 */
package stateEquivalence;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

/**
 * @author bgarhewa
 *
 */
public class StateEquivalence {

	/**
	 * Provides a map from a state in the specification to a map of the name of the sub-specification and the equivalent
	 * state in that sub-specification.
	 * 
	 * @param specification
	 *            The specification {@link CompactDFA} of "String"
	 * @param mapSubSpecifications
	 *            The subSpecifications of type {@link Map}, indexed by a <i>name</i>
	 * @return a map of type integer to a map of type string to integer
	 */
	public static Map<Integer, Map<String, Integer>> calculateEquivalentStates(CompactDFA<String> specification,
			Map<String, CompactDFA<String>> mapSubSpecifications) {
		Map<Integer, Map<String, Integer>> ret = new HashMap<>();
		Iterator<Word<String>> transitionCoverIterator = Covers.stateCoverIterator(specification,
				specification.getInputAlphabet()); // Compute the transition cover
		while (transitionCoverIterator.hasNext()) {
			Word<String> input = transitionCoverIterator.next();
//			System.out.println("Transition: " + input);
			Integer state = specification.getState(input);
			// We need the condition to ignore the generated "transitions" which do not end
			// at a state.
			if (null != state) {
				Set<String> subSpecificationNames = mapSubSpecifications.keySet();
				Map<String, Integer> mapForState = new HashMap<String, Integer>();
				for (String subSpecName : subSpecificationNames) {
					CompactDFA<String> subSpecification = mapSubSpecifications.get(subSpecName);
					Integer subSpecficiationState = getSubSpecificationState(input, subSpecification);
					mapForState.put(subSpecName, subSpecficiationState);
				}
				ret.put(state, mapForState);
			}
		}
		// Hard-coding for the initial state
		Map<String, Integer> initialStateMap = new HashMap<>();
		for(String x : mapSubSpecifications.keySet()) {
			initialStateMap.put(x, 0);
		}
		ret.put(0, initialStateMap);
		return ret;
	}

	/**
	 * Given an input sequence and a DFA, returns the final state of the DFA over the projected input.
	 * 
	 * @param input
	 *            : A {@link Word} of type "String"
	 * @param subSpecification
	 *            : A {@link CompactDFA} of type "String"
	 * @return finalState Integer
	 */
	private static Integer getSubSpecificationState(Word<String> input, CompactDFA<String> subSpecification) {
		List<String> projectedInput = new LinkedList<>();
		Alphabet<String> subSpecificationAlphabet = subSpecification.getInputAlphabet();
		input.forEach(x -> {
			if (subSpecificationAlphabet.contains(x)) {
				projectedInput.add(x);
			}
		});
		return subSpecification.getState(projectedInput);
	}

}
