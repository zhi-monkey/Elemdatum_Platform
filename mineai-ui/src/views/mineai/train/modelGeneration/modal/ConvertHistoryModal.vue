<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    title="模型转换记录"
    :showCancelBtn="false"
    :showOkBtn="false"
    width="600px"
    @cancel="handleModalClose"
  >
    <a-list :loading="loading" item-layout="horizontal" :data-source="dataList" style="margin: 4px">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta :description="item.createTime + ' - ' + item.lastJobTime">
            <template #title>
              <span>{{ item.name }}</span>
            </template>
          </a-list-item-meta>
          <template #actions>
            <div style="display: flex; align-items: center; gap: 8px; margin-left: 8px">
              <a-tag :color="getStatusColor(item.status)" style="margin: 0; flex-shrink: 0">
                {{ getStatusText(item.status) }}
              </a-tag>
              <a-button
                @click="go(`/maTrainingCenter/convertJobDetails/${item.id}_${item.jobType}`)"
                style="flex-shrink: 0"
              >
                查看详情
              </a-button>
            </div>
          </template>
        </a-list-item>
      </template>
    </a-list>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import {
    Button as AButton,
    List as AList,
    ListItem as AListItem,
    ListItemMeta as AListItemMeta,
    Tag as ATag,
  } from 'ant-design-vue';
  import { ref, onUnmounted } from 'vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  defineEmits(['register']);

  const loading = ref(true);
  const dataList = ref<Array<any>>([]);
  const pollingIds = ref<Set<number>>(new Set());
  const pollingTimer = ref<NodeJS.Timeout | null>(null);
  const POLLING_INTERVAL = 3000;

  const [registerModal, { setModalProps }] = useModalInner(async (data) => {
    dataList.value = data.history;
    loading.value = false;
    setModalProps({ confirmLoading: false });
    updatePollingIds();
  });

  const updatePollingIds = () => {
    pollingIds.value = new Set(
      dataList.value.filter((item) => item.status === 1).map((item) => item.id),
    );

    if (pollingIds.value.size === 0) {
      stopPolling();
    } else if (!pollingTimer.value) {
      startPolling();
    }
  };

  const startPolling = () => {
    if (pollingTimer.value) return; // 避免重复启动
    pollingTimer.value = setInterval(pollJobStatus, POLLING_INTERVAL);
  };

  const stopPolling = () => {
    if (pollingTimer.value) {
      clearInterval(pollingTimer.value);
      pollingTimer.value = null;
    }
  };

  const pollJobStatus = async () => {
    if (pollingIds.value.size === 0) {
      stopPolling();
      return;
    }

    try {
      const ids = Array.from(pollingIds.value);
      const results = await Promise.all(
        ids.map((id) =>
          maHttp.get(
            {
              url: 'modelJob/getJobByJobId',
              params: { id },
              headers: { ignoreCancelToken: true },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          ),
        ),
      );

      results.forEach((result, index) => {
        const id = ids[index];
        const itemIndex = dataList.value.findIndex((item) => item.id === id);

        if (itemIndex !== -1) {
          Object.assign(dataList.value[itemIndex], result);

          if (result.status !== 1) {
            pollingIds.value.delete(id);
          }
        }
      });

      if (pollingIds.value.size === 0) {
        stopPolling();
      }
    } catch (error) {
      console.error('轮询任务状态失败:', error);
    }
  };

  const handleModalClose = () => {
    stopPolling();
    pollingIds.value.clear();
  };

  // 确保组件卸载时停止轮询
  onUnmounted(handleModalClose);

  const go = useGo();

  const getStatusText = (status: number) => {
    switch (status) {
      case 6:
        return '转换成功';
      case 1:
        return '转换中';
      case -5:
        return '转换失败';
      case -10:
        return '任务取消';
      default:
        return '未知状态';
    }
  };

  const getStatusColor = (status: number) => {
    switch (status) {
      case 6:
        return 'green';
      case 1:
        return 'yellow';
      case -5:
        return 'red';
      case -10:
        return 'grey';
      default:
        return 'black';
    }
  };
</script>

<style scoped lang="less"></style>
