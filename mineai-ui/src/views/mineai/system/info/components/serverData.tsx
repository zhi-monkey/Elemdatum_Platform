import { BasicColumn } from '/@/components/Table/src/types/table';
import { ref } from 'vue';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

export function getBasicColumns(): BasicColumn[] {
  return [
    {
      title: '设备IP',
      dataIndex: 'controller.ip',
      width: 10,
    },
    {
      title: 'CPU占用率',
      width: 60,
      dataIndex: 'cpuLoad',
      slots: { customRender: 'cpuProcess' },
    },
    {
      title: 'RAM占用率',
      width: 90,
      dataIndex: 'ramLoad',
      slots: { customRender: 'ramProcess' },
    },
    {
      title: '硬盘占用率',
      width: 90,
      dataIndex: 'diskLoad',
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
