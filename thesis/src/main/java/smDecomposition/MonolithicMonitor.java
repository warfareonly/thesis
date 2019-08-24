package smDecomposition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javatuples.Pair;

import invariant.Constraints;
import monitors.ModifyMonitorMonolithic;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.util.automata.cover.Covers;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.words.Word;
import refac.Injection;
import refac.SanityChecker;
import utils.Args;
import utils.Misc;

public class MonolithicMonitor {
    public enum IterationOrder {
        FWD, BWD, RND;
    }

    private Args options;
    private Map<String, Map<Integer, Map<String, Set<Integer>>>> memorylessConstraints;
    private FastDFA<String> specification;
    private FastNFA<String> monitor;
    private Set<Pair<String, Integer>> desiredMonitorGuards;
    private Map<String, FastDFA<String>> subSpecificationsMap;
    private Map<Integer, Integer> specificationToMonitorMap = new HashMap<Integer, Integer>();
    private Map<Pair<String, Integer>, Set<Integer>> subSpecficationActionComboToSpecificationMap;
    private FastNFA<String> monitorSafe;
    private SanityChecker sanityCheck;

    public MonolithicMonitor(Args options, FastDFA<String> dfaSpecification,
            Constraints cons, Map<String, FastDFA<String>> subSpecificationsMap,
            IterationOrder iterationOrder, Set<String> preferredActions)
            throws Exception {
        this.options = options;
        this.memorylessConstraints = cons.getConstraints();
        this.specification = new FastDFA<String>(
                dfaSpecification.getInputAlphabet());
        this.monitor = new FastNFA<String>(dfaSpecification.getInputAlphabet());
        this.subSpecificationsMap = subSpecificationsMap;
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                dfaSpecification, dfaSpecification.getInputAlphabet(),
                this.specification);
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                specification, dfaSpecification.getInputAlphabet(),
                this.monitor);
        this.subSpecficationActionComboToSpecificationMap = generateActionComboMap(
                subSpecificationsMap);
        this.sanityCheck = new SanityChecker(specification,
                options.getInFiles().subList(1, options.getInFiles().size()));

        this.desiredMonitorGuards = generateMonitorNecessarySpecificationStateToActionPairMap(
                subSpecficationActionComboToSpecificationMap,
                Injection.confusedSpecificationStates(dfaSpecification, options
                        .getInFiles().subList(1, options.getInFiles().size())));

        System.out.println(this.subSpecficationActionComboToSpecificationMap);
        System.out.println(Injection.confusedSpecificationStates(
                dfaSpecification,
                options.getInFiles().subList(1, options.getInFiles().size())));
        System.out.println(this.desiredMonitorGuards);
    }

    public void computeMonitor() throws Exception {
        Integer count = 0;
        Integer nakedCount = 0;
        while (count < specification.size() - 1) {
            nakedCount++;
            System.err.println(nakedCount);
            if (nakedCount > 100) {
                System.exit(0);
            }
            System.out.println(
                    "Iteration #" + count + " of " + specification.size());
            System.out.println("Size of monitor =" + this.monitor.size());
            this.monitorSafe = new FastNFA<String>(
                    this.monitor.getInputAlphabet());
            AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                    this.monitor, this.monitor.getInputAlphabet(),
                    this.monitorSafe);
            ModifyMonitorMonolithic mod = new ModifyMonitorMonolithic(
                    this.monitor, null);
            this.monitor = new FastNFA<String>(
                    this.specification.getInputAlphabet());
            AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                    mod.getMonitor(), this.specification.getInputAlphabet(),
                    this.monitor);
            count++;
            if (true) {
                System.out.println("Printing monitor");
                for (FastNFAState x : this.monitor.getStates()) {
                    for (String i : this.monitor.getInputAlphabet()) {
                        if (!this.monitor.getSuccessors(x, i).isEmpty()) {
                            System.out.println(x + " + " + i + " = "
                                    + this.monitor.getSuccessors(x, i));
                        }
                    }
                }
            }
            if (!sanityCheck.checkMonitor(this.monitor)) {
                // Restore the previous one, since the new monitor fails the
                // sanity check
                this.monitor = new FastNFA<String>(
                        this.monitorSafe.getInputAlphabet());
                AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                        this.monitorSafe, this.monitorSafe.getInputAlphabet(),
                        this.monitor);
                // We need to "forget" that the previous monitor was generated
                count--;
            }

            // FIXME: Ugly, disgusting hack.
            if (sanityCheck.checkMonitor(this.monitor)
                    && this.monitor.size() == 2) {
                break;
            }
        }
        Set<String> invariants = computeMonitorConstraints();
        FastDFA<String> cDFA = new FastDFA<String>(
                this.monitor.getInputAlphabet());
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                NFAs.determinize(this.monitor, true, false),
                this.monitor.getInputAlphabet(), cDFA);
        Misc.writeToOutput(options, invariants, cDFA);
    }

    /**
     * Just a helper, prints out the entire transition function to (System.out).
     * Even the undefined transitions, so use with caution on automata with a
     * large number of states.
     * 
     * @param monitor2
     */
    @SuppressWarnings("unused")
    private void printMonitor(CompactDFA<String> monitor2) {
        for (Integer st : monitor2.getStates()) {
            for (String in : monitor2.getInputAlphabet()) {
                System.out.println(st + " + " + in + " = "
                        + monitor2.getSuccessor(st, in));
            }
        }
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
                this.specificationToMonitorMap.put(specState.getId(),
                        monitorState.getId());
            }
        }
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