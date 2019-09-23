import java.io.FileOutputStream;
import java.nio.file.Paths;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import simulation.SimulationDecomp;
import simulation.SimulationRelationImpl;
import utils.Args;
import utils.BharatCustomCIFReader;
import utils.CIFWriter;
import utils.DOTReader;
import com.beust.jcommander.JCommander;

/**
 * Main class, which controls everything!
 * 
 * @author Bharat Garhewal
 *
 */
public class Main {

    /**
     * Constructor for main
     * 
     * @param args
     *            command line arguments for the program
     * @throws Exception
     *             exception
     */
    public static void main(String[] args) throws Exception {
        Args options = new Args();
        JCommander.newBuilder().addObject(options).build().parse(args);
        options.validateOptions();
        performCommand(options);
    }

    public static void performCommand(Args options) throws Exception {

        FastDFA<String> dfaSpecification = BharatCustomCIFReader
                .readCIF(options.getInFiles().get(0));
        if (options.getCommand().equalsIgnoreCase("sim")) {
            // SimulationDecomposition simDecomp = new SimulationDecomposition(
            // options);
            SimulationDecomp simDecomp = new SimulationDecomp(options);
            // simDecomp.computeRequirements();
            // System.out.println(simDecomp.getRequirements());
            System.exit(0);
            return;
        } else if (options.getCommand().contains("cons")) {
            FastNFA<String> product = BharatCustomCIFReader
                    .readNonDetCIF(options.getInFiles().get(1));
            SimulationRelationImpl simulationRelation = new SimulationRelationImpl(
                    dfaSpecification, product);
            System.out.println("Specification is simulated by product: "
                    + simulationRelation.checkIfSimulationRelationExists());
            return;
        } else if (options.getCommand().contains("build")) {
            DOTReader drReader = new DOTReader(
                    Paths.get("ASML_testCase", "custom", "specification.dot"));
            CompactDFA<String> cDFALOEW = drReader.createMachine();
            FastDFA<String> loew = new FastDFA<String>(
                    cDFALOEW.getInputAlphabet());
            loew.clear();
            AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                    cDFALOEW, cDFALOEW.getInputAlphabet(), loew);
            FileOutputStream loewStream = new FileOutputStream(
                    "ASML_testCase/custom/specification.cif");
            CIFWriter.writeCIF(loew, loewStream, "specification", true);
        }
    }
}
