package com.smockin.admin.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import javax.persistence.*;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "PROXY_FORWARD_MAPPING")
@Data
public class ProxyForwardMapping extends Identifier {

    @Column(name = "PATH", length = 1000, nullable = false, unique = true)
    private String path;

    @Column(name = "PROXY_FORWARD_URL", length = 500, nullable = false)
    private String proxyForwardUrl;

    @ColumnDefault("false")
    @Column(name = "IS_DISABLED", nullable = false)
    private boolean disabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY")
    private SmockinUser createdBy;

}
