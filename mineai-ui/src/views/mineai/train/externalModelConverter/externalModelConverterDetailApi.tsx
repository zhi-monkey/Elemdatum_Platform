import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

// 查询所有固件类型
export async function findExternalModelConverter(params) {
  return await maHttp
    .get(
      {
        url: 'externalModelConverter/dynamicFindExternalModelConverterPage', // 确保与后端API路径一致
        params,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((v) => {
      v.items = v.content;
      v.total = v.totalElements;
      v.items.forEach((item) => {
        if (item.chips.length > 0) {
          item.allChipType = item.chips.map((e) => e.chipType).join('; ');
        }
      });
      return v;
    });
}

// 添加和编辑设备
export async function addAndUpdateConverterDeviceManager(data: any) {
  return await maHttp.post(
    {
      url: `externalModelConverter/saveOrUpdate`,
      data,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 删除应用，根据id删除
export async function deleteConverterDeviceManager(id: number) {
  return await maHttp.delete(
    {
      url: `externalModelConverter/delete/${id}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function findChipsByID(id: number) {
  return await maHttp.get(
    {
      url: `chips/findChipsById/${id}`,
      id,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function updateStatus() {
  return await maHttp
    .get(
      {
        url: `externalModelConverter/pingDevice`,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      console.log('res', res);
      return res;
    });
}

export async function isIpExists(ip: string) {
  return await maHttp
    .get(
      {
        url: `externalModelConverter/isipexist/${ip}`, // 确保与后端API路径一致
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((response) => {
      // 假设返回的数据结构是 { exists: true/false }
      return response;
    });
}
