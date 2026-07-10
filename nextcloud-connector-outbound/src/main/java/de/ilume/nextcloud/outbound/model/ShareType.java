package de.ilume.nextcloud.outbound.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import io.camunda.connector.generator.java.annotation.DropdownItem;

public enum ShareType {
  @JsonEnumDefaultValue
  @DropdownItem(label = "User")
  USER("0"),
  @DropdownItem(label = "Group")
  GROUP("1"),
  @DropdownItem(label = "Public link")
  PUBLIC_LINK("3"),
  @DropdownItem(label = "Email")
  EMAIL("4"),
  @DropdownItem(label = "Federated cloud share")
  FEDERATED_CLOUD_SHARE("6"),
  @DropdownItem(label = "Circle")
  CIRCLE("7"),
  @DropdownItem(label = "Talk conversation")
  TALK_CONVERSATION("10");

  private final String value;

  ShareType(String value) { this.value = value; }

  public String getValue() { return value; }
}
