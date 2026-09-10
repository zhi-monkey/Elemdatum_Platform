<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate"> 新增算力控制器</a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              onClick: handleEdit.bind(null, record),
            },
            {
              icon: 'ant-design:paper-clip-outlined',
              tooltip: '绑定服务',
              onClick: bindMineService.bind(null, record),
            },
            // {
            //   icon: 'ant-design:cloud-download-outlined',
            //   tooltip: '下发配置',
            //   popConfirm: {
            //     title: '是否下发配置',
            //     confirm: configDistribute.bind(null, record),
            //   },
            // },
            record.canDelete === true
              ? {
                  icon: 'ant-design:delete-outlined',
                  color: 'error',
                  tooltip: '删除',
                  popConfirm: {
                    title: '是否确认删除',
                    confirm: handleDelete.bind(null, record),
                  },
                }
              : {
                  icon: 'ant-design:delete-outlined',
                  color: 'error',
                  tooltip: '删除',
                  disabled: true,
                  popConfirm: {
                    title: '是否确认删除',
                    confirm: handleDelete.bind(null, record),
                  },
                },
          ]"
        />
      </template>
      <template #RamSizeprocess="{ record }">
        <div class="flex justify-center">
          <span v-if="record.ramSize >= 524288" style="width: 55%">{{
            (record.ramSize / 1024 / 1024).toFixed(2) + 'TB'
          }}</span>
          <span v-else-if="record.ramSize >= 512" style="width: 55%">{{
            (record.ramSize / 1024).toFixed(2) + 'GB'
          }}</span>
          <span v-else style="width: 55%">{{ record.ramSize.toFixed(2) + 'MB' }}</span>
        </div>
      </template>

      <template #diskSizeprocess="{ record }">
        <div class="flex justify-center">
          <span v-if="record.diskSize >= 524288" style="width: 55%">{{
            (record.diskSize / 1024 / 1024).toFixed(2) + 'TB'
          }}</span>
          <span v-else-if="record.diskSize >= 512" style="width: 55%">{{
            (record.diskSize / 1024).toFixed(2) + 'GB'
          }}</span>
          <span v-else style="width: 55%">{{ record.diskSize.toFixed(2) + 'MB' }}</span>
        </div>
      </template>

      <template #expandedRowRender="{ record }">
        <SubTable
          :id="record.id"
          :columns="controllerLoadColumns"
          :url="controllerLoadUrl"
          :url-prefix="MaBackendUrlEnum.CONTROLLER_MANAGER"
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
        <SubTable
          :id="record.id"
          :columns="serviceColumns"
          :url="serviceUrl"
          :url-prefix="MaBackendUrlEnum.CONTROLLER_MANAGER"
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
    </BasicTable>
    <ControllerListModal @register="registerModal" @success="handleSuccess" />
    <ControllerListCreateModal @register="registerCreateModal" @success="handleSuccess" />
  </div>
