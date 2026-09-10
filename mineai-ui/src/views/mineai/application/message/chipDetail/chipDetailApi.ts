// chipDetailApi.ts

import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum'; // 查询所有芯片类型
//
// export async function findChipManager(page1 = 0, size1 = 15, id?: number, chipType?: string) {
//   const response = await maHttp.get(
//     {
//       url: `chips/findAll`,
//       // method: 'POST',
//       params: {
//         page: page1,
//         size: size1,
//         // id,
//         chipType: chipType,
//       },
//     },
//     { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
//   );
//   console.log(response);
//   return response.content;
// }

// 添加和编辑算力芯片
export async function addAndUpdateChipManager(data: any) {
  return await maHttp.post(
    {
      url: `chips/save`,
      // method: 'POST',
      data,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 删除算力芯片，根据id删除
export async function deleteChipManager(id: number) {
  return await maHttp.delete(
    {
      url: `chips/delete/${id}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export const checkChipTypeUnique = (chipType: string) => {
  return maHttp
    .post(
      {
        url: `chips/checkChipTypeUnique`,
        data: {
          chipType,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => res);
};
