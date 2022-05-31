package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.CallAnalyticDAO;
import com.smockin.admin.persistence.dao.CallAnalyticLogDAO;
import com.smockin.admin.persistence.entity.CallAnalytic;
import com.smockin.admin.persistence.entity.CallAnalyticLog;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class CallAnalyticsCaptureServiceImpl implements CallAnalyticsCaptureService {

    private final AtomicReference<List<String>> activeCallAnalyticsRef = new AtomicReference(new ArrayList<>());

    @Autowired
    private CallAnalyticDAO callAnalyticDAO;

    @Autowired
    private CallAnalyticLogDAO callAnalyticLogDAO;


    public void init(final List<String> ids) {

        activeCallAnalyticsRef.compareAndSet(activeCallAnalyticsRef.get(), ids);
    }

    public void clear() {

        activeCallAnalyticsRef.set(new ArrayList<>());
    }

    public void register(final String id) {

        final List<String> activeCallAnalytics = activeCallAnalyticsRef.get();
        activeCallAnalytics.add(id);

        activeCallAnalyticsRef.compareAndSet(activeCallAnalyticsRef.get(), activeCallAnalytics);
    }

    public void remove(final String id) {

        final List<String> activeCallAnalytics = activeCallAnalyticsRef.get();
        activeCallAnalytics.remove(id);

        activeCallAnalyticsRef.compareAndSet(activeCallAnalyticsRef.get(), activeCallAnalytics);
    }

    public Optional<String> extractCallAnalyticId(final String path) {

        return activeCallAnalyticsRef.get()
                .stream()
                .filter(cid ->
                        StringUtils.contains(path, cid))
                .findFirst();
    }

    boolean containsCallAnalyticId(final String path) {

        return activeCallAnalyticsRef.get()
                .stream()
                .anyMatch(cid ->
                        StringUtils.contains(path, cid));
    }

    @Async
    @Transactional
    public void capture(final String analyticCallId,
                        final String method,
                        final String path,
                        final int status) {

        if (!containsCallAnalyticId(analyticCallId)) {
            return;
        }

        final CallAnalytic callAnalytic = callAnalyticDAO.findByExtId(analyticCallId);

        if (callAnalytic == null) {
            throw new RecordNotFoundException();
        }

        final CallAnalyticLog callAnalyticLog = new CallAnalyticLog();
        callAnalyticLog.setOriginType(ServerTypeEnum.RESTFUL);
        callAnalyticLog.setPath(method + " " + path);
        callAnalyticLog.setResult("HTTP Status: " + status);
        callAnalyticLog.setCallAnalytic(callAnalytic);

        callAnalyticLogDAO.save(callAnalyticLog);

    }

}
