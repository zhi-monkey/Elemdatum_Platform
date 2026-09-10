<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate"> 新增场景</a-button>
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
    <SceneModal @register="registerModal" @success="handleSuccess" />
  </div>
</template>
<script lang="ts">
  import { computed, ComputedRef, defineComponent, onActivated } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';

  import { columns, searchFormSchema, simpleColumns, simpleSearchFormSchema } from './scene.data';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Button } from 'ant-design-vue';
  import { useModal } from '/@/components/Modal';
  import SceneModal from './SceneModal.vue';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { RoleEnum } from '/@/enums/roleEnum';

  export default defineComponent({
    name: 'SceneList',
    components: { BasicTable, SceneModal, TableAction, AButton: Button },
    setup() {
      const { hasPermission } = usePermission();
      const userPermission: ComputedRef<boolean> = computed(() => {
        return hasPermission([RoleEnum.SystemUser]);
      });
      const { createMessage } = useMessage();
      const [registerModal, { openModal }] = useModal();
      const [registerTable, { reload }] = useTable(
        userPermission.value
          ? {
              title: '场景列表',
              api: (params) => {
                return maHttp
                  .get(
                    {
                      url: 'scene/dynamicFindScenePage',
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
                labelWidth: 120,
                showAdvancedButton: false,
                schemas: searchFormSchema,
              },
              useSearchForm: true,
              showTableSetting: true,
              bordered: true,
              showIndexColumn: false,
              handleSearchInfoFn(info) {
                if (info.name == undefined) info.name = '';

                const infoString = JSON.stringify(info);
                localStorage.setItem(name, infoString);

                return info;
              },
              actionColumn: {
                width: 80,
                title: '操作',
                dataIndex: 'action',
                slots: { customRender: 'action' },
                fixed: 'right',
              },
            }
          : {
              title: '场景列表',
              api: (params) => {
                return maHttp
                  .get(
                    {
                      url: 'scene/dynamicFindScenePage',
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
                labelWidth: 120,
                showAdvancedButton: false,
                schemas: simpleSearchFormSchema,
              },
              useSearchForm: true,
              showTableSetting: true,
              bordered: true,
              showIndexColumn: false,
              handleSearchInfoFn(info) {
                if (info.name == undefined) info.name = '';

                const infoString = JSON.stringify(info);
                localStorage.setItem(name, infoString);

                return info;
              },
            },
      );

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

      function handleDelete(record: Recordable) {
        console.log(record);
        maHttp
          .post(
            {
              url: 'scene/deleteScene',
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

      function handleSuccess() {
        reload();
      }

      return {
        registerTable,
        registerModal,
        handleCreate,
        handleEdit,
        handleDelete,
        handleSuccess,
        RoleEnum,
        hasPermission,
      };
    },
  });
</script>
