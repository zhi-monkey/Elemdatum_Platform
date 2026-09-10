import { BasicColumn } from '/@/components/Table/src/types/table';
import { h, ref } from 'vue';
import { Tag } from 'ant-design-vue';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

export function getBasicColumns(): BasicColumn[] {
  return [
    {
      title: '设备名称',
      dataIndex: 'monitorName',
      width: 130,
    },
    {
      title: '监控数据类型 ',
      dataIndex: 'dataType',
      width: 100,
      customRender: ({ record }) => {
        const dataType = record.dataType;
        let color = 'yellow';
        let text = '图像';
        if (dataType === '视频') {
          color = 'blue';
          text = '视频';
        } else if (dataType === '音频') {
          color = 'purple';
          text = '音频';
        } else if (dataType === '点云') {
          color = 'green';
          text = '点云';
        }
        return h(Tag, { color: color }, () => text);
      },
    },
    {
      title: '设备场景',
      dataIndex: 'scene.name',
      width: 110,
    },
    {
      title: '推流',
      dataIndex: 'isPushStream',
      width: 100,
      customRender: ({ record }) => {
        const isPushStream = record.isPushStream;
        const enable = ~~isPushStream === 1;
        const color = enable ? 'green' : 'red';
        const text = enable ? '是' : '否';
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
        url: 'monitor/getMonitorListByDataNum',
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
