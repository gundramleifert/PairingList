package gundramleifert.pairing_list.cost_calculators;

import gundramleifert.pairing_list.MatchMatrix;
import gundramleifert.pairing_list.configs.ScheduleConfig;
import gundramleifert.pairing_list.types.Flight;
import gundramleifert.pairing_list.types.Schedule;

import static gundramleifert.pairing_list.FlightWeight.getFlightWeight;

public class CostCalculatorMatchMatrix implements ICostCalculator {

    private ScheduleConfig properties;
    private double[] flightWeight;

    public CostCalculatorMatchMatrix(ScheduleConfig properties) {
        this.properties = properties;
        flightWeight = getFlightWeight();
    }

    public double score(Schedule schedule) {
        double res = 0;
        MatchMatrix matchMatrix = new MatchMatrix(properties.numTeams);
        for (int flightIdx = 0; flightIdx < schedule.flights.length; flightIdx++) {
            Flight flight = schedule.flights[flightIdx];
            matchMatrix.add(flight);
            //if (flightIdx<schedule.flights.length-1){
            //    continue;
            //}
            double avg = matchMatrix.avg();
            double resPart = 0;
            for (byte[] vec : matchMatrix.mat) {
                for (byte v : vec) {
                    int diff = (int) (v - avg);
                    resPart += diff * diff;
                    //resPart +=(v - avg)*(v - avg);
                }
            }
            res += resPart * flightWeight[flightIdx];
        }
        return res;
    }
}
