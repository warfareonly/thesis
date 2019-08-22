/**
 * 
 */
package invariant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for generating invariant "statements" like the ones used in CIF.
 * 
 * @author Bharat Garhewal
 *
 */
public class InvariantStatement {

    String actionName;
    Integer positionAction;
    List<String> accessSequenceAction = new LinkedList<String>();
    String subSpecName;
    Map<String, Set<Integer>> subSpecRequirements = new HashMap<String, Set<Integer>>();

    /**
     * Constructor
     * 
     * @param actName
     * @param posAction
     * @param accSeqAction
     * @param subSpecName
     * @param subSpecReq
     */
    public InvariantStatement(String actName, Integer posAction,
            List<String> accSeqAction, String subSpecName,
            Map<String, Set<Integer>> subSpecReq) {
        this.actionName = actName;
        this.positionAction = posAction;
        this.accessSequenceAction = new LinkedList<String>();
        if (null != accSeqAction)
            this.accessSequenceAction.addAll(accSeqAction);
        this.subSpecName = subSpecName;
        this.subSpecRequirements = subSpecReq;
    }

    /**
     * Generate "unused" action invariant. That is, generates an invariant that
     * blocks an action "actionName" from occurring at state "posAction". We
     * need the name of the sub-specification in order to specify the position,
     * as our states are called "s1" or "s2" or so on for <i>all</i> our
     * sub-specifications. <b> Note: </b>Values of states are incremented by one
     * in order to account for CIF3's insistence of starting from "s1" when
     * minimizing automata.
     * 
     * @param actionName
     * @param posAction
     * @param subSpecName
     * @return
     */
    public static String InvariantStatementUnusedBuild(String actionName,
            Integer posAction, String subSpecName) {
        String invStatement = "invariant " + actionName + " needs not ";
        invStatement = invStatement + subSpecName + ".s" + (posAction + 1)
                + ";\n";
        return (invStatement);

    }

    /**
     * Construct the invariant statement associated with the values stored in
     * the object. <b> Note: </b>Values of states are incremented by one in
     * order to account for CIF3's insistence of starting from "s1" when
     * minimizing automata.
     */
    public String buildInvariantStatement() {
        String inv = "invariant ";
        inv = inv + actionName + " needs ( ";
        inv = inv + subSpecName + ".s" + (positionAction + 1) + " ";
        // inv = inv + "and ";
        Set<String> processSet = subSpecRequirements.keySet();
        processSet.remove(subSpecName);
        Iterator<String> processIterator = processSet.iterator();
        while (processIterator.hasNext()) {
            String process = processIterator.next();
            inv = inv + "and ( ";
            Iterator<Integer> stateIterator = subSpecRequirements.get(process)
                    .iterator();
            while (stateIterator.hasNext()) {
                Integer state = stateIterator.next();
                inv = inv + process + ".s" + (state + 1) + " ";
                if (stateIterator.hasNext()) {
                    inv = inv + "or ";
                }
            }
            inv = inv + ")";
            // if (processIterator.hasNext()) {
            // inv = inv + " and ";
            // }
        }
        inv = inv + ") or not " + subSpecName + ".s" + (positionAction + 1)
                + ";\n";
        return inv;
    }
}
