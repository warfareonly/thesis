package simulation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javatuples.Pair;

import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;

public interface SimulationRelation {

    /**
     * Get the (greatest) simulation relation between the specification and the
     * product automata.
     * 
     * @return the simulation relation
     */
    public Set<Pair<FastDFAState, FastNFAState>> getRelation();

    /**
     * Check if there exists a (greatest) simulation relation between the
     * specification and the product automata.
     * 
     * @return <b>true</b>, if the relation exists; false other-wise.
     */
    public boolean checkIfSimulationRelationExists();

    public boolean checkIfInjectiveSimulationRelation();

    /**
     * Provides the simulation relation (sRp) as a map from s to 2^p.
     * 
     * @return the simulation relation as a map from s to the powerset of p
     */
    public Map<FastDFAState, Set<FastNFAState>> getSimulationRelationAsMap();

    boolean checkIfInjectiveSimulationRelation(
            FastNFA<String> productComposition);
}
