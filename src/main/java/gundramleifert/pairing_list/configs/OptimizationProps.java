package gundramleifert.pairing_list.configs;


import com.fasterxml.jackson.annotation.JsonProperty;
import gundramleifert.pairing_list.Optimizer;
import gundramleifert.pairing_list.Yaml;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class OptimizationProps {

    public static OptimizationProps readYaml(String string) throws IOException, URISyntaxException {
        if (new File(string).exists()) {
            return readYaml(new File(string));
        }
        URL resource = Optimizer.class.getClassLoader().getResource(string);
        if (resource == null) {
            throw new IllegalArgumentException("file not found in resources an on disc");
        }
        File file = new File(resource.toURI());
        return readYaml(file);

    }

    public static OptimizationProps readYaml(final File file) throws IOException {
        OptimizationProps properties = Yaml.dftMapper().readValue(file, OptimizationProps.class);
        return properties;
    }

    public static void writeYaml(final File file, OptimizationProps properties) throws IOException {
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
