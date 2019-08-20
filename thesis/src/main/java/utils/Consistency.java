/**
 * 
 */
package utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javatuples.Triplet;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.commons.util.mappings.Mapping;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.util.automata.fsa.DFAs;
import net.automatalib.words.Word;

/**
 * 
 * Class that checks for consistency between models. Specifically, checks whether the simulation relation we define from
 * the specification to the product automaton holds.
 * 
 * @author Bharat Garhewal
 *
 */
public class Consistency {

	public static CompactDFA<String> check(List<String> subSpecFiles, CompactDFA<String> specification,
			Map<String, CompactDFA<String>> subSpecificationsMap) throws Exception {
		for (Integer state : specification.getStates()) {
			for (String input : specification.getInputAlphabet()) {
				System.out.println(state + " + " + input + " = " + specification.getSuccessor(state, input));
			}
		}
		CIF3operations.parallelCompositionCIF(subSpecFiles, "product.cif");
		CompactDFA<String> product = BharatCustomCIFReader.readCIF("product.cif");
		Iterator<Word<String>> specificationStateCoverIterator = Covers.stateCoverIterator(specification,
				specification.getInputAlphabet());
		while (specificationStateCoverIterator.hasNext()) {
			Word<String> input = specificationStateCoverIterator.next();
			Integer specificationState = specification.getState(input);
			for (String action : specification.getInputAlphabet()) {
				if (null != specification.getSuccessor(specificationState, action)) {
					if (null == product.getSuccessor(product.getState(input), action)) {
						specification.removeAllTransitions(specificationState, action);
					}
				}
			}

		}
		CompactDFA<String> fixedSpecification = DFAs.minimize(
				DFAs.complete(specification, specification.getInputAlphabet()), specification.getInputAlphabet());
		for (Integer state : fixedSpecification.getStates()) {
			for (String input : fixedSpecification.getInputAlphabet()) {
				System.out.println(state + " + " + input + " = " + fixedSpecification.getSuccessor(state, input));
			}
		}
		Integer sinkState = findSinkState(fixedSpecification);
//		fixedSpecification.removeAllTransitions(sinkState);
//		for (Triplet<Integer, String, Integer> x : transitionsToSink(fixedSpecification, sinkState)) {
//			fixedSpecification.removeTransition(x.getValue0(), x.getValue1(), x.getValue2());
//		}
		FastDFA<String> out = new FastDFA<String>(specification.getInputAlphabet());
		Mapping<Integer, FastDFAState> mapping = AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, fixedSpecification,
				fixedSpecification.getInputAlphabet(), out);
		out.removeState(mapping.apply(sinkState));
		fixedSpecification = new CompactDFA<String>(specification.getInputAlphabet());
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, out, out.getInputAlphabet(), fixedSpecification);
		return fixedSpecification;
	}

	private static Integer findSinkState(CompactDFA<String> dfa) {
		Integer ret = 0;
		for (Integer state : dfa.getStates()) {
			boolean sink = true;
			for (String input : dfa.getInputAlphabet()) {
				if (!state.equals(dfa.getSuccessor(state, input))) {
					sink = false;
				}
				if (sink) {
					return state;
				}
			}
		}
		return ret;
	}

	private static Set<Triplet<Integer, String, Integer>> transitionsToSink(CompactDFA<String> dfa, Integer sinkState) {
		Set<Triplet<Integer, String, Integer>> ret = new HashSet<>();
		for (Integer state : dfa.getStates()) {
			for (String input : dfa.getInputAlphabet()) {
				if (null != dfa.getSuccessor(state, input)) {
					Integer destState = dfa.getSuccessor(state, input);
					if (sinkState.equals(destState)) {
						ret.add(new Triplet<Integer, String, Integer>(state, input, dfa.getTransition(state, input)));
					}
				}
			}
		}
		return ret;
	}

}
