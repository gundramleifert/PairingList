package gundram.leifert.sail_schedule.types;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class Race {

    @JsonProperty
    public byte[] teams;

    public Race(byte[] crews) {
        teams = crews;
    }

    public Race copy(int depth) {
        if (depth > 1) {
            byte[] r = new byte[teams.length];
            System.arraycopy(teams, 0, r, 0, teams.length);
            return new Race(r);
        }
        return new Race(teams);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Race race = (Race) o;

        return Arrays.equals(teams, race.teams);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(teams);
    }
}
