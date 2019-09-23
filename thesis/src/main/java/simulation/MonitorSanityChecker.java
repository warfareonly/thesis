/**
 * 
 */
package simulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.words.Word;
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
        // TODO Combine the monitor computed with the product automaton.
        // TODO Implement the thingy for NFAs, right now it is applicable only
        // for DFAs. Will probably be difficult, but that is your lot in life!

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
        System.out.println("Merge was okay: "
                + (simRel.checkIfInjectiveSimulationRelation(productComposition)));
        // && specificationAccessCheck(specification, monitor)));
        System.out.println(simRel.getRelation());
        // Map<Integer, Set<Word<String>>> specificationRefinedStatesTraces =
        // specificationRefinementTracesGenerator(
        // specification, product);
        return simRel.checkIfInjectiveSimulationRelation(productComposition)
                // && specificationAccessCheck(specification, monitor)
                // && specificationCompositionCheck(specification, monitor,
                // specificationRefinedStatesTraces)
                && true;
    }
    //
    // private static boolean specificationCompositionCheck(
    // FastDFA<String> specification, FastNFA<String> monitor,
    // Map<Integer, Set<Word<String>>> specificationRefinedStatesTraces) {
    // for (Integer specificationRefinedState : specificationRefinedStatesTraces
    // .keySet()) {
    // Set<Word<String>> accessWordsSet = specificationRefinedStatesTraces
    // .get(specificationRefinedState);
    //
    // Set<Set<Integer>> endStatesSet = new HashSet<Set<Integer>>();
    // for (Word<String> acWord : accessWordsSet) {
    // Set<Integer> endStatesAcWord = monitor.getStates(acWord)
    // .stream().map(x -> x.getId())
    // .collect(Collectors.toSet());
    // if (endStatesAcWord.size() != 1) {
    // return false;
    // }
    // endStatesSet.add(endStatesAcWord);
    // }
    // if (endStatesSet.size() != 1) {
    // return false;
    // }
    // }
    // System.out.println(specificationRefinedStatesTraces);
    // return true;
    // }
    //
    // private static Map<Integer, Set<Word<String>>>
    // specificationRefinementTracesGenerator(
    // FastDFA<String> specification, FastDFA<String> product) {
    // Map<Integer, Set<Word<String>>> ret = new HashMap<>();
    // Iterator<Word<String>> tcIterator = Covers.transitionCoverIterator(
    // specification, specification.getInputAlphabet());
    //
    // Map<Integer, Set<Integer>> productToSpecificationMap = new HashMap<>();
    // {
    // Map<Integer, Set<Integer>> tempMap = new HashMap<>();
    // for (FastDFAState p : product.getStates()) {
    // tempMap.put(p.getId(), new HashSet<Integer>());
    // }
    // while (tcIterator.hasNext()) {
    // Word<String> word = tcIterator.next();
    // FastDFAState specificationState = specification.getState(word);
    // if (null != specificationState) {
    // FastDFAState productState = product.getState(word);
    // tempMap.get(productState.getId())
    // .add(specificationState.getId());
    // }
    // }
    //
    // // Remove all states which are already injective
    // for (Integer k : tempMap.keySet()) {
    // Set<Integer> v = tempMap.get(k);
    // if (v.size() > 1) {
    // productToSpecificationMap.put(k, v);
    // }
    // }
    // }
    //
    // Set<Integer> refinedStates = new HashSet<>();
    // for (Set<Integer> x : productToSpecificationMap.values()) {
    // refinedStates.addAll(x);
    // }
    //
    // refinedStates.forEach(x -> ret.put(x, new HashSet<Word<String>>()));
    //
    // System.out.println(refinedStates);
    //
    // // Get all the initial access sequences of the refined states
    // tcIterator = Covers.transitionCoverIterator(specification,
    // specification.getInputAlphabet());
    // while (tcIterator.hasNext()) {
    // Word<String> word = tcIterator.next();
    // FastDFAState ss = specification.getState(word);
    // if (null != ss) {
    // Integer specificationState = ss.getId();
    // // System.out.println(specificationState);
    // if (refinedStates.contains(specificationState)) {
    // ret.get(specificationState).add(word);
    // }
    // }
    // }
    //
    // Integer originalInitialState = specification.getInitialState().getId();
    //
    // for (Integer refinedState : ret.keySet()) {
    // // Remove the current initial state
    // specification.setInitialState(null);
    // // Set a refined state as the current initial state
    // specification.setInitialState(specification.getState(refinedState));
    //
    // Set<Word<String>> refinedStateWords = ret.get(refinedState);
    //
    // tcIterator = Covers.transitionCoverIterator(specification,
    // specification.getInputAlphabet());
    // while (tcIterator.hasNext()) {
    // Word<String> tcWord = tcIterator.next();
    // FastDFAState specificationFinalState = specification
    // .getState(tcWord);
    // if (null != specificationFinalState) {
    // if (specificationFinalState.getId() == refinedState) {
    // Set<Word<String>> refinedMoreAccessWords = new HashSet<>();
    // for (Word<String> x : refinedStateWords) {
    // Word<String> x_new = x.concat(tcWord);
    // refinedMoreAccessWords.add(x_new);
    // }
    // refinedStateWords.addAll(refinedMoreAccessWords);
    // }
    // }
    // }
    // }
    // specification.setInitialState(null);
    // specification
    // .setInitialState(specification.getState(originalInitialState));
    // return ret;
//    // }
//
//    private static boolean specificationAccessCheck(
//            FastDFA<String> specification, FastNFA<String> monitor) {
//        Iterator<Word<String>> tcIterator = Covers.transitionCoverIterator(
//                specification, specification.getInputAlphabet());
//        FastDFA<String> detMonitor = new FastDFA<String>(
//                specification.getInputAlphabet());
//        detMonitor.clear();
//        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
//                NFAs.determinize(monitor, true, true),
//                specification.getInputAlphabet(), detMonitor);
//        Map<Integer, Integer> specificationToMonitorStatesMap = new HashMap<>();
//        while (tcIterator.hasNext()) {
//            Word<String> word = tcIterator.next();
//            FastDFAState specificationState = specification.getState(word);
//            if (null != specificationState) {
//                Integer monitorState = monitorGetState(detMonitor, word)
//                        .getId();
//                if (specificationToMonitorStatesMap
//                        .containsKey(specificationState.getId())) {
//                    if (specificationToMonitorStatesMap
//                            .get(specificationState.getId()) != monitorState) {
//                        return false;
//                    }
//                } else {
//                    specificationToMonitorStatesMap
//                            .put(specificationState.getId(), monitorState);
//                }
//            }
//        }
//        return true;
//    }

//    private static FastDFAState monitorGetState(FastDFA<String> monitor,
//            Word<String> input) {
//        FastDFAState state = monitor.getInitialState();
//        FastDFAState destState = state;
//        for (String in : input) {
//            destState = monitor.getSuccessor(state, in);
//            if (null != destState) {
//                state = destState;
//            }
//        }
//        return state;
//    }

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

        // System.out.println("Press any key to continue: ");
        // System.in.read();
        // We have written to a file and now we have to explore the state-space
        // and load it in.
        CIF3operations.exploreStatespaceCIF("product_composition.cif", false);
        // CIF3operations.mcrl2CompositionOperation("product_composition.cif");
        System.out.println("Done with reading and writing!");
        return;
    }
}
