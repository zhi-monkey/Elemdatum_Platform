<template>
  <PageWrapper :title="`绑定算法`" @back="goBack" style="margin: 0 16px 0 16px">
    <div class="py-4 bg-white flex flex-col justify-center items-center">
      <a-transfer
        :data-source="data"
        :target-keys="targetKeys"
        :row-key="(record) => record.id"
        :show-select-all="true"
        :titles="['未绑定算法', '已绑定算法']"
        :operations="['绑定', '解除绑定']"
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
    <ConfigModal @register="registerConfigModal" @success="handleSuccess" />
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
  import ConfigModal from './ConfigModal.vue';
  import { useModal } from '/@/components/Modal';

  const [registerConfigModal, { openModal: openConfigModal }] = useModal();

  type tableColumn = Record<string, any>;
  const TableColumns = [
    {
      dataIndex: 'id',
      title: 'id',
      width: 125,
    },
    {
      dataIndex: 'modelName',
      title: '算法名称',
      width: 188,
    },
  ];

  export default defineComponent({
    name: 'BindServiceToModel',
    components: {
      PageWrapper,
      ATransfer: Transfer,
      ATable: Table,
      ConfigModal,
    },

    setup() {
      const mineServiceId = location.href.substring(
        location.href.lastIndexOf('/') + 1,
        location.href.length,
      );
      const data = ref<any[]>([]);
      const loadingFirst = ref<boolean>(true);
      const loadingSecond = ref<boolean>(true);
      const go = useGo();
      const targetKeys = ref<string[]>([]);
      const leftColumns = ref<tableColumn[]>(TableColumns);
      const rightColumns = ref<tableColumn[]>(TableColumns);

      const initData = function () {
        maHttp
          .get(
            {
              url: 'mineServiceModel/findAllModel',
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
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

        maHttp
          .get(
            {
              url: 'mineServiceModel/findModelIdByMineServiceId',
              params: { mineServiceId: mineServiceId },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
          )
          .then((resp) => {
            for (const item of resp) {
              targetKeys.value.push(item.toString());
            }
            loadingSecond.value = false;
          });
      };

      initData();

      function changeModel(url, model, nextTargetKeys) {
        for (let item in model) {
          model[item] = Number(model[item]);
        }
        maHttp
          .post(
            {
              url: url,
              params: {
                mineServiceId: mineServiceId,
                modelIds: model,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
          )
          .then(() => {
            targetKeys.value = nextTargetKeys;
            loadingFirst.value = false;
            openConfigModal();
          })
          .catch(() => initData());
      }

      function diff(arr1, arr2) {
        let arr3: any[] = [];
        arr3 = difference(arr1, arr2);
        let arr4: any[] = [];
        arr4 = difference(arr2, arr1);
        return arr3.concat(arr4);
      }

      const goBack = () => {
        go('/maTrainingCenter/mineServiceList');
      };

      const onChange = (nextTargetKeys: string[]) => {
        loadingFirst.value = true;
        const url =
          targetKeys.value.length < nextTargetKeys.length
            ? 'mineServiceModel/bindModel'
            : 'mineServiceModel/unBindModel';
        changeModel(url, diff(targetKeys.value, nextTargetKeys), nextTargetKeys);
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
        goBack,
        onChange,
        getRowSelection,
        registerConfigModal,
      };
    },
  });
</script>
<style></style>
