import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

export interface RtspSourceItem {
  id: number;
  name: string;
  rtspUrl: string;
  username?: string;
  description?: string;
  isDelete: number;
}

export interface RtspSourceSaveDTO {
  id?: number;
  name: string;
  rtspUrl: string;
  authRequired?: boolean;
  username?: string;
  password?: string;
  description?: string;
  isDelete?: number;
}

export async function dynamicFindRtspSourcePage(params) {
  return await maHttp
    .get(
      {
        url: 'rtspSource/dynamicFindRtspSourcePage',
        params,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((v) => {
      v.items = v.content;
      v.total = v.totalElements;
      return v;
    });
}

export async function saveOrUpdateRtspSource(data: RtspSourceSaveDTO) {
  return await maHttp.post(
    {
      url: 'rtspSource/saveOrUpdate',
      data,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function deleteRtspSource(id: number) {
  return await maHttp.delete(
    {
      url: `rtspSource/delete/${id}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function getRtspPublicKey(): Promise<string> {
  return await maHttp.get(
    {
      url: 'rtspSource/getPublicKey',
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}
