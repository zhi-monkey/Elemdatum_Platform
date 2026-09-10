import { BasicColumn } from '/@/components/Table/src/types/table';
import { ref } from 'vue';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

interface monitorAlertdata {
  monitorName: string;
  alertNum: number;
}
export function getBasicColumns(): BasicColumn[] {
  return [
    {
      title: '设备名称',
      dataIndex: 'monitorName',
      width: 120,
    },
    {
      title: '设备报警数',
      dataIndex: 'alertNum',
      width: 100,
    },
  ];
}

export const monitorAlert = ref<monitorAlertdata[]>([]);

export async function getData() {
  await maHttp
    .get(
      {
        url: 'modelAlert/getMostModelAlertMonitorListByDateNumInTable',
        params: { dateNum: 7 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((v) => {
      const itemData = ref({});
      itemData.value = v;
      const monitorAlertList: monitorAlertdata[] = [];
      for (const i in itemData.value['nameList']) {
        monitorAlertList.push({
          monitorName: itemData.value['nameList'][i],
          alertNum: itemData.value['numList'][i],
        });
      }
      monitorAlert.value = monitorAlertList;
    });
}
