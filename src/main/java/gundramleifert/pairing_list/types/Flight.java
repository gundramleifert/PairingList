package gundramleifert.pairing_list.types;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class Flight {

    @JsonProperty
    public Race[] races;

    public Flight(){}
    public Flight(Race[] races) {
        this.races = races;
    }

    public Flight copy(int depth) {
        if (depth>1) {
            Race[] races = new Race[this.races.length];
            for (int i = 0; i < races.length; i++) {
                races[i] = this.races[i].copy(depth-1);
            }
            return new Flight(races);
        }
        return new Flight(races);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Flight flight = (Flight) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(races, flight.races);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(races);
    }
}