</template>
<script lang="ts">
  import { computed, ComputedRef, defineComponent, h, onActivated, ref } from 'vue';
  import { Button, Progress } from 'ant-design-vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { columns, searchFormSchema, simpleColumns, simpleSearchFormSchema } from './tableData';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import ControllerListModal from './ControllerListModal.vue';
  import ControllerListCreateModal from './ControllerListCreateModal.vue';
  import { useModal } from '/@/components/Modal';
  import { useGo } from '/@/hooks/web/usePage';
  import SubTable from './Subtable.vue';
  import { RoleEnum } from '/@/enums/roleEnum';
  import { usePermission } from '/@/hooks/web/usePermission';

  export default defineComponent({
    name: 'ControllerList',
    components: {
      BasicTable,
      ControllerListModal,
      ControllerListCreateModal,
      TableAction,
      AButton: Button,
      SubTable,
    },
    setup() {
      /**
       * 包含服务信息的数据
       */
      const serviceColumns = [
        { title: '服务ID', dataIndex: 'id', width: 20 },
        { title: '服务名称', dataIndex: 'name', width: 40 },
        { title: '备注', dataIndex: 'mineServiceName', width: 40 },
      ];
      const serviceUrl = 'mineService/findMineServiceByControllerId?controllerId=';
      let serviceData = ref([]);
      const { hasPermission } = usePermission();
      const userPermission: ComputedRef<boolean> = computed(() => {
        return hasPermission([RoleEnum.SystemUser]);
      });

      /**
       * 包含控制器状态的数据
       */
      const controllerLoadColumns = [
        {
          title: 'CPU占用率',
          dataIndex: 'cpuLoad',
          width: 120,
          sorter: true,
          customRender: ({ record }) => {
            return h(Progress, {
              strokeColor: {
                from: '#108ee9',
                to: '#87d068',
              },
              style: { width: '70%' },
              percent: record.cpuLoad,
            });
          },
        },
        {
          title: 'RAM占用率',
          dataIndex: 'ramLoad',
          width: 160,
          sorter: true,
          customRender: ({ record }) => {
            let unit;
            let unitName;
            if (record.controller.ramSize >= 524288) {
              unit = 1024 * 1024;
              unitName = 'TB';
            } else if (record.controller.ramSize >= 512) {
              unit = 1024;
              unitName = 'GB';
            } else {
              unit = 1;
              unitName = 'MB';
            }
            return h(Progress, {
              strokeColor: {
                from: '#108ee9',
                to: '#87d068',
              },
              style: { width: '70%' },
              percent: (record.ramLoad / record.controller.ramSize) * 100,
              format: () =>
                (record.ramLoad / unit).toFixed(2) +
                '/' +
                parseFloat(record.controller.ramSize / unit).toFixed(2) +
                unitName,
            });
          },
        },
        {
          title: '硬盘占用率',
          dataIndex: 'diskLoad',
          width: 160,
          sorter: true,
          customRender: ({ record }) => {
            let unit;
            let unitName;
            if (record.controller.diskSize >= 524288) {
              unit = 1024 * 1024;
              unitName = 'TB';
            } else if (record.controller.diskSize >= 512) {
              unit = 1024;
              unitName = 'GB';
            } else {
              unit = 1;
              unitName = 'MB';
            }
            return h(Progress, {
              strokeColor: {
                from: '#108ee9',
                to: '#87d068',
              },
              style: { width: '70%' },
              percent: (record.diskLoad / record.controller.diskSize) * 100,
              format: () =>
                (record.diskLoad / unit).toFixed(2) +
                '/' +
                parseFloat(record.controller.diskSize / unit).toFixed(2) +
                unitName,
            });
          },
        },
        { title: '更新时间', dataIndex: 'createTime', width: 100 },
      ];
      const controllerLoadUrl = 'controllerLoad/findcontrollerLoadByControllerId?controllerId=';
      let controllerLoadData = ref([]);

      const go = useGo();
      const { createMessage } = useMessage();
      const [registerModal, { openModal: openModal }] = useModal();
      const [registerCreateModal, { openModal: openCreateModal }] = useModal();
      const [registerTable, { reload }] = useTable(
        userPermission.value
          ? {
              title: '算力控制器列表',

              api: (params) => {
                return maHttp
                  .get(
                    {
                      url: 'controller/dynamicFindControllerPage',
                      params,
                      headers: {
                        // @ts-ignore
                        ignoreCancelToken: true,
                      },
                    },
                    { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
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
                //GET请求被解析为 Vben表格Table的行后，做一些操作
                //console.log('afterFetch', itemList);
                return itemList;
              },
              columns: columns,
              formConfig: {
                labelWidth: 160,
                schemas: searchFormSchema,
                showAdvancedButton: false,
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
                width: 150,
                title: '操作',
                dataIndex: 'action',
                slots: { customRender: 'action' },
                fixed: 'right',
              },
            }
          : {
              title: '算力控制器列表',

              api: (params) => {
                return maHttp
                  .get(
                    {
                      url: 'controller/dynamicFindControllerPage',
                      params,
                      headers: {
                        // @ts-ignore
                        ignoreCancelToken: true,
                      },
                    },
                    { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
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
                //GET请求被解析为 Vben表格Table的行后，做一些操作
                //console.log('afterFetch', itemList);
                return itemList;
              },
              columns: simpleColumns,
              formConfig: {
                labelWidth: 160,
                schemas: simpleSearchFormSchema,
                showAdvancedButton: false,
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
            },
      );

      onActivated(() => {
        reload();
      });

      function handleCreate() {
        openCreateModal(true);
      }

      function handleEdit(record: Recordable) {
        openModal(true, {
          record,
          isUpdate: true,
        });
      }

      function handleDelete(record: Recordable) {
        maHttp
          .post(
            {
              url: 'controller/deleteController',
              params: record,
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
          )
          .then(() => {
            createMessage.success('删除成功！');
            reload();
          });
      }

      function bindMineService(record: Recordable) {
        go(`/maController/bindService/${record.id}`);
      }

      function configDistribute(record: Recordable) {
        maHttp
          .post(
            {
              url: 'controller/createJsonConfig',
              params: record,
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
          )
          .then(() => {
            createMessage.success('配置下发成功！');
            reload();
          })
          .catch(() => {
            createMessage.error('配置下发失败！');
            reload();
          });
      }

      function handleSuccess() {
        reload();
      }

      return {
        registerTable,
        serviceColumns,
        controllerLoadColumns,
        handleCreate,
        handleEdit,
        handleDelete,
        handleSuccess,
        registerModal,
        registerCreateModal,
        openModal,
        openCreateModal,
        bindMineService,
        serviceData,
        controllerLoadData,
        configDistribute,
        serviceUrl,
        MaBackendUrlEnum,
        controllerLoadUrl,
        hasPermission,
        RoleEnum,
      };
    },
  });
</script>
<style>
  .ant-table-content > .ant-table-scroll > .ant-table-body {
    overflow: auto scroll;
  }
</style>
