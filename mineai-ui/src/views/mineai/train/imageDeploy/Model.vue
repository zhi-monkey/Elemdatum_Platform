<template>
  <div class="step">
    <div class="step-form">
      <BasicForm @register="register" />
    </div>
    <Divider />
    <h3>说明</h3>
    <p> 根据需要，新建算法或者选择已有算法，之后将上传模型镜像到该算法下。 </p>
  </div>
</template>
<script lang="ts">
  import { defineComponent } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { step1Schemas } from './data';
  import { Divider } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  export default defineComponent({
    components: {
      BasicForm,
      Divider,
    },
    emits: ['next'],
    setup(_, { emit }) {
      const { createMessage } = useMessage();

      const [register, { validate }] = useForm({
        labelWidth: 100,
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

      async function customSubmitFunc() {
        try {
          const values = await validate([
            'isAddModel',
            'modelName',
            'modelEnglishName',
            'monitorType',
            'datasetType',
            'description',
          ]);
          if (values.isAddModel === 1) {
            delete values['isAddModel'];
            await maHttp
              .post(
                {
                  url: 'model/addModel',
                  params: values,
                  headers: {
                    // @ts-ignore
                    ignoreCancelToken: true,
                  },
                },
                { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
              )
              .then(
                () => {
                  emit('next', {
                    modelName: values.modelName,
                    modelEnglishName: values.modelEnglishName,
                  });
                },
                (err) => {
                  createMessage.error('新建算法失败');
                  console.log(err);
                },
              );
          } else {
            emit('next', {
              modelName: values.modelName,
              modelEnglishName: values.modelEnglishName,
            });
          }
        } catch (error) {
          console.log(error);
        }
      }

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
