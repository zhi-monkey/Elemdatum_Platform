package org.dlut.adv.mineai.model.service;

import org.dlut.adv.mineai.core.entity.RtspSource;
import org.dlut.adv.mineai.core.utils.RsaCryptoUtil;
import org.dlut.adv.mineai.core.utils.RtspPasswordCryptoUtil;
import org.dlut.adv.mineai.model.repository.RtspSourceRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@Service
public class RtspSourceService {

    @Resource
    private RtspSourceRepo rtspSourceRepo;

    @Value("${rtsp.password-aes-key:}")
    private String aesKey;

    @Value("${rsa.private_key:}")
    private String rsaPrivateKey;

    public List<RtspSource> findAllAvailable() {
        return rtspSourceRepo.findAllAvailable();
    }

    public Page<RtspSource> dynamicFindRtspSourcePage(Pageable pageable,
                                                       String name,
                                                       String rtspUrl,
                                                       String username,
                                                       String description) {
        return rtspSourceRepo.dynamicFindRtspSourcePage(name, rtspUrl, username, description, pageable);
    }

    public RtspSource saveOrUpdate(RtspSource rtspSource, String password) {
        if (rtspSource.getId() != null && (password == null || password.trim().isEmpty())) {
            Optional<RtspSource> old = rtspSourceRepo.findById(rtspSource.getId());
            old.ifPresent(source -> rtspSource.setPassword(source.getPassword()));
        } else if (password != null && !password.trim().isEmpty()) {
            String plainPassword = RsaCryptoUtil.decryptByPrivateKey(password, rsaPrivateKey);
            rtspSource.setPassword(RtspPasswordCryptoUtil.encrypt(plainPassword, aesKey));
        }
        return rtspSourceRepo.save(rtspSource);
    }

    public void deleteById(Long id) {
        Optional<RtspSource> source = rtspSourceRepo.findById(id);
        source.ifPresent(rtspSource -> {
            rtspSource.setIsDelete(1);
            rtspSourceRepo.save(rtspSource);
        });
    }
}
