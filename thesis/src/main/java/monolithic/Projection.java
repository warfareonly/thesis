/**
 * 
 */
package monolithic;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.SimpleAlphabet;

/**
 * @author bgarhewa
 *
 */
public class Projection {

	CompactDFA<String> model = new CompactDFA<String>(new SimpleAlphabet<String>());
	List<CompactDFA<String>> listOfModels = new LinkedList<CompactDFA<String>>();

	public Projection(CompactDFA<String> x) {
		this.model = x;
	}

	public static List<String> getPossibleInputs(CompactDFA<String> dfa, List<String> acessSequenceSpecification) {
		if (acessSequenceSpecification.equals(null)) {
			return new LinkedList<String>();
		}
		List<String> access = new LinkedList<>();
		access.addAll(acessSequenceSpecification);
		access.retainAll(dfa.getInputAlphabet());
		System.out.println(access);
		Integer accState = dfa.getState(access);
		List<String> ret = new LinkedList<String>();
		for (String in : dfa.getInputAlphabet()) {
			if (dfa == null) {
				System.out.println("DFA is null!!");
			}
			if (dfa.getSuccessor(accState, in) != null) {
				ret.add(in);
			}
		}
		return ret;
	}

	public static CompactDFA<String> fixModel(String sync_action, CompactDFA<String> dfa, List<String> inputsNotInSpec,
			List<String> inputsInSpec, List<String> accessSeq) {
		System.out.println("Access Seq:");
		System.out.println(accessSeq);
		dfa.addAlphabetSymbol(sync_action);
		List<String> access = new LinkedList<>();
		access.addAll(accessSeq);
		access.retainAll(dfa.getInputAlphabet());
		Integer state = dfa.getState(access);
		inputsInSpec.retainAll(dfa.getInputAlphabet());
		inputsNotInSpec.retainAll(dfa.getInputAlphabet());
		dfa = fixPossible(sync_action, dfa, inputsInSpec, access);
		try {
			dfa = fixImpossible(sync_action, dfa, inputsNotInSpec, access);
		} catch (Exception e) {
			System.out.println("Error again in fixImpossible");
			e.printStackTrace();
		}
		return dfa;
	}

	private static CompactDFA<String> fixPossible(String sync_action, CompactDFA<String> dfa, List<String> input,
			List<String> stateAccessSeq) {
		Integer newState = dfa.addState(true);
		Integer oldState = dfa.getState(stateAccessSeq);
		for (String x : input) {
			Integer destState = dfa.getSuccessor(oldState, x);
			dfa.removeTransition(oldState, x, dfa.getTransition(oldState, x));
			dfa.addTransition(oldState, x, newState, null);
			dfa.addTransition(newState, sync_action, destState, null);
		}
		return dfa;
	}

	private static CompactDFA<String> fixImpossible(String sync_action, CompactDFA<String> dfa, List<String> input,
			List<String> stateAccessSeq) {
		System.out.println(input);
		System.out.println(stateAccessSeq);
		// Integer destStateNum = dfa.getState(stateAccessSeq.isEmpty() ? : stateAccessSeq);
		Integer destStateNum;
		System.out.println("Is the DFA null?: " + dfa == null);
		if (stateAccessSeq.isEmpty()) {
			destStateNum = dfa.getInitialState();
		} else {
			destStateNum = dfa.getState(stateAccessSeq);
		}
		Integer srcStateNum = dfa.addState(true);
		Map<Integer, String> predecessors = getPredecessors(dfa, destStateNum);
		Map<Integer, String> successors = getSuccessors(dfa, destStateNum);
		dfa.removeAllTransitions(destStateNum);
		predecessors.forEach((k, v) -> dfa.addTransition(k, v, srcStateNum, null));
		successors.forEach((k, v) -> dfa.addTransition(destStateNum, v, k, null));
		dfa.addTransition(srcStateNum, sync_action, destStateNum, null);
		return dfa;
	}

	public static List<String> getPossibleInputs(CompactDFA<String> dfa, Integer state) {
		List<String> ret = new LinkedList<String>();
		for (String in : dfa.getInputAlphabet()) {
			if (dfa.getSuccessor(state, in) != (null)) {
				ret.add(in);
			}
		}
		return ret;
	}

	public static Map<Integer, String> getSuccessors(CompactDFA<String> dfa, Integer state) {
		Map<Integer, String> ret = new HashMap<>();
		for (String input : dfa.getInputAlphabet()) {
			if (dfa.getSuccessor(state, input) != null) {
				ret.put(dfa.getSuccessor(state, input), input);
			}
		}
		return ret;
	}

	public static Map<Integer, String> getPredecessors(CompactDFA<String> dfa, Integer state) {
		Map<Integer, String> ret = new HashMap<>();
		for (Integer s : dfa.getStates()) {
			for (String input : dfa.getInputAlphabet()) {
				if (dfa.getSuccessor(s, input) != null) {
					if (dfa.getSuccessor(s, input).equals(state)) {
						ret.put(s, input);
					}
				}
			}
		}
		return ret;
	}

}
