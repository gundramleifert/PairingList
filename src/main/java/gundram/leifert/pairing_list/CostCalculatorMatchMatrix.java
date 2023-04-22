package gundram.leifert.pairing_list;

import gundram.leifert.pairing_list.types.Flight;
import gundram.leifert.pairing_list.types.Schedule;

import static gundram.leifert.pairing_list.FlightWeight.getFlightWeight;

public class CostCalculatorMatchMatrix implements ICostCalculator {

    private ScheduleProps properties;
    private double[] flightWeight;
    public CostCalculatorMatchMatrix(ScheduleProps properties){
        this.properties=properties;
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
                    resPart += (int) Math.abs(v - avg);
                    //resPart +=(v - avg)*(v - avg);
                }
            }
            res += resPart * flightWeight[flightIdx];
        }
        return res;
    }
}
