package com.smockin.admin.dto;

public class ProjectDTO {

    private String extId;
    private String name;

    public ProjectDTO(String extId, String name) {
        this.extId = extId;
        this.name = name;
    }

    public String getExtId() {
        return extId;
    }
    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

}
