package gundramleifert.pairing_list.types;


import com.fasterxml.jackson.annotation.JsonProperty;
import gundramleifert.pairing_list.MatchMatrix;
import gundramleifert.pairing_list.Util;
import gundramleifert.pairing_list.Yaml;
import lombok.SneakyThrows;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class Schedule {
    private int hash = 0;
    private int generation = 0;
    public HashMap<Object, Double> scoreMap = new HashMap<>(2);

    @JsonProperty
    private List<Flight> flights = new ArrayList<>();

    private MatchMatrix matchMatrix;

    private Schedule base;


    private Schedule() {
    }

    public Schedule(int numTeams) {
        this();
        this.matchMatrix = new MatchMatrix(numTeams);
    }

    public Schedule(Schedule base, Flight flight){
        this.base=base;
        flights.add(flight);
    }

//    public void setBase(Schedule schedule) {
//        this.base = schedule;
//    }

    public void add(Flight flight) {
        this.flights.add(flight);
        this.getMatchMatrix().add(flight);
    }

    public void set(int index, Flight flight) {
        flights.set(index, flight);
        throw new RuntimeException("not able with current matchmatrix-calculation");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Schedule schedule = (Schedule) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return flights.equals(schedule.flights);
    }

    @Override
    public int hashCode() {
        if (hash == 0) hash = flights.hashCode();
        return hash;
    }

    public void getOlder() {
        generation++;
    }

    public int getAge() {
        return generation;
    }

    public void resetAge() {
        generation = 0;
    }

    public MatchMatrix getMatchMatrix() {
        if (matchMatrix == null) {
            matchMatrix = new MatchMatrix(base.getMatchMatrix());
            matchMatrix.add(flights.get(flights.size() - 1));
        }
        return matchMatrix;
    }




    public Schedule(MatchMatrix mm, Flight flight) {
        this.base = null;
        this.matchMatrix = mm;
        this.matchMatrix.add(flight);
        this.flights = new ArrayList<>(1);
        this.flights.add(flight);
    }

    public Flight get(int i) {
        return flights.get(i);
    }

    public int size() {
        return flights.size();
    }

    public Schedule copy() {
        if (base == null) {
            //flat
            return new Schedule(matchMatrix, flights.get(0).copy());
        }
        return new Schedule(base, flights.get(flights.size() - 1).copy());
    }

    public static Schedule readYaml(final File file) throws IOException {
        return Yaml.dftMapper().readValue(file, Schedule.class);
    }

    public void writeYaml(final File file) throws IOException {
        Yaml.dftMapper().writeValue(file, this);
    }

    @SneakyThrows
    public void writeCSV(final File file) {
        StringBuilder sb = new StringBuilder();
        int boats = 0;
        int countRace = 0;
        for (int i = 0; i < flights.size(); i++) {
            Flight flight = flights.get(i);
            for (int j = 0; j < flight.races.length; j++) {
                countRace++;
                Race race = flight.races[j];
                boats = Math.max(boats, race.teams.length);
                sb.append(countRace).append(";").append(j + 1).append(";");
                for (int k = 0; k < race.teams.length; k++) {
                    byte team = race.teams[k];
                    sb.append(team + 1).append(";");
                }
                sb.append("\n");
            }
        }
        StringBuilder header = new StringBuilder();
        header.append("Race;Flight");
        for (int i = 0; i < boats; i++) {
            header.append(";Boat ").append(i + 1);
        }
        header.append("\n");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(header + sb.toString());
        writer.close();
    }
}
