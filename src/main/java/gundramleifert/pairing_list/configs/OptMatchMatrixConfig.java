package gundramleifert.pairing_list.configs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OptMatchMatrixConfig extends OptConfig {
    @JsonProperty
    public int swapTeams;
    @JsonProperty
    public int merges;

    @Override
    public String toString() {
        return "OptMatchMatrix{" +
                "swapTeams=" + swapTeams +
                ", merges=" + merges +
                ", loops=" + loops +
                ", individuals=" + individuals +
                ", earlyStopping=" + earlyStopping +
                ", saveEveryN=" + saveEveryN +
                ", showEveryN=" + showEveryN +
                '}';
    }
}
