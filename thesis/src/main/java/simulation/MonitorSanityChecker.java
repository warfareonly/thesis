/**
 * 
 */
package simulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.commons.io.FileUtils;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastNFA;
import utils.BharatCustomCIFReader;
import utils.BharatCustomCIFWriter;
import utils.CIF3operations;

/**
 * @author Bharat Garhewal
 *
 */
public class MonitorSanityChecker {

    /**
     * Check if the provided monitor is a good monitor or not by checking if the
     * simulation relation from S to P || M is injective. For monitor DFAs, this
     * means the relation is also deterministic (i.e., a function); for monitor
     * NFAs, good monitor means that the set of states in the range of the
     * simulation relation are disjoint.
     * 
     * @param specification
     *            the specification DFA
     * @param product
     * @param monitor
     *            the monitor, as a FastNFA, regardless of (non-)determinism
     * @return true, if injective (i.e. good monitor), false otherwise
     * @throws Exception
     */
    public static boolean checkMonitor(FastDFA<String> specification,
            FastDFA<String> product, FastNFA<String> monitor) throws Exception {
        /*
         * Combine the product with the monitor code and then send it for the
         * simulation injection checking!
         */
        writeCompositionOut(product, monitor);
        FastNFA<String> productComposition = BharatCustomCIFReader
                .readNonDetCIF("product_composition.cif");

        /*
         * End the combine the product and the monitor thingy code over here. So
         * far, we have combined the product and the monitor together and will
         * load that into the simulation relation thingy.
         */
        SimulationRelationImpl simRel = new SimulationRelationImpl(
                specification, productComposition);
        return simRel.checkIfInjectiveSimulationRelation(productComposition);
    }

    public static void writeCompositionOut(FastDFA<String> product,
            FastNFA<String> monitor) throws Exception {
        FileOutputStream stream = new FileOutputStream(
                "product_composition.cif");
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(stream))) {
            BharatCustomCIFWriter.writeCIF(product, writer, "product", true);
            stream.flush();
            BharatCustomCIFWriter.writeMonitor(monitor, writer);
            stream.flush();
        }
        stream.close();

        FileUtils.deleteQuietly(new File("product_composition_bkp.cif"));
        FileUtils.copyFile(new File("product_composition.cif"),
                new File("product_composition_bkp.cif"));

        // We have written to a file and now we have to explore the state-space
        // and load it in.
        CIF3operations.exploreStatespaceCIF("product_composition.cif", false);
        return;
    }
}
