package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.ProxyModeTypeEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "PROXY_FORWARD_USER_CONFIG")
@Data
public class ProxyForwardUserConfig extends Identifier {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="SERVER_CONFIG_ID")
    private ServerConfig serverConfig;

    @Column(name = "PROXY_MODE_TYPE", length = 8)
    @Enumerated(EnumType.STRING)
    private ProxyModeTypeEnum proxyModeType;

    @ColumnDefault("false")
    @Column(name = "NO_FORWARD_WHEN_404_MOCK", nullable = false)
    private boolean doNotForwardWhen404Mock;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY")
    private SmockinUser createdBy;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "proxyForwardUserConfig", orphanRemoval = true)
    private List<ProxyForwardMapping> proxyForwardMappings = new ArrayList<>();

}
