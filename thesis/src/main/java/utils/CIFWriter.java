package utils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;

/**
 * @author Bharat Garhewal
 *
 */
public class CIFWriter {

    public static void writeCIF(CompactDFA<String> dfa, OutputStream stream,
            String plantName) throws IOException {
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(stream))) {
            // Write header.

            List<String> inputs = new LinkedList<String>(
                    dfa.getInputAlphabet());
            for (int i = 0; i < inputs.size(); i++) {
                writer.write("controllable ");
                writer.write(inputs.get(i) + ";\n");
            }
            // writer.write(inputs.get(inputs.size() - 1) + ";\n");

            // Write some name for the plant
            writer.write("plant " + plantName + ":\n");

            for (Integer state : dfa.getStates()) {
                writer.write(createLocation(state, dfa.isAccepting(state),
                        dfa.getInitialState() == state,
                        isDeadlocked(dfa, state)));
                for (String input : dfa.getInputAlphabet()) {
                    if (null != dfa.getSuccessor(state, input))
                        writer.write(createTransition(
                                dfa.getSuccessor(state, input), input));
                }
            }
            writer.write("end\n");
        }
    }

    public static void writeCIF(FastDFA<String> dfa, OutputStream stream,
            String plantName, boolean includeActionDeclarations)
            throws IOException {
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(stream))) {
            // Write header.

            List<String> inputs = new LinkedList<String>(
                    dfa.getInputAlphabet());
            if (includeActionDeclarations) {
                for (int i = 0; i < inputs.size(); i++) {
                    writer.write("controllable ");
                    writer.write(inputs.get(i) + ";\n");
                }
            }
            // writer.write(inputs.get(inputs.size() - 1) + ";\n");

            // Write some name for the plant
            writer.write("plant " + plantName + ":\n");

            for (FastDFAState state : dfa.getStates()) {
                writer.write(createLocation(state.getId(),
                        dfa.isAccepting(state), dfa.getInitialState() == state,
                        isDeadlocked(dfa, state)));
                for (String input : dfa.getInputAlphabet()) {
                    if (null != dfa.getSuccessor(state, input))
                        writer.write(createTransition(
                                dfa.getSuccessor(state, input).getId(), input));
                }
            }
            writer.write("end\n");
        }
    }

    /**
     * Used for writing the monitors in the guarded-decompositions
     * 
     * @param monitor2
     * @param writer
     * @param plantName
     * @throws IOException
     */

    public static void writeMonitor(FastDFA<String> monitor2, Writer writer,
            String plantName) throws IOException {
        // try (writer) {
        // Write header.
        // Write some name for the plant
        writer.write("automaton " + plantName + ":\n");
        writer.write("\tmonitor;\n");
        if (null == monitor2.getStates()) {
            System.out.println("INSANIIIIIIIIITY");
        }
        // System.out.println(dfa.getStates());
        for (FastDFAState state : monitor2.getStates()) {
            writer.write(createLocation(state.getId() + 1,
                    monitor2.isAccepting(state),
                    monitor2.getInitialState() == state,
                    isDeadlocked(monitor2, state)).replace("loc_", "s"));
            for (String input : monitor2.getInputAlphabet()) {
                if (null != monitor2.getSuccessor(state, input))
                    writer.write(createTransition(
                            monitor2.getSuccessor(state, input).getId() + 1,
                            input).replace("loc_", "s"));
            }
        }
        writer.write("end\n");
        // }
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

    // private static String createLocation(int num) {
    // if (num == 0) {
    // return "\t" + "location loc_" + num + ":\n" + "\t\tinitial;\n" +
    // "\t\tmarked;\n";
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

    public static void writeMonitor(FastNFA<String> monitor,
            FileOutputStream stream, String string, boolean b) {
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(stream))) {
            writer.write("automaton " + "mon" + ":\n");
            writer.write("\tmonitor;\n");
            if (null == monitor.getStates()) {
                System.out.println("INSANIIIIIIIIITY");
            }
            // System.out.println(dfa.getStates());
            for (FastNFAState state : monitor.getStates()) {
                writer.write(createLocation(state.getId() + 1,
                        monitor.isAccepting(state),
                        monitor.getInitialStates().contains(state),
                        isDeadlocked(monitor, state)).replace("loc_", "s"));
                for (String input : monitor.getInputAlphabet()) {
                    if (null != monitor.getSuccessors(state, input))
                        writer.write(createTransition(
                                1,
//                                monitor.getSuccessors(state, input).getId() + 1,
                                input).replace("loc_", "s"));
                }
            }
            writer.write("end\n");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
