package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.ServerTypeEnum;
import lombok.Data;

import javax.annotation.concurrent.Immutable;
import javax.persistence.*;

@Entity
@Table(name = "CALL_ANALYTIC_LOG")
@Data
@Immutable
public class CallAnalyticLog extends Identifier {

    @ManyToOne
    @JoinColumn(name = "CALL_ANALYTIC_ID", nullable = false, updatable = false)
    private CallAnalytic callAnalytic;

    @Enumerated(EnumType.STRING)
    @Column(name="ORIGIN_TYPE", length = 8, nullable = false, updatable = false)
    private ServerTypeEnum originType;

    @Column(name="PATH", length = 1000, nullable = false, updatable = false)
    private String path;

    @Column(name="RESULT", length = VARCHAR_MAX_VALUE, nullable = false, updatable = false)
    private String result;

}
