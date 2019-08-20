/**
 * 
 */
package modularMonitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.words.Word;

/**
 * @author Bharat Garhewal
 *
 */
public class MonitorGenerator {

	private FastDFA<String> specification;
	private List<Word<String>> confusedStateAccessSeqs;
	private List<FastDFAState> confusedStates = new LinkedList<FastDFAState>();
	private Set<Word<String>> markedTransitions;
	private FastDFA<String> monitor;

	public MonitorGenerator(FastDFA<String> specification, List<Word<String>> confusedStatesAccessSeqs) {
		this.specification = new FastDFA<String>(specification.getInputAlphabet());
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, specification, specification.getInputAlphabet(),
				this.specification);
		extractStateFromAccessSequences(confusedStatesAccessSeqs);
		this.confusedStateAccessSeqs = confusedStatesAccessSeqs.stream().collect(Collectors.toList());
		this.markedTransitions = this.markTransitions();
		this.monitor = MonitorModification.modify(this.specification, this.markedTransitions);
	}

	private Set<Word<String>> markTransitions() {
		Set<Word<String>> ret = new HashSet<Word<String>>();
		for (Word<String> x : confusedStateAccessSeqs) {
			ret.addAll(markTranstionsForState(x));
		}
		return ret;
	}

	private Set<Word<String>> markTranstionsForState(Word<String> stateAccessSequence) {
		FastDFA<String> localDFA = new FastDFA<String>(this.specification.getInputAlphabet());
		AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, this.specification, this.specification.getInputAlphabet(),
				localDFA);
		FastDFAState initState = localDFA.getState(stateAccessSequence);
		List<FastDFAState> confusedStatesForLocalDFA = new LinkedList<>();
		for (Word<String> x : this.confusedStateAccessSeqs) {
			confusedStatesForLocalDFA.add(localDFA.getState(x));
		}
		localDFA.setInitialState(null);
		localDFA.setInitialState(initState);
		Set<Word<String>> ret = new HashSet<Word<String>>();
		Iterator<Word<String>> transitionCoverIterator = Covers.transitionCoverIterator(localDFA,
				localDFA.getInputAlphabet());
		while (transitionCoverIterator.hasNext()) {
			Word<String> input = transitionCoverIterator.next();
			FastDFAState state = localDFA.getState(input);
			if (null != state) {
				if (confusedStatesForLocalDFA.contains(state)) {
					ret.addAll(generatePrefixes(stateAccessSequence.concat(input)));
				}
			}
		}
		return ret;
	}

	private void extractStateFromAccessSequences(List<Word<String>> confusedStatesAccessSeqs) {
		for (Word<String> x : confusedStatesAccessSeqs) {
			confusedStates.add(this.specification.getState(x));
		}
	}

	public FastDFA<String> getMonitor() {
		return this.monitor;
	}

	private Set<Word<String>> generatePrefixes(Word<String> word) {
		Set<Word<String>> ret = new HashSet<>();
		for (int i = 0; i <= word.size(); i++) {
			ret.add(word.subWord(0, i));
		}
		return ret;
	}
}