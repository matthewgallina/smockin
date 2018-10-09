package com.smockin.admin.dto;

import java.util.ArrayList;
import java.util.List;

public class ProxyRestDuplicatePriorityDTO {

    private List<String> proxyPriorityMockIds = new ArrayList<>();

    public List<String> getProxyPriorityMockIds() {
        return proxyPriorityMockIds;
    }
    public void setProxyPriorityMockIds(List<String> proxyPriorityMockIds) {
        this.proxyPriorityMockIds = proxyPriorityMockIds;
    }

}
