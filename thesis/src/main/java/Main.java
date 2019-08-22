import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.words.Word;
import smDecomposition.MonolithicMonitor;
import stateEquivalence.StateEquivalence;
import stateEquivalence.StateGuards;
import utils.Args;
import utils.BharatCustomCIFReader;
import utils.BharatXESReader;
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
		// case ("monolithic"): {
		// MonolithicDecompose decomp = new MonolithicDecompose(options.getInFiles());
		// CompactDFA<String> dfa = decomp.computeController();
		// dfa = DFAs.minimize(dfa, dfa.getInputAlphabet());
		// CIFWriter.writeCIF(dfa, new FileOutputStream(options.getOutFile()),
		// "monolithic_controller");
		// PrintWriter write = new PrintWriter(options.getOutFile().replace("cif",
		// "dot"));
		// GraphDOT.write(dfa, dfa.getInputAlphabet(), write);
		// }
		// break;
		// case ("minimize"): {
		// CompactDFA<String> dfa = Reader.read(options.getInFiles().get(0));
		// dfa = DFAs.minimize(DFAs.complete(dfa, dfa.getInputAlphabet()));
		// dfa = RemoveDeadlocks.removeDeadlocks(dfa);
		// AldebaranUtil.writeAldebaran(dfa, new
		// FileOutputStream(options.getOutFile()));
		// String cif_file = options.getOutFile().split("\\.")[0] + ".cif";
		// CIFWriter.writeCIF(dfa, new FileOutputStream(cif_file),
		// "monolithic_controller");
		// }
		// break;
		// case ("complement"): {
		// CompactDFA<String> dfa =
		// BharatCustomCIFReader.readCIF(options.getInFiles().get(0));
		// for (String x :
		// BharatCustomCIFReader.readCIF(options.getInFiles().get(1)).getInputAlphabet())
		// {
		// dfa.addAlphabetSymbol(x);
		// }
		// dfa = DFAs.complement(dfa,
		// BharatCustomCIFReader.readCIF(options.getInFiles().get(1)).getInputAlphabet());
		// String[] outName = options.getInFiles().get(0).replace(".cif",
		// "").split("/");
		// CIFWriter.writeCIF(dfa, new FileOutputStream(options.getOutFile()),
		// outName[outName.length - 1]);
		// }
		// break;
		// case ("complete"): {
		// CompactDFA<String> dfa =
		// BharatCustomCIFReader.readCIF(options.getInFiles().get(0));
		// for (String x :
		// BharatCustomCIFReader.readCIF(options.getInFiles().get(1)).getInputAlphabet())
		// {
		// dfa.addAlphabetSymbol(x);
		// }
		// dfa = DFAs.complete(dfa, dfa.getInputAlphabet());
		// AldebaranUtil.writeAldebaran(dfa, new FileOutputStream(options.getOutFile() +
		// ".aut"));
		// String[] outName = options.getInFiles().get(0).replace(".cif",
		// "").split("/");
		// CIFWriter.writeCIF(dfa, new FileOutputStream(options.getOutFile()),
		// outName[outName.length - 1]);
		// }
		// break;
		//
		// case ("consistent"): {
		// CompactDFA<String> dfaSpecification =
		// BharatCustomCIFReader.readCIF(options.getInFiles().get(0));
		// Map<String, CompactDFA<String>> subSpecificationsMap = Misc
		// .generateSubSpecificationsMap(options.getInFiles());
		// dfaSpecification = Consistency.check(options.getInFiles().subList(1,
		// options.getInFiles().size()),
		// dfaSpecification, subSpecificationsMap);
		// CIFWriter.writeCIF(dfaSpecification, new
		// FileOutputStream(options.getOutFile()), "specification");
		// CIF3operations.DFAminimizationCIF(options.getOutFile());
		// }
		// break;

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

	/**
	 * case ("rguards"): { CompactDFA<String> dfaSpecification =
	 * BharatCustomCIFReader.readCIF(options.getInFiles().get(0)); Map<String,
	 * CompactDFA<String>> subSpecificationsMap = Misc
	 * .generateSubSpecificationsMap(options.getInFiles()); Map<String, Map<Integer,
	 * Map<String, Set<Integer>>>> constraints = StateGuards
	 * .execStateGuards(dfaSpecification, subSpecificationsMap); // Put the
	 * constraints in the "Constraints" class Constraints cons = new
	 * Constraints(constraints,
	 * Misc.computeActionToSubSpecNames(subSpecificationsMap),
	 * subSpecificationsMap); // Get the set of invariant statements (strings) //
	 * cons.constructInvariantStatements().forEach(x -> System.out.print(x)); // Now
	 * check if it is a sufficient solution, or do we need a monitor? boolean equal
	 * = Misc.writeToOutput(options, cons.constructInvariantStatements()); // If it
	 * is equal, then we need not bother with anything, we are done. // If not, then
	 * start with the decomposition with memory solution. if (!equal) {
	 * System.out.println("Just state guards are not enough, beginning with monitor
	 * computation"); // System.out.println(nonInjectionMapping( //
	 * StateEquivalence.calculateEquivalentStates(dfaSpecification, //
	 * subSpecificationsMap))); System.in.read(); Set<String> prefActions = new
	 * HashSet<>(); // prefActions.add("switchA"); // prefActions.add("switchB");
	 * GlobalMonitor gm = new GlobalMonitor(options, dfaSpecification, cons,
	 * subSpecificationsMap, options.getIterationOrder(), null);
	 * gm.computeMonitor(); } } break;
	 * 
	 * case ("mguards"): { CompactDFA<String> dfaSpecification =
	 * BharatCustomCIFReader.readCIF(options.getInFiles().get(0)); Map<String,
	 * CompactDFA<String>> subSpecificationsMap = Misc
	 * .generateSubSpecificationsMap(options.getInFiles()); // dfaSpecification =
	 * Consistency.check(options.getInFiles().subList(1, //
	 * options.getInFiles().size()), // dfaSpecification, subSpecificationsMap); //
	 * Files.copy(Paths.get(options.getInFiles().get(0)), //
	 * Paths.get(options.getInFiles().get(0).replace(".", "_bkp.")), //
	 * StandardCopyOption.REPLACE_EXISTING); // CIFWriter.writeCIF(dfaSpecification,
	 * new // FileOutputStream(options.getInFiles().get(0)), "specification"); // if
	 * (!consistent) { // return; // } // // Calculate the equivalent state map //
	 * Map<Integer, Map<String, Integer>> equivalentStateMap = StateEquivalence //
	 * .calculateEquivalentStates(dfaSpecification, subSpecificationsMap); //
	 * Compute the constraints // for (String x : subSpecificationsMap.keySet()) {
	 * // System.out.println(x + " : " + //
	 * subSpecificationsMap.get(x).getInputAlphabet()); // } Map<String,
	 * Map<Integer, Map<String, Set<Integer>>>> constraints = StateGuards
	 * .execStateGuards(dfaSpecification, subSpecificationsMap); // Put the
	 * constraints in the "Constraints" class Constraints cons = new
	 * Constraints(constraints,
	 * Misc.computeActionToSubSpecNames(subSpecificationsMap),
	 * subSpecificationsMap); // Get the set of invariant statements (strings) //
	 * cons.constructInvariantStatements().forEach(x -> System.out.print(x)); // Now
	 * check if it is a sufficient solution, or do we need a monitor? boolean equal
	 * = Misc.writeToOutput(options, cons.constructInvariantStatements()); // If it
	 * is equal, then we need not bother with anything, we are done. // If not, then
	 * start with the decomposition with memory solution. if (!equal) {
	 * System.out.println("Just state guards are not enough, beginning with monitor
	 * computation"); Differentiator diff = new Differentiator(dfaSpecification,
	 * nonInjectionMapping(
	 * StateEquivalence.calculateEquivalentStates(dfaSpecification,
	 * subSpecificationsMap))); System.out.println("Calculated equivalent states");
	 * System.err.println(nonInjectionMapping(
	 * StateEquivalence.calculateEquivalentStates(dfaSpecification,
	 * subSpecificationsMap))); Map<List<Integer>, CompactDFA<String>> monitorMaps =
	 * diff.generateMonitors(); System.out.println(monitorMaps); for (List<Integer>
	 * p : monitorMaps.keySet()) { System.out.print(p); CompactDFA<String> i =
	 * monitorMaps.get(p); System.out.println(" " + i.size()); // for (Integer state
	 * : i.getStates()) { // for (String in : i.getInputAlphabet()) { // Integer
	 * dState = i.getSuccessor(state, in); // if (null != dState) { ////
	 * System.out.println(state + " + " + in + " = " + dState); //
	 * System.out.println(state + " + " + in + " = " + dState); // } // } // } } //
	 * System.in.read(); // Set<String> prefActions = new HashSet<>(); // //
	 * prefActions.add("switchA"); // // prefActions.add("switchB"); //
	 * GlobalMonitor gm = new GlobalMonitor(options, dfaSpecification, cons, //
	 * subSpecificationsMap, // options.getIterationOrder(), null); //
	 * gm.computeMonitor(); } } break;
	 * 
	 * // case ("guards"): { // Map<String, CompactDFA<String>> subSpecificationsMap
	 * = new HashMap<String, // CompactDFA<String>>(); // for (int i = 1; i <
	 * options.getInFiles().size(); i++) { // String name =
	 * options.getInFiles().get(i); // name =
	 * name.split("\\\\")[name.split("\\\\").length - 1].replace(".cif", ""); //
	 * System.out.println(name); // subSpecificationsMap.put(name, //
	 * BharatCustomCIFReader.readCIF(options.getInFiles().get(i))); // } //
	 * Invariant inv = new //
	 * Invariant(BharatCustomCIFReader.readCIF(options.getInFiles().get(0)), //
	 * subSpecificationsMap); // inv.computeInvariants(); // //
	 * inv.getConstraints().forEach(x -> System.out.print(x)); // FileOutputStream
	 * stream = new FileOutputStream(options.getOutFile(), false); //
	 * writeDecomposition(options, options.getInFiles().subList(1, //
	 * options.getInFiles().size()), stream, // inv.getConstraints(), false, null);
	 * // // Copy the original file // Files.copy(Paths.get(options.getOutFile()),
	 * // Paths.get(options.getOutFile().replace(".", "gen.")), //
	 * StandardCopyOption.REPLACE_EXISTING); // // Compute the specification using
	 * the decomposition //
	 * CIF3operations.exploreStatespaceCIF(options.getOutFile()); // // Check the
	 * files for language equivalence // List<String> filesToCheck = new
	 * LinkedList<String>(); // filesToCheck.add(options.getOutFile()); //
	 * filesToCheck.add(options.getInFiles().get(0)); // if
	 * (CIF3operations.checkEquivalenceCIF(filesToCheck)) { //
	 * System.out.println("Equal"); // } else { // stream = new
	 * FileOutputStream(options.getOutFile(), false); //
	 * System.out.println("Unequal"); // //
	 * Files.copy(Paths.get(options.getOutFile()), //
	 * Paths.get(options.getOutFile().replace(".", "_without_monitor.")), //
	 * StandardCopyOption.REPLACE_EXISTING); // inv.generateInitialMonitor(); //
	 * CompactDFA<String> monitor = inv.getMonitor(); // System.out.println("size of
	 * the monitor: " + monitor.size()); // writeDecomposition(options,
	 * options.getInFiles().subList(1, // options.getInFiles().size()), stream, //
	 * inv.getConstraints(), true, monitor); // boolean equal = true; // do { // if
	 * (!inv.generateNewMonitor(equal)) { // break; // } // monitor = inv.monitor;
	 * // System.out.println("size of the monitor: " + monitor.size()); // stream =
	 * new FileOutputStream(options.getOutFile(), false); //
	 * writeDecomposition(options, options.getInFiles().subList(1, //
	 * options.getInFiles().size()), stream, // inv.getConstraints(), true,
	 * monitor); // Files.copy(Paths.get(options.getOutFile()), //
	 * Paths.get(options.getOutFile().replace(".", "gen.")), //
	 * StandardCopyOption.REPLACE_EXISTING); //
	 * CIF3operations.exploreStatespaceCIF(options.getOutFile()); // equal =
	 * CIF3operations.checkEquivalenceCIF(filesToCheck); // //
	 * writeDecomposition(options, options.getInFiles().subList(1, //
	 * options.getInFiles().size()), // // stream, inv.getConstraints(), true,
	 * monitor); // } while (true); // System.err.println("Finished..."); // } // }
	 * // break;
	 * 
	 * case ("sanity"): { List<String> listTraces =
	 * BharatXESReader.readXES(options.getInFiles().get(1)); listTraces.removeIf(x
	 * -> x.trim().isEmpty()); CompactDFA<String> dfa =
	 * BharatCustomCIFReader.readCIF(options.getInFiles().get(0)); FileOutputStream
	 * outStream = new FileOutputStream(options.getOutFile()); List<Word<String>>
	 * wordList = new LinkedList<>(); wordList =
	 * BharatXESReader.getTraces(listTraces, dfa.getInputAlphabet()); try (Writer
	 * writer = new BufferedWriter(new OutputStreamWriter(outStream))) { for
	 * (Word<String> input : wordList) { writer.write(dfa.accepts(input) + " : " +
	 * input + "\n"); } } } break; case ("noop"): default: break; } }
	 **/
	//
	// private static void writeDecomposition(Args options, List<String> outNames,
	// FileOutputStream stream,
	// Set<String> constraints, boolean monitor, CompactDFA<String> globalMonitor)
	// throws IOException {
	// try (Writer writer = new BufferedWriter(new OutputStreamWriter(stream))) {
	// for (String inFile : outNames) {
	// List<String> fileContents = FileUtils.readLines(new File(inFile), "utf-8");
	// for (String x : fileContents) {
	// writer.write(x + "\n");
	// }
	// writer.write("\n");
	// }
	// for (String x : constraints) {
	// writer.write(x);
	// }
	// if (monitor) {
	// // System.out.println(constraints);
	// CIFWriter.writeMonitor(globalMonitor, writer, "globalMonitor");
	// for (String x : constraints) {
	// writer.write(x + "\n");
	// }
	// }
	// }
	// }

	// /**
	// * @param options
	// * @param guards
	// * @param outNames
	// * @param stream
	// * @throws IOException
	// */
	// private static void writeStateGuardsToFile(Args options, StateGuards guards,
	// List<String> outNames,
	// FileOutputStream stream) throws IOException {
	// try (Writer writer = new BufferedWriter(new OutputStreamWriter(stream))) {
	// for (String inFile : outNames) {
	// List<String> fileContents = FileUtils.readLines(new File(inFile), "utf-8");
	// fileContents.forEach(x -> {
	// try {
	// writer.write(x + "\n");
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// });
	// writer.write("\n");
	// }
	// List<String> fileContents = FileUtils.readLines(new File(options.getOutFile()
	// + "_delete"), "utf-8");
	// fileContents.forEach(x -> {
	// try {
	// writer.write(x + "\n");
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// });
	// writer.write("\n");
	// CIFWriter.writeMonitor(guards.getSpecification(), writer, "globalMonitor");
	// guards.setupGlobalMonitor().forEach(x -> {
	// try {
	// writer.write(x + "\n");
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// });
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

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
