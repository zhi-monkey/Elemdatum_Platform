package org.dlut.adv.mineai.model.service;

import org.dlut.adv.mineai.core.entity.GpuUrlTarget;
import org.dlut.adv.mineai.core.entity.UserContextHolder;
import org.dlut.adv.mineai.model.repository.GpuUrlTargetRepo;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.net.URI;
import java.util.regex.Pattern;

@Service
public class GpuUrlTargetService {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");

    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("^https?://[^\\s/$.?#].[^\\s]*$");

    @Resource
    private GpuUrlTargetRepo gpuUrlTargetRepo;

    public List<GpuUrlTarget> searchAvailable(String keyword) {
        return gpuUrlTargetRepo.searchAvailable(normalize(keyword));
    }

    public PageImpl<GpuUrlTarget> pageAvailable(String keyword, Integer page, Integer pageSize) {
        List<GpuUrlTarget> all = searchAvailable(keyword);
        int current = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((current - 1) * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        return new PageImpl<>(all.subList(fromIndex, toIndex), PageRequest.of(current - 1, size), all.size());
    }

    public List<GpuUrlTarget> findAvailableByIds(List<Long> ids) {
        List<Long> normalizedIds = normalizeIds(ids);
        if (normalizedIds.isEmpty()) {
            return Collections.emptyList();
        }
        return gpuUrlTargetRepo.findByIdInAndIsDelete(normalizedIds, 0);
    }

    @Transactional
    public GpuUrlTarget saveOrUpdate(GpuUrlTarget target) {
        validate(target);
        if (target.getId() != null) {
            GpuUrlTarget existing = gpuUrlTargetRepo.findById(target.getId())
                    .orElseThrow(() -> new IllegalArgumentException("GPU下发地址不存在：id=" + target.getId()));
            existing.setName(target.getName().trim());
            existing.setIp(target.getIp().trim());
            existing.setPort(target.getPort());
            existing.setGpuUrl(target.getGpuUrl().trim());
            existing.setPlatformIp(target.getPlatformIp().trim());
            existing.setPlatformPort(target.getPlatformPort());
            existing.setAuthCode(normalize(target.getAuthCode()));
            existing.setDescription(normalize(target.getDescription()));
            existing.setIsDelete(target.getIsDelete() == null ? 0 : target.getIsDelete());
            return gpuUrlTargetRepo.save(existing);
        }
        target.setName(target.getName().trim());
        target.setIp(target.getIp().trim());
        target.setGpuUrl(target.getGpuUrl().trim());
        target.setPlatformIp(target.getPlatformIp().trim());
        target.setAuthCode(normalize(target.getAuthCode()));
        target.setDescription(normalize(target.getDescription()));
        target.setIsDelete(target.getIsDelete() == null ? 0 : target.getIsDelete());
        target.setCreateUserId(resolveCurrentUserId());
        return gpuUrlTargetRepo.save(target);
    }

    @Transactional
    public void deleteById(Long id) {
        GpuUrlTarget target = gpuUrlTargetRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GPU下发地址不存在：id=" + id));
        target.setIsDelete(1);
        gpuUrlTargetRepo.save(target);
    }

    public List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && id > 0) {
                normalized.add(id);
            }
        }
        return new ArrayList<>(normalized);
    }

    private void validate(GpuUrlTarget target) {
        if (target == null) {
            throw new IllegalArgumentException("GPU下发地址不能为空");
        }
        if (normalize(target.getName()).isEmpty()) {
            throw new IllegalArgumentException("GPU下发地址名称不能为空");
        }
        if (!isValidIpv4(target.getIp())) {
            throw new IllegalArgumentException("GPU下发地址IP不能为空");
        }
        if (target.getPort() == null || target.getPort() <= 0 || target.getPort() > 65535) {
            throw new IllegalArgumentException("GPU下发地址端口不合法");
        }
        if (!HTTP_URL_PATTERN.matcher(normalize(target.getGpuUrl())).matches()) {
            throw new IllegalArgumentException("GPU服务URL不能为空");
        }
        validateGpuUrlAndFillAddress(target);
        if (!isValidIpv4(target.getPlatformIp())) {
            throw new IllegalArgumentException("平台IP不能为空");
        }
        if (target.getPlatformPort() == null || target.getPlatformPort() <= 0 || target.getPlatformPort() > 65535) {
            throw new IllegalArgumentException("平台端口不合法");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isValidIpv4(String value) {
        return IPV4_PATTERN.matcher(normalize(value)).matches();
    }

    private void validateGpuUrlAndFillAddress(GpuUrlTarget target) {
        try {
            URI uri = URI.create(target.getGpuUrl().trim());
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                throw new IllegalArgumentException("GPU服务URL格式不合法");
            }
            if (uri.getPort() <= 0) {
                throw new IllegalArgumentException("GPU服务URL必须显式包含端口");
            }
            target.setIp(uri.getHost());
            target.setPort(uri.getPort());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("GPU服务URL格式不合法");
        }
    }

    private Long resolveCurrentUserId() {
        try {
            return Long.valueOf(UserContextHolder.getUserContext().getId());
        } catch (Exception e) {
            return null;
        }
    }
}
