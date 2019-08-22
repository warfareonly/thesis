/**
 * 
 */
package refac;

import java.util.List;

import net.automatalib.automata.fsa.impl.FastDFA;
import utils.BharatCustomCIFReader;
import utils.CIF3operations;

/**
 * @author Bharat Garhewal
 *
 */
public class Product {

	/**
	 * Computes the parallel composition of the specificed files
	 * @param inFiles the list of input files provided as a list of Strings
	 * @return {@literal FastDFA<String>} of the product
	 * @throws Exception
	 */
	public static FastDFA<String> computeProduct(List<String> inFiles) throws Exception {
		CIF3operations.parallelCompositionCIF(inFiles, "product.cif");
		return BharatCustomCIFReader.readCIF("product.cif");
	}
}
