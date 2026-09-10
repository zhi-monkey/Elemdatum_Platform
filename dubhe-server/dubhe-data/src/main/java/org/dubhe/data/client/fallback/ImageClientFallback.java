

package org.dubhe.data.client.fallback;

import org.dubhe.biz.base.dto.PtImageIdDTO;
import org.dubhe.biz.base.dto.PtImageIdsDTO;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.base.vo.PtImageVO;
import org.dubhe.biz.dataresponse.factory.DataResponseFactory;
import org.dubhe.data.client.ImageClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImageClientFallback implements ImageClient {

    @Override
    public DataResponseBody<PtImageVO> getById(PtImageIdDTO ptImageIdDTO) {
        return DataResponseFactory.failed("call dubhe-image server getById error ");
    }

    @Override
    public DataResponseBody<List<PtImageVO>> listByIds(PtImageIdsDTO ptImageIdsDTO) {
        return DataResponseFactory.failed("call dubhe-image server listByIds error ");
    }
}
