package com.smockin.mockserver.service.bean;

import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Created by mgallina on 11/08/17.
 */
public class ProxiedKey {

    private final String path;
    private final RestMethodEnum method;

    public ProxiedKey(String path, RestMethodEnum method) {
        this.path = path;
        this.method = method;
    }

    public String getPath() {
        return path;
    }
    public RestMethodEnum getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof ProxiedKey)) {
            return false;
        }

        ProxiedKey pk = (ProxiedKey) o;

        return new EqualsBuilder()
                .append(path, pk.path)
                .append(method, pk.method)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(path)
                .append(method)
                .toHashCode();
    }

}
