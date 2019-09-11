package simulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javatuples.Pair;

import invariant.Constraints;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.words.Word;
import refac.Injection;
import stateEquivalence.StateGuards;
import utils.Args;
import utils.BharatCustomCIFReader;
import utils.CIF3operations;
import utils.Misc;

public class SimulationDecomposition {

    private Args options;
    private FastDFA<String> specification;
    private Map<String, FastDFA<String>> subSpecificationsMap = new HashMap<String, FastDFA<String>>();
    private FastDFA<String> product;
    private FastNFA<String> monitor;
    private Map<Pair<String, Integer>, Set<Integer>> subSpecficationActionComboToSpecificationMap = new HashMap<Pair<String, Integer>, Set<Integer>>();
    private Set<Pair<String, Integer>> desiredMonitorGuards = new HashSet<>();
    private Map<Integer, Integer> specificationToMonitorMap = new HashMap<>();
    private Map<String, Map<Integer, Map<String, Set<Integer>>>> memorylessConstraints = new HashMap<>();

    public SimulationDecomposition(Args options) throws Exception {
        this.options = options;
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

        SimulationMonitor simMon = new SimulationMonitor(specification,
                product);
        simMon.computeMonitor();
        this.monitor = new FastNFA<String>(
                this.specification.getInputAlphabet());
        this.monitor.clear();
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                simMon.getMonitor(), simMon.getMonitor().getInputAlphabet(),
                this.monitor);
    }

    public void computeRequirements() throws Exception {
        Map<String, Map<Integer, Map<String, Set<Integer>>>> constraints = StateGuards
                .execStateGuards(this.specification, subSpecificationsMap);

        // Put the constraints in the "Constraints" class
        Constraints cons = new Constraints(constraints,
                Misc.computeActionToSubSpecNames(subSpecificationsMap),
                subSpecificationsMap);
        this.memorylessConstraints = cons.getConstraints();

        subSpecficationActionComboToSpecificationMap = generateActionComboMap(
                subSpecificationsMap);

        this.desiredMonitorGuards = generateMonitorNecessarySpecificationStateToActionPairMap(
                subSpecficationActionComboToSpecificationMap,
                Injection.confusedSpecificationStates(specification, options
                        .getInFiles().subList(1, options.getInFiles().size())));
        Set<String> req = computeMonitorConstraints();
        System.out.println(req);
        FastDFA<String> cDFA = new FastDFA<String>(
                this.monitor.getInputAlphabet());
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                NFAs.determinize(this.monitor, true, false),
                this.monitor.getInputAlphabet(), cDFA);
        Misc.writeToOutput(options, req, cDFA);
    }

    /**
     * Generate the set of <i>complete</i> constraints, combining the monitor
     * and the memoryless constraints into a single set of strings. Also
     * simplifies them.
     * 
     * @param preferredActions
     *            A set of actions which are preferably in the final monitor
     * @return set of invariant strings
     */
    public Set<String> computeMonitorConstraints() {
        generateSpecificationToMonitorMap();
        Map<String, Map<Integer, Map<String, Set<Integer>>>> constraintsMonitor = generateConstraintsMonitorMap();
        Map<String, Map<Integer, Map<String, Set<Integer>>>> completeConstraints = new HashMap<>();
        Misc.deepMerge(completeConstraints, constraintsMonitor);
        Misc.deepMerge(completeConstraints, memorylessConstraints);

        Map<String, FastDFA<String>> subSpecwithMonitorMap = new HashMap<>();
        subSpecwithMonitorMap.putAll(subSpecificationsMap);
        FastDFA<String> cDFA = new FastDFA<String>(
                this.monitor.getInputAlphabet());
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                NFAs.determinize(this.monitor, true, false),
                this.monitor.getInputAlphabet(), cDFA);
        subSpecwithMonitorMap.put("globalMonitor", cDFA);
        Constraints cons = new Constraints(completeConstraints,
                Misc.computeActionToSubSpecNames(subSpecificationsMap),
                subSpecwithMonitorMap);
        return cons.constructInvariantStatements();
    }

    /**
     * Generates a map from the states of the specification to the states of the
     * monitor.
     */
    private void generateSpecificationToMonitorMap() {
        this.specificationToMonitorMap.clear();
        Iterator<Word<String>> tc = Covers.transitionCoverIterator(
                this.specification, this.specification.getInputAlphabet());
        FastDFA<String> cDFA = new FastDFA<String>(
                this.monitor.getInputAlphabet());
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                NFAs.determinize(this.monitor, true, false),
                this.monitor.getInputAlphabet(), cDFA);
        while (tc.hasNext()) {
            Word<String> input = tc.next();
            FastDFAState specState = this.specification.getState(input);
            if (null != specState) {
                FastDFAState monitorState = monitorGetState(cDFA, input);
                System.out.println(input + " spec: " + specState + " mon: "
                        + monitorState);
                if (this.specificationToMonitorMap
                        .containsKey(specState.getId())) {
                    if (monitorState.getId() != this.specificationToMonitorMap
                            .get(specState.getId())) {
                        System.out.println(
                                "Specification same state has multiple states in the monitor, something is wrong, exiting...");
                    }
                }
                this.specificationToMonitorMap.put(specState.getId(),
                        monitorState.getId());
            }
        }
        this.specificationToMonitorMap.put(0, 0);
        // System.out.println(this.specificationToMonitorMap);
    }

    private FastDFAState monitorGetState(FastDFA<String> monitor2,
            Word<String> input) {
        FastDFAState state = monitor2.getInitialState();
        FastDFAState destState = state;
        for (String in : input) {
            destState = monitor2.getSuccessor(state, in);
            if (null != destState) {
                state = destState;
            }
        }
        return state;
    }

    /**
     * Generate the constraints of the monitor, in the
     * {@literal <action, state>} format map of the other memoryless
     * constraints. The returned constraint map will be merged with the
     * memoryless constraint map.
     * 
     * @return monitor constraints map
     */
    private Map<String, Map<Integer, Map<String, Set<Integer>>>> generateConstraintsMonitorMap() {
        Map<String, Map<Integer, Map<String, Set<Integer>>>> ret = new HashMap<>();
        Set<String> actionNames = new HashSet<>();
        // Initialization of the map, (slightly) complicated!
        for (Pair<String, Integer> x : this.subSpecficationActionComboToSpecificationMap
                .keySet()) {
            actionNames.add(x.getValue0());
            if (ret.containsKey(x.getValue0())) {
                assert x.getValue1() != null;
                assert x.getValue0() != null;
                Map<Integer, Map<String, Set<Integer>>> intMap = ret
                        .get(x.getValue0());
                Map<String, Set<Integer>> monitorMap = new HashMap<String, Set<Integer>>();
                monitorMap.put("globalMonitor", new HashSet<Integer>());
                intMap.put(x.getValue1(), monitorMap);
            } else {
                Map<Integer, Map<String, Set<Integer>>> intMap = new HashMap<>();
                assert x.getValue1() != null;
                assert x.getValue0() != null;
                Map<String, Set<Integer>> monitorMap = new HashMap<String, Set<Integer>>();
                monitorMap.put("globalMonitor", new HashSet<Integer>());
                intMap.put(x.getValue1(), monitorMap);
                ret.put(x.getValue0(), intMap);
            }
        }
        // Initialization of the map is finished.
        // Load up the values!
        for (Pair<String, Integer> actionCombo : this.subSpecficationActionComboToSpecificationMap
                .keySet()) {
            Set<Integer> specStates = this.subSpecficationActionComboToSpecificationMap
                    .get(actionCombo);
            Set<Integer> subSpecificationStates = new HashSet<>();
            for (Integer x : specStates) {
                // Gives the equivalent monitor states for the states of the
                // specification
                assert this.specificationToMonitorMap.get(x) != null;
                subSpecificationStates
                        .add(this.specificationToMonitorMap.get(x));
            }
            for (Integer s : subSpecificationStates) {
                assert s != null;
            }
            if (this.desiredMonitorGuards.contains(actionCombo)) {
                ret.get(actionCombo.getValue0()).get(actionCombo.getValue1())
                        .get("globalMonitor").addAll(subSpecificationStates);
            }
        }
        System.out.println(ret);
        return ret;
    }

    /**
     * Generates a map of the {@literal <action, state>} tuples of all the
     * sub-specifications to their set of states in the specification.
     * 
     * @param specification
     * @param subSpecificationsMap2
     *            map of string to sub-specification
     * @return
     */
    private Map<Pair<String, Integer>, Set<Integer>> generateActionComboMap(
            Map<String, FastDFA<String>> subSpecificationsMap2) {
        Map<Pair<String, Integer>, Set<Integer>> ret = new HashMap<>();
        Iterator<Word<String>> tcIterator = Covers.transitionCoverIterator(
                specification, specification.getInputAlphabet()); // Compute
                                                                  // transition
                                                                  // cover of
                                                                  // the
                                                                  // specification
        Map<String, String> actionToSubSpecificationNameMap = Misc
                .computeActionToSubSpecNames(subSpecificationsMap2);
        // Got the map of action -> sub-specification names
        while (tcIterator.hasNext()) {
            Word<String> input = tcIterator.next();
            FastDFAState specificationState = specification.getState(input);
            if (null != specificationState) {
                // If the word "input" is valid
                List<String> possibleInputs = getPossibleInputs(specification,
                        specificationState);
                // Get all the outgoing actions, we don't care where they lead
                for (String pInput : possibleInputs) {
                    FastDFA<String> subSpec = subSpecificationsMap2
                            .get(actionToSubSpecificationNameMap.get(pInput));
                    List<String> subSpecificationInput = Misc.projectToAlphabet(
                            input, subSpec.getInputAlphabet());
                    if (ret.containsKey(new Pair<String, Integer>(pInput,
                            subSpec.getState(subSpecificationInput).getId()))) {
                        ret.get(new Pair<String, Integer>(pInput,
                                subSpec.getState(subSpecificationInput)
                                        .getId()))
                                .add(specificationState.getId());
                    } else {
                        Set<Integer> x = new HashSet<>();
                        x.add(specificationState.getId());
                        ret.put(new Pair<String, Integer>(pInput, subSpec
                                .getState(subSpecificationInput).getId()), x);
                    }
                }
            }
        }
        return ret;
    }

    private Set<Pair<String, Integer>> generateMonitorNecessarySpecificationStateToActionPairMap(
            Map<Pair<String, Integer>, Set<Integer>> actionStateToSpecificationStatesMap,
            Set<Integer> neededSpecificationStates) {
        Set<Pair<String, Integer>> ret = new HashSet<>();
        // this.specification.getStates().parallelStream()
        // .map(s -> ret.put(s.getId(), new HashSet<>()));
        for (Pair<String, Integer> actionTuple : actionStateToSpecificationStatesMap
                .keySet()) {
            for (Integer specificationState : actionStateToSpecificationStatesMap
                    .get(actionTuple)) {
                if (neededSpecificationStates.contains(specificationState)) {
                    ret.add(actionTuple);
                }
            }
        }
        return ret;
    }

    private List<String> getPossibleInputs(FastDFA<String> dfa,
            FastDFAState state) {
        List<String> ret = new LinkedList<String>();
        for (String in : dfa.getInputAlphabet()) {
            if (dfa.getSuccessor(state, in) != null) {
                ret.add(in);
            }
        }
        return ret;
    }
}
