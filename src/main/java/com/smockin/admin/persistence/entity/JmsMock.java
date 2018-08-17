package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.JmsMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;

import javax.persistence.*;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "JMS_MOCK")
public class JmsMock extends Identifier {

    @Column(name = "NAME", nullable = false, length = 1000, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "JMS_TYPE", nullable = false, length = 5)
    private JmsMockTypeEnum jmsType;

    @Enumerated(EnumType.STRING)
    @Column(name = "REC_STATUS", nullable = false, length = 15)
    private RecordStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY", nullable = true)
    private SmockinUser createdBy;

    public JmsMock() {
    }

    public JmsMock(final String name, final JmsMockTypeEnum jmsType, final RecordStatusEnum status, final SmockinUser createdBy) {
        this.name = name;
        this.jmsType = jmsType;
        this.status = status;
        this.createdBy = createdBy;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public JmsMockTypeEnum getJmsType() {
        return jmsType;
    }
    public void setJmsType(JmsMockTypeEnum jmsType) {
        this.jmsType = jmsType;
    }

    public RecordStatusEnum getStatus() {
        return status;
    }
    public void setStatus(RecordStatusEnum status) {
        this.status = status;
    }

    public SmockinUser getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(SmockinUser createdBy) {
        this.createdBy = createdBy;
    }
}
