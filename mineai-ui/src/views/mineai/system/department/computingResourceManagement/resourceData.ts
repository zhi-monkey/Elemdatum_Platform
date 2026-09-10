import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { maHttp } from '/@/utils/http/axios';

export const getResourceData = async () => {
  return await maHttp.get(
    {
      url: `prometheus/getComputingResource`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

/**
 * 禁用指定的GPU
 * @param gpuUuid GPU的UUID
 */
export const disableGpu = async (gpuUuid: string) => {
  return await maHttp.post(
    {
      url: `prometheus/disableGpu?gpuUuid=${gpuUuid}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

/**
 * 启用指定的GPU
 * @param gpuUuid GPU的UUID
 */
export const enableGpu = async (gpuUuid: string) => {
  return await maHttp.post(
    {
      url: `prometheus/enableGpu?gpuUuid=${gpuUuid}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

/**
 * 获取所有禁用的GPU UUID列表
 */
export const getDisabledGpus = async () => {
  return await maHttp.get(
    {
      url: `prometheus/getDisabledGpus`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};
