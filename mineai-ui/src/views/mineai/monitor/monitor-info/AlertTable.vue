<template>
  <div>
    <div class="pt-1 table">
      <BasicTable @register="registerTable">
        <template #image="{ record }">
          <div class="flex justify-center" v-if="record?.image">
            <Icon
              icon="ant-design:file-image-twotone"
              color="SteelBlue"
              @click="
                openAlertMsgModal(true, {
                  mediaType: 'image',
                  mediaPath: record.imagePath,
                  fileName:
                    record.scene +
                    '_' +
                    record.monitorName +
                    '_' +
                    record.modelName +
                    '_' +
                    record.createTime.replace(/-/g, '').replace(/\s+/g, '').replace(/:/g, '') +
                    '.jpg',
                  alertId: record.id,
                  description:
                    record.description.length > 5
                      ? record.description.substr(0, 6) + '...'
                      : record.description,
                  scene: record.scene,
                  monitor: record.monitorName,
                  alg: record.modelName,
                  status: record.status,
                  createTime: record.createTime,
                  alertMsg: record.description,
                })
              "
            />
          </div>
          <div v-else>-</div>
        </template>
        <template #video="{ record }">
          <div class="flex justify-center" v-if="record?.video">
            <Icon
              color="SteelBlue"
              icon="ant-design:video-camera-twotone"
              @click="
                openAlertMsgModal(true, {
                  mediaType: 'video',
                  mediaPath: record.videoPath,
                  fileName:
                    record.scene +
                    '_' +
                    record.monitorName +
                    '_' +
                    record.modelName +
                    '_' +
                    record?.createTime.replace(/-/g, '').replace(/\s+/g, '').replace(/:/g, '') +
                    '.mp4',
                  alertId: record.id,
                  description:
                    record.description.length > 5
                      ? record.description.substr(0, 6) + '...'
                      : record.description,
                  scene: record.scene,
                  monitor: record.monitorName,
                  alg: record.modelName,
                  status: record.status,
                  createTime: record.createTime,
                  alertMsg: record.description,
                })
              "
            />
          </div>
          <div v-else>-</div>
        </template>
        <template #audio="{ record }">
          <div class="flex justify-center" v-if="record?.audio">
            <Icon
              color="SteelBlue"
              icon="ant-design:audio-twotone"
              @click="
                openAlertMsgModal(true, {
                  mediaType: 'audio',
                  mediaPath: record.audioPath,
                  fileName:
                    record.scene +
                    '_' +
                    record.monitorName +
                    '_' +
                    record.modelName +
                    '_' +
                    record.createTime.replace(/-/g, '').replace(/\s+/g, '').replace(/:/g, '') +
                    '.mp3',
                  alertId: record.id,
                  description:
                    record.description.length > 5
                      ? record.description.substr(0, 6) + '...'
                      : record.description,
                  scene: record.scene,
                  monitor: record.monitorName,
                  alg: record.modelName,
                  status: record.status,
                  createTime: record.createTime,
                  alertMsg: record.description,
                })
              "
            />
          </div>
          <div v-else>-</div>
        </template>
      </BasicTable>
    </div>

    <AlertMsgModal @register="registerAlertMsgModal" :modalData="modalData" />
  </div>
</template>

<script lang="ts" setup name="AlertTable">
  import { onMounted, onUnmounted, ref, watch } from 'vue';
  import { BasicColumn, BasicTable, useTable } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import { Icon } from '/@/components/Icon/index';
  import { useAlertStore } from '/@/store/modules/alert';
  import AlertMsgModal from '/@/views/mineai/monitor/alert-msg/AlertMsgModal.vue';

  const alertStore = useAlertStore();
  const props = defineProps<{ columns: BasicColumn[] }>();
  const [registerAlertMsgModal, { openModal: openAlertMsgModal }] = useModal();
  let dataTimer;

  onMounted(async () => {
    await reload();
    dataTimer = setInterval(reload, 60000);
  });

  onUnmounted(() => {
    clearInterval(dataTimer);
  });

  interface alertFileInfo {
    mediaType: string;
    mediaPath: string;
    fileName: string;
    status: number;
    description: string;
    createTime: string;
    monitor: string;
    alg: string;
    alertId: string;
  }

  const modalData = ref<alertFileInfo>({
    mediaType: '',
    mediaPath: '',
    fileName: '',
    status: 0,
    description: '',
    createTime: '',
    monitor: '',
    alg: '',
    alertId: '',
  });

  const [registerTable, { reload }] = useTable({
    title: '报警列表',
    api: async (params) => {
      await alertStore.loadAllAlerts(params);
      // 返回过滤后的报警信息
      return {
        items: alertStore.data,
        total: alertStore.total,
      };
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
    columns: props.columns,
    showTableSetting: true,
    bordered: true,
    striped: false,
    showIndexColumn: false,
  });

  // 监视alertStore，发生变化重新加载表格
  watch(
    () => alertStore.selectedTreeItems,
    async () => {
      await reload();
    },
  );
</script>
<style lang="less" scoped>
  .table :deep(.ant-table-wrapper),
  :deep(.ant-table-thead > tr > th) {
    background-color: transparent;
  }

  .table :deep(.ant-table),
  :deep(.ant-table-header),
  :deep(.ant-table-fixed-header > .ant-table-content > .ant-table-scroll > .ant-table-body),
  :deep(.ant-table-cell-fix-left),
  :deep(.ant-table-cell-fix-right) {
    background: transparent;
  }
</style>
<style>
  .ant-image-preview-root {
    position: relative;
    z-index: 99999999;
  }
</style>
