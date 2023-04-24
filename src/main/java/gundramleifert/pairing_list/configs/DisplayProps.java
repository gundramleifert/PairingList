package gundramleifert.pairing_list.configs;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.colors.DeviceRgb;
import gundramleifert.pairing_list.Optimizer;
import gundramleifert.pairing_list.Yaml;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DisplayProps {
    public static DisplayProps readYaml(String string) throws IOException, URISyntaxException {
        if (new File(string).exists()) {
            return readYaml(new File(string));
        }
        URL resource = Optimizer.class.getClassLoader().getResource(string);
        if (resource == null) {
            throw new IllegalArgumentException(String.format("file `%s` not found in resources an on disc", string));
        }
        File file = new File(resource.toURI());
        return readYaml(file);

    }

    public static DisplayProps readYaml(final File file) throws IOException {
        DisplayProps scheduleProps;
        ObjectMapper objectMapper = Yaml.dftMapper();
        scheduleProps = objectMapper.readValue(file, DisplayProps.class);
        return scheduleProps;
    }

    public static void writeYaml(final File file, DisplayProps scheduleProps) throws IOException {
        Yaml.dftMapper().writeValue(file, scheduleProps);
    }

    @JsonProperty
    public String title;
    @JsonProperty
    public int fontsize = 10;
    @JsonProperty
    public int cell_height = 5;
    @JsonProperty
    public float width = 600f;

    @JsonProperty
    public boolean show_match_stat = false;

    @JsonProperty
    public boolean show_boat_stat = false;

    @JsonProperty
    public boolean show_schuttle_stat = false;

    @JsonProperty
    public boolean teamwise_list = false;

    @JsonProperty
    public double factor_flight_race_width = 1.0;

    @JsonProperty
    public float opacity_default = 0.5f;

    @JsonProperty
    public float opacity_active = 1.0f;

    @JsonProperty
    public float opacity_inactive = 0.5f;

    @JsonProperty
    public float opacity_same_shuttle = 1.0f;

    @JsonProperty
    public Map<String, int[]> additional_colors = new HashMap<>();


    public static class DeviceRgbWithAlpha extends DeviceRgb {

        public static final DeviceRgbWithAlpha BLACK = fromArray(0);
        public static final DeviceRgbWithAlpha WHITE = fromArray(255);

        public final float alpha;

        public static DeviceRgbWithAlpha fromArray(int k) {
            return fromArray(new int[]{k});
        }

        public static DeviceRgbWithAlpha fromArray(int r, int g, int b) {
            return fromArray(new int[]{r, g, b});
        }

        public static DeviceRgbWithAlpha fromArray(int[] rgba) {
            float[] rgba_ = new float[rgba.length];
            for (int i = 0; i < rgba.length; i++) {
                rgba_[i] = rgba[i] / 255.0f;
            }
            return fromArray(rgba_);

        }

        public static DeviceRgbWithAlpha fromArray(float[] rgba) {
            switch (rgba.length) {
                case 1:
                    return new DeviceRgbWithAlpha(rgba[0], rgba[0], rgba[0], 1.0f);
                case 3:
                    return new DeviceRgbWithAlpha(rgba[0], rgba[1], rgba[2], 1.0f);
                case 4:
                    return new DeviceRgbWithAlpha(rgba[0], rgba[1], rgba[2], rgba[3]);
                default:
                    throw new RuntimeException(String.format("unexpected length of rgba (one of 1,3,4 expected but got %d", rgba.length));
            }
        }

        private DeviceRgbWithAlpha(float r, float g, float b, float a) {
            super(r, g, b);
            alpha = a;
        }

    }

}