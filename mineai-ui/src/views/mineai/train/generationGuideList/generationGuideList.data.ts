import { BasicColumn, FormSchema } from '/@/components/Table';
import { h, ref } from 'vue';
import { Tag } from 'ant-design-vue';
import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';

let modelList: any = [];
const generationName = ref('');
export const columns: BasicColumn[] = [
  {
    title: '名称',
    dataIndex: 'name',
    sorter: true,
    width: 60,
  },
  {
    title: '训练数据集',
    // dataIndex: 'trainDatasetStartAll.name',
    width: 100,
    customRender: ({ record }) => {
      const dataset = record.trainDatasetStartAll;
      let text;

      if (dataset) {
        text = dataset.name; // 存在数据集时使用原名
      } else {
        text = '数据集已删除';
      }

      return { children: h('span', {}, text) };
    },
  },
  // {
  //   title: '对应算法',
  //   dataIndex: 'model',
  //   width: 100,
  //   customRender: ({ record }) => {
  //     let text;
  //     if (record.modelStore) {
  //       text = record.modelStore.modelName;
  //     } else {
  //       text = '-';
  //     }
  //     return { children: text };
  //   },
  // },
  {
    title: '设备-版本',
    dataIndex: 'device_version',
    width: 100,
    customRender: ({ record }) => {
      let text;
      if (record.modelStore) {
        text =
          record.modelApplication.device.deviceName +
          '-' +
          record.modelApplication.device.firmwareVersion;
      } else {
        text = '-';
      }
      return { children: text };
    },
  },

  {
    title: '状态',
    dataIndex: 'statusForGuided',
    width: 80,
    customRender: ({ record }) => {
      let text;
      let color;

      if (record.statusForGuided === 0) {
        text = '数据准备中';
        color = 'yellow';
      } else if (record.statusForGuided === -1) {
        text = '排队中';
        color = 'yellow';
      } else if (record.statusForGuided === 1) {
        text = '训练中';
        color = 'blue';
      } else if (record.statusForGuided === 2) {
        text = '训练完成';
        color = 'green';
      } else if (record.statusForGuided === -2) {
        text = '训练失败';
        color = 'red';
      } else if (record.statusForGuided === 3) {
        text = '转换中';
        color = 'blue';
      } else if (record.statusForGuided === 4) {
        text = '训练-转换完成';
        color = 'green';
      } else if (record.statusForGuided === -4) {
        text = '转换失败';
        color = 'red';
      } else if (record.statusForGuided === -10) {
        text = '已取消';
        color = 'grey';
      } else {
        text = '未知状态';
        color = 'red';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 80,
    sorter: true,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    colProps: { span: 5 },
    componentProps: ({ formActionType }) => ({
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ name: filteredValue });
      },
    }),
  },
];
export const addFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '训练任务名称',
    component: 'Input',
    required: true,
    colProps: { span: 20 },
  },
  {
    field: 'isAddModel',
    component: 'RadioButtonGroup',
    label: '新建算法',
    required: true,
    defaultValue: 1,
    componentProps: ({ formActionType, formModel }) => {
      return {
        options: [
          {
            label: '是',
            value: 1,
          },
          {
            label: '否',
            value: 0,
          },
        ],
        onChange: async () => {
          generationName.value = formModel.name;
          const { resetFields, setFieldsValue } = formActionType;
          await resetFields();
          await setFieldsValue({ name: generationName.value });
        },
      };
    },
  },
  {
    field: 'modelName',
    label: '算法名称',
    required: true,
    component: 'Input',
    show: ({ values }) => {
      return values.isAddModel === 1;
    },
  },
  {
    field: 'modelEnglishName',
    label: '算法英文名称',
    rules: [
      {
        required: true,
        // @ts-ignore
        validator: async (rule, value) => {
          const regCpus = /^[a-z0-9]+$/;
          if (!value) {
            return Promise.resolve();
          }
          if (!regCpus.test(value)) {
            /* eslint-disable-next-line */
            return Promise.reject('输入格式错误，请输入小写英文');
          }
          return Promise.resolve();
        },
        trigger: 'change',
      },
    ],
    required: true,
    component: 'Input',
    show: ({ values }) => {
      return values.isAddModel === 1;
    },
  },

  {
    field: 'description',
    label: '算法功能描述',
    required: true,
    component: 'Input',
    show: ({ values }) => {
      return values.isAddModel === 1;
    },
  },
  {
    field: 'model',
    label: '选择算法',
    component: 'ApiSelect',
    required: true,
    componentProps: ({ formActionType }) => {
      return {
        dropdownAlign: {
          overflow: {
            adjustY: false, // 关闭下拉框垂直位置自适应
          },
        },
        api: () =>
          maHttp
            .get(
              {
                url: 'model/getModel',
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then((v) => {
              modelList = v;
              return v;
            }),
        labelField: 'modelName',
        valueField: 'id',
        immediate: false,
        onChange: (e) => {
          const model = modelList.filter((item) => item.id === e)[0];
          const { setFieldsValue } = formActionType;
          setFieldsValue({ ...model });
        },
      };
    },
    ifShow: ({ values }) => {
      return values.isAddModel === 0;
    },
    colProps: { span: 25 },
  },
  {
    field: 'modelName',
    label: '算法名称',
    component: 'Input',
    required: true,
    componentProps: { readonly: true },
    show: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
  {
    field: 'modelEnglishName',
    label: '算法英文名称',
    component: 'Input',
    required: true,
    componentProps: { readonly: true },
    show: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
  // {
  //   field: 'monitorType',
  //   label: '算法类型',
  //   component: 'Input',
  //   render: ({ model, field }) => {
  //     console.log(model[field]);
  //     let text;
  //     switch (model[field]) {
  //       case 1:
  //         text = '可见光';
  //         break;
  //       case 2:
  //         text = '红外';
  //         break;
  //       case 3:
  //         text = '三维';
  //         break;
  //       default:
  //         text = '无';
  //     }
  //     return h(Tag, { color: 'blue' }, () => text);
  //   },
  //   show: ({ values }) => {
  //     return values.isAddModel === 0;
  //   },
  // },
  // {
  //   field: 'datasetType',
  //   label: '数据类型',
  //   component: 'Input',
  //   render: ({ model, field }) => {
  //     return h(Tag, { color: 'blue' }, () => model[field] ?? '无');
  //   },
  //   show: ({ values }) => {
  //     return values.isAddModel === 0;
  //   },
  // },
  {
    field: 'description',
    label: '算法功能描述',
    component: 'Input',
    componentProps: { readonly: true },
    show: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
];

export const reuseFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '训练任务名称',
    component: 'Input',
    required: true,
    colProps: { span: 20 },
  },
  {
    field: 'isAddModel',
    component: 'RadioButtonGroup',
    label: '新建算法',
    required: true,
    show: false,
    componentProps: ({ formActionType, formModel }) => {
      return {
        options: [
          {
            label: '否',
            value: 0,
          },
        ],
        onChange: async () => {
          generationName.value = formModel.name;
          const { resetFields, setFieldsValue } = formActionType;
          await resetFields();
          await setFieldsValue({ name: generationName.value });
        },
      };
    },
  },
  {
    field: 'modelName',
    label: '算法名称',
    required: true,
    component: 'Input',
    show: ({ values }) => {
      return values.isAddModel === 1;
    },
  },
  {
    field: 'modelEnglishName',
    label: '算法英文名称',
    rules: [
      {
        required: true,
        // @ts-ignore
        validator: async (rule, value) => {
          const regCpus = /^[a-z0-9]+$/;
          if (!value) {
            return Promise.resolve();
          }
          if (!regCpus.test(value)) {
            /* eslint-disable-next-line */
            return Promise.reject('输入格式错误，请输入小写英文');
          }
          return Promise.resolve();
        },
        trigger: 'change',
      },
    ],
    required: true,
    component: 'Input',
    show: ({ values }) => {
      return values.isAddModel === 1;
    },
  },

  {
    field: 'description',
    label: '算法功能描述',
    required: true,
    component: 'Input',
    show: ({ values }) => {
      return values.isAddModel === 1;
    },
  },
  {
    field: 'model',
    label: '选择算法',
    component: 'ApiSelect',
    required: true,
    componentProps: ({ formActionType }) => {
      return {
        dropdownAlign: {
          overflow: {
            adjustY: false, // 关闭下拉框垂直位置自适应
          },
        },
        api: () =>
          maHttp
            .get(
              {
                url: 'model/getModel',
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then((v) => {
              modelList = v;
              return v;
            }),
        labelField: 'modelName',
        valueField: 'id',
        immediate: false,
        onChange: (e) => {
          const model = modelList.filter((item) => item.id === e)[0];
          const { setFieldsValue } = formActionType;
          setFieldsValue({ ...model });
        },
      };
    },
    ifShow: ({ values }) => {
      return values.isAddModel === 0;
    },
    colProps: { span: 25 },
  },
  {
    field: 'modelName',
    label: '算法名称',
    component: 'Input',
    required: true,
    componentProps: { readonly: true },
    show: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
  {
    field: 'modelEnglishName',
    label: '算法英文名称',
    component: 'Input',
    required: true,
    componentProps: { readonly: true },
    show: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
  {
    field: 'description',
    label: '算法功能描述',
    component: 'Input',
    componentProps: { readonly: true },
    show: ({ values }) => {
      return values.isAddModel === 0;
    },
  },
];

export const getDrawerReleasedApplicationName = async () => {
  return maHttp
    .get(
      {
        url: 'modelApplication/getReleasedApplicationName',
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res.map((applicationName) => {
        return {
          label: applicationName,
          value: applicationName,
        };
      });
    });
};

export const getDrawerReleasedDeviceAndFirmWare = async () => {
  return maHttp
    .get(
      {
        url: 'device/getReleasedDeviceAndFirmWare',
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res.map((deviceAndFirmwareName) => {
        return {
          label: deviceAndFirmwareName,
          value: deviceAndFirmwareName,
        };
      });
    });
};

export const getTrainConvertAndOtherHyperParams = (modelId) => {
  return maHttp
    .get(
      {
        url: `modelJob/getHyperParams/${modelId}`,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res;
    });
};

export const getUniqueModelApplication = (applicationName, deviceAndFirmwareName) => {
  return maHttp
    .post(
      {
        url: `modelApplication/getUniqueModelApplication`,
        data: {
          applicationName,
          deviceAndFirmwareName,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res;
    });
};

export const getDeviceFirmwareByBoundApplicationName = async (applicationName) => {
  return maHttp
    .get(
      {
        url: `modelApplication/getDeviceByBoundApplicationName`,
        params: {
          applicationName,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res.map((device) => {
        return {
          label: device.deviceName + '-' + device.firmwareVersion,
          value: device.deviceName + '-' + device.firmwareVersion,
        };
      });
    });
};

export const getApplicationNameByBoundDeviceFirmware = async (deviceFirmware) => {
  return maHttp
    .get(
      {
        url: `modelApplication/getApplicationNameByBoundDeviceFirmware`,
        params: {
          deviceFirmware,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res.map((applicationName) => {
        return {
          label: applicationName.applicationName,
          value: applicationName.applicationName,
        };
      });
    });
};

export const getAllDatasets = async () => {
  return maHttp
    .get(
      {
        url: 'modelDataset/findAllDatasets',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res.map((dataset) => {
        return {
          label: dataset.fullName,
          value: dataset.id,
        };
      });
    });
};
export const findPrivateAndPublicDatasets = async () => {
  return maHttp
    .get(
      {
        url: `modelDataset/findPrivateAndPublicDatasets`,
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res.map((dataset) => {
        return {
          label: dataset.fullName,
          value: dataset.id,
        };
      });
    });
};

export const startGuidedGeneration = async (params) => {
  return maHttp
    .post(
      {
        url: 'modelGeneration/startGuidedGeneration',
        data: params,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res;
    });
  // console.log('final', params);
};

export const startForkGuidedGeneration = async (params) => {
  return maHttp
    .post(
      {
        url: 'modelGeneration/startForkGuidedGeneration',
        data: params,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res;
    });
};

export const getModelExploreByModelId = async (modelId) => {
  return maHttp
    .get(
      {
        url: `model/getModelExploreByModelId/${modelId}`,
      },
      {
        urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
      },
    )
    .then((res) => {
      return {
        id: res,
      };
    });
};

export const getConvertJobsByTrainJobID = async (trainJobID: number) => {
  return maHttp
    .get(
      {
        url: `modelJob/getConvertJobsByTrainJobID`,
        params: { trainJobID },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res; // 返回转换作业列表
    });
};

export const getJobByID = async (jobID: number) => {
  return maHttp
    .get(
      {
        url: 'modelJob/getJobByJobId',
        params: {
          id: jobID,
        },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
        timeout: 102400000,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res;
    });
};

export const getModelByApplicationNameAndDevice = async (applicationName, deviceName) => {
  return maHttp
    .get(
      {
        url: `model/getModelByApplicationNameAndDevice`,
        params: {
          applicationName,
          deviceName,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res;
    });
};

export const getModelFileApi = async (jobId: number) => {
  return maHttp
    .get(
      {
        url: `model/getWeightPathWithSize`,
        params: {
          jobId: jobId,
        },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res;
    });
};

export const downloadWeightFilesByFileName = async (
  filePath: string,
): Promise<{
  success: boolean;
  message?: string;
}> => {
  try {
    const response = await maHttp.get(
      {
        url: `model/downloadWeightFilesByFileName`,
        params: { filePath: filePath },
        responseType: 'blob',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      {
        isReturnNativeResponse: true, // 返回原生响应
        isTransformResponse: false,
        urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
      },
    );
    // 处理data为null 的情况
    if (!response.data) {
      return { success: false, message: '下载失败' };
    }
    // 获取 Blob 数据
    const blob = new Blob([response.data]); // 注意这里是 response.data，获取实际的 Blob 数据

    // 创建下载链接
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    // 动态设置下载的文件名
    if (filePath) {
      const fileName = filePath.split('/').pop() || 'default-filename';
      a.download = fileName;
    } else {
      a.download = 'default-filename';
    }

    document.body.appendChild(a);
    // 自动触发下载
    a.click();
    // 移除下载链接
    a.remove();
    window.URL.revokeObjectURL(url); // 释放 URL 对象

    // 返回成功结果
    return { success: true };
  } catch (error) {
    // 捕获错误并返回错误信息
    console.error('Error fetching file:', error);
    return { success: false, message: error instanceof Error ? error.message : 'Unknown error' };
  }
};

export const createFork = async (modelGenerationId: number) => {
  return maHttp
    .post(
      {
        url: `modelGeneration/createGuidedFork?modelGenerationId=${modelGenerationId}`,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res; // 返回接口的响应结果
    });
};

export const ResourceSchemas: FormSchema[] = [
  {
    field: 'resource',
    label: '选择资源配置',
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
            url: 'modelGeneration/findAllHardwareParams',
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        ),
      labelField: 'name',
      valueField: 'id',
      immediate: true,
      placeholder: '请选择资源配置',
    },
    colProps: { span: 20 },
  },
];

export const generateGuidedModelGenerationNameByApplicationName = async (applicationName) => {
  return maHttp
    .get(
      {
        url: `modelGeneration/generateGuidedModelGenerationNameByApplicationName`,
        params: {
          applicationName: applicationName,
        },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res;
    });
};

export const startGuidedGenerationParams = async (params) => {
  return maHttp
    .post(
      {
        url: 'modelGeneration/startGuidedGenerationParams',
        data: params,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res;
    });
  // console.log('final', params);
};

export const getLatestJobId = async (modelGenerationId: Number) => {
  return maHttp
    .get(
      {
        url: `modelGeneration/getLatestJobIdByGenerationId`,
        params: {
          modelGenerationId: modelGenerationId,
        },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((res) => {
      return res;
    });
};

export const importDatasetFromDatasetVersion = async (targetDatasetId, datasetVersionId) => {
  return await maHttp.post(
    {
      url: `datasets/importDatasetFromDatasetVersion?targetDatasetId=${targetDatasetId}&datasetVersionId=${datasetVersionId}`,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const deleteGuidedModelGeneration = async (modelGenerationId) => {
  return maHttp.get(
    {
      url: 'modelGeneration/deleteModelGeneration',
      params: { modelGenerationId: modelGenerationId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export const deleteDatasetBatch = async (datasetIds) => {
  return maHttp.delete(
    {
      url: 'datasets',
      data: { ids: datasetIds },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getDatasetGroupNameByModelGenerationId = async (modelGenerationId) =>
  maHttp.get(
    {
      url: 'modelGeneration/getDatasetGroupNameByModelGenerationId',
      params: {
        modelGenerationId,
      },
      headers: {},
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
