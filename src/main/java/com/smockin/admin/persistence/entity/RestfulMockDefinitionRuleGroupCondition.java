package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.RuleComparatorEnum;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;

import javax.persistence.*;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "REST_MOCK_RULE_GRP_COND")
public class RestfulMockDefinitionRuleGroupCondition extends Identifier {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_MOCK_RULE_GRP_ID", nullable = false)
    private RestfulMockDefinitionRuleGroup group;

    @Column(name="FIELD", nullable = true, length = 200)
    private String field;

    @Enumerated(EnumType.STRING)
    @Column(name="DATA_TYPE", nullable = false, length = 15)
    private RuleDataTypeEnum dataType;

    @Enumerated(EnumType.STRING)
    @Column(name="COMP", nullable = false, length = 15)
    private RuleComparatorEnum comparator;

    // TODO May need to make this a CLOB instead...
    @Column(name="MATCH_VALUE", nullable = true, length = 5000)
    private String matchValue;

    @Enumerated(EnumType.STRING)
    @Column(name="MATCH_ON", nullable = false, length = 22)
    private RuleMatchingTypeEnum ruleMatchingType;

    @Column(name="IS_CASE_STIV", nullable = true)
    private Boolean caseSensitive;

    public RestfulMockDefinitionRuleGroupCondition() {
    }

    public RestfulMockDefinitionRuleGroupCondition(final RestfulMockDefinitionRuleGroup group, final String field, final RuleDataTypeEnum dataType, final RuleComparatorEnum comparator, final String matchValue, final RuleMatchingTypeEnum ruleMatchingType, final Boolean caseSensitive) {
        this.group = group;
        this.field = field;
        this.dataType = dataType;
        this.comparator = comparator;
        this.matchValue = matchValue;
        this.ruleMatchingType = ruleMatchingType;
        this.caseSensitive = caseSensitive;
    }

    public RestfulMockDefinitionRuleGroup getGroup() {
        return group;
    }
    public void setGroup(RestfulMockDefinitionRuleGroup group) {
        this.group = group;
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

    public String getMatchValue() {
        return matchValue;
    }
    public void setMatchValue(String matchValue) {
        this.matchValue = matchValue;
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
