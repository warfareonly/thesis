/**
 * 
 */
package simulation;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Bharat Garhewal
 *
 */
public class Requirements {

    public static String makeRequirement(Set<Integer> locationsToBlock,
            Map<Integer, Integer> productMonitorToMonitorMap,
            Map<Integer, Map<String, Integer>> productMonitorToSubSpecificationMap) {
        String ret = "";
        Iterator<Integer> locationIterator = locationsToBlock.iterator();
        while (locationIterator.hasNext()) {
            Integer productMonitorLocation = locationIterator.next();
            Integer monitorLocation = productMonitorToMonitorMap
                    .get(productMonitorLocation);
            Map<String, Integer> subSpecificationLocationMap = productMonitorToSubSpecificationMap
                    .get(productMonitorLocation);
            ret = ret + " not ( " + makeRequirementForSingleLocation(
                    monitorLocation, subSpecificationLocationMap) + " )";
            if (locationIterator.hasNext()) {
                ret = ret + " and";
            }
        }
        ret = ret + " ;\n";
        return ret;
    }

    private static String makeRequirementForSingleLocation(
            Integer monitorLocation,
            Map<String, Integer> subSpecificationLocationMap) {
        String ret = "";
        ret = ret + " " + "globalMonitor.s" + (monitorLocation + 1)
                + " and ";
        Iterator<String> subSpecificationIterator = subSpecificationLocationMap
                .keySet().iterator();
        while (subSpecificationIterator.hasNext()) {
            String subSpecName = subSpecificationIterator.next();
            Integer subSpecLocation = subSpecificationLocationMap
                    .get(subSpecName);
            ret = ret + " " + subSpecName + ".s" + (subSpecLocation + 1)
                    + "";
            if (subSpecificationIterator.hasNext()) {
                ret = ret + " and ";
            }
        }
        ret = ret + "";
        return ret;
    }

}
