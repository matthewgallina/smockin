package com.smockin.mockserver.engine;

import com.smockin.mockserver.dto.ProxyForwardConfigCacheDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class ProxyMappingCache {

    private AtomicReference<Map<String, ProxyForwardConfigCacheDTO>> proxyForwardConfigRef = new AtomicReference();

    public void init(final List<ProxyForwardConfigCacheDTO> allProxyForwardConfig) {

        proxyForwardConfigRef.set(

            allProxyForwardConfig
                .stream()
                .map(pm ->
                    filterProxyConfigMappings(pm))
                .filter(Optional::isPresent)
                .collect(Collectors.toMap(k -> k.get().getUserCtxPath(), v -> v.get()))

        );

    }

    // Handles adding, updating and removal
    public void update(final ProxyForwardConfigCacheDTO proxyForwardConfigDTO) {

        final Optional<ProxyForwardConfigCacheDTO> filteredProxyForwardConfigCacheOpt = filterProxyConfigMappings(proxyForwardConfigDTO);

        // Only add to cache if active mappings were found
        if (filteredProxyForwardConfigCacheOpt.isPresent()) {

            proxyForwardConfigRef.getAndUpdate(pfm -> {

                final ProxyForwardConfigCacheDTO dto = filteredProxyForwardConfigCacheOpt.get();
                pfm.put(dto.getUserCtxPath(), dto);

                return pfm;
            });

            return;
        }

        // As there are no active mappings, remove existing config for this user (if present)
        proxyForwardConfigRef.getAndUpdate(pfm -> {

            pfm.remove(proxyForwardConfigDTO.getUserCtxPath());

            return pfm;
        });

    }

    public Optional<ProxyForwardConfigCacheDTO> find(final String userCtxPath) {

        final ProxyForwardConfigCacheDTO dto = proxyForwardConfigRef.get().get(userCtxPath);

        return (dto != null)
                ? Optional.of(dto)
                : Optional.empty();
    }

    private Optional<ProxyForwardConfigCacheDTO> filterProxyConfigMappings(final ProxyForwardConfigCacheDTO proxyForwardConfig) {

        proxyForwardConfig.setProxyForwardMappings(

                proxyForwardConfig.getProxyForwardMappings()
                        .stream()
                        .filter(p ->
                                !p.isDisabled())
                        .collect(Collectors.toList())

        );

        // If no active mappings are present, then there is no point adding this mapping config to the cache
        return proxyForwardConfig.getProxyForwardMappings().isEmpty()
                ? Optional.empty()
                : Optional.of(proxyForwardConfig);
    }

}
