package simulation;

import org.javatuples.Pair;

import monitors.ModifyMonitorMonolithic;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.util.automata.copy.AutomatonCopyMethod;
import net.automatalib.util.automata.copy.AutomatonLowLevelCopy;
import refac.StoppingCondition;

/**
 * 
 * @author Bharat Garhewal
 *
 */
public class SimulationMonitor {

    private FastDFA<String> specification;
    private FastDFA<String> product;
    private FastNFA<String> monitor;
    private StoppingCondition stopCondition;

    public SimulationMonitor(FastDFA<String> spec, FastDFA<String> product)
            throws Exception {
        this.specification = spec;
        this.product = product;

        // monitor initialization
        this.monitor = new FastNFA<String>(
                this.specification.getInputAlphabet());
        this.monitor.addInitialState(true);
        if (!MonitorSanityChecker.checkMonitor(specification, product,
                this.monitor)) {
            this.monitor.clear();
            AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                    this.specification, this.specification.getInputAlphabet(),
                    this.monitor);
            this.stopCondition = new StoppingCondition(this.monitor);
            computeMonitor();
        }
    }

    public void computeMonitor() throws Exception {
        Integer count = 0;
        Integer total_count = stopCondition.getNumPairs();
        while (true) {
            // Declare a safe monitor variable
            FastNFA<String> monitorSafe = new FastNFA<String>(
                    this.monitor.getInputAlphabet());

            // Copy the current, safe, monitor to monitorSafe
            AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                    this.monitor, this.monitor.getInputAlphabet(), monitorSafe);

            Pair<FastNFAState, FastNFAState> statePairToMerge = this.stopCondition
                    .getNextPairOfStates();
            System.out.println("count " + count + " of " + total_count);

            // Ran out of state-pairs, so return the current monitor.
            if (null == statePairToMerge) {
                return;
            } else {
                ModifyMonitorMonolithic mod = new ModifyMonitorMonolithic(
                        this.monitor, statePairToMerge);
                // this.monitor, setPairs);
                this.monitor = new FastNFA<String>(
                        this.specification.getInputAlphabet());
                this.monitor.clear();
                AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                        mod.getMonitor(), this.specification.getInputAlphabet(),
                        this.monitor);
            }
            boolean isMonitorCorrect = MonitorSanityChecker.checkMonitor(
                    this.specification, this.product, this.monitor);
            if (!isMonitorCorrect) {
                // System.out.println(
                // "Since the monitor was incorrect, we are rolling back to the
                // previous one");
                this.monitor = new FastNFA<String>(
                        monitorSafe.getInputAlphabet());
                AutomatonLowLevelCopy.copy(AutomatonCopyMethod.STATE_BY_STATE,
                        monitorSafe, monitorSafe.getInputAlphabet(),
                        this.monitor);
                MonitorSanityChecker.checkMonitor(specification, product,
                        this.monitor);
                // To check for the correct monitor after the backtracking
                MonitorSanityChecker.writeCompositionOut(product, this.monitor);
                count++;
            } else {
                count = 0;
                stopCondition = new StoppingCondition(monitor);
                total_count = stopCondition.getNumPairs();
            }
        }

    }

    /**
     * @return the monitor
     */
    public FastNFA<String> getMonitor() {
        return monitor;
    }
}
