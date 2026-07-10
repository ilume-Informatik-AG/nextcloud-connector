package de.ilume.nextcloud.outbound.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import io.camunda.connector.generator.java.annotation.DropdownItem;

public enum Permissions {
    @JsonEnumDefaultValue
            @DropdownItem(label = "Read Only")
    READ(1),
    @DropdownItem(label = "Update Only")
    UPDATE(2),
    @DropdownItem(label = "Create Only")
    CREATE(4),
    @DropdownItem(label = "Delete Only")
    DELETE(8),
    @DropdownItem(label = "Share Only")
    SHARE(16),

    // Combinations (Sum of Bitmasks)
    @DropdownItem(label = "Read & Update")
    READ_UPDATE(3),
    @DropdownItem(label = "Read, Create & Update (Upload)")
    READ_CREATE_UPDATE(7),
    @DropdownItem(label = "Full Control (All)")
    ALL(31);

    private final int value;

    Permissions(int value) { this.value = value; }

    public int getValue() {
        return value;
    }
}
