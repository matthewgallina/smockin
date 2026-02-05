package com.smockin.admin.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "REST_MOCK_JS_HANDLER")
@Data
public class RestfulMockJavaScriptHandler extends Identifier {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_MOCK_ID", nullable = false)
    private RestfulMock restfulMock;

    @Column(name = "SYNTAX", length = VARCHAR_MAX_VALUE)
    private String syntax;

}
