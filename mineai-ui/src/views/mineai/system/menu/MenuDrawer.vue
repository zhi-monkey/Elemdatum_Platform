<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    showFooter
    :title="getTitle"
    width="50%"
    @ok="handleSubmit"
  >
    <BasicForm @register="registerForm" />
  </BasicDrawer>
</template>
<script lang="ts" setup>
  import { ref, computed, unref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { formSchema } from './menuData';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { add, edit, getMenusTree } from '/@/views/mineai/system/api/menu';

  const emits = defineEmits(['success', 'register']);
  const isUpdate = ref(true);

  const [registerForm, { resetFields, setFieldsValue, updateSchema, validate }] = useForm({
    labelWidth: 100,
    schemas: formSchema,
    showActionButtonGroup: false,
    baseColProps: { lg: 12, md: 24 },
  });

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    resetFields();
    setDrawerProps({ confirmLoading: false });
    isUpdate.value = !!data?.isUpdate;

    if (unref(isUpdate)) {
      setFieldsValue({
        ...data.record,
      });
    }
    /*    const treeData = await getMenusTree();
    updateSchema({
      field: 'pid',
      componentProps: { treeData },
    });
  });*/
    const treeData = await getMenusTree();
    const options = treeData.map((node) => ({
      label: node.label, // 显示的名称
      value: node.id, // 对应的值
    }));

    //console.log(options);
    updateSchema({
      field: 'pid',
      componentProps: { options },
    });
  });

  const getTitle = computed(() => (!unref(isUpdate) ? '新增菜单' : '编辑菜单'));

  async function handleSubmit() {
    try {
      const values = await validate();
      setDrawerProps({ confirmLoading: true });
      !unref(isUpdate) ? await add(values) : await edit(values);
      closeDrawer();
      emits('success');
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>
