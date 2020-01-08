package com.smockin.admin.persistence.entity;

import javax.persistence.*;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "REST_MOCK_JS_HANDLER")
public class RestfulMockJavaScriptHandler extends Identifier {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_MOCK_ID", nullable = false)
    private RestfulMock restfulMock;

    @Column(name = "SYNTAX", length = Integer.MAX_VALUE)
    private String syntax;

    public RestfulMock getRestfulMock() {
        return restfulMock;
    }
    public void setRestfulMock(RestfulMock restfulMock) {
        this.restfulMock = restfulMock;
    }

    public String getSyntax() {
        return syntax;
    }
    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

}
