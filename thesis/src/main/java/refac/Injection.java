/**
 * 
 */
package refac;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.automatalib.automata.fsa.impl.FastDFA;

/**
 * 
 * Class for check for Injection from the specification to the product. Helpful for determining whether we need a
 * monitor or not.
 * 
 * @author Bharat Garhewal
 *
 */
public class Injection {

	/**
	 * Given a specification and list of files of the sub-specifications, return whether a map from the set of states of
	 * the specification to the set of states of the product is injective (true) or not (false).
	 * 
	 * @param specification
	 *            specification automaton ("S")
	 * @param subSpecificationsList
	 *            list of files of the sub-specifications as {@literal List<String>}
	 * @return boolean true (injective) or false (non-injective)
	 * @throws Exception
	 */
	public static boolean checkInjectionFromSpecificationToProduct(FastDFA<String> specification,
			List<String> subSpecificationsList) throws Exception {
		FastDFA<String> product = Product.computeProduct(subSpecificationsList);
		return checkInjectionFromSpecificationToProduct(specification, product);
	}

	/**
	 * Given a specification and a product automaton, return whether a map from the set of states of the specification
	 * to the set of states of the product is injective (true) or not (false).
	 * 
	 * @param specification
	 *            specification automaton ("S")
	 * @param product
	 *            product automaton ("P")
	 * @return boolean true (injective) or false (non-injective)
	 */
	private static boolean checkInjectionFromSpecificationToProduct(FastDFA<String> specification,
			FastDFA<String> product) {
		Map<String, FastDFA<String>> productMap = new HashMap<>(1);
		productMap.put("product", product);
		Map<Integer, Integer> specificationStateToProductStateMap = unwrapProductMap(
				stateEquivalence.StateEquivalence.calculateEquivalentStates(specification, productMap));
		return specificationStateToProductStateMap.size() == specificationStateToProductStateMap.values().stream()
				.distinct().count();
	}

	/**
	 * Unwrap the complicated State-Map received from the
	 * {@link StateEquivalence#calculateEquivalentStates(FastDFA, Map)} method to be {@literal Map<Integer, Integer>}
	 * since we only have one element (the product) in the map we receive.
	 * 
	 * @param specStateMap
	 *            the {@link StateEquivalence#calculateEquivalentStates(FastDFA, Map)} map
	 * @return a map from the set of states of the specification automaton to the set of states of the product automaton
	 */
	private static Map<Integer, Integer> unwrapProductMap(Map<Integer, Map<String, Integer>> specStateMap) {
		Map<Integer, Integer> ret = new HashMap<Integer, Integer>(specStateMap.size());
		for (Integer x : specStateMap.keySet()) {
			ret.put(x, specStateMap.get(x).get("product"));
		}
		return ret;
	}
}
