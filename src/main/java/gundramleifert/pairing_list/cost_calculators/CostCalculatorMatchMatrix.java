package gundramleifert.pairing_list.cost_calculators;

import gundramleifert.pairing_list.MatchMatrix;
import gundramleifert.pairing_list.configs.ScheduleConfig;
import gundramleifert.pairing_list.types.Flight;
import gundramleifert.pairing_list.types.Schedule;

public class CostCalculatorMatchMatrix implements ICostCalculator{

    private final ScheduleConfig properties;

    public CostCalculatorMatchMatrix() {
        this(null);
    }
    public CostCalculatorMatchMatrix(ScheduleConfig properties) {
        this.properties = properties;
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
        return score(schedule.getMatchMatrix());
    }
}
