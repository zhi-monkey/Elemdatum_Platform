<template>
  <div>
    <BasicTable @register="registerTable" :loading="loading">
      <template
        #toolbar
        v-if="
          hasPermission([PermissionEnum.MONITOR_CREATE]) ||
          hasPermission([PermissionEnum.MONITOR_CONFIG_PUBLISH])
        "
      >
        <a-button
          v-if="hasPermission([PermissionEnum.MONITOR_CREATE])"
          type="primary"
          @click="handleCreate"
        >
          新增设备
        </a-button>
        <a-button
          v-if="hasPermission([PermissionEnum.MONITOR_CONFIG_PUBLISH])"
          type="primary"
          @click="handleConfig"
          :loading="buttonLoading"
          >配置下发
        </a-button>
      </template>
      <template #action="{ record }" v-if="hasPermission([PermissionEnum.DATA])">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:gold-outlined',
              tooltip: '绑定算法',
              disabled: record.subsystem !== 'CENTRAL_PLATFORM',
              onClick: bindModel.bind(null, record),
            },
            {
              icon: 'ant-design:paper-clip-outlined',
              tooltip: '绑定数据集',
              onClick: bindDataset.bind(null, record),
              auth: PermissionEnum.MONITOR_BIND_DATASET,
            },

            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              disabled: record.subsystem !== 'CENTRAL_PLATFORM',
              onClick: handleEdit.bind(null, record),
            },
            {
              icon: 'ant-design:edit-outlined',
              tooltip: '自定义原始流视频名称',
              disabled: record.subsystem !== 'CENTRAL_PLATFORM',
              onClick: editMonitorVideoName.bind(null, record),
            },
            {
              icon: 'ant-design:delete-outlined',
              color: 'error',
              tooltip: '删除',
              disabled: record.canDelete !== true || record.subsystem !== 'CENTRAL_PLATFORM',
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
      <template #expandedRowRender="{ record }">
        <SubTable
          :id="record.id"
          :columns="modelColumns"
          :url="modelUrl"
          :url-prefix="MaBackendUrlEnum.MODEL_MANAGER"
          :pagination="{
            current: record.page,
            pageSize: 7,
            total: record.total,
            showSizeChanger: false, //隐藏每页显示记录数切换器
            showQuickJumper: false, //隐藏快速跳转输入框

            // 当分页发生变化时执行的回调函数。这里使用箭头函数定义了一个匿名函数，将新的页码赋值给record对象的page属性，并调用fetchChildData方法重新获取子表格数据。
            onChange: (page) => {
              record.page = page;
              fetchChildData(record);
            },
          }"
        />
        <DatasetSubTable
          @success="handleSuccess"
          v-if="hasPermission([PermissionEnum.MONITOR_VIEW_DATASET])"
          :id="record.id"
          :is-Record="record.isRecord"
          :max-second="record.maxSecond"
          :columns="datasetColumns"
          :url="datasetUrl"
          :url-prefix="MaBackendUrlEnum.MONITOR_ACCESSOR"
          :pagination="{
            current: record.page,
            pageSize: 7,
            total: record.total,
            showSizeChanger: false, //隐藏每页显示记录数切换器
            showQuickJumper: false, //隐藏快速跳转输入框

            // 当分页发生变化时执行的回调函数。这里使用箭头函数定义了一个匿名函数，将新的页码赋值给record对象的page属性，并调用fetchChildData方法重新获取子表格数据。
            onChange: (page) => {
              record.page = page;
              fetchChildData(record);
            },
          }"
        />
      </template>
      >
    </BasicTable>
    <MonitorModal @register="registerModal" @success="handleSuccess" />
    <MonitorVideoName @register="registerEditMonitorVideoModal" @success="handleSuccess" />
    <ConfigModal @register="registerConfigModal" @success="handleSuccess" />
  </div>
