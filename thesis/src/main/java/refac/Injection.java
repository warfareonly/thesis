/**
 * 
 */
package refac;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.words.Word;
import utils.Misc;

/**
 * 
 * Class for check for Injection from the specification to the product. Helpful
 * for determining whether we need a monitor or not.
 * 
 * @author Bharat Garhewal
 *
 */
public class Injection {

    /**
     * Given a specification and list of files of the sub-specifications, return
     * whether a map from the set of states of the specification to the set of
     * states of the product is injective (true) or not (false).
     * 
     * @param specification
     *            specification automaton ("S")
     * @param subSpecificationsList
     *            list of files of the sub-specifications as
     *            {@literal List<String>}
     * @return boolean true (injective) or false (non-injective)
     * @throws Exception
     */
    public static boolean checkInjectionFromSpecificationToProduct(
            FastDFA<String> specification, List<String> subSpecificationsList)
            throws Exception {
        FastDFA<String> product = Product.computeProduct(subSpecificationsList);
        return checkInjectionFromSpecificationToProduct(specification, product);
    }

    /**
     * Given a specification and a product automaton, return whether a map from
     * the set of states of the specification to the set of states of the
     * product is injective (true) or not (false).
     * 
     * @param specification
     *            specification automaton ("S")
     * @param product
     *            product automaton ("P")
     * @return boolean true (injective) or false (non-injective)
     */
    private static boolean checkInjectionFromSpecificationToProduct(
            FastDFA<String> specification, FastDFA<String> product) {
        Map<String, FastDFA<String>> productMap = new HashMap<>(1);
        productMap.put("product", product);
        Map<Integer, Integer> specificationStateToProductStateMap = unwrapProductMap(
                stateEquivalence.StateEquivalence
                        .calculateEquivalentStates(specification, productMap));
        return specificationStateToProductStateMap
                .size() == specificationStateToProductStateMap.values().stream()
                        .distinct().count();
    }

    /**
     * Unwrap the complicated State-Map received from the
     * {@link stateEquivalence.StateEquivalence#calculateEquivalentStates(FastDFA, Map)}
     * method to be {@literal Map<Integer, Integer>} since we only have one
     * element (the product) in the map we receive.
     * 
     * @param specStateMap
     *            the
     *            {@link StateEquivalence#calculateEquivalentStates(FastDFA, Map)}
     *            map
     * @return a map from the set of states of the specification automaton to
     *         the set of states of the product automaton
     */
    private static Map<Integer, Integer> unwrapProductMap(
            Map<Integer, Map<String, Integer>> specStateMap) {
        Map<Integer, Integer> ret = new HashMap<Integer, Integer>(
                specStateMap.size());
        for (Integer x : specStateMap.keySet()) {
            ret.put(x, specStateMap.get(x).get("product"));
        }
        return ret;
    }

    /**
     * Produces a set of a set of words that are being "confused" by the
     * function "f". Every set in the returned set contains words that lead to
     * states mapping to the same state in the product automaton.
     * 
     * @param specification
     *            the specification dfa
     * @param subSpecificationsList
     *            the list of files of the sub-specifications
     * @return set of set of confused words
     * @throws Exception
     */
    public static Set<Set<Word<String>>> nonInjectiveAccessSequences(
            FastDFA<String> specification, List<String> subSpecificationsList)
            throws Exception {
        // The return variable
        Set<Set<Word<String>>> ret = new HashSet<>();
        FastDFA<String> product = Product.computeProduct(subSpecificationsList);
        Iterator<Word<String>> stateCoverIterator = Covers.stateCoverIterator(
                specification, specification.getInputAlphabet());

        // Map from Word -> specificationState
        Map<Word<String>, FastDFAState> specificationAccessMap = new HashMap<>();

        // Map from specificationState -> productState
        Map<FastDFAState, FastDFAState> specificationToProductMap = new HashMap<>();

        // Filling in the above two maps
        while (stateCoverIterator.hasNext()) {
            Word<String> input = stateCoverIterator.next();
            FastDFAState productState = product.getState(input);
            FastDFAState specificationState = specification.getState(input);
            specificationAccessMap.put(input, specificationState);
            specificationToProductMap.put(specificationState, productState);
        }

        // Inverse the specificationState -> productState map.
        // Since we are assuming that we have a non-injective mapping, we
        // create a map from productState -> set of specificationStates.
        Map<FastDFAState, Set<FastDFAState>> productToSpecificationMap = Misc
                .invertMapNonUnqiue(specificationToProductMap);

        // Remove the entries from the above map which map to a set of size 1.
        // We do not care about those, as they are the injective bits.
        removeUniqueEntries(productToSpecificationMap);

        // Reverse the word -> specificationState map, which is unique
        Map<FastDFAState, Word<String>> accessSpecificationMap = Misc
                .invertMapUnqiue(specificationAccessMap);

        // First, calculate the set of specificationStates mapped to a single
        // product state. Next, obtain the access words of those specification
        // states. The aforementioned access words then become a set of words
        // which are "confused". If we combine all sets of words which are
        // confused, we obtain the desired result.
        for (FastDFAState productState : productToSpecificationMap.keySet()) {
            Set<FastDFAState> specificationStatesSet = productToSpecificationMap
                    .get(productState);
            Set<Word<String>> confusedStates = new HashSet<>();
            for (FastDFAState specificationState : specificationStatesSet) {
                confusedStates
                        .add(accessSpecificationMap.get(specificationState));
            }
            ret.add(confusedStates);
        }
        return ret;
    }

    /**
     * Remove the injective entries present in a map of style key -> set of
     * values
     * 
     * @param productToSpecificationMap
     */
    private static void removeUniqueEntries(
            Map<FastDFAState, Set<FastDFAState>> productToSpecificationMap) {
        Set<FastDFAState> thingsToRemove = new HashSet<FastDFAState>();
        for (FastDFAState x : productToSpecificationMap.keySet()) {
            if (productToSpecificationMap.get(x).size() == 1) {
                thingsToRemove.add(x);
            }
        }
        thingsToRemove.forEach(x -> productToSpecificationMap.remove(x));
        return;
    }

}
