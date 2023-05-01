package gundramleifert.pairing_list.cost_calculators;

import gundramleifert.pairing_list.MatchMatrix;
import gundramleifert.pairing_list.configs.ScheduleConfig;
import gundramleifert.pairing_list.types.Flight;
import gundramleifert.pairing_list.types.Schedule;

public class CostCalculatorMatchMatrix {

    private final ScheduleConfig properties;
    private final MatchMatrix matchMatrixBase;

    public CostCalculatorMatchMatrix() {
        this(null,null);
    }
    public CostCalculatorMatchMatrix(ScheduleConfig properties, MatchMatrix matchMatrixBase) {
        this.properties = properties;
        this.matchMatrixBase = matchMatrixBase;
    }

    public double scoreWithCache(Flight fligth) {
        if (!fligth.scoreMap.containsKey(this)) {
            fligth.scoreMap.put(this, score(fligth));
        }
        return fligth.scoreMap.get(this);
    }

    public double score(Flight flight) {
        MatchMatrix mm = new MatchMatrix(matchMatrixBase);
        mm.add(flight, false);
        return score(mm);
    }

    public double score(MatchMatrix matchMatrix) {
        double res = 0;
        double avg = matchMatrix.avg();
        for (int i = 0; i < matchMatrix.mat.length; i++) {
            byte[] vec = matchMatrix.mat[i];
            for (int j = 0; j < i; j++) {
                double diff = vec[j] - avg;
                res += Math.abs(diff * diff * diff);
            }
        }
        return res;
    }

    public double score(Schedule schedule) {
        return score(schedule.matchMatrix);
    }
}
