package com.smockin.admin.persistence.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina on 17/08/17.
 */
@Entity
@Table(name = "REST_CATGY")
public class RestfulCategory extends Identifier {

    @Column(name="NAME", length = 50, nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="PARENT_ID", nullable = true)
    private RestfulCategory parent;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "parent", orphanRemoval = true)
    @OrderBy("name ASC")
    private List<RestfulCategory> categories = new ArrayList<RestfulCategory>();

    @OneToMany(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY, mappedBy = "category", orphanRemoval = false)
    private List<RestfulMock> restfulMocks = new ArrayList<RestfulMock>();


    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public RestfulCategory getParent() {
        return parent;
    }
    public void setParent(RestfulCategory parent) {
        this.parent = parent;
    }

    public List<RestfulCategory> getCategories() {
        return categories;
    }
    public void setCategories(List<RestfulCategory> categories) {
        this.categories = categories;
    }

    public List<RestfulMock> getRestfulMocks() {
        return restfulMocks;
    }
    public void setRestfulMocks(List<RestfulMock> restfulMocks) {
        this.restfulMocks = restfulMocks;
    }

}
