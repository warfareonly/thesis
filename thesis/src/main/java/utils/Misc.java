/**
 * 
 */
package utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import nl.tue.cif.v3x0x0.common.CifEvalException;

/**
 * Miscellaneous utility class
 * 
 * @author Bharat Garhewal
 *
 */
public class Misc {

    /**
     * Invert a non-injective map.
     * 
     * @param map
     *            the map of key -> value
     * @return a map of value -> set of keys
     */
    public static <K, V> Map<V, Set<K>> invertMapNonUnqiue(Map<K, V> map) {
        Map<V, Set<K>> ret = map.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors
                        .mapping(Map.Entry::getKey, Collectors.toSet())));
        return ret;
    }

    public static void printMonitor(FastNFA<String> nfa) {
        for (FastNFAState state : nfa.getStates()) {
            for (String input : nfa.getInputAlphabet()) {
                if (null != nfa.getSuccessors(state, input)
                        && !nfa.getSuccessors(state, input).isEmpty()) {
                    System.out.println(state + " + " + input + " = "
                            + nfa.getSuccessors(state, input));
                }
            }
        }
        return;
    }

    /**
     * Invert an injective map. <b>Note</b> we do not check if the map is
     * actually injective or not.
     * 
     * @param map
     *            the map of key -> value
     * @return the map of value -> key
     */
    public static <K, V> Map<V, K> invertMapUnqiue(Map<K, V> map) {
        Map<V, K> ret = map.entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        return ret;
    }

    public static Map<String, FastDFA<String>> generateSubSpecificationsMap(
            List<String> inFiles) throws CifEvalException, IOException {
        // We remove the first file as it is always the specification file, and
        // not a
        // sub-specification file
        List<String> filesToRead = inFiles.subList(1, inFiles.size());
        return zipToMap(stripIdentifierFromFilename(filesToRead),
                genListOfSubSpecifications(filesToRead));
    }

    /**
     * Java version of the popular zip to map functionality
     * 
     * @param keys
     * @param values
     * @return
     */
    private static <K, V> Map<K, V> zipToMap(List<K> keys, List<V> values) {
        Iterator<K> keyIter = keys.iterator();
        Iterator<V> valIter = values.iterator();
        return IntStream.range(0, keys.size()).boxed().collect(
                Collectors.toMap(_i -> keyIter.next(), _i -> valIter.next()));
    }

    /**
     * Like the name suggests: gets the filenames (without the extension) from
     * the list of string of files.
     * 
     * @param listFilenames
     * @return
     */
    private static List<String> stripIdentifierFromFilename(
            List<String> listFilenames) {
        List<String> ret = new LinkedList<>();
        for (String x : listFilenames) {
            if (SystemUtils.IS_OS_WINDOWS) {
                x = x.split("/")[x.split("/").length - 1].replace(".cif", "");
            } else {
                x = x.split("/")[x.split("/").length - 1].replace(".cif", "");
            }
            ret.add(x);
        }
        return ret;
    }

    /**
     * Reads all the inFiles and returns the corresponding automata as a list.
     * 
     * @param inFiles
     * @return
     * @throws IOException
     * @throws CifEvalException
     */
    private static List<FastDFA<String>> genListOfSubSpecifications(
            List<String> inFiles) throws CifEvalException, IOException {
        List<FastDFA<String>> ret = new LinkedList<>();
        for (String x : inFiles) {
            ret.add(BharatCustomCIFReader.readCIF(x));
        }
        return ret;
    }

    /**
     * Compute a map from action label to name of sub-specification.
     * 
     * @param subSpecificationsMap
     *            map from name of sub-specification to specification DFA
     * @return
     */
    public static Map<String, String> computeActionToSubSpecNames(
            Map<String, FastDFA<String>> subSpecificationsMap) {
        Map<String, String> ret = new HashMap<>();
        for (String subSpecName : subSpecificationsMap.keySet()) {
            for (String action : subSpecificationsMap.get(subSpecName)
                    .getInputAlphabet()) {
                ret.put(action, subSpecName);
            }
        }
        return ret;
    }

    /**
     * Write the computed automata, monitor, invariants etc. I should probably
     * re-write this properly, but it is low priority.
     * 
     * @param options
     * @param constraints
     * @return
     * @throws Exception
     */
    public static boolean writeToOutput(Args options, Set<String> constraints)
            throws Exception {
        FileOutputStream stream = new FileOutputStream(options.getOutFile(),
                false);
        writeDecomposition(options,
                options.getInFiles().subList(1, options.getInFiles().size()),
                stream, constraints, false, null);
        // Copy the original file
        Files.copy(Paths.get(options.getOutFile()),
                Paths.get(options.getOutFile().replace(".", "gen.")),
                StandardCopyOption.REPLACE_EXISTING);
        // Compute the specification using the decomposition
        CIF3operations.exploreStatespaceCIF(options.getOutFile());
        // Check the files for language equivalence
        List<String> filesToCheck = new LinkedList<String>();
        filesToCheck.add(options.getOutFile());
        filesToCheck.add(options.getInFiles().get(0));
        if (CIF3operations.checkEquivalenceCIF(filesToCheck)) {
            System.out.println("Equal");
            return true;
        } else {
            System.out.println("Unequal");
            return false;
        }
    }

    public static boolean writeToOutput(Args options, Set<String> constraints,
            FastDFA<String> monitor) throws Exception {
        FileOutputStream stream = new FileOutputStream(options.getOutFile(),
                false);
        writeDecomposition(options,
                options.getInFiles().subList(1, options.getInFiles().size()),
                stream, constraints, true, monitor);
        // Copy the original file
        Files.copy(Paths.get(options.getOutFile()),
                Paths.get(options.getOutFile().replace(".", "gen.")),
                StandardCopyOption.REPLACE_EXISTING);
        // Compute the specification using the decomposition
        CIF3operations.exploreStatespaceCIF(options.getOutFile(), true);
        // Check the files for language equivalence
        List<String> filesToCheck = new LinkedList<String>();
        filesToCheck.add(options.getOutFile());
        filesToCheck.add(options.getInFiles().get(0));
        if (CIF3operations.checkEquivalenceCIF(filesToCheck)) {
            System.out.println("Equal");
            return true;
        } else {
            System.out.println("Unequal");
            return false;
        }
    }

    private static void writeDecomposition(Args options, List<String> outNames,
            FileOutputStream stream, Set<String> constraints, boolean monitor,
            FastDFA<String> monitor2) throws IOException {
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(stream))) {
            for (String inFile : outNames) {
                List<String> fileContents = FileUtils
                        .readLines(new File(inFile), "utf-8");
                for (String x : fileContents) {
                    writer.write(x + "\n");
                }
                writer.write("\n");
            }
            if (monitor) {
                CIFWriter.writeMonitor(monitor2, writer, "globalMonitor");
                for (String x : constraints) {
                    writer.write(x + "\n");
                }
            } else {
                for (String x : constraints) {
                    writer.write(x);
                }
            }
        }
    }

    /**
     * I have no clue how this works, but it works! I am also <b>not</b> the
     * original author.
     * 
     * @param original
     *            the map to which we will merge <b>newMap</b>
     * @param newMap
     *            the map to merge
     * @return original merged map
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Map deepMerge(Map original, Map newMap) {
        for (Object key : newMap.keySet()) {
            if (newMap.get(key) instanceof Map
                    && original.get(key) instanceof Map) {
                Map originalChild = (Map) original.get(key);
                Map newChild = (Map) newMap.get(key);
                original.put(key, deepMerge(originalChild, newChild));
            } else if (newMap.get(key) instanceof List
                    && original.get(key) instanceof List) {
                List originalChild = (List) original.get(key);
                List newChild = (List) newMap.get(key);
                for (Object each : newChild) {
                    if (!originalChild.contains(each)) {
                        originalChild.add(each);
                    }
                }
            } else {
                original.put(key, newMap.get(key));
            }
        }
        return original;
    }

    /**
     * Converts the Word to type List and then calls the overloaded function
     * again.
     * 
     * @see #projectToAlphabet(List, Alphabet)
     * @param input
     * @param alphabet
     * @return
     */
    public static List<String> projectToAlphabet(Word<String> input,
            Alphabet<String> alphabet) {
        return projectToAlphabet(input.asList(), alphabet);
    }

    /**
     * Obtains the projection of an input sequence over an alphabet and returns
     * it as a new list.
     * 
     * @param input
     * @param alphabet
     * @return projected input sequence
     */
    public static List<String> projectToAlphabet(List<String> input,
            Alphabet<String> alphabet) {
        List<String> projectedInputs = new LinkedList<>();
        projectedInputs.addAll(input);
        projectedInputs.retainAll(alphabet);
        return projectedInputs;
    }

}
