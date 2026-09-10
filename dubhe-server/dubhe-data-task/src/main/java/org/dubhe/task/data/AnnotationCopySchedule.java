

package org.dubhe.task.data;

import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.data.service.DatasetVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @description 标注文件复制
 * @date 2020-12-20
 */
@Component
public class AnnotationCopySchedule {

    @Autowired
    private DatasetVersionService datasetVersionService;

    /**
     * 标注文件复制
     */
    @Scheduled(fixedDelay = 3000)
    public void annotationFileCopy() {
        try {
            datasetVersionService.annotationFileCopy();
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "annotation file copy error", e);
        }

    }

}
