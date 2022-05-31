package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CALL_ANALYTIC", uniqueConstraints={
        @UniqueConstraint(columnNames = {"NAME", "CREATED_BY"})
})
@Data
public class CallAnalytic extends Identifier {

    @Column(name="NAME", length = 50, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "REC_STATUS", nullable = false, length = 15)
    private RecordStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY", nullable = false)
    private SmockinUser createdBy;

    @OneToMany(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY, mappedBy = "callAnalytic", orphanRemoval = true)
    @OrderBy("dateCreated ASC")
    private List<CallAnalyticLog> logs = new ArrayList<>();

}
