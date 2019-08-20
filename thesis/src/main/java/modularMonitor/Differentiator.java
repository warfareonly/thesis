/**
 * 
 */
package modularMonitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import algorithmicMonitor.Reachability;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.commons.util.mappings.Mapping;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.words.Word;

/**
 * @author Bharat Garhewal
 *
 */
public class Differentiator {

	private FastDFA<String> specification;
	private Map<Integer, List<FastDFAState>> fMap = new HashMap<>();
	private Map<FastDFAState, Word<String>> stateToAccessSeqWord = new HashMap<>();

	public Differentiator(CompactDFA<String> specification, Map<Integer, List<Integer>> fMap) {
		this.specification = new FastDFA<String>(specification.getInputAlphabet());
		Mapping<Integer, FastDFAState> stateMapping = AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, specification,
				specification.getInputAlphabet(), this.specification);
		this.fMap = filterMap(fMap, stateMapping);
		this.stateToAccessSeqWord = accessSequenceGenerator();
	}

	private Map<Integer, List<FastDFAState>> filterMap(Map<Integer, List<Integer>> fMap,
			Mapping<Integer, FastDFAState> stateMapping) {
		Map<Integer, List<FastDFAState>> ret = new HashMap<Integer, List<FastDFAState>>();
		for (Integer x : fMap.keySet()) {
			if (fMap.get(x).size() > 1) {
				List<FastDFAState> d = new LinkedList<>();
				fMap.get(x).forEach(i -> d.add(stateMapping.apply(i)));
				ret.put(x, d);
			}
		}
		return ret;
	}

	public Map<List<Integer>, CompactDFA<String>> generateMonitors() {
		Map<List<FastDFAState>, FastDFA<String>> ret = new HashMap<List<FastDFAState>, FastDFA<String>>();
		for (Integer state : this.fMap.keySet()) {
			List<FastDFAState> confusedStates = this.fMap.get(state);
			List<Word<String>> confusedStatesAccessSeqs = confusedStates.stream()
					.map(x -> this.stateToAccessSeqWord.get(x)).collect(Collectors.toList());
			MonitorGenerator monGen = new MonitorGenerator(this.specification, confusedStatesAccessSeqs);
			ret.put(confusedStates, monGen.getMonitor());
		}
		return generateMonitors(ret);
	}

	private Map<List<Integer>, CompactDFA<String>> generateMonitors(
			Map<List<FastDFAState>, FastDFA<String>> fastMonitors) {
		Map<List<Integer>, CompactDFA<String>> ret = new HashMap<List<Integer>, CompactDFA<String>>();
		Integer i = 0;
		for (List<FastDFAState> listStates : fastMonitors.keySet()) {
			FastDFA<String> fastMonitor = fastMonitors.get(listStates);
			CompactDFA<String> compactMonitor = new CompactDFA<String>(fastMonitor.getInputAlphabet());
			Mapping<FastDFAState, Integer> mapping = AutomatonLowLevelCopy.copy(AutomatonCopyMethod.BFS, fastMonitor,
					fastMonitor.getInputAlphabet(), compactMonitor);
			List<Integer> x = new LinkedList<>();
			x.add(i++);
			ret.put(x, compactMonitor);
		}
		return ret;
	}

	private Map<FastDFAState, Word<String>> accessSequenceGenerator() {
		Map<FastDFAState, Word<String>> ret = new HashMap<>(this.specification.size());
		Iterator<Word<String>> stateCoverIterator = Covers.stateCoverIterator(this.specification,
				this.specification.getInputAlphabet());
		while (stateCoverIterator.hasNext()) {
			Word<String> input = stateCoverIterator.next();
			FastDFAState state = this.specification.getState(input);
			if (null != state) {
				ret.put(state, input);
			}
		}
		assert ret.size() > 0;
		return ret;
	}
}