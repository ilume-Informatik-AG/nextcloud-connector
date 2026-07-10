package de.ilume.nextcloud.outbound.model;


import com.fasterxml.jackson.annotation.JsonValue;

public enum MetadataSelection {
    DEFAULT("default", "Default Metadata"),
    ALL("all", "All Metadata Fields"),
    CUSTOM("custom", "Custom Selection");

    private final String value;
    private final String label;

    MetadataSelection(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return label;
    }
}
