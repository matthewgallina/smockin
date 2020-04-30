package com.smockin.admin.dto;

public class UserKeyValueDataDTO {

    private String extId;
    private String key;
    private String value;

    public UserKeyValueDataDTO() {

    }

    public UserKeyValueDataDTO(final String extId, final String key, final String value) {
        this.extId = extId;
        this.key = key;
        this.value = value;
    }

    public String getExtId() {
        return extId;
    }
    public void setExtId(String extId) {
        this.extId = extId;
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
