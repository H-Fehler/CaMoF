package camos.modeexecution.mobilitymodels.tsphelpers;

import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.End;
import com.graphhopper.jsprit.core.problem.solution.route.activity.Start;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;

public class ActivityOrderConstraint implements HardActivityConstraint {
    @Override
    public ConstraintsStatus fulfilled(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct, TourActivity nextAct, double prevActDepTime) {
        if(prevAct instanceof Start && nextAct instanceof End){
            return ConstraintsStatus.FULFILLED;
        }else if(prevAct instanceof Start){
            int newPriority = ((TourActivity.JobActivity) newAct).getJob().getPriority();
            int nextPriority = ((TourActivity.JobActivity) nextAct).getJob().getPriority();
            if(newPriority <= nextPriority){
                return ConstraintsStatus.FULFILLED;
            }else{
                return ConstraintsStatus.NOT_FULFILLED;
            }
        }else if(nextAct instanceof End){
            int prevPriority = ((TourActivity.JobActivity) prevAct).getJob().getPriority();
            int newPriority = ((TourActivity.JobActivity) newAct).getJob().getPriority();
            if(prevPriority <= newPriority){
                return ConstraintsStatus.FULFILLED;
            }else{
                return ConstraintsStatus.NOT_FULFILLED;
            }
        }

        int prevPriority = ((TourActivity.JobActivity) prevAct).getJob().getPriority();
        int newPriority = ((TourActivity.JobActivity) newAct).getJob().getPriority();
        int nextPriority = ((TourActivity.JobActivity) nextAct).getJob().getPriority();

        if(prevPriority<=newPriority && newPriority <= nextPriority){
            return ConstraintsStatus.FULFILLED;
        }else{
            return ConstraintsStatus.NOT_FULFILLED;
        }
    }
}
