/**
 * 
 */
package monolithic;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.fsa.DFAs;
import net.automatalib.words.impl.SimpleAlphabet;
import nl.tue.cif.v3x0x0.common.CifEvalException;
import utils.CIFWriter;
import utils.Reader;

import org.apache.commons.io.IOUtils;

/**
 * @author Bharat Garhewal
 *
 */
public class MonolithicDecompose {
	CompactDFA<String> specification;
	List<CompactDFA<String>> listInterfaces = new LinkedList<CompactDFA<String>>();
	List<String> files = null;

	public MonolithicDecompose(List<String> inFiles) throws CifEvalException, IOException {
		this.specification = Reader.read(inFiles.get(0));
		this.specification = DFAs.complete(this.specification, this.specification.getInputAlphabet());
		this.listInterfaces = new LinkedList<CompactDFA<String>>();
		this.files = new LinkedList<String>();
		for (int i = 1; i < inFiles.size(); i++) {
			CompactDFA<String> x = Reader.read(inFiles.get(i));
			this.listInterfaces.add(DFAs.complete(x, x.getInputAlphabet()));
			this.files.add(inFiles.get(i));
		}
		// this.validate();
		// super(inFiles);
	}

	/*
	 * Follow the procedure in "The Unknown Component Problem" book to find the resulting "component" (or as we call it,
	 * the monolithic controller)
	 */
	public CompactDFA<String> computeController() {
		CompactDFA<String> complementOfSpecficiationSoFar = DFAs.complement(this.specification,
				this.specification.getInputAlphabet());
		for (CompactDFA<String> interfaceDFA : this.listInterfaces) {
			complementOfSpecficiationSoFar = removeInterface(complementOfSpecficiationSoFar, interfaceDFA);
		}
		// Return a minimal DFA, not a big one (for reasons obvious)
		// Mostly cause the big ones are difficult to read (by humans)
		// So why make it more complicated than necessary?
		CompactDFA<String> x = DFAs.complement(complementOfSpecficiationSoFar,
				complementOfSpecficiationSoFar.getInputAlphabet());
		System.out.println(x);
		return x;
	}

	private CompactDFA<String> computeProduct() throws CifEvalException, IOException {
		System.out.println("Compute Product Called!");
		ProcessBuilder pbMerge = new ProcessBuilder("cif3merge.bat", "complementOfSpecficiationSoFar.cif",
				"interface.cif", "-o", "merged.cif");
		ProcessBuilder pbProduct = new ProcessBuilder("cif3prod.bat", "merged.cif", "-o", "product.cif");
		try {
			Process pb = pbMerge.start();
			IOUtils.copy(pb.getErrorStream(), System.out);
			System.out.println("Merge exit value: " + pb.waitFor());
			System.out.println("Merged");
			pb = pbProduct.start();
			IOUtils.copy(pb.getErrorStream(), System.out);
			System.out.println("Product exit value: " + pb.waitFor());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Reader.read("product.cif");
	}

	/*
	 * Write out the complement of C and A, and then compute A || (complement of C) Read it back and return it.
	 */
	private CompactDFA<String> removeInterface(CompactDFA<String> complementOfSpecficiationSoFar,
			CompactDFA<String> interfaceDFA) {
		CompactDFA<String> ret = new CompactDFA<String>(new SimpleAlphabet<String>());
		try {
			System.out.println("removeInterface Called!");
			CIFWriter.writeCIF(complementOfSpecficiationSoFar,
					new FileOutputStream("complementOfSpecficiationSoFar.cif"), "component");
			CIFWriter.writeCIF(interfaceDFA, new FileOutputStream("interface.cif"), "interface");
			ret = computeProduct();
		} catch (FileNotFoundException e) {
			// Do nothing
			e.printStackTrace();
		} catch (IOException e) {
			// Do nothing
			e.printStackTrace();
		} catch (CifEvalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

}
