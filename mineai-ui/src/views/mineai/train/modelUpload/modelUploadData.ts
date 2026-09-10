import { FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { ref } from 'vue';

export const selectedModelName = ref();
// 个人信息 form
export const schemas: FormSchema[] = [
  {
    field: 'modelName',
    label: '算法',
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      api: () =>
        maHttp.get(
          {
            url: 'model/getModel',
            params: {},
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        ),
      labelField: 'modelName',
      valueField: 'modelName',
      immediate: true,
      placeholder: '请选择要使用的算法',
      onChange: (e) => {
        selectedModelName.value = e;
      },
    },
    colProps: { span: 16 },
    required: true,
  },
];
