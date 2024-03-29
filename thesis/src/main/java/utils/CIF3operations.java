/**
 * 
 */
package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;

import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.automata.simple.SimpleAutomaton;
import net.automatalib.serialization.InputModelData;
import net.automatalib.serialization.aut.AUTParser;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import net.automatalib.words.Alphabet;
import nl.tue.cif.v3x0x0.eventbased.equivalence.LangEquivCalculation;

/**
 * Utility class for performing various CIF operations.
 * 
 * @author Bharat Garhewal
 *
 */
public class CIF3operations {

    /**
     * Explore the un-timed state-space of the CIF3 specification Can also be
     * used for the parallel composition of different automata Performs the
     * operation in-place! (Always!)
     * 
     * @param fileToExplore
     * @return the minimized state-space of the CIF3 file being explored
     * @throws Exception
     */
    public static String exploreStatespaceCIF(String fileToExplore)
            throws Exception {
        return cif3SingleFileOperationInPlace(cif3SingleFileOperationInPlace(
                fileToExplore, "cif3explorer.bat"), "cif3dfamin.bat");
    }

    /**
     * Explore the un-timed state-space of the CIF3 specification Can also be
     * used for the parallel composition of different automata Performs the
     * operation in-place! (Always!)
     * 
     * @param fileToExplore
     * @param minimize
     *            <i>true</i> is automaton has to be minimized, <i>false</i>
     *            otherwise.
     * @return the minimized state-space of the CIF3 file being explored
     * @throws Exception
     */
    public static String exploreStatespaceCIF(String fileToExplore,
            boolean minimize) throws Exception {
        if (minimize) {
            return cif3SingleFileOperationInPlace(
                    cif3SingleFileOperationInPlace(fileToExplore,
                            "cif3explorer.bat"),
                    "cif3dfamin.bat");
        } else {
            return cif3SingleFileOperationInPlace(fileToExplore,
                    "cif3explorer.bat");
        }
    }

    //
    // @SuppressWarnings("restriction")
    // public static void directCIF() {
    // List<String> x = new LinkedList<>();
    // x.add("--gui=off");
    // x.add("-report=xyz");
    // // x.add("~/Desktop/thesis/git/chefs/decomposedgen.cif");
    // x.add("chefs/decomposedgen.cif");
    // x.add("-o");
    // // x.add("~/Desktop/thesis/git/chefs/decomposed.cif");
    // x.add("chefs/decomposed.cif");
    // try {
    // nl.tue.cif.v3x0x0.explorer.app.ExplorerApplication.main(x.toArray(new
    // String[x.size()]));
    // } catch (Exception e) {
    // }
    // List<String> p = new LinkedList<>();
    // p.add("-h");
    // // p.add("chefs/decomposed.cif");
    // // p.add("chefs/specification.cif");
    // nl.tue.cif.v3x0x0.eventbased.apps.LanguageEquivalenceCheckApplication.main(p.toArray(new
    // String[p.size()]));
    // return;
    // }
    //
    /**
     * Check if the two input files have the same language.
     * 
     * @param files
     * @return
     * @throws Exception
     */
    public static boolean checkEquivalenceCIF(List<String> files)
            throws Exception {
        if (files.size() > 2) {
            System.err.println(files);
            throw new Exception("Cannot check >2 automata for equivalence!!");
        }
        System.out.println(files);
        String mergedFile = mergeCIF(files, "temp_merged.cif");
        ProcessBuilder pbCommand = new ProcessBuilder("cif3lngeqv.bat",
                mergedFile);
        StringBuilder builder = new StringBuilder();
        String line = null;
        Process pb = pbCommand.start();
        IOUtils.copy(pb.getErrorStream(), System.out);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(pb.getInputStream()));
        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        String result = builder.toString();
        if (result.contains("differ")) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Minimize DFA in the specified file.
     * 
     * @param fileToMinimize
     * @return
     * @throws Exception
     */
    public static String DFAminimizationCIF(String fileToMinimize)
            throws Exception {
        return cif3SingleFileOperationInPlace(fileToMinimize, "cif3dfamin.bat");
    }

