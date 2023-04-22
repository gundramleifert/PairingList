package gundram.leifert.pairing_list;

import gundram.leifert.pairing_list.types.Flight;
import gundram.leifert.pairing_list.types.Race;
import gundram.leifert.pairing_list.types.Schedule;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class MutationUtil {

    public static Schedule swapBoats(Schedule schedule, Random random) {
        Schedule res = schedule.copy(100);
        int f_idx = random.nextInt(schedule.flights.length);
        Flight f = res.flights[f_idx];
        int races = f.races.length;
        int r_idx = random.nextInt(races);
        Race r = f.races[r_idx];
        int boats = r.teams.length;
        int b_idx1 = random.nextInt(boats);
        int b_idx2 = (b_idx1 + 1 + random.nextInt(boats - 1)) % boats;
        byte boat = r.teams[b_idx1];
        r.teams[b_idx1]=r.teams[b_idx2];
        r.teams[b_idx2]=boat;
        // NO sort!!
        return res;
    }

    public static Schedule swapRaces(Schedule schedule, Random random) {
        Schedule res = schedule.copy(100);
        Flight f = res.flights[random.nextInt(res.flights.length)];
        int races = f.races.length;
        int r1_idx = random.nextInt(races);
        int r2_idx = (r1_idx + 1 + random.nextInt(races - 1)) % races;
        Race race = f.races[r1_idx];
        f.races[r1_idx] = f.races[r2_idx];
        f.races[r2_idx] = race;
        return res;
    }

    public static Schedule swapBetweenRaces(Schedule schedule, Random random) {
        Schedule res = schedule.copy(100);
        int f_idx = random.nextInt(schedule.flights.length);
        Flight f = res.flights[f_idx];
        int races = f.races.length;
        int r1_idx = random.nextInt(races);
        int r2_idx = (r1_idx + 1 + random.nextInt(races - 1)) % races;
        Race r1 = f.races[r1_idx];
        Race r2 = f.races[r2_idx];
        int t1_idx = random.nextInt(r1.teams.length);
        int t2_idx = random.nextInt(r2.teams.length);
        byte team1 = r1.teams[t1_idx];
        r1.teams[t1_idx] = r2.teams[t2_idx];
        r2.teams[t2_idx] = team1;
        Arrays.sort(r1.teams);
        Arrays.sort(r2.teams);
        Arrays.sort(f.races, Comparator.comparingInt(race -> race.teams[0]));
        return res;
    }

    public static Schedule mutation(Schedule schedule1, Schedule schedule2, Random random) {
        int split = 1 + random.nextInt(schedule1.flights.length - 1);
        Flight[] flights = new Flight[schedule1.flights.length];
        for (int i = 0; i < schedule1.flights.length; i++) {
            flights[i] = ((i < split) ? schedule1.flights : schedule2.flights)[i].copy(100);
        }
        return new Schedule(flights);
    }


}
