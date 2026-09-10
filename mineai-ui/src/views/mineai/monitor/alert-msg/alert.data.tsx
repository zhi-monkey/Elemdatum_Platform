import { BasicColumn, FormSchema } from '/@/components/Table';
import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { h, ref } from 'vue';
import { Tag } from 'ant-design-vue';

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

export const columns: BasicColumn[] = [
  {
    title: '警报信息ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 50,
    sorter: true,
  },
  {
    title: '上报时间',
    dataIndex: 'createTime',
    width: 80,
  },
  {
    title: '报警内容',
    dataIndex: 'description',
    width: 100,
  },
  {
    title: '监控设备',
    dataIndex: 'monitorName',
    customRender: ({ record }) => {
      const monitorName =
        record.subsystem !== 'CENTRAL_PLATFORM' ? record.monitorName : record.monitor.monitorName;
      return { children: monitorName };
    },
    width: 80,
  },
  {
    title: '算法名称',
    dataIndex: 'modelName',
    customRender: ({ record }) => {
      const modelName =
        record.subsystem !== 'CENTRAL_PLATFORM' ? record.modelName : record.model.modelName;
      return { children: modelName };
    },

    width: 80,
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 50,
    customRender: ({ record }) => {
      const status = record.status;
      let color;
      let text;
      if (status === 1) {
        color = 'red';
        text = '未处理';
      } else if (status === 2) {
        color = 'blue';
        text = '已忽略';
      } else {
        color = 'green';
        text = '已线下处理';
      }
      return h(
        Tag,
        { color: color },
        {
          default: () => text,
        },
      );
    },
  },
  {
    title: '图片',
    dataIndex: 'image',
    width: 50,
    slots: { customRender: 'image' },
  },
  {
    title: '视频',
    dataIndex: 'video',
    width: 50,
    slots: { customRender: 'video' },
  },
  {
    title: '音频',
    dataIndex: 'audio',
    width: 50,
    slots: { customRender: 'audio' },
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'monitorName',
    label: '监控设备',
    component: 'Select',
    componentProps: ({ formActionType }) => {
      return {
        options: monitorData.value,
        onChange: (e) => {
          const { setFieldsValue } = formActionType;
          setFieldsValue({ modelName: null }).then(() => {
            modelData.value = [];
          });
          getModelData(e).then();
        },
      };
    },
  },
  {
    field: 'modelName',
    label: '算法名称',
    component: 'Select',
    componentProps: () => {
      return {
        options: modelData.value,
      };
    },
  },
  {
    field: 'startTime',
    component: 'DatePicker',
    componentProps: {
      showTime: true,
    },
    label: '起始时间',
    colProps: {
      span: 4,
    },
  },
  {
    field: 'endTime',
    component: 'DatePicker',
    componentProps: {
      showTime: true,
    },
    label: '结束时间',
    colProps: {
      span: 4,
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
    },
    label: '状态',
    colProps: {
      span: 4,
    },
  },
];

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
//批量处理报警信息用的form表单
export const formSchemaPi: FormSchema[] = [
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
