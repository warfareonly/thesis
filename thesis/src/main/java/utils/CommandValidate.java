package utils;

import java.util.LinkedList;
import java.util.List;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class CommandValidate implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        List<String> allowedCommands = new LinkedList<>();
        allowedCommands.add("complement");
        allowedCommands.add("consistent");
        allowedCommands.add("complete");
        allowedCommands.add("minimize");
        allowedCommands.add("monolithic");
        allowedCommands.add("modular");
        allowedCommands.add("guards");
        allowedCommands.add("rguards");
        allowedCommands.add("mguards");
        allowedCommands.add("xguards");
        allowedCommands.add("sanity");
        allowedCommands.add("noop");
        if (!allowedCommands.contains(value)) {
            throw new ParameterException(
                    "Parameter " + name + " has no command " + value);
        }
    }

}
