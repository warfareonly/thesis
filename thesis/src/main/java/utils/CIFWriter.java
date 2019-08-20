package utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;

/**
 * @author Bharat Garhewal
 *
 */
public class CIFWriter {

	public static void writeCIF(CompactDFA<String> dfa, OutputStream stream, String plantName) throws IOException {
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(stream))) {
			// Write header.

			List<String> inputs = new LinkedList<String>(dfa.getInputAlphabet());
			for (int i = 0; i < inputs.size(); i++) {
				writer.write("controllable ");
				writer.write(inputs.get(i) + ";\n");
			}
			// writer.write(inputs.get(inputs.size() - 1) + ";\n");

			// Write some name for the plant
			writer.write("plant " + plantName + ":\n");

			for (Integer state : dfa.getStates()) {
				writer.write(createLocation(state, dfa.isAccepting(state), dfa.getInitialState() == state,
						isDeadlocked(dfa, state)));
				for (String input : dfa.getInputAlphabet()) {
					if (null != dfa.getSuccessor(state, input))
						writer.write(createTransition(dfa.getSuccessor(state, input), input));
				}
			}
			writer.write("end\n");
		}
	}

	/**
	 * Used for writing the monitors in the guarded-decompositions
	 * 
	 * @param dfa
	 * @param writer
	 * @param plantName
	 * @throws IOException
	 */

	public static void writeMonitor(CompactDFA<String> dfa, Writer writer, String plantName) throws IOException {
		// try (writer) {
		// Write header.
		// Write some name for the plant
		writer.write("automaton " + plantName + ":\n");
		writer.write("\tmonitor;\n");
		if (null == dfa.getStates()) {
			System.out.println("INSANIIIIIIIIITY");
		}
		// System.out.println(dfa.getStates());
		for (Integer state : dfa.getStates()) {
			writer.write(createLocation(state + 1, dfa.isAccepting(state), dfa.getInitialState() == state,
					isDeadlocked(dfa, state)).replace("loc_", "s"));
			for (String input : dfa.getInputAlphabet()) {
				if (null != dfa.getSuccessor(state, input))
					writer.write(createTransition(dfa.getSuccessor(state, input) + 1, input).replace("loc_", "s"));
			}
		}
		writer.write("end\n");
		// }
	}

	private static String createLocation(Integer state, boolean accepting, boolean initial, boolean deadlocked) {
		if (initial) {
			if (accepting) {
				if (deadlocked) {
					return "\t" + "location loc_" + state + ":\n" + "\t\tinitial;\n" + "\t\tmarked;\n";
				} else {
					return "\t" + "location loc_" + state + ":\n" + "\t\tinitial;\n" + "\t\tmarked;\n";
				}
			} else {
				if (deadlocked) {
					return "\t" + "location loc_" + state + ":\n" + "\t\tinitial;\n";
				} else {
					return "\t" + "location loc_" + state + ":\n" + "\t\tinitial;\n";
				}
			}
		} else {
			if (accepting) {
				if (deadlocked) {
					return "\t" + "location loc_" + state + ":\n" + "\t\tmarked;\n";
				} else {
					return "\t" + "location loc_" + state + ":\n" + "\t\tmarked;\n";
				}
			} else {
				if (deadlocked) {
					return "\t" + "location loc_" + state + ";\n";
				} else {
					return "\t" + "location loc_" + state + ":\n";
				}
			}
		}
	}

	private static String createTransition(int destination, String label) {
		return "\t\t" + "edge " + label + " goto loc_" + destination + ";\n";
	}

	// private static String createLocation(int num) {
	// if (num == 0) {
	// return "\t" + "location loc_" + num + ":\n" + "\t\tinitial;\n" + "\t\tmarked;\n";
	// } else {
	// return "\t" + "location loc_" + num + ":\n" + "\t\tmarked;\n";
	//
	// }
	// }

	private static boolean isDeadlocked(CompactDFA<String> dfa, Integer state) {
		for (String in : dfa.getInputAlphabet()) {
			if (null != dfa.getSuccessor(state, in)) {
				return false;
			}
		}
		return true;
	}
}
