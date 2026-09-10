import { BasicColumn } from '/@/components/Table/src/types/table';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { ref } from 'vue';
export function getBasicColumns(): BasicColumn[] {
  return [
    {
      title: '监控设备',
      dataIndex: 'monitor.monitorName',
      width: '20%',
      customRender: ({ record }) => {
        return record.subsystem === 'CENTRAL_PLATFORM'
          ? record.monitor.monitorName
          : record.monitorName;
      },
    },
    {
      title: '算法',
      dataIndex: 'model.modelName',
      width: '20%',
      customRender: ({ record }) => {
        return record.subsystem === 'CENTRAL_PLATFORM' ? record.model.modelName : record.modelName;
      },
    },
    {
      title: '描述',
      dataIndex: 'description',
      width: '40%',
    },
    {
      title: '警报时间',
      dataIndex: 'createTime',
      width: '20%',
    },
  ];
}

export const data = ref<[]>();

export function getData() {
  maHttp
    .get(
      {
        url: 'modelAlert/getModelAlertListByDataNum',
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
