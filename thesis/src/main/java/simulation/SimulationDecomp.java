/**
 * 
 */
package simulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.javatuples.Pair;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import utils.Args;
import utils.BharatCustomCIFReader;
import utils.BharatCustomCIFWriter;
import utils.CIF3operations;
import utils.Misc;

/**
 * @author Bharat Garhewal
 *
 */
public class SimulationDecomp {

    private FastDFA<String> specification;
    private Map<String, FastDFA<String>> subSpecificationsMap;
    private FastDFA<String> product;
    private FastNFA<String> monitor;
    private FastDFA<String> productMonitorComposition;
    private Set<Pair<Integer, Integer>> productMonitorToProductRelation = new HashSet<>();
    private Map<Integer, Map<String, Integer>> productToSubSpecificationMap = new HashMap<>();
    public Map<Integer, Map<String, Integer>> productMonitorToSubSpecificationMap = new HashMap<>();
    private Set<Pair<Integer, Integer>> specificationToProductMonitorRelation = new HashSet<>();
    public Map<String, Set<Integer>> transitionsToBlock = new HashMap<>();
    public Map<Integer, Integer> productMonitorToMonitorMap = new HashMap<>();
    private FastDFA<String> dfaMonitor;
    private Set<String> requirements = new HashSet<>();

    public SimulationDecomp(Args options) throws Exception {
        this.requirements = new HashSet<String>();
        this.specification = BharatCustomCIFReader
                .readCIF(options.getInFiles().get(0));
        this.subSpecificationsMap = Misc
                .generateSubSpecificationsMap(options.getInFiles());
        this.product = BharatCustomCIFReader
                .readCIF(
                        CIF3operations.parallelCompositionCIF(
                                options.getInFiles().subList(1,
                                        options.getInFiles().size()),
                                "product.cif"));

        // Compute the productToSub-specificationMap
        this.productToSubSpecificationMap = computeProductToSubSpecificationsMap();

        SimulationMonitor simMon = new SimulationMonitor(specification,
                product);
        this.monitor = new FastNFA<String>(
                this.specification.getInputAlphabet());
        this.monitor.clear();
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                simMon.getMonitor(), simMon.getMonitor().getInputAlphabet(),
                this.monitor);

