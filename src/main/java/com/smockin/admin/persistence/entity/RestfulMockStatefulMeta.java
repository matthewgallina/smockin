package com.smockin.admin.persistence.entity;

import javax.persistence.*;


/**
 * Created by mgallina.
 */
@Entity
@Table(name = "REST_MOCK_STATEFUL_META")
public class RestfulMockStatefulMeta extends Identifier {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_MOCK_ID", nullable = false)
    private RestfulMock restfulMock;

    @Column(name = "INITIAL_RESPONSE_BODY", length = Integer.MAX_VALUE)
    private String initialResponseBody;

    @Column(name = "ID_FIELD_NAME", length = 35, nullable = false)
    private String idFieldName = "id";

    @Column(name = "ID_FIELD_LOCATION", length = 200)
    private String idFieldLocation;

    @Column(name = "ENF_DATA_STRUCTURE", nullable = false)
    private boolean enforceDataStructure;

    public RestfulMock getRestfulMock() {
        return restfulMock;
    }
    public void setRestfulMock(RestfulMock restfulMock) {
        this.restfulMock = restfulMock;
    }

    public String getInitialResponseBody() {
        return initialResponseBody;
    }
    public void setInitialResponseBody(String initialResponseBody) {
        this.initialResponseBody = initialResponseBody;
    }

    public String getIdFieldName() {
        return idFieldName;
    }
    public void setIdFieldName(String idFieldName) {
        this.idFieldName = idFieldName;
    }

    public String getIdFieldLocation() {
        return idFieldLocation;
    }
    public void setIdFieldLocation(String idFieldLocation) {
        this.idFieldLocation = idFieldLocation;
    }

    public boolean isEnforceDataStructure() {
        return enforceDataStructure;
    }
    public void setEnforceDataStructure(boolean enforceDataStructure) {
        this.enforceDataStructure = enforceDataStructure;
    }

}
