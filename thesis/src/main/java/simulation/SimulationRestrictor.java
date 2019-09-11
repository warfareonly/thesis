/**
 * 
 */
package simulation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.javatuples.Pair;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.words.Word;

/**
 * @author Bharat Garhewal
 *
 */
public class SimulationRestrictor {

    public static Set<Pair<FastDFAState, FastNFAState>> restrict(
            FastDFA<String> specification, FastNFA<String> product,
            Set<Pair<FastDFAState, FastNFAState>> relation) {
        Set<Pair<FastDFAState, FastNFAState>> ret = new HashSet<>(relation);

        // Create a transition cover iterator
        Iterator<Word<String>> tcIteratorSpecification = Covers
                .transitionCoverIterator(specification,
                        specification.getInputAlphabet());

        // Variable to keep the relations we want
        Set<Pair<FastDFAState, FastNFAState>> thingsToKeep = new HashSet<>();

        // Add the initial states, as they are always necessary
        product.getInitialStates().forEach(x -> thingsToKeep
                .add(new Pair<>(specification.getInitialState(), x)));
        while (tcIteratorSpecification.hasNext()) {
            Word<String> word = tcIteratorSpecification.next();
            if (null != specification.getState(word)) {
                FastDFAState specificationState = specification.getState(word);
                Set<FastNFAState> productStates = product.getStates(word);
                productStates.forEach(x -> thingsToKeep
                        .add(new Pair<>(specificationState, x)));
            }
        }

        // Remove all relations that are not part of the relations we want
        ret.removeIf(x -> !thingsToKeep.contains(x));
        return ret;
    }

}
