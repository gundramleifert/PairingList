package gundram.leifert.pairing_list;

import gundram.leifert.pairing_list.types.Flight;
import gundram.leifert.pairing_list.types.Race;
import gundram.leifert.pairing_list.types.Schedule;

import java.util.*;


public class Util {
    public static Map<String, Integer> getMatchMatrix(Schedule schedule) {
        Map<String, Integer> matches = new HashMap<>();
        Flight[] flights = schedule.flights;
        String pair;
        for (Flight flight : flights) {
            for (Race race : flight.races) {
                byte[] teams = race.teams;
                for (int i = 0; i < teams.length; i++) {
                    byte team1 = teams[i];
                    for (int j = 0; j < i; j++) {
                        byte team2 = teams[j];
                        if (team1 < team2) {
                            pair = String.format("%d %d", team1, team2);
                        } else {
                            pair = String.format("%d %d", team2, team1);
                        }
                        matches.put(pair, matches.getOrDefault(pair, 0) + 1);
                    }
                }
            }
        }
        return matches;
    }

    public static void shuffle(byte[] bytes, Random rnd) {
        for (int i = 0; i < bytes.length; i++) {
            int j = rnd.nextInt(bytes.length);
            byte swap = bytes[i];
            bytes[i] = bytes[j];
            bytes[j] = swap;
        }
    }

    public static void printCount(ScheduleProps props, Schedule schedule) {
        int[] cnts = new int[schedule.flights.length + 1];
        Map<String, Integer> matchMatrix = getMatchMatrix(schedule);
        StringBuilder sb1 = new StringBuilder();
        for (Integer value : matchMatrix.values()) {
            cnts[value]++;
        }
        int max_value = 1;
        for (int i = 0; i < cnts.length; i++) {
            if (cnts[i] > 0) {
                max_value = i;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j <= max_value; j++) {
            sb1.append(String.format("%4d", j));
            sb.append(String.format("%4d", cnts[j]));
        }
        System.out.println(sb1.toString());
        System.out.println(sb.toString());
    }

    public static void printMatchMatrix(ScheduleProps props, Schedule schedule) {
        MatchMatrix mm = new MatchMatrix(props.numTeams);
        for (Flight f : schedule.flights) {
            mm.add(f);
        }
        StringBuilder sb1 = new StringBuilder();
        sb1.append(String.format("%3s", "-"));
        for (int j = 0; j < props.bytes.length; j++) {
            sb1.append(String.format("%3s", props.bytes[j]));
        }
        System.out.println(sb1.toString());
        for (int i = 0; i < mm.mat.length; i++) {
            byte[] vec = mm.mat[i];
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%3s", i));
            for (int j = 0; j < vec.length; j++) {
                byte t2 = vec[j];
                //String v = String.format("%3d", t2);
                sb.append(String.format("%3d", t2));
            }
            System.out.println(sb.toString());
        }
    }

    public static byte[] copy(byte[] bytes) {
        return copy(bytes, 0, bytes.length);
    }

    public static byte[] copy(byte[] bytes, int from, int length) {
        byte[] res = new byte[length];
        System.arraycopy(bytes, from, res, 0, length);
        return res;
    }


    public static Schedule shuffleBoats(Schedule schedule, Random random) {
        schedule = schedule.copy(100);
        for (Flight flight : schedule.flights) {
            for (Race race : flight.races) {
                shuffle(race.teams, random);
            }
        }
        return schedule;
    }

    public static Schedule getRandomSchedule(ScheduleProps properties, Random random) {
        Flight[] flights = new Flight[properties.flights];
        for (int i = 0; i < properties.flights; i++) {
            Race[] races = new Race[properties.getRaces()];
            byte[] teams = properties.bytes;
            Util.shuffle(teams, random);
            int off = 0;
            for (int j = races.length; j > 0; j--) {
                int remaining = (teams.length - off) / j;
                Race race = new Race(Util.copy(teams, off, remaining));
                Arrays.sort(race.teams);
                races[j - 1] = race;
                off += race.teams.length;
                if (off > properties.numTeams) {
                    break;
                }
            }
            Arrays.sort(races, Comparator.comparingInt(race -> race.teams[0]));
            flights[i] = new Flight(races);
        }
        return new Schedule(flights);

    }

}
