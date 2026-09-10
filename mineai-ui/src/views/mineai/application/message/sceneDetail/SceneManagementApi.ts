import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

// 查询场景数据
export async function findScene(
  page: number,
  pageSize: number,
  order = 'asc', // 排序参数，默认按 'desc' 排序
  vagueInfo?: string, // 添加模糊查询参数
  id?: number,
  name?: string,
) {
  return await maHttp.get(
    {
      url: 'scene/dynamicFindScenePage', // 确保与后端API路径一致
      params: {
        page,
        pageSize,
        order,
        vagueInfo, // 传递模糊查询参数
        id, // 传递id作为动态查询条件
        name, // 传递sceneName作为动态查询条件
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 添加和编辑新场景
export async function addAndUpdateScene(data: any) {
  return await maHttp.post(
    {
      url: `scene/save`,
      data,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 删除场景，根据id删除
export async function deleteScene(id: number) {
  return await maHttp.delete(
    {
      url: `scene/delete/${id}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 获取所有未删除的场景
export async function getAllScenes() {
  return await maHttp.get(
    {
      url: 'scene/findAllActive',
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function getAllScenesWithApplications() {
  return await maHttp.get(
    {
      url: 'scene/findAllActiveWithApplications',
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}
