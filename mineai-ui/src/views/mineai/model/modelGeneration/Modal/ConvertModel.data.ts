import { FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

export const formSchema: FormSchema[] = [
  {
    field: 'type',
    label: '设备类型',
    component: 'Select',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      options: [
        { value: 'rk1806', label: 'rk1806' },
        { value: 'rk1808', label: 'rk1808' },
        { value: 'rk3399pro', label: 'rk3399pro' },
        { value: 'rv1126', label: 'rv1126' },
        { value: 'rv1109', label: 'rv1109' },
      ],
    },
    required: true,
  },
  {
    field: 'datasetPath',
    label: '数据集',
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        maHttp
          .get(
            {
              url: `datarepos/queryAllDataRepos`,
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
          )
          .then((v) => {
            return v;
          }),
      labelField: 'name',
      valueField: 'uri',
      immediate: false,
      placeholder: '请选择数据集',
    },
    required: true,
  },
];
