/**
 * 
 */
package simulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;

/**
 * This class is used to block events from the parallel composition never used
 * by the specification, as we can then avoid using the monitor states in those
 * invariant statements to begin with!
 * 
 * @author Bharat Garhewal
 *
 */
public class ParallelCompositionRequirements {

    private FastDFA<String> specification;
    private FastNFA<String> product;
    private SimulationRelationImpl simulationRelation;
    private Map<Integer, Set<Integer>> productToSpecificationMap;
    private Set<String> requirements;

    public ParallelCompositionRequirements(FastDFA<String> specification,
            FastDFA<String> product,
            Map<Integer, Map<String, Integer>> productToSubSpecificationMap) {
        this.specification = specification;
        this.product = new FastNFA<String>(
                this.specification.getInputAlphabet());
        this.product.clear();
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE, product,
                product.getInputAlphabet(), this.product);

        // Compute the simulation relation from specification to product
        this.simulationRelation = new SimulationRelationImpl(specification,
                this.product);

        // Pair of specificationState -> ProductState
        Set<Pair<Integer, Integer>> restrictedSimulationRelation = SimulationRestrictor
                .restrict(this.specification, this.product,
                        this.simulationRelation.getRelation())
                .stream()
                .map(x -> new Pair<Integer, Integer>(x.getValue0().getId(),
                        x.getValue1().getId()))
                .collect(Collectors.toSet());

        this.productToSpecificationMap = new HashMap<Integer, Set<Integer>>();
        this.product.getStates().forEach(x -> this.productToSpecificationMap
                .put(x.getId(), new HashSet<Integer>()));

        // Which state of the product maps to which states of the specification?
        for (Pair<Integer, Integer> pair : restrictedSimulationRelation) {
            Integer productState = pair.getValue1();
            Integer specificationState = pair.getValue0();
            this.productToSpecificationMap.get(productState)
                    .add(specificationState);
        }

        Map<String, Set<Integer>> transitionsToBlock = new HashMap<String, Set<Integer>>();
        // Initialize
        this.product.getInputAlphabet().forEach(
                x -> transitionsToBlock.put(x, new HashSet<Integer>()));

        // Compute which actions to block where?
        for (Entry<Integer, Set<Integer>> productSpecificationMap : this.productToSpecificationMap
                .entrySet()) {
            Integer productState = productSpecificationMap.getKey();
            Set<Integer> specificationStates = productSpecificationMap
                    .getValue();

            Set<String> unusedActions = getPossibleInputs(product,
                    productState);

            // Haha, have fun understanding this!
            // For each state, simply remove the actions which are used in the
            // specification
            specificationStates.stream()
                    .map(x -> getPossibleInputs(specification, x))
                    .forEach(x -> unusedActions.removeAll(x));

            unusedActions
                    .forEach(x -> transitionsToBlock.get(x).add(productState));
        }

        this.requirements = constructRequirements(transitionsToBlock,
                productToSubSpecificationMap);
    }

    private Set<String> constructRequirements(
            Map<String, Set<Integer>> transitionsToBlock,
            Map<Integer, Map<String, Integer>> productToSubSpecificationMap) {
        Set<String> ret = new HashSet<String>();
        for (Entry<String, Set<Integer>> eventBlockMap : transitionsToBlock
                .entrySet()) {
            String event = eventBlockMap.getKey();
            Set<Integer> productLocations = eventBlockMap.getValue();
            String requirement = "invariant " + event + " needs ";

            Iterator<Integer> prodLocIterator = productLocations.iterator();
            while (prodLocIterator.hasNext()) {
                Integer productLocation = prodLocIterator.next();

                Map<String, Integer> subSpecificationLocationMap = productToSubSpecificationMap
                        .get(productLocation);
                requirement = requirement + " ( "
                        + makeRequirementForSingleLocation(
                                subSpecificationLocationMap)
                        + " )";
                if (prodLocIterator.hasNext()) {
                    requirement = requirement + " and ";
                }
            }
            if (!productLocations.isEmpty()) {
                ret.add(requirement + " ;\n");
            }
        }
        return ret;
    }

    private String makeRequirementForSingleLocation(
            Map<String, Integer> subSpecificationLocationMap) {
        String ret = "";
        Iterator<String> subSpecificationIterator = subSpecificationLocationMap
                .keySet().iterator();
        while (subSpecificationIterator.hasNext()) {
            String subSpecName = subSpecificationIterator.next();
            Integer subSpecLocation = subSpecificationLocationMap
                    .get(subSpecName);
            ret = ret + " not " + subSpecName + ".s" + (subSpecLocation + 1)
                    + "";
            if (subSpecificationIterator.hasNext()) {
                ret = ret + " or ";
            }
        }
        ret = ret + "";
        return ret;
    }

    private static Set<String> getPossibleInputs(FastDFA<String> dfa,
            Integer currState) {
        Set<String> ret = new HashSet<>();
        for (String input : dfa.getInputAlphabet()) {
            if (null != dfa.getSuccessor(dfa.getState(currState), input)) {
                ret.add(input);
            }
        }
        return ret;
    }

    /**
     * @return the requirements
     */
    public Set<String> getRequirements() {
        return requirements;
    }
}
