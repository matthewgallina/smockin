package com.smockin.admin.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

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
    @JoinColumn(name="S3_MOCK_FILE_ID")
    private S3MockFile s3MockFile;

    @Column(name="CONTENT", nullable = false, length = Integer.MAX_VALUE)
    private String content;

    public S3MockFileContent(S3MockFile s3MockFile, String content) {
        this.s3MockFile = s3MockFile;
        this.content = content;
    }

}
