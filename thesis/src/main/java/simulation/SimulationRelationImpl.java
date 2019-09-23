/**
 * 
 */
package simulation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.javatuples.Pair;

import com.google.common.collect.Sets;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;

/**
 * @author Bharat Garhewal
 *
 */
public class SimulationRelationImpl implements SimulationRelation {

    private Set<Pair<FastDFAState, FastNFAState>> simulationRelation = new HashSet<>();
    private FastDFA<String> specification;
    private FastNFA<String> product;
    private boolean isSimulation;
    private Pair<FastDFAState, FastNFAState> initialPair;
    private Set<Pair<FastDFAState, FastNFAState>> simulationRelationUnRestricted = new HashSet<>();

    public SimulationRelationImpl(FastDFA<String> specification,
            FastNFA<String> product) {
        assert product.getInitialStates().size() == 1;

        this.specification = specification;
        this.product = product;

        FastDFAState specificationInitialState = specification
                .getInitialState();
        FastNFAState productInitialState = (FastNFAState) product
                .getInitialStates().toArray()[0];
        this.initialPair = new Pair<>(specificationInitialState,
                productInitialState);

        computeRelation(this.specification, this.product);

        this.simulationRelationUnRestricted = new HashSet<>(
                this.simulationRelation);

        this.simulationRelation = SimulationRestrictor.restrict(
                this.specification, this.product, this.simulationRelation);

        this.isSimulation = checkSimulation();
    }

    private boolean checkSimulation() {
        return this.simulationRelation.contains(this.initialPair);
    }

    private void computeRelation(FastDFA<String> specification2,
            FastNFA<String> product2) {
        // R := Q(S) X Q(T)
        Set<Pair<FastDFAState, FastNFAState>> tempRelation = cartesianProductOfStates(
                this.specification.getStates(), this.product.getStates());

        // R' := R
        Set<Pair<FastDFAState, FastNFAState>> tempRelationPrime = new HashSet<>(
                tempRelation);

        do {
            // R := R'
            tempRelation.clear();
            tempRelation = new HashSet<>(tempRelationPrime);

            Set<Pair<FastDFAState, FastNFAState>> rho = new HashSet<>();

            // The for-loop is to calculate rho
            // For every pair of states in R'
            for (Pair<FastDFAState, FastNFAState> pair : tempRelation) {

                Map<String, Set<FastDFAState>> succForSpecification = Neighbors
                        .getSuccessors(specification, pair.getValue0());
                Map<String, Set<FastNFAState>> succForProduct = Neighbors
                        .getSuccessors(product, pair.getValue1());

                if (!succForProduct.keySet()
                        .containsAll(succForSpecification.keySet())) {
                    rho.add(pair);
                } else {
                    for (String inputSpec : succForSpecification.keySet()) {
                        Set<FastDFAState> specificationSuccessor = succForSpecification
                                .get(inputSpec);
                        Set<FastNFAState> productSuccessor = succForProduct
                                .get(inputSpec);

                        for (FastDFAState specState : specificationSuccessor) {
                            for (FastNFAState productState : productSuccessor) {
                                if (!tempRelation.contains(
                                        new Pair<FastDFAState, FastNFAState>(
                                                specState, productState))) {
                                    rho.add(pair);
                                }
                            }
                        }
                    }
                }
            }
            tempRelationPrime.removeAll(rho);
        } while (!tempRelation.equals(tempRelationPrime));

        for (Pair<FastDFAState, FastNFAState> x : tempRelation) {
            this.simulationRelation
                    .add(new Pair<>(x.getValue0(), x.getValue1()));
        }
        return;
    }

    /**
     * Compute the cartesian product of the specification and product states,
     * given the two collections of the states
     * 
     * @param specificationStates
     * @param productStates
     * @return
     */
    private Set<Pair<FastDFAState, FastNFAState>> cartesianProductOfStates(
            Collection<FastDFAState> specificationStates,
            Collection<FastNFAState> productStates) {
        Set<Pair<FastDFAState, FastNFAState>> ret = new HashSet<>(
                specificationStates.size() * productStates.size());

        List<Set<Object>> interimList = new LinkedList<>();
        interimList
                .add(specificationStates.stream().collect(Collectors.toSet()));
        interimList.add(productStates.stream().collect(Collectors.toSet()));

        Set<List<Object>> temp = Sets.cartesianProduct(interimList);
        temp.forEach(x -> ret.add(new Pair<FastDFAState, FastNFAState>(
                (FastDFAState) x.get(0), (FastNFAState) x.get(1))));
        return ret;
    }

    /*
     * (non-Javadoc)
     * 
     * @see simulation.SimulationRelation#checkIfSimulationRelation()
     */
    @Override
    public boolean checkIfSimulationRelationExists() {
        return this.isSimulation;
    }

    @Override
    public boolean checkIfInjectiveSimulationRelation(
            FastNFA<String> productComposition) {
        Map<FastDFAState, Set<FastNFAState>> map = this
                .getSimulationRelationAsMap();
        for (FastDFAState specificationSourceState : this.specification
                .getStates()) {
            for (String input : this.specification.getInputAlphabet()) {
                FastDFAState specificationDestinationState = this.specification
                        .getSuccessor(specificationSourceState, input);
                if (null != specificationDestinationState) {
                    Set<FastNFAState> setProductSourceStates = map
                            .get(specificationSourceState);
                    Set<FastNFAState> setProductDestinationStates = setProductSourceStates
                            .stream()
                            .map(st -> productComposition.getSuccessors(st,
                                    input))
                            .flatMap(c -> c.stream())
                            .collect(Collectors.toSet());
                    if (!map.get(specificationDestinationState)
                            .equals(setProductDestinationStates)) {
                        return false;
                    }
                }
            }
        }
        
        // if (!map.values().stream().allMatch(x -> x.size() == 1)) {
        // return false;
        // }
        List<Set<FastNFAState>> listRangeTemp = map.values().stream()
                .collect(Collectors.toList());
        List<Set<Integer>> listRange = new LinkedList<>();
        for (Set<FastNFAState> x : listRangeTemp) {
            Set<Integer> states = new HashSet<>(x.size());
            x.forEach(i -> states.add(i.getId()));
            listRange.add(states);
        }
        for (int i = 0; i < listRange.size(); i++) {
            for (int j = i + 1; j < listRange.size(); j++) {
                if (!Collections.disjoint(listRange.get(i), listRange.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see simulation.SimulationRelation#getRelation()
     */
    @Override
    public Set<Pair<FastDFAState, FastNFAState>> getRelation() {
        return this.simulationRelation;
    }

    /*
     * (non-Javadoc)
     * 
     * @see simulation.SimulationRelation#getRelation()
     */
    // @Override
    public Set<Pair<FastDFAState, FastNFAState>> getUnrestrictedRelation() {
        return this.simulationRelationUnRestricted;
    }

    @Override
    public Map<FastDFAState, Set<FastNFAState>> getSimulationRelationAsMap() {
        Map<FastDFAState, Set<FastNFAState>> ret = new HashMap<>(
                this.specification.size());

        // Initialize the map
        this.simulationRelation
                .forEach(x -> ret.put(x.getValue0(), new HashSet<>()));

        // Load the values
        for (Pair<FastDFAState, FastNFAState> pair : this.simulationRelation) {
            ret.get(pair.getValue0()).add(pair.getValue1());
        }
        // Loading finished, return it now
        return ret;
    }

    @Override
    public boolean checkIfInjectiveSimulationRelation() {
        // TODO Auto-generated method stub
        return false;
    }
}
