package com.smockin.admin.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "S3_MOCK_DIR")
@Getter
@Setter
@NoArgsConstructor
public class S3MockDir extends Identifier {

    @Column(name = "NAME", nullable = false, length = 80)
    private String name;

    @ManyToOne
    @JoinColumn(name="S3_MOCK")
    private S3Mock s3Mock;

    @ManyToOne
    @JoinColumn(name="S3_MOCK_DIR_PARENT")
    private S3MockDir parent;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "parent", orphanRemoval = true)
    private List<S3MockDir> children = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "s3MockDir", orphanRemoval = true)
    private List<S3MockFile> files = new ArrayList<>();

    public S3MockDir(final String name, final S3Mock bucketParent) {
        this.name = name;
        this.s3Mock = bucketParent;
    }

    public S3MockDir(final String name, final S3MockDir dirParent) {
        this.name = name;
        this.parent = dirParent;
    }

}
