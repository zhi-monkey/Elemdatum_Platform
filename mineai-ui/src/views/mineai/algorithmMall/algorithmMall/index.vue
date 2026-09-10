<template>
  <a-card title="应用商城" class="out-card">
    <a-spin v-if="isLoading" tip="加载中..." class="loading-spin" />
    <div v-else class="container-wrapper">
      <div v-if="records.length !== 0" class="content-wrapper">
        <a-row :gutter="24" class="cards-container">
          <a-col :span="8" v-for="record in paginatedRecords" :key="record.applicationName">
            <a-card class="inner-card">
              <template #title>
                <h2 class="card-title">{{ record.applicationName }}</h2>
              </template>
              <template #extra>
                <a @click="gotoApplicationDetail(record)" class="detail-link">查看详情</a>
              </template>

              <div class="card-content">
                <!-- 任务数和场景信息块 -->
                <div class="info-block primary-block">
                  <div class="block-item">
                    <div class="item-header">
                      <svg
                        class="item-icon"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                      >
                        <path
                          d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                        />
                      </svg>
                      <span class="item-label">已发布任务数</span>
                    </div>
                    <div class="item-value primary-value">{{ record.modelCount }}</div>
                  </div>

                  <div class="divider"></div>

                  <div class="block-item">
                    <div class="item-header">
                      <svg
                        class="item-icon"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                      >
                        <path
                          d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9"
                        />
                      </svg>
                      <span class="item-label">适用场景</span>
                    </div>
                    <div class="item-value scene-value">{{ record.scenes }}</div>
                  </div>
                </div>

                <!-- 支持设备信息块 -->
                <div class="info-block device-block">
                  <div class="block-item">
                    <div class="item-header">
                      <svg
                        class="item-icon"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                      >
                        <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                        <line x1="8" y1="21" x2="16" y2="21" />
                        <line x1="12" y1="17" x2="12" y2="21" />
                      </svg>
                      <span class="item-label">支持设备</span>
                    </div>
                    <div class="device-tags">
                      <span
                        v-for="(device, index) in record.deviceName.split('，')"
                        :key="index"
                        :class="['device-tag', `device-tag-color-${index % 5}`]"
                      >
                        {{ device }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </a-card>
          </a-col>
        </a-row>
      </div>
      <div v-else class="empty-wrapper">
        <a-alert
          message="当前没有可用的算法应用"
          description="There are currently no available algorithm applications."
          type="info"
          show-icon
        />
      </div>
      <div class="pagination-container">
        <a-pagination
          :current="currentPage"
          :pageSize="pageSize"
          :total="records.length"
          :show-size-changer="false"
          @change="onPageChange"
        />
      </div>
    </div>
  </a-card>
</template>

<script lang="ts" setup>
  import {
    Alert as AAlert,
    Card as ACard,
    Col as ACol,
    Pagination as APagination,
    Row as ARow,
    Spin as ASpin,
  } from 'ant-design-vue';
  import { computed, onMounted, ref } from 'vue';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { useGo } from '/@/hooks/web/usePage';
  import {
    getModelApplicationList,
    getModelApplicationListForReal,
  } from '/@/views/mineai/application/taskManagement/api/api';

  const go = useGo();
  const { hasPermission } = usePermission();
  const records = ref<any[]>([]);
  const currentPage = ref(1);
  const pageSize = ref(9); // 固定每页显示9条
  const isLoading = ref(true); // 加载状态

  const fetchData = async () => {
    try {
      const response = await getModelApplicationListForReal({});
      const filteredRecords = response.content.filter((item) => item.isReleased === '已发布');
      console.log(filteredRecords);

      const itemListss = filteredRecords.map((item) => ({
        applicationName: item.applicationName.applicationName,
        modelCount: filteredRecords.filter(
          (i) => i.applicationName.applicationName === item.applicationName.applicationName,
        ).length,
        deviceName:
          filteredRecords
            .filter(
              (i) => i.applicationName.applicationName === item.applicationName.applicationName,
            )
            .map((i) => i.device.deviceName)
            .filter((name) => name)
            .join('，') || '',
        scenes: Array.from(
          new Set(
            filteredRecords
              .filter(
                (i) => i.applicationName.applicationName === item.applicationName.applicationName,
              )
              .flatMap((i) => i.applicableScene.map((scene) => scene.name.trim())),
          ),
        ).join('; '),
      }));

      records.value = Array.from(new Set(itemListss.map((item) => item.applicationName)))
        .map((applicationName) =>
          itemListss.find((item) => item.applicationName === applicationName),
        )
        .filter((item) => item !== undefined);
    } catch (error) {
      console.error('获取应用列表失败:', error);
    } finally {
      isLoading.value = false; // 数据加载完成，设置加载状态为 false
    }
  };

  const gotoApplicationDetail = (record) => {
    go(`/maAlgorithmMall/modelApplicationDetail/${record.applicationName}`);
  };

  // 计算当前页的记录
  const paginatedRecords = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return records.value.slice(start, start + pageSize.value);
  });

  const onPageChange = (page) => {
    currentPage.value = page;
  };

  onMounted(() => {
    fetchData(); // 在组件首次加载时调用
  });
</script>

