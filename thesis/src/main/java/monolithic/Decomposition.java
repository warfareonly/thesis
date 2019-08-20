package monolithic;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.fsa.DFAs;
import net.automatalib.words.Alphabet;
import nl.tue.cif.v3x0x0.common.CifEvalException;
import utils.Reader;

public class Decomposition {
	boolean modular;
	CompactDFA<String> specification;
	List<CompactDFA<String>> listInterfaces = new LinkedList<CompactDFA<String>>();
	List<String> files = null;

	public Decomposition(List<String> inFiles) throws CifEvalException, IOException {
		this.specification = Reader.read(inFiles.get(0));
		this.specification = DFAs.complete(this.specification, this.specification.getInputAlphabet());
		this.listInterfaces = new LinkedList<CompactDFA<String>>();
		this.files = new LinkedList<String>();
		for (int i = 1; i < inFiles.size(); i++) {
			CompactDFA<String> x = Reader.read(inFiles.get(i));
			this.listInterfaces.add(DFAs.complete(x, x.getInputAlphabet()));
			this.files.add(inFiles.get(i));
		}
		this.validate();
	}

	/*
	 * Check if the alphabet of the specification model is contains the alphabet of all the interface models. If it does
	 * not, then throw an exception (and exit, being implied).
	 * 
	 * Also complain if there are no interface models provided. I like complaining.
	 */
	private void validate() {
		Alphabet<String> specAlphabet = this.specification.getInputAlphabet();
		int i = 0;
		for (CompactDFA<String> dfa : listInterfaces) {
			if (!specAlphabet.containsAll(dfa.getInputAlphabet())) {
				try {
					throw new Exception(
							"Interface " + this.files.get(i) + " is not contained in the" + " specification model.");
				} catch (Exception e) {
					// Do nothing;
				}
			}
			i++;
		}
		if (listInterfaces.size() == 0) {
			try {
				throw new Exception("No interface models provided.");
			} catch (Exception e) {
				// Do nothing;
			}
		}

	}
}
