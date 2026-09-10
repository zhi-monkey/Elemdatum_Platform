import { BasicColumn } from '/@/components/Table/src/types/table';
import { ref } from 'vue';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

export function getBasicColumns(): BasicColumn[] {
  return [
    {
      title: '设备IP',
      dataIndex: 'controller.ip',
      width: 50,
    },
    {
      title: 'CPU占用率',
      dataIndex: 'cpuLoad',
      width: 100,
      sorter: true,
      slots: { customRender: 'cpuProcess' },
    },
    {
      title: 'RAM占用率',
      width: 120,
      dataIndex: 'ramLoad',
      sorter: true,
      slots: { customRender: 'ramProcess' },
    },
    {
      title: '硬盘占用率',
      width: 120,
      dataIndex: 'diskLoad',
      sorter: true,
      slots: { customRender: 'diskProcess' },
    },
  ];
}

export const data = ref<[]>();

export async function getData() {
  await maHttp
    .get(
      {
        url: 'controllerLoad/getControllerLoadListByDataNum',
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
