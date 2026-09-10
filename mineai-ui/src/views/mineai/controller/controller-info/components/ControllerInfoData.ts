import { BasicColumn } from '/@/components/Table/src/types/table';

import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { h, ref } from 'vue';
import { Tag } from 'ant-design-vue';

export function getBasicColumns(): BasicColumn[] {
  return [
    {
      title: '设备IP',
      dataIndex: 'ip',
      width: 100,
    },
    {
      title: '更新时间',
      dataIndex: 'lastModifiedTime',
      width: 100,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      customRender: ({ record }) => {
        const status = record.status;
        let color = 'green';
        let text = '运行';
        if (status === -1) {
          color = 'red';
          text = '故障';
        }
        return h(Tag, { color: color }, () => text);
      },
    },
  ];
}

export const data = ref<[]>();

export async function getData() {
  await maHttp
    .get(
      {
        url: 'controller/getControllerListByDataNum',
        params: { num: 50 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    )
    .then((v) => {
      if (v.length != 0) data.value = v;
      else data.value = [];
    });
}
