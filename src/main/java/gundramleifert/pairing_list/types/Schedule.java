package gundramleifert.pairing_list.types;


import com.fasterxml.jackson.annotation.JsonProperty;
import gundramleifert.pairing_list.Yaml;
import lombok.SneakyThrows;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class Schedule {
    private int hash = 0;
    private int generation = 0;
    public HashMap<Object, Double> scoreMap = new HashMap<>(2);


    public Schedule() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Schedule schedule = (Schedule) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(flights, schedule.flights);
    }

    @Override
    public int hashCode() {
        if (hash == 0) hash = Arrays.hashCode(flights);
        return hash;
    }

    public void getOlder() {
        generation++;
    }

    public int getAge() {
        return generation;
    }

    public void resetAge(){
        generation=0;
    }

    @JsonProperty
    public Flight[] flights;

    public Schedule(Flight[] flights) {
        this.flights = flights;
    }

    public void verify() {
        for (Flight flight : this.flights) {
            for (Race race : flight.races) {
                for (int i = 0; i < race.teams.length; i++) {
                    for (int i1 = i + 1; i1 < race.teams.length; i1++) {
                        if (race.teams[i] == race.teams[i1]) {
                            throw new RuntimeException("oha");
                        }
                    }
                }
            }
        }
    }

    public Schedule copy(int depth) {
        if (depth > 1) {
            Flight[] flights = new Flight[this.flights.length];
            for (int i = 0; i < flights.length; i++) {
                flights[i] = this.flights[i].copy(depth - 1);
            }
            return new Schedule(flights);
        }
        return new Schedule(this.flights);
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
        for (int i = 0; i < flights.length; i++) {
            Flight flight = flights[i];
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
