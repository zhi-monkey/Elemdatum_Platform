<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="getTitle"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <BasicForm @register="registerForm">
      <template #menu>
        <el-tree
          ref="permissionTreeRef"
          :data="permissionList"
          show-checkbox
          node-key="id"
          style="margin-top: 3px"
        />
      </template>
    </BasicForm>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { ref, computed, unref, nextTick, onMounted } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from './permission.data';
  import { getPermissionTree, add, edit } from '/@/views/mineai/system/api/permission';
  import { checkLeafNode } from '/@/utils/dubhe/utils';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emits = defineEmits(['register', 'success']);
  const { createMessage } = useMessage();
  const isUpdate = ref(true);
  const permissionTreeRef = ref();
  const permissionList = ref([]);
  const permissionLeafNodeIdList = computed(() => {
    return checkLeafNode(permissionList.value, []).map((p) => p.id);
  });

  onMounted(async () => {
    permissionList.value = await getPermissionTree();
  });

  const initForm = (originForm) => {
    // Object.keys(form).forEach((key) => {
    //   form[key] = originForm[key];
    // });
    nextTick(async () => {
     // console.log("originForm.permissions",originForm.permissions)
      permissionList.value = await getPermissionTree();
      permissionTreeRef.value.setCheckedKeys(
        originForm.permissions
          .map((p) => p.id)
          .filter((p) => permissionLeafNodeIdList.value.includes(p)),
      );
    });
  };

  const handleCancel = () => {
    permissionTreeRef.value!.setCheckedKeys([], false);
  };

  const [registerForm, { resetFields, setFieldsValue, validate }] = useForm({
    labelWidth: 100,
    schemas: formSchema,
    showActionButtonGroup: false,
  });

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    await resetFields();
    isUpdate.value = !!data?.isUpdate;
    if (data?.isUpdate) {
      initForm(data.record);
      await setFieldsValue({
        id: data.record.id,
        authCode: data.record.authCode,
        description: data.record.description,
      });
    }
    setModalProps({ confirmLoading: false });
  });

  const getTitle = computed(() => (!unref(isUpdate) ? '创建权限组' : '编辑权限组'));

  async function handleSubmit() {
    const permissions = permissionTreeRef.value
      .getCheckedKeys()
      .concat(permissionTreeRef.value.getHalfCheckedKeys());

    if (permissions.length === 0) {
      createMessage.error('请至少选择一个操作权限！');
    }

    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });
      if (isUpdate.value) {
        await edit({ ...values, permissions, id: parseInt(values.id) });
      } else {
        await add({ ...values, permissions });
      }
      closeModal();
      emits('success');
    } catch (e) {
      console.error(e);
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }
</script>
