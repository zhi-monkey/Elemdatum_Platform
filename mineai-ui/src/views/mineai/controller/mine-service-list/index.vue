<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate" v-if="hasPermission([RoleEnum.SystemUser])">
          新增服务
        </a-button>
      </template>
      <template #action="{ record }" v-if="hasPermission([RoleEnum.SystemUser])">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:gold-outlined',
              tooltip: '绑定算法',
              onClick: BindServiceToModel.bind(null, record),
            },
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              onClick: handleEdit.bind(null, record),
            },
            {
              icon: 'ant-design:delete-outlined',
              color: 'error',
              tooltip: '删除',
              disabled: record.canDelete !== true,
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
      <template #expandedRowRender="{ record }">
        <Subtable
          :id="record.id"
          :columns="innerColumns"
          :url="mineServiceLoadUrl"
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
      </template>
    </BasicTable>
    <MineServiceModal @register="registerModal" @success="handleSuccess" />
  </div>
</template>
<script lang="ts">
  import { computed, ComputedRef, defineComponent, onActivated, ref, unref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { columns, searchFormSchema, simpleColumns } from './tableData';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button } from 'ant-design-vue';
  import { useModal } from '/@/components/Modal';
  import MineServiceModal from './MineServiceModal.vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { RoleEnum } from '/@/enums/roleEnum';
  import Subtable from '/@/views/mineai/controller/mine-service-list/Subtable.vue';

  export default defineComponent({
    name: 'MineServiceList',
    components: {
      Subtable,
      BasicTable,
      MineServiceModal,
      TableAction,
      AButton: Button,
    },
    setup() {
      const innerColumns = [
        { title: '算法ID', dataIndex: 'id', width: 20 },
        { title: '算法名称', dataIndex: 'modelName', width: 40 },
        { title: '描述', dataIndex: 'description', width: 40 },
      ];
      //包含模型信息的数据
      const mineServiceLoadUrl = 'model/findModelbyMineServiceId?mineServiceId=';
      let modelData = ref([]);
      const { hasPermission } = usePermission();
      const userPermission: ComputedRef<boolean> = computed(() => {
        return hasPermission([RoleEnum.SystemUser]);
      });
      const go = useGo();
      const { createMessage } = useMessage();
      const [registerModal, { openModal }] = useModal();
      const [registerTable, { reload }] = useTable({
        title: '服务列表',
        api: (params) => {
          return maHttp
            .get(
              {
                url: 'mineService/dynamicFindMineServicePage',
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
              //获取模型数据
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
          return itemList;
        },
        columns: unref(userPermission) ? columns : simpleColumns,
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
          width: 150,
          title: '操作',
          dataIndex: 'action',
          slots: { customRender: 'action' },
          fixed: 'right',
          ifShow: userPermission.value,
        },
      });

      onActivated(() => {
        reload();
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

      function BindServiceToModel(record: Recordable) {
        go(`/maTrainingCenter/serviceBindModel/${record.id}`);
      }

      function handleDelete(record: Recordable) {
        maHttp
          .post(
            {
              url: 'mineService/deleteMineService',
              params: record,
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
          )
          .then(() => {
            reload();
            createMessage.success('删除成功！');
          });
      }

      function handleSuccess() {
        reload();
      }

      return {
        registerTable,
        registerModal,
        innerColumns,
        handleCreate,
        handleEdit,
        handleDelete,
        handleSuccess,
        modelData,
        BindServiceToModel: BindServiceToModel,
        RoleEnum,
        hasPermission,
        mineServiceLoadUrl,
        MaBackendUrlEnum,
      };
    },
  });
</script>
<style>
  .ant-table-content > .ant-table-scroll > .ant-table-body {
    overflow: auto scroll;
  }
</style>
