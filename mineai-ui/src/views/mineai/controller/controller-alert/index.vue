<template>
  <div>
    <div>
      <ResultBar />
    </div>
    <div>
      <div class="p-1">
        <BasicTable @register="registerTable" />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import { defineComponent, onMounted, onUnmounted } from 'vue';
  import { BasicTable, useTable } from '/@/components/Table';
  import { columns } from './alertData';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useModal } from '/@/components/Modal';
  import ResultBar from './ResultBar.vue';

  export default defineComponent({
    name: 'AlertManagement',
    components: {
      BasicTable,
      ResultBar,
    },
    setup() {
      let dataTimer;
      onMounted(() => {
        dataTimer = setInterval(reload, 60000);
      });
      onUnmounted(() => {
        clearInterval(dataTimer);
      });

      async function getData(params) {
        const v = await maHttp.get(
          {
            url: 'controllerAlert/dynamicFindControllerAlert',
            params,
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
        );
        //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
        v.items = v.content;
        v.total = v.totalElements;
        return v;
      }

      const [registerModal, { openModal }] = useModal();
      const [registerTable, { reload }] = useTable({
        title: '报警列表',
        api: async (params) => {
          return await getData(params);
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
        columns,
        formConfig: {
          labelWidth: 120,
          showAdvancedButton: false,
          disabled: true,
        },
        useSearchForm: false,
        showTableSetting: true,
        bordered: true,
        showIndexColumn: false,
        handleSearchInfoFn(info) {
          return info;
        },
      });

      function handleEdit(record: Recordable) {
        openModal(true, {
          record,
          isUpdate: true,
        });
      }

      function handleSuccess() {
        reload();
      }

      return {
        registerTable,
        registerModal,
        handleEdit,
        handleSuccess,
      };
    },
  });
</script>
