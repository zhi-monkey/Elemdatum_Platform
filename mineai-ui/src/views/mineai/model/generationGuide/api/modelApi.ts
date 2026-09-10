// 按照 id 获取硬件信息
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

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
