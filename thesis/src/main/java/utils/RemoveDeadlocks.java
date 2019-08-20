/**
 * 
 */
package utils;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.fsa.DFAs;
import net.automatalib.words.Alphabet;

/**
 * @author Bharat Garhewal
 *
 */
public class RemoveDeadlocks {

	public static CompactDFA<String> removeDeadlocks(CompactDFA<String> dfa) {
		Alphabet<String> alphabet = dfa.getInputAlphabet();
		int loops = dfa.size();
		for (int i = 0; i < loops; i++) {
			for (Integer s : dfa.getStates()) {
				boolean DeadLock = true;
				for (String input : alphabet) {
					if (dfa.getSuccessor(s, input) != null) {
						DeadLock = false;
					}
				}
				if (DeadLock) {
					dfa.removeAllTransitions(s);
				}
			}
		}
		return DFAs.minimize(dfa);
	}
}
