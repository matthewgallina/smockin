package com.smockin.admin.dto.response;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.RuleDTO;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by mgallina.
 */
public class RestfulMockResponseDTO extends RestfulMockDTO {

    private String extId;
    private Date dateCreated;
    private String createdBy;
    private String userCtxPath;
    private List<RuleDTO> rules = new ArrayList<RuleDTO>();

    public RestfulMockResponseDTO(String extId, String path, String userCtxPath, RestMethodEnum method, RecordStatusEnum status, RestMockTypeEnum mockType, Date dateCreated, String createdBy, long proxyTimeoutInMillis, long webSocketTimeoutInMillis, long sseHeartBeatInMillis, boolean proxyPushIdOnConnect, boolean randomiseDefinitions, boolean proxyForwardWhenNoRuleMatch) {
        super(path, method, status, mockType, proxyTimeoutInMillis, webSocketTimeoutInMillis, sseHeartBeatInMillis, proxyPushIdOnConnect, randomiseDefinitions, proxyForwardWhenNoRuleMatch);
        this.extId = extId;
        this.dateCreated = dateCreated;
        this.createdBy = createdBy;
        this.userCtxPath = userCtxPath;
    }

    public String getExtId() {
        return extId;
    }
    public void setExtId(String extId) {
        this.extId = extId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUserCtxPath() {
        return userCtxPath;
    }
    public void setUserCtxPath(String userCtxPath) {
        this.userCtxPath = userCtxPath;
    }

    public List<RuleDTO> getRules() {
        return rules;
    }
    public void setRules(List<RuleDTO> rules) {
        this.rules = rules;
    }
}
