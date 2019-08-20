package utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

public class BharatXESReader {

	public static List<String> readXES(String filename) {
		List<String> ret = new LinkedList<String>();
		try (Stream<String> stream = Files.lines(Paths.get(filename))) {
			// stream.forEach(System.out::println);
			Iterator<String> iteratorStream = stream.iterator();
			String currLine = "";
			while (iteratorStream.hasNext()) {
				String line = iteratorStream.next();
				if (line.contains("xes:trace")) {
					ret.add(currLine);
					currLine = "";
				}
				if (line.contains("KSXAx")) {
					String[] x = line.split("\\\"");
					for (int i = 0; i < x.length - 1; i++) {
						if (x[i].contains("KSXAx")) {
							line = x[i];
						}
					}
					currLine = currLine + " " + line;
				}
				if (line.contains("lifecycle:transition")) {
					if (line.contains("start")) {
						currLine = currLine + "_start";
					} else if (line.contains("complete")) {
						currLine = currLine + "_complete";
					}

				}

			}
		} catch (

		IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	public static List<Word<String>> getTraces(List<String> listTraces, Alphabet<String> alphabet) {
		List<Word<String>> wordsList = new LinkedList<>();
		for (String trace : listTraces) {
			WordBuilder<String> wb = new WordBuilder<String>();
			List<String> strings = new LinkedList<String>();
			(new ArrayList<String>(Arrays.asList(trace.split(" ")))).forEach(x -> strings.add(x.trim()));
			for (String x : strings) {
				for (String alphabetX : alphabet) {
					if (x.contains(alphabetX)) {
						wb.append(alphabetX);
					}
				}
			}
			wordsList.add(wb.toWord());
			wb.clear();
		}
		return wordsList;
	}
}
