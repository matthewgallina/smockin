package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.RuleComparatorEnum;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;


/**
 * Created by mgallina.
 */
public class RuleConditionDTO {

    private String extId;
    private String field;
    private RuleDataTypeEnum dataType;
    private RuleComparatorEnum comparator;
    private String value;
    private RuleMatchingTypeEnum ruleMatchingType;
    private Boolean caseSensitive;

    public RuleConditionDTO() {
    }

    public RuleConditionDTO(String extId, String field, RuleDataTypeEnum dataType, RuleComparatorEnum comparator, String value, RuleMatchingTypeEnum ruleMatchingType, Boolean caseSensitive) {
        this.extId = extId;
        this.field = field;
        this.dataType = dataType;
        this.comparator = comparator;
        this.value = value;
        this.ruleMatchingType = ruleMatchingType;
        this.caseSensitive = caseSensitive;
    }

    public RuleConditionDTO(String field, RuleDataTypeEnum dataType, RuleComparatorEnum comparator, String value, RuleMatchingTypeEnum ruleMatchingType, Boolean caseSensitive) {
        this.field = field;
        this.dataType = dataType;
        this.comparator = comparator;
        this.value = value;
        this.ruleMatchingType = ruleMatchingType;
        this.caseSensitive = caseSensitive;
    }

    public String getExtId() {
        return extId;
    }
    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getField() {
        return field;
    }
    public void setField(String field) {
        this.field = field;
    }

    public RuleDataTypeEnum getDataType() {
        return dataType;
    }
    public void setDataType(RuleDataTypeEnum dataType) {
        this.dataType = dataType;
    }

    public RuleComparatorEnum getComparator() {
        return comparator;
    }
    public void setComparator(RuleComparatorEnum comparator) {
        this.comparator = comparator;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    public RuleMatchingTypeEnum getRuleMatchingType() {
        return ruleMatchingType;
    }
    public void setRuleMatchingType(RuleMatchingTypeEnum ruleMatchingType) {
        this.ruleMatchingType = ruleMatchingType;
    }

    public Boolean isCaseSensitive() {
        return caseSensitive;
    }
    public void setCaseSensitive(Boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

}
