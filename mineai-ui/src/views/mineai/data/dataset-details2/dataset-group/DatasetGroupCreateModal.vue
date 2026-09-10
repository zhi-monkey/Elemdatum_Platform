<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="getTitle"
    :width="600"
    @ok="handleSubmit"
    @cancel="handleCancel"
    :confirmLoading="confirmLoading"
    :destroyOnClose="true"
  >
    <div class="pt-3px pr-3px">
      <BasicForm @register="registerForm" />
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicModal, useModalInner } from '/src/components/Modal';
  import { BasicForm, useForm } from '/src/components/Form';
  import { datasetGroupFormSchema } from './dataset-group.data';
  import { maHttp } from '/src/utils/http/axios';
  import { DubheBackendUrlEnum } from '/src/enums/mineaiEnum';
  import { useMessage } from '/src/hooks/web/useMessage';

  const emit = defineEmits(['success', 'register']);
  const { createMessage } = useMessage();

  const isUpdate = ref(false);
  const rowId = ref<number | null>(null);
  const confirmLoading = ref(false);
  const currentName = ref('');

  const [registerForm, { setFieldsValue, resetFields, validate }] = useForm({
    labelWidth: 100,
    baseColProps: { span: 24 },
    schemas: datasetGroupFormSchema,
    showActionButtonGroup: false,
    actionColOptions: {
      span: 23,
    },
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    setModalProps({ confirmLoading: false });

    isUpdate.value = !!data?.isUpdate;

    if (unref(isUpdate)) {
      rowId.value = data.record.id;
      currentName.value = data.record.name;
      await setFieldsValue({
        ...data.record,
      });
    } else {
      rowId.value = null;
    }
  });

  const getTitle = computed(() => (!unref(isUpdate) ? '新增数据集组' : '编辑数据集组'));

  async function handleSubmit() {
    try {
      confirmLoading.value = true;
      const values = await validate();

      if (unref(isUpdate)) {
        values.id = rowId.value;
        // 编辑数据集组
        await maHttp.put(
          {
            url: `datasets/group/editGroup`,
            data: values,
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
      } else {
        // 新增数据集组
        await maHttp.post(
          {
            url: 'datasets/group/createGroup',
            data: values,
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        );
      }

      closeModal();
      emit('success', { isUpdate: unref(isUpdate) });
    } catch (error) {
      console.error('数据集组操作失败:', error);
      if (error.message === '组名已存在') {
        return;
      }
      createMessage.error(unref(isUpdate) ? '数据集组编辑失败！' : '数据集组创建失败！');
    } finally {
      confirmLoading.value = false;
    }
  }

  function handleCancel() {
    resetFields();
    closeModal();
  }
</script>

<script lang="ts">
  export default {
    name: 'DatasetGroupModal',
  };
</script>
