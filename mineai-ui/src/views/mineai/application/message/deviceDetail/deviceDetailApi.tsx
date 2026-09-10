// deviceDetailApi.ts

import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum'; // 查询所有芯片类型

// 查询所有固件类型
// export async function findDeviceManager(page = 0, size = 15, id?: number, chipType?: string) {
//   return await maHttp.get(
//     {
//       url: `device/findAll`,
//       params: {
//         page,
//         size,
//         id,
//         chipType,
//       },
//     },
//     { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
//   );
// }

// 添加和编辑固件
export async function addAndUpdateDeviceManager(data: any) {
  return await maHttp.post(
    {
      url: `device/save`,
      data,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 删除应用，根据id删除
export async function deleteDeviceManager(id: number) {
  return await maHttp.delete(
    {
      url: `device/delete/${id}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}
