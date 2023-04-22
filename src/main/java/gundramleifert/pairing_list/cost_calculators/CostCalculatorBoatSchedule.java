package gundramleifert.pairing_list.cost_calculators;

import gundramleifert.pairing_list.types.BoatMatrix;
import gundramleifert.pairing_list.configs.OptimizationProps;
import gundramleifert.pairing_list.configs.ScheduleProps;
import gundramleifert.pairing_list.types.Flight;
import gundramleifert.pairing_list.types.Race;
import gundramleifert.pairing_list.types.Schedule;

import java.util.ArrayList;

import static gundramleifert.pairing_list.FlightWeight.getFlightWeight;

public class CostCalculatorBoatSchedule implements ICostCalculator {

    private ScheduleProps properties;
    private double[] flightWeight;
    private OptimizationProps.OptBoatUsage optBoatUsage;

    public static class InterFlightStat {
        public ArrayList<Byte> teamsChangeBoats = new ArrayList<>();
        public ArrayList<Byte> teamsStayOnBoat = new ArrayList<>();
        public ArrayList<Byte> teamsAtWaterAtLastRace = new ArrayList<>();
        public ArrayList<Byte> teamsAtWaterAtFirstRace = new ArrayList<>();
        public int shuttleLastRace;
        public int shuttleFirstRace;
        public int shuttleBetweenFlight;
    }

    public CostCalculatorBoatSchedule(ScheduleProps properties, OptimizationProps.OptBoatUsage optBoatUsage) {
        this.properties = properties;
        flightWeight = getFlightWeight();
        this.optBoatUsage = optBoatUsage;
    }

    private static int addteamsOnWaterAndReturnTeamsToTransfer(ArrayList<Byte> teamsAtWater, Race race1, Race race2) {
        int teamsToTranfer = Math.max(race1.teams.length, race2.teams.length);
        for (int i = 0; i < race1.teams.length; i++) {
            byte team1 = race1.teams[i];
            for (int j = 0; j < race2.teams.length; j++) {
                byte team2 = race2.teams[j];
                if (team1 == team2) {
                    teamsAtWater.add(team1);
                    teamsToTranfer--;
                    break;
                }
            }
        }
        return teamsToTranfer;
    }

    public static InterFlightStat getInterFlightStat(Flight before, Flight after) {
        InterFlightStat res = new InterFlightStat();
        Race race1 = before.races[before.races.length - 1];
        Race race2 = after.races[0];
        int teamsToTranfer = Math.max(race1.teams.length, race2.teams.length);
        for (int i = 0; i < race1.teams.length; i++) {
            byte team1 = race1.teams[i];
            for (int j = 0; j < race2.teams.length; j++) {
                byte team2 = race2.teams[j];
                if (team1 == team2) {
                    if (i == j) {
                        res.teamsStayOnBoat.add(team1);
                        teamsToTranfer--;
                    } else {
                        res.teamsChangeBoats.add(team1);
                    }
                }
            }
        }
        res.shuttleBetweenFlight = teamsToTranfer;
        if (before.races.length > 1) {
            res.shuttleLastRace = addteamsOnWaterAndReturnTeamsToTransfer(
                    res.teamsAtWaterAtLastRace,
                    before.races[before.races.length - 2],
                    after.races[0]);
            res.shuttleFirstRace = addteamsOnWaterAndReturnTeamsToTransfer(
                    res.teamsAtWaterAtFirstRace,
                    before.races[before.races.length - 1],
                    after.races[1]);
        }
        return res;
        //TODO: max? then shuttle always on water, min? 1 boat can sail to habour

        // neededShuttles += (Math.max(race1.teams.length, race2.teams.length) + 1 - stayOnBoats) / 2;
        //changesBetweenBoats += changesBetweenBoatsActual;

    }

    private static int shuttlesPerTeams(int teams) {
        return (teams + 1) / 2;
    }

    public double score(Schedule schedule) {
        double res = 0;
        BoatMatrix matchMatrix = new BoatMatrix(properties);
        double resPart = 0;
        for (int flightIdx = 0; flightIdx < schedule.flights.length; flightIdx++) {
            Flight flight = schedule.flights[flightIdx];
            matchMatrix.add(flight);
            double avg = matchMatrix.average();
            for (byte[] vec : matchMatrix.mat) {
                for (byte v : vec) {
                    resPart += (int) Math.abs(v - avg);
                    //resPart +=(v - avg)*(v - avg);
                }
            }
            if (flightIdx > 0) {
                InterFlightStat interFlightStat = getInterFlightStat(schedule.flights[flightIdx - 1], schedule.flights[flightIdx]);
                resPart += interFlightStat.teamsChangeBoats.size() * optBoatUsage.weightChangeBetweenBoats;
                resPart += shuttlesPerTeams(interFlightStat.shuttleFirstRace) * optBoatUsage.weightStayOnShuttle;
                resPart += shuttlesPerTeams(interFlightStat.shuttleLastRace) * optBoatUsage.weightStayOnShuttle;
                resPart += shuttlesPerTeams(interFlightStat.shuttleBetweenFlight) * optBoatUsage.weightStayOnBoat;
            }

            res += resPart * flightWeight[flightIdx];
        }
        return res;
    }
}
