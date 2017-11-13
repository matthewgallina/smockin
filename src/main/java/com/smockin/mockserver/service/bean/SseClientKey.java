package com.smockin.mockserver.service.bean;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Created by mgallina.
 */
public class SseClientKey {

    private String id;
    private String path;

    public SseClientKey() {}

    public SseClientKey(final String id, final String path) {
        this.id = id;
        this.path = path;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof SseClientKey)) {
            return false;
        }

        SseClientKey pk = (SseClientKey) o;

        return new EqualsBuilder()
                .append(id, pk.id)
                .append(path, pk.path)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(path)
                .toHashCode();
    }

}
