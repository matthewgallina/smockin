package com.smockin.admin.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "REST_PROJ")
@Data
public class RestfulProject extends Identifier {

    @Column(name="NAME", length = 100, nullable = false, unique = true)
    private String name;

    @OneToMany(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY, mappedBy = "project", orphanRemoval = false)
    private List<RestfulMock> restfulMocks = new ArrayList<>();

}
