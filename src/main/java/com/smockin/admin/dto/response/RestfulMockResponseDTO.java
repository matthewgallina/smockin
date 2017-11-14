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
    private List<RuleDTO> rules = new ArrayList<RuleDTO>();

    public RestfulMockResponseDTO(String extId, String path, RestMethodEnum method, RecordStatusEnum status, RestMockTypeEnum mockType, Date dateCreated, long proxyTimeoutInMillis, long webSocketTimeoutInMillis, long sseHeartBeatInMillis, boolean proxyPushIdOnConnect, boolean randomiseDefinitions) {
        super(path, method, status, mockType, proxyTimeoutInMillis, webSocketTimeoutInMillis, sseHeartBeatInMillis, proxyPushIdOnConnect, randomiseDefinitions);
        this.extId = extId;
        this.dateCreated = dateCreated;
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

    public List<RuleDTO> getRules() {
        return rules;
    }
    public void setRules(List<RuleDTO> rules) {
        this.rules = rules;
    }
}