<style scoped>
  .out-card {
    background-color: #181d31;
  }

  .out-card :deep(.ant-card-body) {
    background-color: #181d31;
  }

  .out-card :deep(.ant-card-head-title) {
    font-size: 24px;
    color: #fff;
    text-align: center;
  }

  .out-card :deep(.ant-card-head) {
    background-color: #181d31;
    border-bottom-color: #5469a8;
  }

  .inner-card {
    margin-bottom: 24px;
    border: 1px solid rgba(24, 144, 255, 0.2);
    border-radius: 8px;
    background-color: #192542;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 4px 16px rgba(24, 144, 255, 0.3);
      transform: translateY(-2px);
      border-color: rgba(24, 144, 255, 0.5);
    }
  }

  .inner-card :deep(.ant-card-head) {
    border-bottom: 1px solid rgba(24, 144, 255, 0.2);
  }

  .inner-card :deep(.ant-card-head-title) {
    text-align: left;
  }

  .card-title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 0;
    color: #fff;
  }

  .detail-link {
    font-size: 14px;
    color: #1890ff;
    transition: color 0.3s;

    &:hover {
      color: #40a9ff;
    }
  }

  .card-content {
    font-size: 14px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .info-block {
    padding: 16px;
    border-radius: 8px;
    border: 1px solid rgba(24, 144, 255, 0.2);
    transition: all 0.3s ease;

    &:hover {
      border-color: rgba(24, 144, 255, 0.4);
      box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
    }
  }

  .primary-block {
    background: linear-gradient(135deg, rgba(24, 144, 255, 0.08) 0%, rgba(24, 144, 255, 0.03) 100%);
    display: flex;
    flex-direction: row;
    gap: 16px;
    align-items: stretch;
  }

  .device-block {
    background: linear-gradient(135deg, rgba(24, 144, 255, 0.08) 0%, rgba(24, 144, 255, 0.03) 100%);
    border-color: rgba(24, 144, 255, 0.2);

    &:hover {
      border-color: rgba(24, 144, 255, 0.4);
      box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
    }
  }

  .block-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
    flex: 1;
  }

  .item-header {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .item-icon {
    width: 18px;
    height: 18px;
    color: #1890ff;
    flex-shrink: 0;
  }

  .device-block .item-icon {
    color: #52c41a;
  }

  .item-label {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.65);
    font-weight: 500;
  }

  .item-value {
    padding-left: 26px;
    font-weight: 600;
    line-height: 1.5;
  }

  .primary-value {
    font-size: 24px;
    color: #40a9ff;
  }

  .scene-value {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.85);
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 1.5;
    height: 3em;
    min-height: 3em;
  }

  .device-value {
    font-size: 14px;
    color: #73d13d;
  }

  .device-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding-left: 26px;
  }

  .device-tag {
    padding: 4px 12px;
    font-size: 13px;
    border-radius: 4px;
    transition: all 0.3s ease;
    font-weight: 500;
  }

  .device-tag-color-0 {
    color: #73d13d;
    background: rgba(82, 196, 26, 0.1);
    border: 1px solid rgba(82, 196, 26, 0.25);

    &:hover {
      background: rgba(82, 196, 26, 0.15);
      border-color: rgba(82, 196, 26, 0.4);
    }
  }

  .device-tag-color-1 {
    color: #40a9ff;
    background: rgba(24, 144, 255, 0.1);
    border: 1px solid rgba(24, 144, 255, 0.25);

    &:hover {
      background: rgba(24, 144, 255, 0.15);
      border-color: rgba(24, 144, 255, 0.4);
    }
  }

  .device-tag-color-2 {
    color: #ff85c0;
    background: rgba(235, 47, 150, 0.1);
    border: 1px solid rgba(235, 47, 150, 0.25);

    &:hover {
      background: rgba(235, 47, 150, 0.15);
      border-color: rgba(235, 47, 150, 0.4);
    }
  }

  .device-tag-color-3 {
    color: #ffc53d;
    background: rgba(250, 173, 20, 0.1);
    border: 1px solid rgba(250, 173, 20, 0.25);

    &:hover {
      background: rgba(250, 173, 20, 0.15);
      border-color: rgba(250, 173, 20, 0.4);
    }
  }

  .device-tag-color-4 {
    color: #9254de;
    background: rgba(114, 46, 209, 0.1);
    border: 1px solid rgba(114, 46, 209, 0.25);

    &:hover {
      background: rgba(114, 46, 209, 0.15);
      border-color: rgba(114, 46, 209, 0.4);
    }
  }

  .divider {
    width: 1px;
    background: linear-gradient(
      180deg,
      rgba(24, 144, 255, 0) 0%,
      rgba(24, 144, 255, 0.3) 50%,
      rgba(24, 144, 255, 0) 100%
    );
    align-self: stretch;
  }

  .container-wrapper {
    min-height: calc(100vh - 150px);
    display: flex;
    flex-direction: column;
  }

  .content-wrapper {
    flex: 1;
    padding-bottom: 24px;
  }

  .cards-container {
    margin-bottom: 24px;
  }

  .empty-wrapper {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: auto;
    padding-top: 16px;
    width: 100%;
  }
</style>
