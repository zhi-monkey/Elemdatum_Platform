import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

/**
 * 获取当前 JobId 对应的训练集中的图片数量
 *
 * @param jobId
 */
export async function getMaxPicCount(jobId: number) {
  return await maHttp.get(
    {
      url: `modelConvert/getDataRepoPicCount/${jobId}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

/**
 * 获取当前 datasetVersionId 对应的训练集中的图片数量
 *
 * @param datasetVersionId
 */
export async function getMaxPicCountByDatasetVersionId(datasetVersionId: number) {
  return await maHttp.get(
    {
      url: `modelConvert/getTrainSetCountByDatasetVersionId/${datasetVersionId}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}
