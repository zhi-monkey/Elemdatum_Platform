import { BasicColumn } from '/@/components/Table/src/types/table';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { h, ref } from 'vue';
import { Tag } from 'ant-design-vue';

export function getBasicColumns(): BasicColumn[] {
  return [
    {
      title: 'ID',
      dataIndex: 'id',
      fixed: 'left',
      width: 100,
    },
    {
      title: '设备名称',
      dataIndex: 'monitorName',
      fixed: 'left',
      width: 100,
    },
    {
      title: '场景',
      dataIndex: 'scene.name',
      width: 100,
    },
    {
      title: '类型',
      dataIndex: 'dataType',
      width: 100,
    },
    {
      title: '推流',
      width: 100,
      dataIndex: 'isPushStream',
      customRender: ({ record }) => {
        const isPushStream = record.isPushStream;
        let stateColor = 'green';
        let stateText = '是';
        if (isPushStream === 2) {
          stateText = '否';
          stateColor = 'yellow';
        }
        return h(Tag, { color: stateColor }, () => stateText);
      },
    },
  ];
}
export const data = ref<[]>();

export async function getData() {
  await maHttp
    .get(
      {
        url: 'monitorDataset/findMonitorsLinkedDatasetByNum',
        params: { num: 10 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    )
    .then((v) => {
      if (v.length != 0) data.value = v;
      else data.value = [];
    });
}
