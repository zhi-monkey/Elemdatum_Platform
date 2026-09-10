import { FormSchema } from '/@/components/Form';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { ref } from 'vue';

interface data {
  label: string;
  value: string;
}
export const monitorData = ref<data[]>([]);
export const modelData = ref<data[]>([]);

export async function getMonitorData() {
  maHttp
    .get(
      {
        url: 'modelAlert/getMonitorNameWithAlert',
        params: {},
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((maList) => {
      const monitorNameList: data[] = [];

      maList.forEach((v) => {
        monitorNameList.push({ label: v, value: v });
      });
      monitorData.value = monitorNameList;
    });
}
export async function getModelData(monitorName) {
  if (monitorName == undefined) {
    //如果没有选择监控设备
    maHttp
      .get(
        {
          url: 'modelAlert/getModelNameWithAlert',
          params: {},
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((maList) => {
        const modelNameList: data[] = [];

        maList.forEach((v) => {
          modelNameList.push({ label: v, value: v });
        });
        modelData.value = modelNameList;
      });
  } else {
    //选了监控设备之后，在数据库里面查询是否有对应监控设备
    maHttp
      .get(
        {
          url: 'monitor/findMonitorByMonitorName',
          params: { monitorName: monitorName },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
      )
      .then((monitor) => {
        //判断这个报警信息的监控设备是否存在于平台数据库里
        if (monitor == null) {
          //平台中不存在当前监控设备，根据字段查询
          maHttp
            .get(
              {
                url: 'modelAlert/getModelNameByMonitorString',
                params: { monitorName: monitorName },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then((v) => {
              const modelNames: data[] = [];
              if (v != null) {
                v.forEach((v) => {
                  modelNames.push({ label: v, value: v });
                });
              }
              modelData.value = modelNames;
            });
        } else {
          //监控设备在数据库中，若是子系统的，根据字段查询
          if (monitor.subsystem != 'CENTRAL_PLATFORM') {
            maHttp
              .get(
                {
                  url: 'modelAlert/getModelNameByMonitorString',
                  params: { monitorName: monitorName },
                  headers: {
                    // @ts-ignore
                    ignoreCancelToken: true,
                  },
                },
                { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
              )
              .then((v) => {
                const modelNames: data[] = [];
                if (v != null) {
                  v.forEach((v) => {
                    modelNames.push({ label: v, value: v });
                  });
                }
                modelData.value = modelNames;
              });
          } else {
            //如果监控设备在数据库表里面，是中央平台的话，根据外键查询
            maHttp
              .get(
                {
                  url: 'modelAlert/getModelsByMonitorInModelAlert',
                  params: { monitorId: monitor.id },
                  headers: {
                    // @ts-ignore
                    ignoreCancelToken: true,
                  },
                },
                { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
              )
              .then((v) => {
                const modelNames: data[] = [];
                if (v != null) {
                  v.forEach((v) => {
                    modelNames.push({ label: v, value: v });
                  });
                }
                modelData.value = modelNames;
              });
          }
        }
      });
  }
}

export const searchFormSchema: FormSchema[] = [
  {
    field: 'monitorName',
    label: '监控设备',
    component: 'Select',
    componentProps: ({ formActionType }) => {
      return {
        showSearch: true,
        options: monitorData.value,
        colProps: { span: 4 },
        onChange: (e) => {
          selectedMonitorName.value = e;
          const { setFieldsValue } = formActionType;
          setFieldsValue({ modelName: null }).then(() => {
            modelData.value = [];
          });
          getModelData(e).then();
        },
      };
    },
    colProps: {
      xl: 12,
      xxl: 8,
    },
  },
  {
    field: 'modelName',
    label: '算法名称',
    component: 'Select',
    componentProps: () => {
      return {
        showSearch: true,
        colProps: { span: 4 },
        options: modelData.value,
        onChange: (model) => {
          selectedModelName.value = model;
        },
      };
    },
    colProps: {
      xl: 12,
      xxl: 8,
    },
  },
  {
    field: 'status',
    component: 'Select',
    componentProps: {
      options: [
        { label: '未处理', value: 1 },
        { label: '已忽略', value: 2 },
        { label: '已线下处理', value: 3 },
      ],
      onChange: (e) => {
        selectedStatus.value = e;
      },
    },
    label: '状态',
    colProps: {
      xl: 12,
      xxl: 8,
    },
  },

  {
    field: 'startTime',
    component: 'DatePicker',
    componentProps: {
      valueFormat: 'YYYY-MM-DD HH:mm:ss',
      showTime: {
        format: 'YYYY-MM-DD HH:mm:ss',
        showTime: true,
      },
      onChange: (e) => {
        selectedStartTime.value = e;
      },
    },
    label: '起始时间',
    colProps: {
      xl: 12,
      xxl: 8,
    },
  },
  {
    field: 'endTime',
    component: 'DatePicker',
    componentProps: {
      valueFormat: 'YYYY-MM-DD HH:mm:ss',
      showTime: {
        format: 'YYYY-MM-DD HH:mm:ss',
        showTime: true,
      },
      onChange: (e) => {
        selectedEndTime.value = e;
      },
    },
    label: '结束时间',
    colProps: {
      xl: 12,
      xxl: 8,
    },
  },
];
// 处理状态modal使用的表单
export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: 'ID',
    component: 'Input',
    colProps: { span: 4 },
    show: false,
  },
  {
    field: 'status',
    label: '状态',
    component: 'RadioButtonGroup',
    required: true,
    componentProps: {
      options: [
        { label: '已忽略', value: 2 },
        { label: '已线下处理', value: 3 },
      ],
    },
  },
];

// 新增筛选框选项
export const selectedMonitorName = ref('');
export const selectedModelName = ref('');
export const selectedStatus = ref(0);
export const selectedStartTime = ref();
export const selectedEndTime = ref();
