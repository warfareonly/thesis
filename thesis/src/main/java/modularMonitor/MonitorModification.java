/**
 * 
 */
package modularMonitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

/**
 * @author Bharat Garhewal
 *
 */
public class MonitorModification {

	public static FastDFA<String> modify(FastDFA<String> specification, Set<Word<String>> markedTransitions) {
		FastDFA<String> ret = new FastDFA<String>(specification.getInputAlphabet());
		FastDFA<String> localSpec = new FastDFA<String>(specification.getInputAlphabet());
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, specification, specification.getInputAlphabet(), localSpec);

		// Generate marked transitions map

		Map<FastDFAState, Set<String>> markedTransitionsMap = new HashMap<FastDFAState, Set<String>>();
		for (FastDFAState x : localSpec.getStates()) {
			markedTransitionsMap.put(x, new HashSet<>());
		}
		for (Word<String> word : markedTransitions) {
			Word<String> accessWord;
			if (word.length() == 0) {
				accessWord = word;
			} else if (word.length() == 1) {
				accessWord = (new WordBuilder<String>()).append(Word.epsilon()).toWord();
			} else {
				accessWord = word.subWord(0, word.length() == 0 ? 1 : word.length() - 1);
			}
			if (word.equals(Word.epsilon())) {
				continue;
			}
			String transition = word.lastSymbol();
			markedTransitionsMap.get(localSpec.getState(accessWord)).add(transition);
		}

		// Done generating
		Iterator<Word<String>> transitionCoverIterator = Covers.transitionCoverIterator(localSpec,
				localSpec.getInputAlphabet());
		while (transitionCoverIterator.hasNext()) {
			Word<String> input = transitionCoverIterator.next();
			FastDFAState state = localSpec.getState(input);
			if (null != state) {
				Word<String> accessWord;
				if (input.length() == 0) {
					accessWord = input;
				} else if (input.length() == 1) {
					accessWord = (new WordBuilder<String>()).append(Word.epsilon()).toWord();
				} else {
					accessWord = input.subWord(0, input.length() == 0 ? 1 : input.length() - 1);
				}
				if (input.equals(Word.epsilon())) {
					continue;
				}
				String transition = input.lastSymbol();
				if (!markedTransitionsMap.get(localSpec.getState(accessWord)).contains(transition)) {
					localSpec.removeAllTransitions(localSpec.getState(accessWord), transition);
				}
			}
		}
		ret = cleanMonitor(localSpec);
		assert ret != null;
		return ret;
	}

	private static FastDFA<String> cleanMonitor(FastDFA<String> localSpec) {
		Iterator<Word<String>> scI = Covers.stateCoverIterator(localSpec, localSpec.getInputAlphabet());
		while (scI.hasNext()) {
			Word<String> input = scI.next();
			FastDFAState state = localSpec.getState(input);
			if (null != state) {
				if (isDeadLocked(localSpec, input)) {
					localSpec.removeState(state, null);
				}
			}
		}
		// FastNFA<String> tempRet = new FastNFA<String>(localSpec.getInputAlphabet());
		// for (@SuppressWarnings("unused") FastDFAState s : localSpec.getStates()) {
		// tempRet.addState();
		// }
		// for (FastNFAState s : tempRet.getStates()) {
		// tempRet.setInitial(s, false);
		// }
		// Map<FastDFAState, FastNFAState> dfaToNfaMap = new HashMap<FastDFAState, FastNFAState>();
		// List<FastDFAState> listLocalSpecStates = localSpec.getStates().stream().collect(Collectors.toList());
		// List<FastNFAState> listTempRetStates = tempRet.getStates().stream().collect(Collectors.toList());
		// for (int i = 0; i < tempRet.size(); i++) {
		// dfaToNfaMap.put(listLocalSpecStates.get(i), listTempRetStates.get(i));
		// if (localSpec.isAccepting(listLocalSpecStates.get(i))) {
		// tempRet.setInitial(listTempRetStates.get(i), true);
		// }
		// }
		// for (FastNFAState s : tempRet.getStates()) {
		// tempRet.setAccepting(s, true);
		// }
		// for (int i = 0; i < listLocalSpecStates.size(); i++) {
		// FastDFAState localState = listLocalSpecStates.get(i);
		// FastNFAState nfaState = dfaToNfaMap.get(localState);
		// for (String in : localSpec.getInputAlphabet()) {
		// FastDFAState succ = localSpec.getSuccessor(localState, in);
		// if (null != succ) {
		// FastNFAState nfaSucc = dfaToNfaMap.get(succ);
		// tempRet.addTransition(nfaState, in, nfaSucc);
		// }
		// }
		// }
		CompactNFA<String> tempRet = new CompactNFA<String>(localSpec.getInputAlphabet());
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, localSpec, localSpec.getInputAlphabet(), tempRet);
		FastDFA<String> ret = new FastDFA<String>(localSpec.getInputAlphabet());
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, NFAs.determinize(tempRet, true, true),
				localSpec.getInputAlphabet(), ret);
//		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, localSpec, localSpec.getInputAlphabet(), ret);
		return ret;
	}

	private static boolean isDeadLocked(FastDFA<String> localSpec, Word<String> input) {
		FastDFAState state = localSpec.getState(input);
		for (String x : localSpec.getInputAlphabet()) {
			if (null != localSpec.getSuccessor(state, x)) {
				return false;
			}
		}
		return true;
	}
}
