package com.smockin.admin.persistence.entity;

import lombok.Data;

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
@Data
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

}