</template>
<script lang="ts" setup>
  import { computed, ComputedRef, onActivated, ref, watch } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { columns, searchFormSchema } from './monitor.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import MonitorModal from './MonitorModal.vue';
  import MonitorVideoName from './MonitorVideoName.vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { PermissionEnum } from '/@/enums/PermissionEnum';
  import SubTable from './Subtable.vue';
  import DatasetSubTable from './DatasetSubtable.vue';
  import ConfigModal from '/@/views/mineai/controller/scene-list/ConfigModal.vue';
  import { useTableCellStore } from '/@/store/modules/tableCell';

  const modelColumns = [
    { title: '算法ID', dataIndex: 'id', width: 20, align: 'center' },
    { title: '算法名称', dataIndex: 'modelName', width: 40, align: 'center' },
    { title: '描述', dataIndex: 'description', width: 40, align: 'center' },
    {
      title: '自定义视频名称',
      dataIndex: 'action',
      width: 20,
      align: 'center',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  ];

  const tableCellStore = useTableCellStore();

  let buttonLoading = ref(false);
  let loading = ref(true);
  const modelUrl = 'model/findModelsByMonitorId?monitorId=';
  let originalStreamName = ref();
  const { hasPermission } = usePermission();
  const userPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([PermissionEnum.MONITOR_VIEW_ALL]);
  });
  const datasetColumns = [
    { title: '数据集ID', dataIndex: 'id', width: 20, align: 'center' },
    { title: '数据集名称', dataIndex: 'name', width: 40, align: 'center' },
    { title: '描述', dataIndex: 'remark', width: 40, align: 'center' },
    {
      title: '改变录制状态',
      dataIndex: 'action',
      width: 20,
      align: 'center',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  ];

  const datasetUrl = 'monitorDataset/findDatasetIdByMonitorId?monitorId=';

  const go = useGo();
  const { createMessage } = useMessage();
  const [registerModal, { openModal: openModal }] = useModal();
  const [registerConfigModal, { openModal: openConfigModal }] = useModal();
  const [registerEditMonitorVideoModal, { openModal: openEditMonitorVideoModal }] = useModal();
  const [registerTable, { reload, getForm }] = useTable({
    title: '监控设备列表',
    api: (params) => {
      return maHttp
        .get(
          {
            url: 'monitor/dynamicFindMonitorPage',
            params,
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
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
    afterFetch: (itemList) => {
      //GET请求被解析为 VBen表格Table的行后，做一些操作
      //console.log('afterFetch', itemList);
      return itemList;
    },
    columns: columns,
    formConfig: {
      labelWidth: 160,
      showAdvancedButton: false,
      schemas: searchFormSchema,
    },
    useSearchForm: true,
    showTableSetting: true,
    bordered: true,
    showIndexColumn: false,
    expandRowByClick: true,
    handleSearchInfoFn(info) {
      if (info.name == undefined) info.name = '';
      return info;
    },
    actionColumn: {
      width: 200,
      title: '操作',
      //ifShow: userPermission.value,
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  function handleCreate() {
    openModal(true, {
      isUpdate: false,
    });
  }

  function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
    });
  }

  function editMonitorVideoName(record: Recordable) {
    loading.value = true;
    maHttp
      .get(
        {
          url: 'monitor/findMonitorById',
          params: { monitorId: record.id },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
      )
      .then((v) => {
        originalStreamName.value = v.originalStreamName;
        openEditMonitorVideoModal(true, { record, originalStreamName });
        loading.value = false;
      });
  }

  onActivated(() => {
    const { updateSchema } = getForm();
    updateSchema({
      field: 'sceneName',
      componentProps: {
        api: () =>
          maHttp.get(
            {
              url: 'scene/getScene',
              params: {},
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
          ),
        labelField: 'name',
        valueField: 'id',
        immediate: true,
      },
    });
    reload();
  });

  function bindDataset(record: Recordable) {
    go(`/maMonitor/bindDataset/${record.id}`);
  }

  function bindModel(record: Recordable) {
    go(`/maMonitor/bindModel/${record.id}_${record.monitorType}`);
  }

  function handleDelete(record: Recordable) {
    maHttp
      .post(
        {
          url: 'monitor/deleteMonitor',
          params: record,
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
      )
      .then((v) => {
        reload();
        createMessage.success('删除成功！');
        if (v === true) {
          openConfigModal(true);
        }
      });
  }

  function handleConfig() {
    buttonLoading.value = true;
    maHttp
      .get(
        {
          url: 'controller/createAllControllerJsonConfig',
          //flag=0重启
          params: {
            flag: 0,
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
      )
      .then(() => {
        buttonLoading.value = false;
        createMessage.success('配置下发成功！');
        reload();
      })
      .catch(() => {
        buttonLoading.value = false;
        reload();
      });
  }

  function handleSuccess() {
    reload();
  }

  // 监视tableCellStore
  watch(
    () => tableCellStore.title,
    () => {
      console.log(tableCellStore.getCurrentTitle());
    },
  );
</script>
<style>
  .ant-table-content > .ant-table-scroll > .ant-table-body {
    overflow: auto scroll;
  }
</style>
