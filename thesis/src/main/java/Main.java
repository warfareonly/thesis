import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.automatalib.automata.fsa.impl.FastDFA;
import smDecomposition.MonolithicMonitor;
import stateEquivalence.StateGuards;
import utils.Args;
import utils.BharatCustomCIFReader;
import utils.Misc;
import com.beust.jcommander.JCommander;

import invariant.Constraints;
//import invariant.Invariant;

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
		switch (options.getCommand()) {

		case ("xguards"): {
			FastDFA<String> dfaSpecification = BharatCustomCIFReader.readCIF(options.getInFiles().get(0));
			Map<String, FastDFA<String>> subSpecificationsMap = Misc.generateSubSpecificationsMap(options.getInFiles());
			Map<String, Map<Integer, Map<String, Set<Integer>>>> constraints = StateGuards
					.execStateGuards(dfaSpecification, subSpecificationsMap);
			// Put the constraints in the "Constraints" class
			Constraints cons = new Constraints(constraints, Misc.computeActionToSubSpecNames(subSpecificationsMap),
					subSpecificationsMap);
			// Get the set of invariant statements (strings)
			// cons.constructInvariantStatements().forEach(x -> System.out.print(x));
			// Now check if it is a sufficient solution, or do we need a monitor?
			boolean equal = Misc.writeToOutput(options, cons.constructInvariantStatements());
			// If it is equal, then we need not bother with anything, we are done.
			// If not, then start with the decomposition with memory solution.
			if (!equal) {
				System.out.println("Just state guards are not enough, beginning with monitor computation");
				// System.out.println(nonInjectionMapping(
				// StateEquivalence.calculateEquivalentStates(dfaSpecification,
				// subSpecificationsMap)));
				// System.in.read();
				Set<String> prefActions = new HashSet<>();
				// prefActions.add("switchA");
				// prefActions.add("switchB");
				// GlobalMonitor gm = new GlobalMonitor(options, dfaSpecification, cons,
				// subSpecificationsMap,
				// options.getIterationOrder(), null);
				//
				MonolithicMonitor mm = new MonolithicMonitor(options, dfaSpecification, cons, subSpecificationsMap,
						options.getIterationOrder(), prefActions);
				mm.computeMonitor();
			}
		}
			break;
		}
	}

	private static Map<Integer, List<Integer>> nonInjectionMapping(Map<Integer, Map<String, Integer>> stateMap) {
		Map<Integer, Integer> midMap = new HashMap<Integer, Integer>();
		for (Integer i : stateMap.keySet()) {
			midMap.put(i, stateMap.get(i).get("product"));
		}
		Map<Integer, List<Integer>> ret = new HashMap<>();
		try {
			ret = midMap.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue,
					Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
		} catch (NullPointerException nullptrE) {
			System.err.print("Null pointer exception in the non-injective map computation, ");
			System.err.println("perhaps you forgot to provide the product automaton?");
		}
		return ret;
	}
}
