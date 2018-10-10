package com.smockin.admin.enums;

public enum DeploymentStatusEnum {
    ACTIVE, // actively deployed to server
    PENDING, // previous version is currently deployed to server
    INACTIVE; // not currently deployed to server
}