    /**
     * Compute the parallel composition of the CIF files specified and store the
     * result in the outFile.
     * 
     * @param files
     * @param outFile
     * @return the parallel composition file
     * @throws Exception
     */
    public static String parallelCompositionCIF(List<String> files,
            String outFile) throws Exception {
        outFile = mergeCIF(files, outFile);
        return cif3SingleFileOperationInPlace(
                cif3SingleFileOperationInPlace(outFile, "cif3explorer.bat"),
                "cif3dfamin.bat");
    }

    /**
     * Merge the specified files into a single outFile.
     * 
     * @param files
     * @param outFile
     * @return
     * @throws Exception
     */
    private static String mergeCIF(List<String> files, String outFile)
            throws Exception {
        System.err.println("merge.bat");
        List<String> xyz = new LinkedList<String>();
        xyz.addAll(files);
        xyz.add(0, "cif3merge.bat");
        xyz.add("-o");
        xyz.add(outFile);
        ProcessBuilder pbMerge = new ProcessBuilder(xyz);
        Process pb = pbMerge.start();
        IOUtils.copy(pb.getErrorStream(), System.out);
        return outFile;
    }

    /**
     * Generic wrapper for a single file function.
     * 
     * @param file
     * @param command
     * @return
     * @throws Exception
     */
    private static String cif3SingleFileOperationInPlace(String file,
            String command) throws Exception {
        ProcessBuilder pbCommand = new ProcessBuilder(command, file, "-o",
                file);
        // directCIF();
        // System.err.println(command);
        if (command.contains("dfamin")) {
            String[] nameOfResult = file.split("/");
            String result = nameOfResult[nameOfResult.length - 1]
                    .replace(".cif", "");
            pbCommand = new ProcessBuilder(command, file, "-n", result, "-o",
                    file);
        } else {
            pbCommand = new ProcessBuilder(command, file, "-o", file);
        }
        Process pb = pbCommand.start();
        IOUtils.copy(pb.getErrorStream(), System.out);
        if (pb.waitFor() != 0) {
            throw new Exception("Something wrong with the process!!");
        }
        return file;
    }

    public static String mcrl2CompositionOperation(String file)
            throws InterruptedException, Exception {
        ProcessBuilder cif2mcrl2Command = new ProcessBuilder("cif3mcrl2.bat", file,
                "-o", "composedProcess.mcrl2");
        Process pb = cif2mcrl2Command.start();
        IOUtils.copy(pb.getErrorStream(), System.out);
        if (pb.waitFor() != 0) {
            throw new Exception(
                    "Something wrong with the mcrl2 conversion process!!");
        }

        ProcessBuilder mcrl22lpsCommand = new ProcessBuilder("mcrl22lps.exe",
                "composedProcess.mcrl2", "composedProcess.lps");
        pb = mcrl22lpsCommand.start();
        IOUtils.copy(pb.getErrorStream(), System.out);
        if (pb.waitFor() != 0) {
            throw new Exception(
                    "Something wrong with the mcrl22lps conversion process!!");
        }
        ProcessBuilder lps2ltsCommand = new ProcessBuilder("lps2lts.exe",
                "composedProcess.lps", "composedProcess.aut");
        pb = lps2ltsCommand.start();
        IOUtils.copy(pb.getErrorStream(), System.out);
        if (pb.waitFor() != 0) {
            throw new Exception(
                    "Something wrong with the lps2lts conversion process!!");
        }
        FileInputStream inStream = new FileInputStream("composedProcess.aut");
        CompactNFA<String> compactNFA = (new BharatAUTParser(inStream))
                .parse(Function.identity());
        FastNFA<String> comp = new FastNFA<String>(
                compactNFA.getInputAlphabet());
        comp.clear();
        AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                compactNFA, compactNFA.getInputAlphabet(), comp);
        FileOutputStream outStream = new FileOutputStream(
                file);
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(outStream))) {
            BharatCustomCIFWriter.writeNFA(comp, writer);

        }
        return file;
    }
}
