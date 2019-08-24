import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.automatalib.automata.fsa.impl.FastDFA;
import refac.Injection;
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
