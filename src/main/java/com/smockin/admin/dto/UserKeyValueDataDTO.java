package com.smockin.admin.dto;

public class UserKeyValueDataDTO {

    private String id;
    private String key;
    private String value;

    public UserKeyValueDataDTO() {

    }

    public UserKeyValueDataDTO(final String id, final String key, final String value) {
        this.id = id;
        this.key = key;
        this.value = value;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

}
