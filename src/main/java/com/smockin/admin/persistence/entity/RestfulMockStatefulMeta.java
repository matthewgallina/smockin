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
@Table(name = "REST_MOCK_STATEFUL_META")
@Data
public class RestfulMockStatefulMeta extends Identifier {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_MOCK_ID", nullable = false)
    private RestfulMock restfulMock;

    @Column(name = "INITIAL_RESPONSE_BODY", length = VARCHAR_MAX_VALUE)
    private String initialResponseBody;

    @Column(name = "ID_FIELD_NAME", length = 35, nullable = false)
    private String idFieldName = "id";

    @Column(name = "ID_FIELD_LOCATION", length = 200)
    private String idFieldLocation;

}
