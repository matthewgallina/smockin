package com.smockin.admin.dto.response;

import com.smockin.admin.dto.JmsMockDTO;
import com.smockin.admin.enums.DeploymentStatusEnum;
import com.smockin.admin.persistence.enums.JmsMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import java.util.Date;


/**
 * Created by mgallina.
 */
public class JmsMockResponseDTO extends JmsMockDTO {

    private String extId;
    private String userCtxPath;
    private DeploymentStatusEnum deploymentStatus;
    private Date dateCreated;

    public JmsMockResponseDTO() {

    }

    public JmsMockResponseDTO(final String extId, final String userCtxPath, final DeploymentStatusEnum deploymentStatus, final String name, final RecordStatusEnum status, final JmsMockTypeEnum mockType, final Date dateCreated) {
        super(name,  status, mockType);
        this.extId = extId;
        this.userCtxPath = userCtxPath;
        this.deploymentStatus = deploymentStatus;
        this.dateCreated = dateCreated;
    }

    public String getExtId() {
        return extId;
    }
    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getUserCtxPath() {
        return userCtxPath;
    }
    public void setUserCtxPath(String userCtxPath) {
        this.userCtxPath = userCtxPath;
    }

    public DeploymentStatusEnum getDeploymentStatus() {
        return deploymentStatus;
    }
    public void setDeploymentStatus(DeploymentStatusEnum deploymentStatus) {
        this.deploymentStatus = deploymentStatus;
    }

    public Date getDateCreated() {
        return dateCreated;
    }
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

}
