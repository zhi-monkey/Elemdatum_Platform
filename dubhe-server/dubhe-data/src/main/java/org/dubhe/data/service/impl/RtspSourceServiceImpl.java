package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.data.dao.RtspSourceMapper;
import org.dubhe.data.domain.entity.RtspSource;
import org.dubhe.data.service.RtspSourceService;
import org.dubhe.data.util.RsaCryptoUtil;
import org.dubhe.data.util.RtspPasswordCryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class RtspSourceServiceImpl extends ServiceImpl<RtspSourceMapper, RtspSource> implements RtspSourceService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    @Value("${rtsp.password-aes-key:}")
    private String aesKey;

    @Value("${rsa.private_key:}")
    private String rsaPrivateKey;

    @Override
    public List<RtspSource> findAllAvailable() {
        return lambdaQuery()
                .eq(RtspSource::getIsDelete, NOT_DELETED)
                .orderByDesc(RtspSource::getId)
                .list();
    }

    @Override
    public Page<RtspSource> page(Page<RtspSource> page, RtspSource query) {
        RtspSource q = query == null ? new RtspSource() : query;
        LambdaQueryWrapper<RtspSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RtspSource::getIsDelete, NOT_DELETED)
                .like(q.getName() != null && !q.getName().isEmpty(), RtspSource::getName, q.getName())
                .like(q.getRtspUrl() != null && !q.getRtspUrl().isEmpty(), RtspSource::getRtspUrl, q.getRtspUrl())
                .like(q.getUsername() != null && !q.getUsername().isEmpty(), RtspSource::getUsername, q.getUsername())
                .like(q.getDescription() != null && !q.getDescription().isEmpty(), RtspSource::getDescription, q.getDescription())
                .orderByDesc(RtspSource::getId);
        return page(page, wrapper);
    }

    @Override
    public RtspSource saveOrUpdate(RtspSource rtspSource, String password) {
        if (rtspSource.getId() != null && (password == null || password.trim().isEmpty())) {
            RtspSource old = getById(rtspSource.getId());
            if (old != null) {
                rtspSource.setPassword(old.getPassword());
            }
        } else if (password != null && !password.trim().isEmpty()) {
            String plainPassword = RsaCryptoUtil.decryptByPrivateKey(password, rsaPrivateKey);
            rtspSource.setPassword(RtspPasswordCryptoUtil.encrypt(plainPassword, aesKey));
        }
        if (rtspSource.getIsDelete() == null) {
            rtspSource.setIsDelete(NOT_DELETED);
        }
        super.saveOrUpdate(rtspSource);
        return rtspSource;
    }

    @Override
    public boolean softDelete(Long id) {
        return lambdaUpdate()
                .eq(RtspSource::getId, id)
                .eq(RtspSource::getIsDelete, NOT_DELETED)
                .set(RtspSource::getIsDelete, DELETED)
                .update();
    }
}
