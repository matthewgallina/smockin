package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.S3SyncModeEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "S3_MOCK")
@Getter
@Setter
@NoArgsConstructor
public class S3Mock extends Identifier {

    @Column(name = "BUCKET", nullable = false, length = 80, unique = true)
    private String bucketName;

    @Enumerated(EnumType.STRING)
    @Column(name = "REC_STATUS", nullable = false, length = 15)
    private RecordStatusEnum status;

    @Enumerated(EnumType.STRING)
    @Column(name = "SYNC_MODE", nullable = false, length = 15)
    private S3SyncModeEnum syncMode;

    // Anonymous buckets created by connected S3 clients will be created under the default super admin user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY", nullable = false)
    private SmockinUser createdBy;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "s3Mock", orphanRemoval = true)
    private List<S3MockDir> childrenDirs = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "s3Mock", orphanRemoval = true)
    private List<S3MockFile> files = new ArrayList<>();

    public S3Mock(final String bucketName, final RecordStatusEnum status, final S3SyncModeEnum syncMode, final SmockinUser createdBy) {
        this.bucketName = bucketName;
        this.status = status;
        this.syncMode = syncMode;
        this.createdBy = createdBy;
    }

}
