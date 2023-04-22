package gundram.leifert.pairing_list;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class ScheduleProps {
    public static ScheduleProps readYaml(String string) throws IOException, URISyntaxException {
        if (new File(string).exists()) {
            return readYaml(new File(string));
        }
        URL resource = Environment.class.getClassLoader().getResource(string);
        if (resource == null) {
            throw new IllegalArgumentException("file not found in resources an on disc");
        }
        File file = new File(resource.toURI());
        return readYaml(file);

    }

    public static ScheduleProps readYaml(final File file) throws IOException {
        ScheduleProps scheduleProps;
        ObjectMapper objectMapper = Yaml.dftMapper();
        scheduleProps = objectMapper.readValue(file, ScheduleProps.class);
        scheduleProps.init();
        return scheduleProps;
    }

    public static void writeYaml(final File file, ScheduleProps scheduleProps) throws IOException {
        Yaml.dftMapper().writeValue(file, scheduleProps);
    }

    public void init() {
        this.numBoats = boats.length;
        this.numTeams = teams.length;
        this.bytes = toByteArray();
    }


    @JsonProperty
    public String title;
    @JsonProperty
    public int flights;
    @JsonProperty
    public float fontsize = 10;
    @JsonProperty
    public String[] teams;

    @JsonProperty
    public String[] boats;

    public int numBoats;
    public int numTeams;
    public float width = 600f;

    public byte[] bytes;

    public double avg = calcAvg(this);

    public int getRaces() {
        return ((numTeams + numBoats - 1) / numBoats);
    }

    private static double calcAvg(ScheduleProps props) {
        return props.flights * (((double) (props.numBoats - 1) / props.numTeams));
    }

    private byte[] toByteArray() {
        byte[] res = new byte[numTeams];
        for (int i = 0; i < numTeams; i++) {
            res[i] = (byte) i;
        }
        return res;
    }


}
