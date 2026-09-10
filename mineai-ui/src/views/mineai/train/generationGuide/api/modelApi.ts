import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

/**
 * 按照 id 获取硬件信息
 *
 * @param hardwareParamsId
 */
export const getHardwareParamsById = async (hardwareParamsId: number) => {
  return await maHttp.get(
    {
      url: `hardwareParams/${hardwareParamsId}`,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

/**
 * 根据 ModelGeneration 的 id 获取 所有的 ModelJob 的信息
 *
 * @param modelGenerationId
 */
export const getModelJobWeights = async (modelGenerationId: number) => {
  return await maHttp.get(
    {
      url: `modelGeneration/preWeights/${modelGenerationId}`,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};


