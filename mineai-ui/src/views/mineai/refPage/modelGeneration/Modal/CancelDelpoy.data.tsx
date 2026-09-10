import { FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { ref } from 'vue';

export const modelVersionId = ref();
export const formSchema: FormSchema[] = [
  {
    field: 'deploy',
    label: '要取消的部署',
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
              url: 'modelDeployment/findModelDeploymentByModelVersionId',
              params: { modelVersionId: modelVersionId.value },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            const result: { name: string; value: any }[] = v.map((item) => {
              return { name: item.description, value: JSON.stringify(item) };
            });
            return result;
          }),
      labelField: 'name',
      valueField: 'value',
      immediate: false,
    },
  },
];
