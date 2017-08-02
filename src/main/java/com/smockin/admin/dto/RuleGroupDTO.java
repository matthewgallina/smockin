package com.smockin.admin.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
public class RuleGroupDTO {

    private String extId;
    private int orderNo;
    private List<RuleConditionDTO> conditions = new ArrayList<RuleConditionDTO>();

    public RuleGroupDTO() {
    }

    public RuleGroupDTO(String extId, int orderNo) {
        this.extId = extId;
        this.orderNo = orderNo;
    }

    public String getExtId() {
        return extId;
    }
    public void setExtId(String extId) {
        this.extId = extId;
    }

    public int getOrderNo() {
        return orderNo;
    }
    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public List<RuleConditionDTO> getConditions() {
        return conditions;
    }
    public void setConditions(List<RuleConditionDTO> conditions) {
        this.conditions = conditions;
    }

}
