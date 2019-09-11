import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javatuples.Pair;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import refac.Injection;
import simulation.SimulationDecomposition;
import simulation.SimulationRelation;
import simulation.SimulationRelationImpl;
import simulation.SimulationRestrictor;
import smDecomposition.MonolithicMonitor;
import stateEquivalence.StateGuards;
import utils.Args;
import utils.BharatCustomCIFReader;
import utils.Misc;
import com.beust.jcommander.JCommander;

import invariant.Constraints;

/**
 * Main class, which controls everything!
 * 
 * @author Bharat Garhewal
 *
 */
public class Main {

    /**
     * Constructor for main
     * 
     * @param args
     *            command line arguments for the program
     * @throws Exception
     *             exception
     */
    public static void main(String[] args) throws Exception {
        Args options = new Args();
        JCommander.newBuilder().addObject(options).build().parse(args);
        options.validateOptions();
        performCommand(options);
    }

    public static void performCommand(Args options) throws Exception {

        FastDFA<String> dfaSpecification = BharatCustomCIFReader
                .readCIF(options.getInFiles().get(0));
        if (options.getCommand().equalsIgnoreCase("sim")) {
            SimulationDecomposition simDecomp = new SimulationDecomposition(
                    options);
            simDecomp.computeRequirements();
            System.exit(0);
            // SimulationDecomposition simDecomp = new SimulationDecomposition(
            // options);
            FastNFA<String> product = new FastNFA<String>(
                    dfaSpecification.getInputAlphabet());
            // product.clear();
            AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                    BharatCustomCIFReader.readCIF(options.getInFiles().get(1)),
                    dfaSpecification.getInputAlphabet(), product);

            SimulationRelationImpl simRel = new SimulationRelationImpl(
                    dfaSpecification, product);

            System.out.println(simRel.getRelation());
            // assert simRel.checkIfSimulationRelationExists();
            // simRel.getRelation().forEach(x -> System.out.println(x));

            System.out.println("Simulation relation exists : "
                    + simRel.checkIfSimulationRelationExists()
                    + " and is injective : "
                    + simRel.checkIfInjectiveSimulationRelation());
            // System.out.println(simRel.getSimulationRelationAsMap());
            // Map<String, FastDFA<String>> productMap = new HashMap<>();
            // productMap.put("product",
            // BharatCustomCIFReader.readCIF(options.getInFiles().get(1)));
            // Map<Integer, Integer> specificationStateToProductStateMap =
            // Injection
            // .unwrapProductMap(stateEquivalence.StateEquivalence
            // .calculateEquivalentStates(dfaSpecification,
            // productMap));
            // System.err.println(specificationStateToProductStateMap);
            return;
        }
        Map<String, FastDFA<String>> subSpecificationsMap = Misc
                .generateSubSpecificationsMap(options.getInFiles());
        Map<String, Map<Integer, Map<String, Set<Integer>>>> constraints = StateGuards
                .execStateGuards(dfaSpecification, subSpecificationsMap);

        // Put the constraints in the "Constraints" class
        Constraints cons = new Constraints(constraints,
                Misc.computeActionToSubSpecNames(subSpecificationsMap),
                subSpecificationsMap);

        // If false, then the mapping from the specification to the product is
        // not injective, that is, we need a monitor!
        if (!Injection.checkInjectionFromSpecificationToProduct(
                dfaSpecification,
                options.getInFiles().subList(1, options.getInFiles().size()))) {
            System.out.println(
                    "Just state guards are not enough, beginning with monitor computation");
            // System.exit(0);
            Set<String> prefActions = new HashSet<>();
            // prefActions.add("switchA");
            // prefActions.add("switchB");
            // GlobalMonitor gm = new GlobalMonitor(options, dfaSpecification,
            // cons,
            // subSpecificationsMap,
            // options.getIterationOrder(), null);
            //
            MonolithicMonitor mm = new MonolithicMonitor(options,
                    dfaSpecification, cons, subSpecificationsMap,
                    options.getIterationOrder(), prefActions);
            mm.computeMonitor();
        } else {
            Misc.writeToOutput(options, cons.constructInvariantStatements());
            System.out.println("Finished");
        }
    }
}
