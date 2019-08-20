/**
 * 
 */
package algorithmicMonitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.commons.util.mappings.Mapping;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.words.Word;
import net.automatalib.words.impl.SimpleAlphabet;

/**
 * @author Bharat Garhewal
 *
 */
public class Reachability {

	private FastDFA<String> fDFA = new FastDFA<String>(new SimpleAlphabet<String>());
	private Mapping<FastDFAState, FastDFAState> oldToNewMapping;
	private Map<FastDFAState, FastDFAState> newToOldMap = new HashMap<FastDFAState, FastDFAState>();

	public Reachability(FastDFA<String> x) {
		fDFA = new FastDFA<String>(x.getInputAlphabet());
		oldToNewMapping = AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, x, x.getInputAlphabet(), fDFA);
		reverseMapping(oldToNewMapping, x.getStates());
	}

	public Map<Word<String>, List<Word<String>>> computeReachability() {
		Map<Word<String>, List<Word<String>>> ret = new HashMap<>();
		Iterator<Word<String>> stateCoverIterator = Covers.stateCoverIterator(fDFA, fDFA.getInputAlphabet());
		while (stateCoverIterator.hasNext()) {
			Word<String> input = stateCoverIterator.next();
			FastDFAState state = fDFA.getState(input);
			if (null != state) {
				ret.put(input, getReachableStates(input, fDFA));
			}
		}
		return ret;
	}

	private List<Word<String>> getReachableStates(Word<String> input, FastDFA<String> fDFA2) {
		List<Word<String>> ret = new LinkedList<>();
		FastDFA<String> dfa = new FastDFA<String>(fDFA2.getInputAlphabet());
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, fDFA2, fDFA2.getInputAlphabet(), dfa);
		FastDFAState st = dfa.getState(input);
		// for (FastDFAState x : dfa.getStates()) {
		// dfa.setInitial(x, false);
		// dfa.set
		// }
		dfa.setInitialState(null);
		dfa.setInitialState(st);
		Iterator<Word<String>> stateIterator = Covers.stateCoverIterator(dfa, dfa.getInputAlphabet());
		while (stateIterator.hasNext()) {
			Word<String> in = stateIterator.next();
			FastDFAState state = dfa.getState(in);
			if (null != state) {
				ret.add(in);
			}
		}
		return ret;
	}

	private void reverseMapping(Mapping<FastDFAState, FastDFAState> oldToNewMapping2,
			Collection<FastDFAState> collection) {
		for (FastDFAState x : collection) {
			newToOldMap.put(oldToNewMapping2.get(x), x);
		}
		return;
	}
}
