package org.matsim.analysis;


import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Based on {@link TransportPlanningMainModeIdentifier}
 * <p>
 * ModeStatsControlerListener takes modes from scoreConfig.getAllModes() and ignores everything else.
 * So keep this in mind before using this class.
 *
 * @author nagel / gleich
 */
public class RuhrgebietIntermodalAnalysisModeIdentifier {

    private final List<String> modeHierarchy = new ArrayList<>();
    private static final Logger log = Logger.getLogger(RuhrgebietIntermodalAnalysisModeIdentifier.class);
    public static final String ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS = "pt_w_drt_used";

    @Inject
    public RuhrgebietIntermodalAnalysisModeIdentifier() {

        modeHierarchy.add(TransportMode.walk);
        modeHierarchy.add(TransportMode.bike);
        modeHierarchy.add("bicycle");
        modeHierarchy.add(TransportMode.ride);
        modeHierarchy.add("scar");
        modeHierarchy.add("sbike");
        modeHierarchy.add(TransportMode.car);
        modeHierarchy.add(TransportMode.pt);


        // NOTE: This hierarchical stuff is not so great: is park-n-ride a car trip or a pt trip?  Could weigh it by distance, or by time spent
        // in respective mode.  Or have combined modes as separate modes.  In any case, can't do it at the leg level, since it does not
        // make sense to have the system calibrate towards something where we have counted the car and the pt part of a multimodal
        // trip as two separate trips. kai, sep'16
    }

    public String identifyMainMode(List<? extends PlanElement> planElements) {
        int mainModeIndex = -1;
        List<String> modesFound = new ArrayList<>();
        for (PlanElement pe : planElements) {
            int index;
            String mode;
            if (pe instanceof Leg) {
                Leg leg = (Leg) pe;
                mode = leg.getMode();
            } else {
                continue;
            }
            if (mode.equals(TransportMode.non_network_walk)) {
                // skip, this is only a helper mode for access, egress and pt transfers
                continue;
            }
            if (mode.equals(TransportMode.transit_walk)) {
                mode = TransportMode.walk;
            }
            modesFound.add(mode);
            index = modeHierarchy.indexOf(mode);
            if (index < 0) {
                throw new RuntimeException("unknown mode=" + mode);
            }
            if (index > mainModeIndex) {
                mainModeIndex = index;
            }
        }
        if (mainModeIndex == -1) {
            throw new RuntimeException("no main mode found for trip " + planElements);
        }

        String mainMode = modeHierarchy.get(mainModeIndex);
        // differentiate pt monomodal/intermodal
        if (mainMode.equals(TransportMode.pt)) {
            boolean isDrtPt = false;
            for (String modeFound : modesFound) {
                if (modeFound.equals(TransportMode.pt)) {
                    continue;
                } else if (modeFound.equals(TransportMode.walk)) {
                    continue;
                } else {
                    log.error("unknown intermodal pt trip: " + planElements);
                    throw new RuntimeException("unknown intermodal pt trip");
                }
            }

            if (isDrtPt) {
                return RuhrgebietIntermodalAnalysisModeIdentifier.ANALYSIS_MAIN_MODE_PT_WITH_DRT_USED_FOR_ACCESS_OR_EGRESS;
            } else {
                return TransportMode.pt;
            }

        } else {
            return mainMode;
        }
    }
}

