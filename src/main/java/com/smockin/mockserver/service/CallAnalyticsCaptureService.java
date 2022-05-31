package com.smockin.mockserver.service;

import java.util.List;
import java.util.Optional;

public interface CallAnalyticsCaptureService {

    void init(final List<String> ids);
    void clear();
    void register(final String id);
    void remove(final String id);
    Optional<String> extractCallAnalyticId(final String path);
    void capture(final String analyticCallId,
                 final String method,
                 final String path,
                 final int status);

}
