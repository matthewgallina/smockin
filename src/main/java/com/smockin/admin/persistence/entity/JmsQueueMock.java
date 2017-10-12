package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.hibernate.annotations.ColumnDefault;

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
    @Column(name = "REC_STATUS", nullable = false, length = 15)
    private RecordStatusEnum status;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "jmsQueueMock", orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<JmsQueueMockDefinitionOrder> definitions = new ArrayList<JmsQueueMockDefinitionOrder>();

    public JmsQueueMock() {
    }

    public JmsQueueMock(String name, RecordStatusEnum status) {
        this.name = name;
        this.status = status;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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
