<template>
  <PageWrapper :title="`绑定算法`" @back="goBack" style="margin: 0 16px 0 16px">
    <div class="py-4 bg-white flex flex-col justify-center items-center">
      <a-transfer
        :data-source="data"
        :target-keys="targetKeys"
        :row-key="(record) => record.id"
        :show-select-all="true"
        :titles="['未绑定算法', '已绑定算法']"
        :operations="['进行绑定', '解除绑定']"
        :filter-option="filterOption"
        show-search
        @change="onChange"
      >
        <template
          #children="{ direction, filteredItems, selectedKeys, onItemSelectAll, onItemSelect }"
        >
          <a-table
            :pagination="{ pageSize: 15 }"
            :loading="loading_1 || loading_2"
            :row-selection="
              getRowSelection({
                selectedKeys,
                onItemSelectAll,
                onItemSelect,
              })
            "
            :columns="direction === 'left' ? leftColumns : rightColumns"
            :data-source="filteredItems"
            size="small"
            :custom-row="
              ({ key }) => ({
                onClick: () => {
                  onItemSelect(key, !selectedKeys.includes(key));
                },
              })
            "
          />
        </template>
      </a-transfer>
    </div>
    <!--    <ConfigModal-->
    <!--      @register="registerModal"-->
    <!--      @success="handleSuccess"-->
    <!--      :change-model="changeModel"-->
    <!--      :diff="diff"-->
    <!--      :on-Change="onChange"-->
    <!--      :change-loading="changeLoading"-->
    <!--    />-->
    <BindModelConfigModal @register="registerConfigModal" @success="handleSuccess" />
  </PageWrapper>
</template>

<script lang="ts">
  import { defineComponent, ref } from 'vue';
  import { difference } from 'lodash-es';
  import { PageWrapper } from '/@/components/Page';
  import { useGo } from '/@/hooks/web/usePage';
  import { Table, Transfer } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import BindModelConfigModal from './BindModelConfigModal.vue';
  import { useModal } from '/@/components/Modal';
  import { useTabs } from '/@/hooks/web/useTabs';

  const [registerModal, { openModal }] = useModal();
  const [registerConfigModal, { openModal: openConfigModal }] = useModal();
  type tableColumn = Record<string, any>;
  const TableColumns = [
    {
      dataIndex: 'id',
      title: 'id',
      width: 100,
      ifShow: false,
    },
    {
      dataIndex: 'model',
      title: '算法',
      width: 300,
    },
    // {
    //   dataIndex: 'mineService',
    //   title: '服务',
    //   width: 200,
    // },
    {
      dataIndex: 'controller',
      title: '边缘控制器',
      width: 300,
    },
  ];

  export default defineComponent({
    name: 'BindModel',
    components: {
      BindModelConfigModal,
      PageWrapper,
      ATransfer: Transfer,
      ATable: Table,
    },

    setup() {
      const monitorInfo = location.href.substring(
        location.href.lastIndexOf('/') + 1,
        location.href.length,
      );
      const monitorId = monitorInfo.substring(0, monitorInfo.lastIndexOf('_'));
      const monitorType = monitorInfo.substring(
        monitorInfo.lastIndexOf('_') + 1,
        monitorInfo.length,
      );
      const data = ref<any[]>([]);
      const loadingFirst = ref<boolean>(true);
      const loadingSecond = ref<boolean>(true);
      const go = useGo();
      const targetKeys = ref<string[]>([]);
      const leftColumns = ref<tableColumn[]>(TableColumns);
      const rightColumns = ref<tableColumn[]>(TableColumns);
      const { closeCurrent } = useTabs();
      let modelIds = ref();

      function changeLoading() {
        loadingFirst.value = true;
      }
      // maHttp
      //   .get(
      //     {
      //       url: 'model/getModel',
      //       headers: {
      //         // @ts-ignore
      //         ignoreCancelToken: true,
      //       },
      //     },
      //     { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      //   )
      //   .then((resp) => {
      //     for (const item of resp) {
      //       item.id = item.id.toString();
      //     }
      //     data.value = resp;
      //     loadingFirst.value = false;
      //   });

      maHttp
        .get(
          {
            url: 'model/findModelIdByMonitorId',
            params: { monitorId: monitorId },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then((resp) => {
          for (const item of resp) {
            targetKeys.value.push(item.toString());
          }
          loadingSecond.value = false;
        });

      maHttp
        .get(
          {
            url: 'controller/findAllModelHasMineServiceAndHasController',
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
            params: { monitorType: monitorType },
          },
          { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
        )
        .then((resp) => {
          for (const item of resp) {
            item.id = item.id.toString();
          }
          data.value = resp;
          loadingFirst.value = false;
        });

      function changeModel(url, model, nextTargetKeys, open) {
        for (let item in model) {
          model[item] = Number(model[item]);
        }
        maHttp
          .post(
            {
              url: url,
              params: {
                monitorId: monitorId,
                modelIds: model,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then(() => {
            targetKeys.value = nextTargetKeys;
            modelIds.value = model;

            openConfigModal();
            // if (open === true) {
            //   openModal(true, { modelIds, monitorId, nextTargetKeys, url });
            // }
            loadingFirst.value = false;
          })
          .catch(() => location.reload());
      }

      function diff(arr1, arr2) {
        let arr3: any[] = [];
        arr3 = difference(arr1, arr2);
        let arr4: any[] = [];
        arr4 = difference(arr2, arr1);
        return arr3.concat(arr4);
      }

      const goBack = () => {
        closeCurrent();
        go('/maMonitor/monitorList');
      };

      const onChange = (nextTargetKeys: string[]) => {
        loadingFirst.value = true;
        const url =
          targetKeys.value.length < nextTargetKeys.length ? 'model/bindModel' : 'model/unBindModel';
        changeModel(url, diff(targetKeys.value, nextTargetKeys), nextTargetKeys, true);
      };

      const getRowSelection = ({
        selectedKeys,
        onItemSelectAll,
        onItemSelect,
      }: Record<string, any>) => {
        return {
          onSelectAll(selected: boolean, selectedRows: Record<string, any | boolean>[]) {
            const treeSelectedKeys = selectedRows.map(({ key }) => key);
            const diffKeys = selected
              ? difference(treeSelectedKeys, selectedKeys)
              : difference(selectedKeys, treeSelectedKeys);
            onItemSelectAll(diffKeys, selected);
          },
          onSelect({ key }: Record<string, any>, selected: boolean) {
            onItemSelect(key, selected);
          },
          selectedRowKeys: selectedKeys,
        };
      };

      const filterOption = (inputValue, item) => {
        const flagFirst = item.id.indexOf(inputValue) > -1;
        const flagSecond = item.modelName.indexOf(inputValue) > -1;
        return flagFirst || flagSecond;
      };

      return {
        loading_1: loadingFirst,
        loading_2: loadingSecond,
        data,
        targetKeys,
        leftColumns,
        rightColumns,
        filterOption,
        changeModel,
        goBack,
        onChange,
        getRowSelection,
        registerModal,
        openModal,
        modelIds,
        diff,
        changeLoading,
        registerConfigModal,
      };
    },
  });
</script>
<style></style>
