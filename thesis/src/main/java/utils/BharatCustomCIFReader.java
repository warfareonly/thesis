/**
 * 
 */
package utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.google.common.base.Preconditions;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.words.impl.SimpleAlphabet;
import nl.tue.app.framework.AppEnv;
import nl.tue.cif.v3x0x0.common.CifEdgeUtils;
import nl.tue.cif.v3x0x0.common.CifEvalException;
import nl.tue.cif.v3x0x0.common.CifEventUtils;
import nl.tue.cif.v3x0x0.common.CifTextUtils;
import nl.tue.cif.v3x0x0.io.CifReader;
import nl.tue.cif.v3x0x0.metamodel.cif.Specification;
import nl.tue.cif.v3x0x0.metamodel.cif.automata.Edge;
import nl.tue.cif.v3x0x0.metamodel.cif.automata.Location;
import nl.tue.cif.v3x0x0.metamodel.cif.declarations.Event;
import nl.tue.cif.v3x0x0.metamodel.cif.automata.Automaton;

//import java.lang.String;
/**
 * A custom reader for CIF specification files, nothing more.
 * 
 * @author Bharat Garhewal
 *
 */
public class BharatCustomCIFReader {

	/**
	 * Read and parse the (deterministic) automaton stored in the parameter file.
	 * 
	 * @param file
	 * @return the DFA stored in the specified file
	 * @throws CifEvalException
	 * @throws IOException
	 */
	public static CompactDFA<String> readCIF(java.lang.String file) throws CifEvalException, IOException {
		CompactDFA<String> dfa = new CompactDFA<String>(new SimpleAlphabet<String>());
		dfa.addIntInitialState();
		Map<java.lang.String, Integer> cifLocationAutoamatonMap = new HashMap<java.lang.String, Integer>();
		CifReader reader = new CifReader();
		AppEnv.registerSimple();
		reader.init(file, file, false);
		@SuppressWarnings("resource")
		String text = new Scanner(new File(file)).useDelimiter("\\Z").next();
		Specification spec = reader.read(text);
		Automaton aut = (Automaton) spec.getComponents().get(0);
		for (Location loc : aut.getLocations()) {
			boolean initial = loc.getInitials().size() == 1 ? true : false;
			boolean marked = loc.getMarkeds().size() != 1 ? false : true;
			for (Edge edge : loc.getEdges()) {
//				Preconditions.checkArgument(edge.getEvents().size() == 1);
				Event event = CifEventUtils.getEventFromEdgeEvent(edge.getEvents().get(0));
				String eventName = CifTextUtils.getName(event);
				if (!dfa.getInputAlphabet().containsSymbol(eventName)) {
					dfa.addAlphabetSymbol(eventName);
				}
			}
			if (initial) {
				dfa.setAccepting(dfa.getIntInitialState(), marked);
				cifLocationAutoamatonMap.put(CifTextUtils.getName(loc), dfa.getInitialState());
			} else {
				Integer number = dfa.addState(marked);
				cifLocationAutoamatonMap.put(CifTextUtils.getName(loc), number);
			}

		}

		for (Location loc : aut.getLocations()) {
			for (Edge edge : loc.getEdges()) {
				Location targetLoc = CifEdgeUtils.getTarget(edge);
//				Preconditions.checkArgument(edge.getEvents().size() == 1);
				Event event = CifEventUtils.getEventFromEdgeEvent(edge.getEvents().get(0));
				String eventName = CifTextUtils.getName(event);
				dfa.addTransition(cifLocationAutoamatonMap.get(loc.getName()), eventName,
						cifLocationAutoamatonMap.get(targetLoc.getName()));
			}
		}
		AppEnv.unregisterThread();
		return dfa;
	}

}
