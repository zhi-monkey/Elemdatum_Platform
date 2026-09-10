import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

// 查询应用数据
export async function findApplicationName(
  page: number,
  pageSize: number,
  order = 'asc', // 排序参数，默认按 'desc' 排序
  vagueInfo?: string, // 添加模糊查询参数
  id?: number,
  applicationName?: string,
) {
  return await maHttp.get(
    {
      url: 'applicationName/dynamicFindApplicationNamePage', // 确保与后端API路径一致
      params: {
        page,
        pageSize,
        order,
        vagueInfo, // 传递模糊查询参数
        id, // 传递id作为动态查询条件
        applicationName, // 传递applicationName作为动态查询条件
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 添加和编辑应用
export async function addAndUpdateApplicationName(data: any) {
  return await maHttp.post(
    {
      url: `applicationName/save`,
      data,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 删除应用，根据id删除
export async function deleteApplicationName(id: number) {
  return await maHttp.delete(
    {
      url: `applicationName/delete/${id}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 判断是否存在同名应用
// 根据应用名称精确查询
export async function findByApplicationName(applicationName: string) {
  return await maHttp.get(
    {
      url: `applicationName/findByApplicationName`,
      params: { applicationName },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}
