import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
//动态条件查询
export async function dynamicFindModelVersionPage(
  page = 0,
  pageSize = 15,
  order = 'desc',
  vagueInfo?: string,
  chipType?: string,
  showName?: string,
  description?: string,
) {
  return await maHttp.get(
    {
      url: 'modelVersion/dynamicFindModelVersionPage',
      params: {
        page,
        pageSize,
        order,
        vagueInfo,
        chipType,
        showName,
        description,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}
//删除操作
export async function deleteModelVersionImage(record: { id: string }) {
  return await maHttp.get(
    {
      url: 'modelVersion/deleteImage',
      params: {
        modelVersionId: record.id,
      },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

//查找所有算力芯片
export async function findAllChips(page = 0, pageSize = 10) {
  return await maHttp.get(
    {
      url: 'chips/findAll',
      params: {
        page,
        pageSize,
      },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}
