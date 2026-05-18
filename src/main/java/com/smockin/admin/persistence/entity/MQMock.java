package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.MQTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "MQ_MOCK")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MQMock extends Identifier {

    @Column(name = "NAME", nullable = false, length = 120, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "MQ_TYPE", nullable = false, length = 20)
    private MQTypeEnum mqType;

    @Column(name = "DESTINATION_NAME", nullable = false, length = 200)
    private String destinationName;

    @Column(name = "IS_TOPIC", nullable = false)
    private boolean topic = false;

    @Column(name = "BROKER_URL", nullable = true, length = 500)
    private String brokerUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "MQ_MOCK_PROPERTIES", joinColumns = @JoinColumn(name = "MQ_MOCK_ID"))
    @MapKeyColumn(name = "PROP_KEY")
    @Column(name = "PROP_VALUE", length = 1000)
    private Map<String, String> properties = new HashMap<>();

    @Column(name = "SAVE_MESSAGES", nullable = false)
    private boolean saveMessages = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "REC_STATUS", nullable = false, length = 15)
    private RecordStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREATED_BY", nullable = false)
    private SmockinUser createdBy;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "mqMock", orphanRemoval = true)
    private List<MQMockMessage> messages = new ArrayList<>();

    public MQMock(final String name, final MQTypeEnum mqType, final String destinationName,
                  final boolean topic, final RecordStatusEnum status, final SmockinUser createdBy,
                  final boolean saveMessages) {
        this.name = name;
        this.mqType = mqType;
        this.destinationName = destinationName;
        this.topic = topic;
        this.status = status;
        this.createdBy = createdBy;
        this.saveMessages = saveMessages;
    }
}
