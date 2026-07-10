package de.ilume.nextcloud.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OcsResponseDTO(Ocs ocs) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ocs(
            Meta meta,
            Map<String, Object> data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
            String status,
            int statuscode,
            String message
    ) {}
}