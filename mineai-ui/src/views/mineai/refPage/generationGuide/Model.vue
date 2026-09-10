<template>
  <div class="step">
    <div class="step-form">
      <BasicForm @register="register" />
    </div>
    <Divider />
    <h3>说明</h3>
    <p> 选择已有算法商城中的算法，进行训练，并自动比较。 </p>
  </div>
</template>
<script lang="ts">
  import { defineComponent, watch } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { selectedModel, step1Schemas } from './data';
  import { Divider } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { hyperParamTrainPrefix } from '/@/views/mineai/model/generationEdit/data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useUserStore } from '/@/store/modules/user';

  const userStore = useUserStore();
  const userData = userStore.getUserInfo;
  export default defineComponent({
    components: {
      BasicForm,
      Divider,
    },
    emits: ['next'],
    setup(_, { emit }) {
      let hyperParamTrainList: string[] = [];
      const [register, { validate, appendSchemaByField, removeSchemaByFiled }] = useForm({
        labelWidth: 200,
        schemas: step1Schemas,
        actionColOptions: {
          span: 14,
        },
        showResetButton: false,
        submitButtonOptions: {
          text: '下一步',
        },
        submitFunc: customSubmitFunc,
      });
      const { createMessage } = useMessage();

      //提交按钮
      async function customSubmitFunc() {
        try {
          let modelId;
          let generationId;
          let generationName;
          //获取常规配置参数
          const values = await validate();
          modelId = values.model;
          generationName = values.name;
          let infoValues = {};
          infoValues['name'] = values.name;
          infoValues['model'] = { id: modelId };
          infoValues['description'] = values.description;
          infoValues['splitSize'] = values.splitSize;
          infoValues['datasetSource'] = values.datasetSource;
          infoValues['trainDataset'] = values.trainDataset;
          if (values.testDataset != null) {
            infoValues['testDataset'] = values.testDataset;
          }
          if (values.valDataset != null) {
            infoValues['valDataset'] = values.valDataset;
          }
          if (values.resource != null) {
            infoValues['hardwareParams'] = { id: values.resource };
          }
          let params = {};
          //拆出训练超参
          Object.keys(values).forEach((key) => {
            if (key.length > hyperParamTrainPrefix.length) {
              const prefix = key.slice(0, hyperParamTrainPrefix.length);
              const hyperParam = key.slice(hyperParamTrainPrefix.length);
              if (prefix === hyperParamTrainPrefix) {
                params[hyperParam] = values[key];
              }
            }
          });
          params['MODE_WORKING_MODE'] = '1';
          infoValues['hyperParams'] = params;
          infoValues['isReuse'] = true;
          infoValues['isGuided'] = true;
          infoValues['userId'] = userData.id;
          //生产引导类型mg
          await maHttp
            .post(
              {
                url: 'modelGeneration/addModelGeneration',
                params: infoValues,
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then((v) => {
              generationId = v;
              createMessage.success('训练任务创建成功！');
            });

          emit('next', {
            generationId: generationId,
            generationName: generationName,
          });
        } catch (error) {
          console.log(error);
        }
      }

      watch(selectedModel, async () => {
        hyperParamTrainList.forEach((value) => {
          removeSchemaByFiled(hyperParamTrainPrefix + value);
        });
        let trainParams = selectedModel.value.trainModelVersion.modelConfigList;
        hyperParamTrainList = [];
        if (trainParams != null) {
          trainParams.forEach((e) => {
            if (e.field != null && e.field != '') hyperParamTrainList.push(e.field);
          });
          hyperParamTrainList.forEach((value) => {
            appendSchemaByField(
              {
                field: hyperParamTrainPrefix + value,
                label: '超参数:' + value,
                component: 'Input',
                componentProps: {
                  placeholder: '请输入对应超参',
                },
                required: true,
                colProps: { span: 20 },
              },
              undefined,
              undefined,
            );
          });
        }
      });
      return { register };
    },
  });
</script>
<style lang="less" scoped>
  .step {
    &-form {
      width: 450px;
      margin: 0 auto;
    }

    h3 {
      margin: 0 0 12px 10px;
      font-size: 16px;
      line-height: 32px;
    }

    h4 {
      margin: 0 0 4px 10px;
      font-size: 14px;
      line-height: 22px;
    }

    p {
      margin-left: 10px;
    }
  }
</style>
