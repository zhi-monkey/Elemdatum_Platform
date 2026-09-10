<template>
  <PageWrapper :title="`算法管理`" @back="goBack" contentFullHeight style="margin: 0 16px 0 16px">
    <div class="lg:flex flex-row lg:space-x-4">
      <a-card title="填写作业信息" style="height: 100%" class="lg:w-1/4 myForm">
        <BasicForm @register="registerForm" ref="formElRef" />
        <div class="flex flex-row justify-end">
          <a-button type="primary" @click="handleSubmit"> 提交</a-button>
          <a-button type="dashed" @click="reset" style="margin-left: 30px"> 重置</a-button>
        </div>
      </a-card>
      <a-card class="lg:w-3/4" title="历史作业记录">
        <BasicTable @register="registerTable" :pagination="{ pageSize: 10 }" class="myTable">
          <template #expandedRowRender="{ record }">
            <SubTable :jobId="record.id" />
          </template>
        </BasicTable>
      </a-card>
    </div>
  </PageWrapper>
</template>
<script lang="ts">
  import { defineComponent, onMounted, watch } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import {
    columns,
    hyperParamList,
    hyperParamPrefix,
    jobSchemas,
    oldId,
    selectedJobType,
    selectedModel,
    selectedModelVersion,
  } from './job';
  import { Card as aCard } from 'ant-design-vue';
  import { BasicTable, useTable } from '/@/components/Table';
  import { data } from 'autoprefixer';
  import * as handleEdit from 'lodash-es';
  import * as handleDelete from 'lodash-es';
  import borderHorizontalOutlined from '@ant-design/icons-vue/lib/icons/BorderHorizontalOutlined';
  import AButton from '/@/components/Button/src/BasicButton.vue';
  import { PageWrapper } from '/@/components/Page';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useGo } from '/@/hooks/web/usePage';
  import { useRoute } from 'vue-router';
  import SubTable from '/@/views/mineai/model/createJob/SubTable.vue';
  import { useMessage } from '/@/hooks/web/useMessage';

  export default defineComponent({
    components: {
      AButton,
      aCard,
      SubTable,
      BasicTable,
      BasicForm,
      PageWrapper,
    },
    setup() {
      /**
       * 创建三个临时变量，用来在每次reload之后变为0，并且存放selected变量的值
       * */
      let currentModel;
      let currentModelVersion;
      let currentJobType;
      const go = useGo();
      const { createMessage } = useMessage();
      const [
        registerForm,
        { resetFields, setFieldsValue, updateSchema, appendSchemaByField, validate },
      ] = useForm({
        labelWidth: 150,
        schemas: jobSchemas,
        showActionButtonGroup: false,
        labelAlign: 'left',
      });

      //在这获取模型管理页面传过来的参数:分别是模型id,模型版本id,作业类型,是否使用GPU
      const route = useRoute();
      const modelId = route.query.modelId;
      const modelVersionId = route.query.modelVersionId;
      const modelName = route.query.modelName;
      const modelVersionName = route.query.modelVersionName;
      const jobType = route.query.jobType;
      const [registerTable, { reload }] = useTable({
        showIndexColumn: false,
        canResize: true,
        api: (params) => {
          /**
           * 通过http请求获取历史作业记录列表
           * */
          return maHttp
            .get(
              {
                url: 'modelJob/jobList',
                params: {
                  model: currentModel,
                  modelVersion: currentModelVersion,
                  jobType: currentJobType,
                  ...params,
                },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then((v) => {
              //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
              v.items = v.content;
              v.total = v.totalElements;
              return v;
            });
        },
        beforeFetch: (v) => {
          //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
          v.page -= 1;
          return v;
        },
        columns,
        showTableSetting: true,
        bordered: true,
      });

      //reset函数功能：重置表单，重置历史记录作业表
      function reset() {
        resetFields();
        setFieldsValue({
          modelName: parseInt(modelId as string),
          modelVersionId: parseInt(modelVersionId as string),
          showedModelName: modelName,
          showedModelVersion: modelVersionName,
        });
        updateJobType(parseInt(modelVersionId as string));
        reload();
      }

      const goBack = () => {
        go('/maTrainingCenter/modelList');
      };
      const goNext = (jobId) => {
        go(`/maTrainingCenter/jobDetails/${jobId}`);
      };

      function updateJobType(mvId: number) {
        maHttp
          .get(
            {
              url: 'modelVersion/findModelVersionById',
              params: { id: mvId },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then(() => {
            if (jobType === '1') {
              updateSchema([
                {
                  field: 'jobType',
                  label: '作业类型',
                  component: 'RadioButtonGroup',
                  componentProps: ({}) => {
                    return {
                      options: [{ label: '训练', value: 1 }],

                      // onChange: (e) => {
                      //   const { updateSchema } = formActionType;
                      //   selectedJobType.value = e;
                      //   if (e === 1) updateSchema({ field: 'weightPath', show: false });
                      //   if (e === 2) updateSchema({ field: 'weightPath', show: true });
                      // },
                    };
                  },
                  required: true,
                },
                { field: 'weightPath', show: false },
              ]);
              setFieldsValue({ jobType: 1 });
            } else {
              updateSchema([
                {
                  field: 'jobType',
                  label: '作业类型',
                  component: 'RadioButtonGroup',
                  componentProps: ({}) => {
                    return {
                      options: [{ label: '质检', value: 2 }],
                      // onChange: (e) => {
                      //   const { updateSchema } = formActionType;
                      //   selectedJobType.value = e;
                      //   if (e === 1) updateSchema({ field: 'weightPath', show: false });
                      //   if (e === 2) updateSchema({ field: 'weightPath', show: true });
                      // },
                    };
                  },
                  required: true,
                },
                { field: 'weightPath', show: true },
              ]);
              setFieldsValue({ jobType: 2 });
            }
          });
      }

      /**
       * 当页面加载之后
       * */
      onMounted(() => {
        selectedModel.value = null;
        selectedModelVersion.value = null;
        selectedJobType.value = 0;

        maHttp
          .get(
            {
              url: 'deploy/getGpuMax',
              params: {},
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            setFieldsValue({ gpu: v.gpuRemained + '(GiB)' + ' / ' + v.gpuNum + '(GiB)' });
          });

        // 判断如果有传过来的相关参数那就放到页面里
        if (modelId != null) {
          //首先更新model的表单，把传过来的modelId值设置到modelName这个表单里面
          setFieldsValue({
            showedModelName: modelName,
            showedModelVersion: modelVersionName,
            modelName: modelId,
          });
          selectedModel.value = parseInt(modelId as string);

          //然后更新modelVersionId这个表单
          updateSchema({
            field: 'modelVersionId',
            label: '算法版本',
            component: 'ApiSelect',
            required: true,
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
                      url: 'modelVersion/findModelVersionsTrainOrInspectable',
                      params: { modelId },
                      headers: {
                        // @ts-ignore
                        ignoreCancelToken: true,
                      },
                    },
                    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                  )
                  .then((v) => {
                    v.forEach((value) => {
                      if (value.modelConfigList != null) {
                        const arr: string[] = [];
                        value.modelConfigList.forEach((e) => {
                          if (e.field != null && e.field != '') arr.push(e.field);
                        });
                        hyperParamList[value.id] = arr;
                      }
                    });
                    if (modelVersionId != null) {
                      setFieldsValue({ modelVersionId: parseInt(modelVersionId as string) });
                      updateJobType(parseInt(modelVersionId as string));
                      oldId.value = parseInt(modelVersionId as string);
                      selectedModelVersion.value = parseInt(modelVersionId as string);
                      updateSchema({
                        field: 'weightPath',
                        label: '选择权重文件',
                        component: 'ApiSelect',
                        componentProps: {
                          placeholder: '请选择权重文件',
                          dropdownAlign: {
                            overflow: {
                              adjustY: false, // 关闭下拉框垂直位置自适应
                            },
                          },
                          api: () =>
                            maHttp
                              .get(
                                {
                                  url: 'modelJob/getJobByModelVersionId',
                                  params: { id: modelVersionId },
                                  headers: {
                                    // @ts-ignore
                                    ignoreCancelToken: true,
                                  },
                                },
                                { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                              )
                              .then((v) => {
                                return v;
                              }),
                          labelField: 'description',
                          valueField: 'name',
                          immediate: false,
                        },
                      });
                      //追加超参输入框
                      hyperParamList[parseInt(modelVersionId as string)].forEach((value) => {
                        appendSchemaByField(
                          {
                            field: hyperParamPrefix + value,
                            label: value,
                            component: 'Input',
                            required: true,
                          },
                          undefined,
                          undefined,
                        );
                        if (value == 'HP_BATCH_SIZE') {
                          setFieldsValue({ hyperParamPrefixHP_BATCH_SIZE: '20' });
                        } else if (value == 'HP_EPOCHES') {
                          setFieldsValue({ hyperParamPrefixHP_EPOCHES: '20' });
                        } else if (value == 'HP_LEARNING_RATE') {
                          setFieldsValue({ hyperParamPrefixHP_LEARNING_RATE: '0.001' });
                        }
                      });
                    }
                    return v;
                  }),
              labelField: 'name',
              valueField: 'id',
              immediate: true,
            },
          });
          updateSchema({
            //更新当前的dataset表单
            field: 'dataset',
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
                      url: 'modelDataset/findDatasetByDataType',
                      params: { modelId: modelId },
                      headers: {
                        // @ts-ignore
                        ignoreCancelToken: true,
                      },
                    },
                    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
                  )
                  .then((v) => {
                    return v;
                  }),
              labelField: 'name',
              valueField: 'id',
              immediate: false,
              placeholder: '请选择作业需要的数据集',
            },
            required: true,
          });
        } else {
          currentModel = null;
          currentModelVersion = null;
          currentJobType = 0;
        }
        /**
         * 判断传过来的jobType，如果不是NaN也不是0，就赋值，否则的话就把selectedJobType 设为 0
         * */
        if (!isNaN(parseInt(jobType as string)) && parseInt(jobType as string) != 0) {
          setFieldsValue({ jobType: parseInt(jobType as string) });
          selectedJobType.value = parseInt(jobType as string);
          updateSchema({
            field: 'weightPath',
            show: () => {
              return parseInt(jobType as string) === 2;
            },
          });
        } else {
          selectedJobType.value = 0;
        }
      });
      //监视所选模型版本以及作业类型变量，当这两个监视对象发生改变时，更新历史记录表格
      watch([selectedModelVersion, selectedJobType], () => {
        if (
          selectedJobType.value === 0 ||
          selectedJobType.value === 1 ||
          selectedJobType.value === 2
        ) {
          currentModel = selectedModel.value;
          currentModelVersion = selectedModelVersion.value;
          currentJobType = selectedJobType.value;
          reload();
        }
      });
      return {
        goBack,
        registerForm,
        registerTable,
        reset,
        handleSubmit: async () => {
          const values = await validate();
          let modelValues = { params: {} };
          Object.keys(values).forEach((key) => {
            if (key.length <= hyperParamPrefix.length) {
              modelValues[key] = values[key];
            } else {
              const prefix = key.slice(0, hyperParamPrefix.length);
              const hyperParam = key.slice(hyperParamPrefix.length);
              if (prefix === hyperParamPrefix) {
                modelValues['params'][hyperParam] = values[key];
              } else {
                modelValues[key] = values[key];
              }
            }
          });
          modelValues['modelVersion'] = values['modelVersion'];
          const model_version_id = values.modelVersionId;
          await maHttp
            .get(
              {
                url: 'modelVersion/findModelVersionById',
                params: { id: model_version_id },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then((v) => {
              values.modelVersion = v;
              modelValues['modelVersion'] = v;
              // modelValues[]
            });

          modelValues['dataset'] = { id: values.dataset };
          await maHttp
            .post(
              {
                url: 'modelJob/addModelJob',
                params: {
                  modelValues,
                },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then((v) => {
              reload();
              createMessage.success('新建作业成功！');
              goNext(v);
            });
        },
      };
    },
    computed: {
      handleDelete() {
        return handleDelete;
      },
      handleEdit() {
        return handleEdit;
      },
      data() {
        return data;
      },
    },
    methods: {
      borderHorizontalOutlined,
    },
  });
</script>
<style scoped>
  .myTable >>> .ant-table-body {
    overflow-x: scroll;
  }

  .myCard >>> .ant-card-body {
    height: 900px;
    overflow: auto;
  }

  .myForm >>> .vben-basic-form {
    height: 70vh;
    overflow: auto;
  }
</style>
