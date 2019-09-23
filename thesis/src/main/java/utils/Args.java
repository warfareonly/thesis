/**
 * 
 */
package utils;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * @author Bharat Garhewal
 *
 */
public class Args {
    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = { "--command",
            "-c" }, description = "command to execute", validateWith = CommandValidate.class)
    private String command = "noop";

    @Parameter(names = {
            "-x" }, description = "iteration order for monitor computation", validateWith = IterationValidate.class)
    private String iterationOrder = "fwd";

    @Parameter(names = { "--inFiles",
            "-i" }, description = "List of input files", variableArity = true)
    private List<String> inFiles = new ArrayList<String>();

    @Parameter(names = { "--outFile",
            "-o" }, description = "Output file", validateWith = OutFileValidate.class)
    private String outFile = "default.out";

    public void validateOptions() {
        if (command.equalsIgnoreCase("complement") && inFiles.size() != 2) {
            throw new ParameterException(
                    "Complement operation must have a model and a reference");
        }
        if (command.equalsIgnoreCase("complete") && inFiles.size() == 0) {
            throw new ParameterException(
                    "Complement operation must have a model and a reference");
        }
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @return the inFiles
     */
    public List<String> getInFiles() {
        return inFiles;
    }

    /**
     * @return the outFile
     */
    public String getOutFile() {
        return outFile;
    }

}
