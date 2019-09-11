/**
 * 
 */
package simulation;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import utils.BharatCustomCIFReader;
import utils.CIF3operations;
import utils.CIFWriter;
import utils.Misc;

/**
 * @author Bharat Garhewal
 *
 */
public class SimulationSanityChecker {

    private Map<FastDFAState, FastNFAState> mapSimRelSpecToProd;
    private Map<Set<FastDFAState>, FastNFAState> confusionSet;
    private FastDFA<String> product;

    public SimulationSanityChecker(
            Map<FastDFAState, Set<FastNFAState>> mapSimRelSpecProduct,
            FastDFA<String> prod) {
        this.mapSimRelSpecToProd = simplifyDeterministicRelationToFunction(
                mapSimRelSpecProduct);

        this.product = prod;

        // Confusion set contains sets of confused states. Each set of confused
        // states maps to a single state in the product automaton. The task of
        // the sanity checker is that the simulated states of the confused
        // states are disjoint!
        this.confusionSet = computeConfusedMap();
    }

    private Map<FastDFAState, FastNFAState> simplifyDeterministicRelationToFunction(
            Map<FastDFAState, Set<FastNFAState>> mapSimRelSpecProduct) {
        Map<FastDFAState, FastNFAState> ret = new HashMap<>(
                mapSimRelSpecProduct.size());
        for (FastDFAState key : mapSimRelSpecProduct.keySet()) {
            FastNFAState value = (FastNFAState) mapSimRelSpecProduct.get(key)
                    .toArray()[0];
            ret.put(key, value);
        }
        return ret;
    }

    private Map<Set<FastDFAState>, FastNFAState> computeConfusedMap() {
        Map<Set<FastDFAState>, FastNFAState> ret = new HashMap<>();

        // First, we need to invert the mapSimRelSpecToProduct map
        Map<FastNFAState, Set<FastDFAState>> invertedMap = Misc
                .invertMapNonUnqiue(this.mapSimRelSpecToProd);

        // Then, simply reverse the inverted map!
        ret = Misc.invertMapUnqiue(invertedMap);
        return ret;
    }

    public boolean checkSanity(FastNFA<String> monitor) throws Exception {
        FileOutputStream stream = new FileOutputStream("product_monitor.cif");
        CIFWriter.writeCIF(this.product, stream, "product", true);
        CIFWriter.writeMonitor(monitor, stream, "monitor", true);

        CIF3operations.exploreStatespaceCIF("product_monitor.cif");

        FastDFA<String> productMonitorComposed = BharatCustomCIFReader
                .readCIF("product_monitor.cif");
        return false;
    }
}
