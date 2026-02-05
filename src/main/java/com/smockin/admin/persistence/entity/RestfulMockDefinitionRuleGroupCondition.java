package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.RuleComparatorEnum;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "REST_MOCK_RULE_GRP_COND")
@Data
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

}
