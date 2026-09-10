<template>
  <BasicModal v-bind="$attrs" @register="registerModal" :title="getTitle" @ok="handleSubmit">
    <BasicForm @register="registerForm" class="myForm" />
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent, ref, computed, unref, onMounted } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from '../algorithm.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useUserStore } from '/@/store/modules/user';

  export default defineComponent({
    name: 'UserModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const isUpdate = ref(true);
      const [registerForm, { resetFields, setFieldsValue, validate, updateSchema }] = useForm({
        labelWidth: 100,
        schemas: formSchema,
        showActionButtonGroup: false,
      });
      let modelId = 0;
      let createTime = '';
      let modelStatus = 0;
      let isBoundWithGeneration = 0;
      let releaseStatus = 0;
      let applicantId = 0;
      let applyTime = '';
      let source = 0;
      let creatorId = 0;
      //获取用户信息
      const userStore = useUserStore();
      const userData = userStore.getUserInfo;
      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        await resetFields();
        setModalProps({ confirmLoading: false });
        isUpdate.value = !!data?.isUpdate;
        await updateSchema({
          field: 'isAddModel',
          label: '新建算法',
          ifShow: !isUpdate.value,
        });
        if (unref(isUpdate)) {
          let imageList = [1];
          if (data.record.deployModelVersion != null) {
            imageList.push(2);
          }
          if (data.record.convertModelVersion != null) {
            imageList.push(3);
          }
          await setFieldsValue({
            ...data.record,
            trainImage: data.record.trainModelVersion.id,
            imageCheck: imageList,
          });
          modelId = data.record.id;
          createTime = data.record.createTime;
          modelStatus = data.record.modelStatus;
          isBoundWithGeneration = data.record.isBoundWithGeneration;
          releaseStatus = data.record.releaseStatus;
          creatorId = data.record.creator.id;
          applicantId = data.record.applicant.id;
          applyTime = data.record.applyTime;
          source = data.record.source;
        }
      });

      const getTitle = computed(() => (!unref(isUpdate) ? '新增算法' : '编辑算法'));

      async function handleSubmit() {
        try {
          //处理表单数据
          const values = await validate();
          let modelValues = {};
          Object.keys(values).forEach((key) => {
            modelValues[key] = values[key];
          });
          //处理表单所选镜像

          if (values.trainImage != null) {
            modelValues['trainModelVersion'] = { id: values.trainImage };
          }
          if (values.deployImage != null) {
            modelValues['deployModelVersion'] = { id: values.deployImage };
          }
          if (values.convertImage != null) {
            modelValues['convertModelVersion'] = { id: values.convertImage };
          }

          setModalProps({ confirmLoading: true });
          const getUrl = computed(() =>
            !unref(isUpdate) ? 'modelExplore/addModel' : 'modelExplore/updateModel',
          );
          if (isUpdate.value) {
            modelValues['id'] = modelId;
            modelValues['createTime'] = createTime;
            modelValues['modelStatus'] = modelStatus;
            modelValues['isBoundWithGeneration'] = isBoundWithGeneration;
            modelValues['releaseStatus'] = releaseStatus;
            modelValues['creatorId'] = creatorId;
            modelValues['applicantId'] = applicantId;
            modelValues['applyTime'] = applyTime;
            modelValues['source'] = source;
          } else {
            modelValues['creatorId'] = userData.id;
            modelValues['source'] = 1;
          }
          await maHttp
            .post(
              {
                url: getUrl.value,
                params: modelValues,
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then(() => {
              closeModal();
              emit('success');
            });
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      onMounted(async () => {});

      return { registerModal, registerForm, getTitle, handleSubmit };
    },
  });
</script>

<style scoped>
  .myForm >>> .ant-radio-button-wrapper-disabled {
    background-color: #292421;
    color: grey;
  }
</style>
