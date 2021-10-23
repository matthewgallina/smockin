package com.smockin.admin.persistence.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "S3_MOCK_FILE")
@Data
@NoArgsConstructor
public class S3MockFile extends Identifier {

    @Column(name = "NAME", nullable = false, length = 500)
    private String name;

    @Column(name = "MIME_TYPE", nullable = false, length = 50)
    private String mimeType;

    @Column(name="CONTENT", nullable = false, length = Integer.MAX_VALUE)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="S3_MOCK_ID", nullable = false)
    private S3Mock s3Mock;

    public S3MockFile(String name, String mimeType, String content, S3Mock s3Mock) {
        this.name = name;
        this.mimeType = mimeType;
        this.content = content;
        this.s3Mock = s3Mock;
    }

}
