import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

export interface IAlert {
  id: number;
  description: string;
  createTime: string;
  subsystem: any;
  model: any;
  monitor: any;
  isDelete: number;
  image: boolean;
  imagePath: string;
  video: boolean;
  videoPath: string;
  audio: boolean;
  audioPath: string;
  data: any;
}

export const getAlerts = async (params) => {
  return await maHttp.get(
    {
      url: 'modelAlert/dynamicModelAlert',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};
