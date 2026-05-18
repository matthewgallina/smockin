package com.smockin.admin.persistence.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "REST_PROJ")
@Data
@lombok.EqualsAndHashCode(callSuper = true)
public class RestfulProject extends Identifier {

    @Column(name="NAME", length = 100, nullable = false, unique = true)
    private String name;

    @OneToMany(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY, mappedBy = "project", orphanRemoval = false)
    private List<RestfulMock> restfulMocks = new ArrayList<>();

}
