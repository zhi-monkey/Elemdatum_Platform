<template>
  <BasicDrawer
    v-bind="$attrs"
    @register="registerDrawer"
    showFooter
    :title="getTitle"
    width="20%"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <Spin :spinning="spinning">
      <el-tree
        ref="drawerTreeRef"
        :data="drawerList"
        show-checkbox
        node-key="id"
        style="margin-top: 3px"
      />
    </Spin>
  </BasicDrawer>
</template>
<script lang="ts" setup>
  import { ref, computed, unref, nextTick } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { getMenusTree } from '/@/views/mineai/system/api/menu';
  import { getPermissionTree } from '/@/views/mineai/system/api/permission';
  import { editMenu, editOperations } from '/@/views/mineai/system/api/role';
  import { checkLeafNode } from '/@/utils/dubhe';
  import { Spin } from 'ant-design-vue';

  const emits = defineEmits(['success', 'register']);
  const drawerTreeRef = ref();
  const drawerList = ref([]);
  const roleId = ref(0);
  const drawerLeafNodeIdList = computed(() => {
    return checkLeafNode(drawerList.value, []).map((p) => p.id);
  });
  const spinning = ref<boolean>(false);
  const hiddenMenuLabels = new Set(['算法应用商城', '算法应用详情']);

  const filterMenuTree = (nodes = []) => {
    return nodes
      .filter((node: any) => !hiddenMenuLabels.has(node.label))
      .map((node: any) => ({
        ...node,
        children: Array.isArray(node.children) ? filterMenuTree(node.children) : node.children,
      }));
  };

  const isMenu = ref(true);
  const handleCancel = () => {
    drawerTreeRef.value!.setCheckedKeys([], false);
    drawerList.value = [];
  };

  const initForm = (originForm, isMenu) => {
    nextTick(async () => {
      if (isMenu) {
        drawerList.value = filterMenuTree(await getMenusTree());
        drawerTreeRef.value.setCheckedKeys(
          originForm.menus.map((p) => p.id).filter((p) => drawerLeafNodeIdList.value.includes(p)),
        );
        console.log('originForm' + originForm);
      } else {
        drawerList.value = await getPermissionTree();
        drawerList.value = drawerList.value.filter((permission) => {
          permission.children = permission.children.filter((childPermission) => {
            if (
              originForm.menus
                .filter((p) => p)
                .map((p) => p.id)
                .includes(childPermission.menu)
            ) {
              return true;
            }
            return false;
          });
          if (permission.children.length > 0) {
            return true;
          }
          return false;
        });
        drawerTreeRef.value.setCheckedKeys(
          originForm.permissions
            .map((p) => p.id)
            .filter((p) => drawerLeafNodeIdList.value.includes(p)),
        );
      }
      setDrawerProps({ confirmLoading: false });
      spinning.value = false;
    });
  };

  const [registerDrawer, { setDrawerProps, closeDrawer }] = useDrawerInner(async (data) => {
    setDrawerProps({ confirmLoading: true });
    spinning.value = true;
    isMenu.value = !!data?.isMenu;
    roleId.value = data.record.id;
    initForm(data.record, isMenu.value);
  });

  const getTitle = computed(() => (!unref(isMenu) ? '编辑权限' : '编辑菜单'));

  async function handleSubmit() {
    try {
      const values = drawerTreeRef.value
        .getCheckedKeys()
        .concat(drawerTreeRef.value.getHalfCheckedKeys());
      // console.log(values);
      setDrawerProps({ confirmLoading: true });
      if (unref(isMenu)) {
        await editMenu({
          id: roleId.value,
          menus: values.map((e) => {
            return { id: e };
          }),
        });
      } else {
        await editOperations({
          id: roleId.value,
          permissions: values.map((e) => {
            return { id: e };
          }),
        });
      }
      closeDrawer();
      emits('success');
    } finally {
      setDrawerProps({ confirmLoading: false });
    }
  }
</script>
