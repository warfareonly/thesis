/**
 * 
 */
package utils;

import java.io.FileInputStream;
import java.io.IOException;

import com.asml.automatalib.extensions.util.Dot2Dfa;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.words.impl.SimpleAlphabet;
import nl.tue.cif.v3x0x0.common.CifEvalException;

/**
 * @author bgarhewa
 * 
 *
 */
public class Reader {
	
	public Reader() {
		super();
	}

	public static CompactDFA<String> read(String filename) throws CifEvalException, IOException {
		if (filename.split("\\.")[1].equalsIgnoreCase("dot")) {
			CompactDFA<String> dfa = new CompactDFA<String>(new SimpleAlphabet<String>());
			dfa = (new Dot2Dfa()).readDot(dfa, new FileInputStream(filename));
			return dfa;

		} else if (filename.split("\\.")[1].equalsIgnoreCase("cif")) {
			System.out.println(filename);
			return BharatCustomCIFReader.readCIF(filename);
		}
		return null;

	}

}
