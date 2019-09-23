/**
 * 
 */
package utils;

import java.util.HashSet;
import java.util.Set;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * @author Bharat Garhewal
 *
 */
public class IterationValidate implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
        Set<String> allowedIterationOrders = new HashSet<String>();
        allowedIterationOrders.add("fwd");
        allowedIterationOrders.add("bwd");
        allowedIterationOrders.add("rnd");
        if (!allowedIterationOrders.contains(value)) {
            throw new ParameterException("Parameter " + name
                    + " has no valid iteration order " + value);
        }
    }

}
