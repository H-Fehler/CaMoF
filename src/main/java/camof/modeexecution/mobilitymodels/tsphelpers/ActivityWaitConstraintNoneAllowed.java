package camof.modeexecution.mobilitymodels.tsphelpers;

import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

public class ActivityWaitConstraintNoneAllowed implements HardActivityConstraint {

    @Override
    public ConstraintsStatus fulfilled(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct, TourActivity nextAct, double prevActDepTime) {
        TransportCosts tc = new TransportCosts();
        double transportTime = tc.getTransportTime(prevAct.getLocation(), newAct.getLocation(), prevActDepTime,
                null,null);
        double arrTimeAtNewAct = prevActDepTime + transportTime;
        double transportTime2 = tc.getTransportTime(newAct.getLocation(),nextAct.getLocation(),arrTimeAtNewAct,null,null);
        double diff = newAct.getTheoreticalEarliestOperationStartTime() - arrTimeAtNewAct;
        double arrTimeAtNextAct = arrTimeAtNewAct + transportTime2;
        double diff2 = nextAct.getTheoreticalEarliestOperationStartTime() - arrTimeAtNextAct;
        if(diff>0 || diff2>0){
            return ConstraintsStatus.NOT_FULFILLED;
        }
        return ConstraintsStatus.FULFILLED;
    }

}
