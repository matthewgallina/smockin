package com.smockin.admin.persistence.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "REST_PROJ")
@Data
public class RestfulProject extends Identifier {

    @Column(name="NAME", length = 100, nullable = false, unique = true)
    private String name;

    @OneToMany(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY, mappedBy = "project")
    private List<RestfulMock> restfulMocks = new ArrayList<>();

}
