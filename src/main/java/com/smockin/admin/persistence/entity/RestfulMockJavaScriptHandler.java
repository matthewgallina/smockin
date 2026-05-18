package com.smockin.admin.persistence.entity;

import lombok.Data;

import jakarta.persistence.*;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "REST_MOCK_JS_HANDLER")
@Data
@lombok.EqualsAndHashCode(callSuper = true)
public class RestfulMockJavaScriptHandler extends Identifier {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_MOCK_ID", nullable = false)
    private RestfulMock restfulMock;

    @Column(name = "SYNTAX", length = VARCHAR_MAX_VALUE)
    private String syntax;

}
