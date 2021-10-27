package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "S3_MOCK", uniqueConstraints={
    @UniqueConstraint(columnNames = {"S3_MOCK_PARENT", "BUCKET", "CREATED_BY"})
})
@Data
@NoArgsConstructor
public class S3Mock extends Identifier {

    @Column(name = "BUCKET", nullable = false, length = 500)
    private String bucket;

    // Only status at parent level is really relevant
    @Enumerated(EnumType.STRING)
    @Column(name = "REC_STATUS", nullable = false, length = 15)
    private RecordStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY", nullable = false)
    private SmockinUser createdBy;

    @ManyToOne
    @JoinColumn(name="S3_MOCK_PARENT")
    private S3Mock parent;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "parent", orphanRemoval = true)
    private List<S3Mock> children = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "s3Mock", orphanRemoval = true)
    private List<S3MockFile> files = new ArrayList<>();

    public S3Mock(final String bucket, final RecordStatusEnum status, final SmockinUser createdBy, final S3Mock parent) {
        this.bucket = bucket;
        this.status = status;
        this.createdBy = createdBy;
        this.parent = parent;
    }

}
