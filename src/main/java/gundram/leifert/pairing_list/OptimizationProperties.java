package gundram.leifert.pairing_list;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class OptimizationProperties {

    public static OptimizationProperties readYaml(String string) throws IOException, URISyntaxException {
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

    public static OptimizationProperties readYaml(final File file) throws IOException {
        OptimizationProperties properties = Yaml.dftMapper().readValue(file, OptimizationProperties.class);
        return properties;
    }

    public static void writeYaml(final File file, OptimizationProperties properties) throws IOException {
        Yaml.dftMapper().writeValue(file, properties);
    }

    public static class OptMatchMatrix {
        @JsonProperty
        public int swapTeams;
        @JsonProperty
        public int merges;

        @JsonProperty
        public int loops;
        @JsonProperty
        public int individuals;
    }

    public static class OptBoatUsage {

        @JsonProperty
        public int swapBoats;
        @JsonProperty
        public int swapRaces;
        @JsonProperty
        public int loops;
        @JsonProperty
        public int individuals;
        @JsonProperty
        public int maxStayOnBoat;

        @JsonProperty
        public double weightStayOnBoat;
        @JsonProperty
        public double weightStayOnShuttle;
        @JsonProperty
        public double weightChangeBetweenBoats;
    }

    @JsonProperty
    public List<OptBoatUsage> optBoatUsage;
    @JsonProperty
    public List<OptMatchMatrix> optMatchMatrix;


    @JsonProperty
    public int seed;

}
