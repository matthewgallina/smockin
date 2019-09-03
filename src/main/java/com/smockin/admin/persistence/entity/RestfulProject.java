package com.smockin.admin.persistence.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "REST_PROJ")
public class RestfulProject extends Identifier {

    @Column(name="NAME", length = 100, nullable = false, unique = true)
    private String name;

    @OneToMany(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY, mappedBy = "project", orphanRemoval = false)
    private List<RestfulMock> restfulMocks = new ArrayList<>();

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public List<RestfulMock> getRestfulMocks() {
        return restfulMocks;
    }
    public void setRestfulMocks(List<RestfulMock> restfulMocks) {
        this.restfulMocks = restfulMocks;
    }

}
