/**
 * 
 */
package utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.google.common.collect.Lists;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.util.automata.builders.AutomatonBuilders;
import net.automatalib.util.automata.builders.DFABuilder;
import net.automatalib.util.automata.builders.MealyBuilder;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;

/**
 * @author bgarhewa
 *
 */
public class DOTReader {
    public static class Edge {
        public String from;
        public String to;
        public String label;

        Edge(String b, String e, String l) {
            from = b;
            to = e;
            label = l;
        }
    }

    public Set<String> nodes;
    public Set<Edge> edges;

    DOTReader(Path filename) throws IOException {
        nodes = new HashSet<>();
        edges = new HashSet<>();

        Scanner s = new Scanner(filename);
        while (s.hasNextLine()) {
            String line = s.nextLine();

            if (!line.contains("label"))
                continue;

            if (line.contains("->")) {
                int e1 = line.indexOf('-');
                int e2 = line.indexOf('[');
                int b3 = line.indexOf('"');
                int e3 = line.lastIndexOf('"');

                String from = line.substring(0, e1).trim();
                String to = line.substring(e1 + 2, e2).trim();
                String label = line.substring(b3 + 1, e3).trim();

                edges.add(new Edge(from, to, label));
            } else {
                int end = line.indexOf('[');
                if (end <= 0)
                    continue;
                String node = line.substring(0, end).trim();

                nodes.add(node);
            }
        }
        s.close();
    }

    CompactMealy<String, String> createMealyMachine() {
        Set<String> inputs = new HashSet<>();
        for (DOTReader.Edge e : edges) {
            String[] io = e.label.split("/");
            inputs.add(io[0].trim());
        }

        List<String> inputList = Lists.newArrayList(inputs.iterator());
        Alphabet<String> alphabet = Alphabets.fromList(inputList);

        MealyBuilder<?, String, ?, String, CompactMealy<String, String>>.MealyBuilder__1 builder = AutomatonBuilders
                .<String, String>newMealy(alphabet).withInitial("s0");
        for (DOTReader.Edge e : edges) {
            String[] io = e.label.split("/");

            builder.from(e.from).on(io[0].trim()).withOutput(io[1].trim())
                    .to(e.to);
        }

        return builder.create();
    }

    CompactDFA<String> createMachine() {
        Set<String> inputs = new HashSet<>();
        for (DOTReader.Edge e : edges) {
            String[] io = e.label.split("/");
            inputs.add(io[0].trim());
        }

        List<String> inputList = Lists.newArrayList(inputs.iterator());
        Alphabet<String> alphabet = Alphabets.fromList(inputList);

        DFABuilder<Integer, String, CompactDFA<String>>.DFABuilder__1 builder = AutomatonBuilders
                .newDFA(alphabet).withInitial("s0");
        for (DOTReader.Edge e : edges) {
            String io = e.label.trim();

            builder.from(e.from).on(io).to(e.to);
        }

        CompactDFA<String> ret = builder.create();
        for (int i = 0; i < ret.size(); i++) {
            ret.setAccepting(i, true);
        }
        return ret;
    }

}
