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
    private List<RuleDTO> rules = new ArrayList<>();

    public RestfulMockResponseDTO() {

    }

    public RestfulMockResponseDTO(final String extId, final String path, final String userCtxPath, final RestMethodEnum method, final RecordStatusEnum status,
                                  final RestMockTypeEnum mockType, final Date dateCreated, final String createdBy, final long proxyTimeoutInMillis, final long webSocketTimeoutInMillis, final long sseHeartBeatInMillis,
                                  final boolean proxyPushIdOnConnect, final boolean randomiseDefinitions, final boolean proxyForwardWhenNoRuleMatch,
                                  boolean randomiseLatency, long randomiseLatencyRangeMinMillis, long randomiseLatencyRangeMaxMillis, String projectId, String customJsSyntax) {
        super(path, method, status, mockType, proxyTimeoutInMillis, webSocketTimeoutInMillis, sseHeartBeatInMillis, proxyPushIdOnConnect,
                randomiseDefinitions, proxyForwardWhenNoRuleMatch,
                randomiseLatency, randomiseLatencyRangeMinMillis, randomiseLatencyRangeMaxMillis, projectId, customJsSyntax);
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
