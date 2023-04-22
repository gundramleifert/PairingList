package gundram.leifert.pairing_list;

import gundram.leifert.pairing_list.types.Flight;
import gundram.leifert.pairing_list.types.Race;

import java.util.Arrays;

public class MatchMatrix {

    public byte[][] mat;
    int matches = 0;
    int races = 0;
    int sum = 0;
    int flights = 0;


    public MatchMatrix(int teams) {
        this.mat = new byte[teams][];
        for (int i = 0; i < teams; i++) {
            this.mat[i] = new byte[i];
        }
    }

    public int[] getMatchDistribution() {
        int[] res = new int[flights + 1];
        for (byte[] vec : mat) {
            for (byte v : vec) {
                res[v] += 2;
            }
        }
        return res;
    }

    public double avg() {
        int cnt = 0;
        double sum = 0;
        for (int i = 0; i < this.mat.length; i++) {
            byte[] vec = this.mat[i];
            cnt += vec.length;
            for (int j = 0; j < vec.length; j++) {
                sum += vec[j];
            }
        }
        return sum / cnt;
    }

    public MatchMatrix(MatchMatrix toCopy) {
        byte[][] srcMat = toCopy.mat;
        this.mat = new byte[srcMat.length][];
        for (int i = 0; i < srcMat.length; i++) {
            byte[] src = srcMat[i];
            byte[] tgt = new byte[src.length];
            this.mat[i] = tgt;
            System.arraycopy(src, 0, tgt, 0, src.length);
        }
        this.matches = toCopy.matches;
        this.races = toCopy.races;
    }

    public void add(Flight flight, boolean sortBoats) {
        if (sortBoats) {
            flight = flight.copy(100);
            for (Race race : flight.races) {
                Arrays.sort(race.teams);
            }
        }
        add(flight);
    }

    public void add(Flight flight) {
        for (Race r : flight.races) {
            matches += r.teams.length * (r.teams.length - 1);
            for (int idxLower = 0; idxLower < r.teams.length; idxLower++) {
                byte teamLower = r.teams[idxLower];
                for (int idxHigher = idxLower + 1; idxHigher < r.teams.length; idxHigher++) {
                    mat[r.teams[idxHigher]][teamLower]++;
                }
            }
        }
        races += flight.races.length;
        flights++;
    }
}
