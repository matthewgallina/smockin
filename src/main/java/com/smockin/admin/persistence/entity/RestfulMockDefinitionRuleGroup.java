package com.smockin.admin.persistence.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 *
 * Defines a group of conditions, associated to one another by 'AND'.
 *
 */
@Entity
@Table(name = "REST_MOCK_RULE_GRP")
public class RestfulMockDefinitionRuleGroup extends Identifier {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_MOCK_RULE_ID", nullable = false)
    private RestfulMockDefinitionRule rule;

    @Column(name = "ORDER_NO", nullable = false)
    private int orderNo;

    // Conditions within this group are always chained by 'AND', so having an order is not necessary.
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "group", orphanRemoval = true)
    private List<RestfulMockDefinitionRuleGroupCondition> conditions = new ArrayList<RestfulMockDefinitionRuleGroupCondition>();

    public RestfulMockDefinitionRuleGroup() {
    }

    public RestfulMockDefinitionRuleGroup(final RestfulMockDefinitionRule rule, final int orderNo) {
        this.rule = rule;
        this.orderNo = orderNo;
    }

    public RestfulMockDefinitionRule getRule() {
        return rule;
    }
    public void setRule(RestfulMockDefinitionRule rule) {
        this.rule = rule;
    }

    public int getOrderNo() {
        return orderNo;
    }
    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public List<RestfulMockDefinitionRuleGroupCondition> getConditions() {
        return conditions;
    }
    public void setConditions(List<RestfulMockDefinitionRuleGroupCondition> conditions) {
        this.conditions = conditions;
    }

}
