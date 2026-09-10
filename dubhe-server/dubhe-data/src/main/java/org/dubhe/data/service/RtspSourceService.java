package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.domain.entity.RtspSource;

import java.util.List;

public interface RtspSourceService extends IService<RtspSource> {

    List<RtspSource> findAllAvailable();

    Page<RtspSource> page(Page<RtspSource> page, RtspSource query);

    RtspSource saveOrUpdate(RtspSource rtspSource, String password);

    boolean softDelete(Long id);
}
