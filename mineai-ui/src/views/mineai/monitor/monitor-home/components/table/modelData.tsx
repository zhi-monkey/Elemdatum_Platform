import { BasicColumn } from '/@/components/Table/src/types/table';

import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { ref } from 'vue';
export function getBasicColumns(): BasicColumn[] {
  return [
    {
      title: '算法',
      dataIndex: 'modelName',
      width: 80,
    },
    {
      title: '描述',
      dataIndex: 'description',
      width: 150,
    },
  ];
}

export const data = ref<[]>();

export function getData() {
  maHttp
    .get(
      {
        url: 'model/findModelListByDataNumAndDesc',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((v) => {
      if (v.length != 0) data.value = v;
      else data.value = [];
    });
}
