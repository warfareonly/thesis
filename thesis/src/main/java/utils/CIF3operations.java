/**
 * 
 */
package utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;

/**
 * Utility class for performing various CIF operations.
 * 
 * @author Bharat Garhewal
 *
 */
public class CIF3operations {

	/**
	 * Explore the un-timed state-space of the CIF3 specification Can also be used for the parallel composition of
	 * different automata Performs the operation in-place! (Always!)
	 * 
	 * @param fileToExplore
	 * @return the minimized state-space of the CIF3 file being explored
	 * @throws Exception
	 */
	public static String exploreStatespaceCIF(String fileToExplore) throws Exception {
		return cif3SingleFileOperationInPlace(cif3SingleFileOperationInPlace(fileToExplore, "cif3explorer.bat"),
				"cif3dfamin.bat");
	}

	/**
	 * Check if the two input files have the same language.
	 * 
	 * @param files
	 * @return
	 * @throws Exception
	 */
	public static boolean checkEquivalenceCIF(List<String> files) throws Exception {
		if (files.size() > 2) {
			System.err.println(files);
			throw new Exception("Cannot check >2 automata for equivalence!!");
		}
		System.out.println(files);
		String mergedFile = mergeCIF(files, "temp_merged.cif");
		ProcessBuilder pbCommand = new ProcessBuilder("cif3lngeqv.bat", mergedFile);
		StringBuilder builder = new StringBuilder();
		String line = null;
		Process pb = pbCommand.start();
		IOUtils.copy(pb.getErrorStream(), System.out);
		BufferedReader reader = new BufferedReader(new InputStreamReader(pb.getInputStream()));
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
	public static String DFAminimizationCIF(String fileToMinimize) throws Exception {
		return cif3SingleFileOperationInPlace(fileToMinimize, "cif3dfamin.bat");
	}

	/**
	 * Compute the parallel composition of the CIF files specified and store the result in the outFile.
	 * 
	 * @param files
	 * @param outFile
	 * @return the parallel composition file
	 * @throws Exception
	 */
	public static String parallelCompositionCIF(List<String> files, String outFile) throws Exception {
		outFile = mergeCIF(files, outFile);
		return cif3SingleFileOperationInPlace(cif3SingleFileOperationInPlace(outFile, "cif3explorer.bat"),
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
	private static String mergeCIF(List<String> files, String outFile) throws Exception {
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
	private static String cif3SingleFileOperationInPlace(String file, String command) throws Exception {
		ProcessBuilder pbCommand = new ProcessBuilder(command, file, "-o", file);
		System.err.println(command);
		if (command.contains("dfamin")) {
			String[] nameOfResult = file.split("\\\\");
			String result = nameOfResult[nameOfResult.length - 1].replace(".cif", "");
			pbCommand = new ProcessBuilder(command, file, "-n", result, "-o", file);
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
}
