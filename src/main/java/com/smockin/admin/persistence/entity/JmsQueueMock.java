package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.JmsMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "JMS_QUEUE_MOCK")
public class JmsQueueMock extends Identifier {

    @Column(name = "NAME", nullable = false, length = 1000, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "MOCK_TYPE", nullable = false, length = 10)
    private JmsMockTypeEnum mockType;

    @Enumerated(EnumType.STRING)
    @Column(name = "REC_STATUS", nullable = false, length = 15)
    private RecordStatusEnum status;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "jmsQueueMock", orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<JmsQueueMockDefinitionOrder> definitions = new ArrayList<JmsQueueMockDefinitionOrder>();

    public JmsQueueMock() {
    }

    public JmsQueueMock(final String name, final JmsMockTypeEnum mockType, final RecordStatusEnum status) {
        this.name = name;
        this.mockType = mockType;
        this.status = status;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public JmsMockTypeEnum getMockType() {
        return mockType;
    }
    public void setMockType(JmsMockTypeEnum mockType) {
        this.mockType = mockType;
    }

    public RecordStatusEnum getStatus() {
        return status;
    }
    public void setStatus(RecordStatusEnum status) {
        this.status = status;
    }

    public List<JmsQueueMockDefinitionOrder> getDefinitions() {
        return definitions;
    }
    public void setDefinitions(List<JmsQueueMockDefinitionOrder> definitions) {
        this.definitions = definitions;
    }

}
