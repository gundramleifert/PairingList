package gundram.leifert.sail_schedule;

import gundram.leifert.sail_schedule.types.Schedule;

public interface ICostCalculator {
    double score(Schedule schedule);

    default double scoreWithCache(Schedule schedule) {
        if (!schedule.scoreMap.containsKey(this)) {
            schedule.scoreMap.put(this, score(schedule));
        }
        return schedule.scoreMap.get(this);
    }

}
