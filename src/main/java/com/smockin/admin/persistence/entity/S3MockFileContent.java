package com.smockin.admin.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "S3_MOCK_FILE_CONTENT")
@Getter
@Setter
@NoArgsConstructor
public class S3MockFileContent extends Identifier {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="S3_MOCK_FILE_ID", nullable=false)
    private S3MockFile s3MockFile;

    @Column(name="CONTENT", nullable=false, length=VARCHAR_MAX_VALUE)
    private String content;

    public S3MockFileContent(S3MockFile s3MockFile, String content) {
        this.s3MockFile = s3MockFile;
        this.content = content;
    }

}
