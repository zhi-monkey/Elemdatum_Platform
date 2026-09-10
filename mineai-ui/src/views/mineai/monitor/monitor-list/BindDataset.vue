<template>
  <PageWrapper :title="`绑定数据集`" @back="goBack" style="margin: 0 16px 0 16px">
    <div class="py-4 bg-white flex flex-col justify-center items-center">
      <a-transfer
        :data-source="data"
        :target-keys="targetKeys"
        :row-key="(record) => record.id"
        :show-select-all="true"
        :titles="['未绑定数据集', '已绑定数据集']"
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
    <RecordModal @register="registerRecordModal" />
  </PageWrapper>
</template>

<script lang="ts">
  import { defineComponent, ref } from 'vue';
  import { difference } from 'lodash-es';
  import { PageWrapper } from '/@/components/Page';
  import { useGo } from '/@/hooks/web/usePage';
  import { Table, Transfer } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import RecordModal from '/@/views/mineai/monitor/monitor-list/RecordModal.vue';
  import { useModal } from '/@/components/Modal';
  import { useTabs } from '/@/hooks/web/useTabs';
  import { useMessage } from '/@/hooks/web/useMessage';

  type tableColumn = Record<string, any>;
  const TableColumns = [
    {
      dataIndex: 'id',
      title: 'id',
      width: 100,
    },
    {
      dataIndex: 'name',
      title: '数据集名称',
      width: 200,
    },
    {
      dataIndex: 'remark',
      title: '描述',
      width: 200,
    },
  ];

  export default defineComponent({
    name: 'BindDataset',
    components: {
      PageWrapper,
      ATransfer: Transfer,
      ATable: Table,
      RecordModal,
    },

    setup() {
      const monitorId = location.href.substring(
        location.href.lastIndexOf('/') + 1,
        location.href.length,
      );
      const data = ref<any[]>([]);
      const { closeCurrent } = useTabs();
      const loadingFirst = ref<boolean>(true);
      const loadingSecond = ref<boolean>(true);
      const go = useGo();
      const targetKeys = ref<string[]>([]);
      const leftColumns = ref<tableColumn[]>(TableColumns);
      const rightColumns = ref<tableColumn[]>(TableColumns);
      const [registerRecordModal, { openModal: openRecordModal }] = useModal();
      const { createMessage } = useMessage();

      maHttp
        .get(
          {
            url: 'datasets/getAllDatasets',
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        )
        .then((resp) => {
          const datasetList = resp;
          for (const item of datasetList) {
            item.id = item.id.toString();
          }
          data.value = datasetList;
          loadingFirst.value = false;
        });

      maHttp
        .get(
          {
            url: 'monitorDataset/findDatasetIdByMonitorId',
            params: { monitorId: monitorId },
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
        )
        .then((resp) => {
          if (resp) {
            targetKeys.value.push(resp.toString());
          }
          loadingSecond.value = false;
        });

      function changeDataset(url, datasetList, _nextTargetKeys) {
        console.log(datasetList, _nextTargetKeys);
        if (datasetList.length > 1) {
          createMessage.error('最多绑定一个数据集');
          loadingFirst.value = false;
          return;
        }
        maHttp
          .post(
            {
              url: url,
              params: {
                monitorId: monitorId,
                datasetId: Number(datasetList[0]),
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
          )
          .then(() => {
            targetKeys.value = _nextTargetKeys;
            loadingFirst.value = false;
            if (url === 'monitorDataset/bindDataset') {
              openRecordModal(true, { monitorId });
            }
          })
          .catch(() => {
            console.log('error');
            loadingFirst.value = false;
          });
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
          targetKeys.value.length < nextTargetKeys.length
            ? 'monitorDataset/bindDataset'
            : 'monitorDataset/unBindDataset';
        changeDataset(url, diff(targetKeys.value, nextTargetKeys), nextTargetKeys);
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
        const flagSecond = item.name.indexOf(inputValue) > -1;
        const flagThird = item.description.indexOf(inputValue) > -1;
        return flagFirst || flagSecond || flagThird;
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
        registerRecordModal,
        openRecordModal,
      };
    },
  });
</script>
<style></style>
