/**
 * 
 */
package utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;

/**
 * @author Bharat Garhewal
 *
 */
public class BharatCustomCIFWriter {

    public static void writeMonitor(FastNFA<String> monitor, Writer writer)
            throws IOException {
        writer.write("automaton mon:\n");
        writer.write("\tmonitor;\n");
        if (null == monitor.getStates()) {
            System.err.println("Monitor does not have any initial states");
            System.exit(0);
        }

        for (FastNFAState state : monitor.getStates()) {
            writer.write(createLocation(state.getId() + 1,
                    monitor.isAccepting(state),
                    monitor.getInitialStates().contains(state),
                    isDeadlocked(monitor, state)).replace("loc_", "s"));
            for (String input : monitor.getInputAlphabet()) {
                if (null != monitor.getSuccessors(state, input)) {
                    Set<FastNFAState> successors = monitor.getSuccessors(state,
                            input);
                    for (FastNFAState s : successors) {
                        writer.write(createTransition(s.getId() + 1, input)
                                .replace("loc_", "s"));
                    }
                }
            }
        }
        writer.write("end\n");
    }

    public static void writeCIF(FastDFA<String> dfa, Writer writer,
            String plantName, boolean includeActionDeclarations)
            throws IOException {
        List<String> inputs = new LinkedList<String>(dfa.getInputAlphabet());
        if (includeActionDeclarations) {
            for (int i = 0; i < inputs.size(); i++) {
                writer.write("controllable ");
                writer.write(inputs.get(i) + ";\n");
            }
        }

        // Write some name for the plant
        writer.write("plant " + plantName + ":\n");

        for (FastDFAState state : dfa.getStates()) {
            writer.write(createLocation(state.getId(), dfa.isAccepting(state),
                    dfa.getInitialState() == state, isDeadlocked(dfa, state)));
            for (String input : dfa.getInputAlphabet()) {
                if (null != dfa.getSuccessor(state, input))
                    writer.write(createTransition(
                            dfa.getSuccessor(state, input).getId(), input));
            }
        }
        writer.write("end\n");
    }

    private static boolean isDeadlocked(FastDFA<String> dfa,
            FastDFAState state) {
        for (String in : dfa.getInputAlphabet()) {
            if (null != dfa.getSuccessor(state, in)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDeadlocked(FastNFA<String> dfa,
            FastNFAState state) {
        for (String in : dfa.getInputAlphabet()) {
            if (null != dfa.getSuccessors(state, in)) {
                return false;
            }
        }
        return true;
    }

    private static String createLocation(Integer state, boolean accepting,
            boolean initial, boolean deadlocked) {
        if (initial) {
            if (accepting) {
                if (deadlocked) {
                    return "\t" + "location loc_" + state + ":\n"
                            + "\t\tinitial;\n" + "\t\tmarked;\n";
                } else {
                    return "\t" + "location loc_" + state + ":\n"
                            + "\t\tinitial;\n" + "\t\tmarked;\n";
                }
            } else {
                if (deadlocked) {
                    return "\t" + "location loc_" + state + ":\n"
                            + "\t\tinitial;\n";
                } else {
                    return "\t" + "location loc_" + state + ":\n"
                            + "\t\tinitial;\n";
                }
            }
        } else {
            if (accepting) {
                if (deadlocked) {
                    return "\t" + "location loc_" + state + ":\n"
                            + "\t\tmarked;\n";
                } else {
                    return "\t" + "location loc_" + state + ":\n"
                            + "\t\tmarked;\n";
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

}
