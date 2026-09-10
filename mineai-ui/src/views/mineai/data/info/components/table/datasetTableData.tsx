import { BasicColumn } from '/@/components/Table/src/types/table';
import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
import { h, ref } from 'vue';
import { Tag } from 'ant-design-vue';
import { formatDateTime } from '/@/utils';

export function getBasicColumns(): BasicColumn[] {
  return [
    {
      title: 'ID',
      dataIndex: 'id',
      fixed: 'left',
      width: 100,
    },
    {
      title: '名称',
      dataIndex: 'name',
      width: 100,
    },
    {
      title: '标注类型',
      dataIndex: 'annotateType',
      customRender: ({ record }) => {
        // 102是目标检测，103是语义分割
        const annotateType = record.annotateType;
        let color = 'yellow';
        let text = '目标检测';
        if (annotateType === 103) {
          color = 'blue';
          text = '目标分割';
        }
        return h(Tag, { color: color }, () => text);
      },
      width: 100,
    },
    {
      title: '创建时间',
      width: 100,
      dataIndex: 'createTime',
      customRender: ({ record }) => {
        return formatDateTime(record.createTime);
      },
    },
  ];
}

export const data = ref<[]>();

export async function getData() {
  await maHttp
    .get(
      {
        url: 'datasets',
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    )
    .then((v) => {
      if (v.result.length != 0) data.value = v.result;
      else data.value = [];
    });
}
