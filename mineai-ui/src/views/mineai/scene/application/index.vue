<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary" @click="goManageModelType"> 场景管理</a-button>
        <a-button type="primary" @click="handleCreate"> 新增应用</a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:info-circle-outlined',
              tooltip: '应用详情',
              onClick: gotoApplicationDetail.bind(null, record),
            },
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              onClick: handleEdit.bind(null, record),
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除',
              color: 'error',
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <ApplicationModal @register="createAndUpdateModal" />
  </div>
</template>
<script lang="ts" setup>
  import { computed, ComputedRef, onActivated } from 'vue';
  import { Button as AButton } from 'ant-design-vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { columns, searchFormSchema } from './model.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { useGo } from '/@/hooks/web/usePage';
  import { RoleEnum } from '/@/enums/roleEnum';
  import ApplicationModal from './applicationModal.vue';

  const go = useGo();
  const { hasPermission } = usePermission();
  const [createAndUpdateModal, { openModal: openCreateAndUpdateModal }] = useModal();
  const userPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([RoleEnum.SystemUser]);
  });
  const { createMessage } = useMessage();
  const [registerTable, { reload }] = useTable({
    title: '应用列表',
    api: (params) => {
      return Promise.resolve({
        items: [
          { id: 1, name: '安全帽检测', scene: '某软硬件平台' },
          { id: 2, name: '人脸识别', scene: '视频监控系统' },
          { id: 3, name: '车牌识别', scene: '交通管理系统' },
          { id: 4, name: '文字识别', scene: '文档处理软件' },
          { id: 5, name: '产品缺陷检测', scene: '制造业自动化' },
          { id: 6, name: '植被分析', scene: '农业监测系统' },
          { id: 7, name: '人群计数', scene: '零售店铺分析' },
          { id: 8, name: '手势识别', scene: '人机交互系统' },
          { id: 9, name: '目标跟踪', scene: '安防监控系统' },
          { id: 10, name: '图像分类', scene: '图像搜索引擎' },
          { id: 11, name: '物体检测', scene: '自动驾驶汽车' },
          { id: 12, name: '医学影像分析', scene: '医疗诊断系统' },
          { id: 13, name: '航空目标识别', scene: '军事侦察系统' },
          { id: 14, name: '工业缺陷检测', scene: '质量控制系统' },
          { id: 15, name: '建筑物识别', scene: '地理信息系统' },
          { id: 16, name: '舞蹈动作评估', scene: '体育培训软件' },
          { id: 17, name: '射线检测', scene: '安全检查设备' },
          { id: 18, name: '动作识别', scene: '虚拟现实游戏' },
          { id: 19, name: '生物识别', scene: '环境保护监测' },
          { id: 20, name: '材料分类', scene: '资源回收管理' },
        ],
        total: 20,
      });
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v.page -= 1;
      return v;
    },
    afterFetch: (itemList) => {
      //GET请求被解析为 VBen表格Table的行后，做一些操作
      //console.log('afterFetch', itemList);
      return itemList;
    },
    columns: columns,
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: searchFormSchema,
    },
    useSearchForm: true,
    showTableSetting: true,
    bordered: false,
    showIndexColumn: false,
    expandRowByClick: true,
    handleSearchInfoFn(info) {
      return info;
    },
    actionColumn: {
      width: 100,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
      //ifShow: userPermission.value,
    },
  });

  // 跳转算法类型管理页面
  function goManageModelType() {
    go('/maScene/sceneInfoManagement');
  }

  function handleCreate() {
    openCreateAndUpdateModal(true, {
      isUpdate: false,
    });
  }

  function handleEdit(record: Recordable) {
    openCreateAndUpdateModal(true, {
      record,
      isUpdate: true,
    });
  }

  const gotoApplicationDetail = () => {
    go('/maScene/applicationDetail');
  };

  function handleDelete(record: Recordable) {
    maHttp
      .post(
        {
          url: 'model/deleteModel',
          params: record,
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('删除成功！');
        reload();
      });
  }

  onActivated(() => {
    reload();
  });
</script>