        this.dfaMonitor = new FastDFA<String>(this.monitor.getInputAlphabet());
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                this.monitor, this.monitor.getInputAlphabet(), this.dfaMonitor);

        // // Restore original product!
        // this.product = BharatCustomCIFReader
        // .readCIF(
        // CIF3operations.parallelCompositionCIF(
        // options.getInFiles().subList(1,
        // options.getInFiles().size()),
        // "product.cif"));

        this.productMonitorComposition = computeProductMonitorComposition();

        FastNFA<String> prodMon = new FastNFA<String>(
                this.specification.getInputAlphabet());
        prodMon.clear();
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                this.productMonitorComposition,
                this.specification.getInputAlphabet(), prodMon);

        this.specificationToProductMonitorRelation = (new SimulationRelationImpl(
                specification, prodMon))
                        // .getUnrestrictedRelation().stream()
                        .getRelation().stream()
                        .map(x -> new Pair<Integer, Integer>(
                                x.getValue0().getId(), x.getValue1().getId()))
                        .collect(Collectors.toSet());

        FastNFA<String> prod = new FastNFA<String>(
                this.specification.getInputAlphabet());
        prodMon.clear();
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                this.product, this.specification.getInputAlphabet(), prod);

        // Compute the relation from P||M to P
        this.productMonitorToProductRelation = (new SimulationRelationImpl(
                this.productMonitorComposition, prod)).getRelation()
                        .parallelStream()
                        .map(x -> new Pair<Integer, Integer>(
                                x.getValue0().getId(), x.getValue1().getId()))
                        .collect(Collectors.toSet());

        // Compute the productMonitorToSubSpecifications map
        this.productMonitorToSubSpecificationMap = computeProductMonitorToSubSpecificationsMap();

        this.productMonitorToMonitorMap = computeProductMonitorToMonitorMap();

        // Compute the transitions to block in P||M when compared to the
        // specification S
        computeTransitionsToBlock();
        System.out.println(this.transitionsToBlock);

        this.requirements = constructRequirements();

        Misc.writeToOutput(options, requirements, dfaMonitor);
    }

    private Set<String> constructRequirements() {
        Set<String> ret = new HashSet<>();
        for (String event : this.transitionsToBlock.keySet()) {
            Set<Integer> locationsToBlock = this.transitionsToBlock.get(event);

            // Empty requirement
            String requirement = "invariant " + event + " needs ";
            // Do not "block" anything where there is nothing to be blocked!
            if (!locationsToBlock.isEmpty()) {
                String req = Requirements.makeRequirement(locationsToBlock,
                        this.productMonitorToMonitorMap,
                        this.productMonitorToSubSpecificationMap);
                requirement = requirement + req;
                ret.add(requirement);
            }
        }
        return ret;
    }

    private Map<Integer, Integer> computeProductMonitorToMonitorMap() {
        Map<Integer, Integer> ret = new HashMap<>(
                this.productMonitorComposition.size());

        Iterator<Word<String>> scPMIterator = Covers.stateCoverIterator(
                this.productMonitorComposition,
                this.productMonitorComposition.getInputAlphabet());
        while (scPMIterator.hasNext()) {
            Word<String> inputWord = scPMIterator.next();
            Integer productMonitorState = this.productMonitorComposition
                    .getState(inputWord).getId();
            Integer monitorState = monitorGetState(this.dfaMonitor, inputWord)
                    .getId();

            // Insert the relevant states in the map
            ret.put(productMonitorState, monitorState);
        }
        return ret;
    }

    private FastDFAState monitorGetState(FastDFA<String> monitor,
            Word<String> input) {
        FastDFAState state = monitor.getInitialState();
        FastDFAState destState = state;
        for (String in : input) {
            destState = monitor.getSuccessor(state, in);
            if (null != destState) {
                state = destState;
            }
        }
        return state;
    }

    private Map<Integer, Map<String, Integer>> computeProductMonitorToSubSpecificationsMap() {
        Map<Integer, Map<String, Integer>> ret = new HashMap<>();
        // For every tuple of the relation from P||M to P, put the subspecMap
        // for the P||M state.
        for (Pair<Integer, Integer> pair : this.productMonitorToProductRelation) {
            Integer productMonitorState = pair.getValue0();
            Integer productState = pair.getValue1();
            ret.putIfAbsent(productMonitorState,
                    this.productToSubSpecificationMap.get(productState));
        }
        // }
        return ret;
    }

    private Map<Integer, Map<String, Integer>> computeProductToSubSpecificationsMap() {
        Map<Integer, Map<String, Integer>> ret = new HashMap<>();

        // Initialize the (empty) map with just the keys for now
        for (Integer i = 0; i < this.product.size(); i++) {
            ret.put(i, new HashMap<>());
        }

        Iterator<Word<String>> productSCIterator = Covers.stateCoverIterator(
                this.product, this.product.getInputAlphabet());
        while (productSCIterator.hasNext()) {
            Word<String> inputWord = productSCIterator.next();
            FastDFAState productState = this.product.getState(inputWord);
            if (null != productState) {
                Map<String, Integer> subSpecMapForOneState = new HashMap<>(
                        this.subSpecificationsMap.size());
                for (String subSpecificationName : this.subSpecificationsMap
                        .keySet()) {
                    Alphabet<String> subSpecificationAlphabet = this.subSpecificationsMap
                            .get(subSpecificationName).getInputAlphabet();
                    List<String> projectedInputWord = inputWord.stream()
                            .filter(x -> subSpecificationAlphabet.contains(x))
                            .collect(Collectors.toList());
                    FastDFAState subSpecificationState = this.subSpecificationsMap
                            .get(subSpecificationName)
                            .getState(projectedInputWord);
                    subSpecMapForOneState.put(subSpecificationName,
                            subSpecificationState.getId());
                }
                ret.put(productState.getId(), subSpecMapForOneState);
            }
        }
        return ret;
    }

    private void computeTransitionsToBlock() {

        // Initialize the transitionsToBlock data structure
        // Simply put in the empty set of states for each action, we will fill
        // them in next.
        this.productMonitorComposition.getInputAlphabet()
                .forEach(x -> this.transitionsToBlock.put(x, new HashSet<>()));

        // For every pair of (s,p) where s is specification state and p is
        // productMonitor composition state.
        for (Pair<Integer, Integer> pair : this.specificationToProductMonitorRelation) {
            Integer specificationState = pair.getValue0();
            Integer productMonitorState = pair.getValue1();
            Set<String> specificationPossibleInputs = getPossibleInputs(
                    specification, specificationState);
            Set<String> productMonitorPossibleInputs = getPossibleInputs(
                    this.productMonitorComposition, productMonitorState);

            // Keep all the inputs to block
            productMonitorPossibleInputs.removeAll(specificationPossibleInputs);

            // Add the product state at which the specified action is to be
            // blocked.
            productMonitorPossibleInputs
                    .forEach(input -> this.transitionsToBlock.get(input)
                            .add(productMonitorState));
        }
    }

    private Set<String> getPossibleInputs(FastDFA<String> dfa,
            Integer currState) {
        Set<String> ret = new HashSet<>();
        for (String input : dfa.getInputAlphabet()) {
            if (null != dfa.getSuccessor(dfa.getState(currState), input)) {
                ret.add(input);
            }
        }
        return ret;
    }

    private FastDFA<String> computeProductMonitorComposition()
            throws Exception {
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

        return BharatCustomCIFReader.readCIF("product_composition.cif");
    }

    /**
     * @return the requirements
     */
    public Set<String> getRequirements() {
        return this.requirements;
    }
}
