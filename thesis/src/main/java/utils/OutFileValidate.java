package utils;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class OutFileValidate implements IParameterValidator {

	@Override
	public void validate(String name, String value) throws ParameterException {
		if(value.contentEquals("default.out")) {
//			throw new ParameterException("Parameter " + name + " not specified.");
			System.out.println("No output file specified: using default\\.out");
		}
		
	}

}
