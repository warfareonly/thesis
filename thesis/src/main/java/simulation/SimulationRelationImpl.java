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
import utils.Misc;

/**
 * @author bgarhewa
 *
 */
public class SimulationRelationImpl implements SimulationRelation {

    private Set<Pair<FastDFAState, FastNFAState>> simulationRelation = new HashSet<>();
    private FastDFA<String> specification;
    private FastNFA<String> product;
    private boolean isSimulation;
    private Pair<FastDFAState, FastNFAState> initialPair;

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

        this.simulationRelation = SimulationRestrictor.restrict(
                this.specification, this.product, this.simulationRelation);

        this.isSimulation = checkSimulation();

        System.out.println("SimRelImpl simulation relation as map: "
                + this.getSimulationRelationAsMap());
    }

    private boolean checkSimulation() {
        return this.simulationRelation.contains(this.initialPair);
    }

    private void computeRelation(FastDFA<String> specification2,
            FastNFA<String> product2) {
        // R := Q(S) X Q(T)
        Set<Pair<FastDFAState, FastNFAState>> tempRelation = cartesianProductOfStates(
                this.specification.getStates(), this.product.getStates());

        System.out.println(tempRelation);
        // R' := R
        Set<Pair<FastDFAState, FastNFAState>> tempRelationPrime = new HashSet<>(
                tempRelation);
        // System.out.println("TempRelPrime : " + tempRelationPrime.size());

        do {
            // R := R'
            tempRelation.clear();
            tempRelation = new HashSet<>(tempRelationPrime);
            // System.out.println("TempRel : " + tempRelation.size());
            Set<Pair<FastDFAState, FastNFAState>> rho = new HashSet<>();

            // The for-loop is to calculate rho
            // System.out.println(tempRelationPrime);
            // For every pair of states in R'
            for (Pair<FastDFAState, FastNFAState> pair : tempRelation) {

                Map<String, Set<FastDFAState>> succForSpecification = Neighbors
                        .getSuccessors(specification, pair.getValue0());
                Map<String, Set<FastNFAState>> succForProduct = Neighbors
                        .getSuccessors(product, pair.getValue1());

                if (!succForProduct.keySet()
                        .containsAll(succForSpecification.keySet())) {
                    // System.out.println(pair);
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
            // System.out.println("Rho : " + rho.size());
            tempRelationPrime.removeAll(rho);
        } while (!tempRelation.equals(tempRelationPrime));
        System.out.println("R : " + tempRelation);

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
    public boolean checkIfInjectiveSimulationRelation() {
        Map<FastDFAState, Set<FastNFAState>> map = this
                .getSimulationRelationAsMap();
        // for (FastDFAState x : map.keySet()) {
        // if (1 != map.get(x).size()) {
        // return false;
        // }
        // }
        List<Set<FastNFAState>> listRange = map.values().stream()
                .collect(Collectors.toList());
        if (!map.values().stream().allMatch(x -> x.size() == 1)) {
            return false;
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
}
