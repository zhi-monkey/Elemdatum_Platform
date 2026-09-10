<template>
  <div class="dataset-group-container">
    <!-- 数据集组列表 -->
    <div>
      <!-- 搜索栏 -->
      <div
        class="p-4 bg-white"
        style="display: flex; justify-content: flex-end; align-items: center"
      >
        <a-space>
          <a-select
            v-model:value="isGuidedFilter"
            placeholder="筛选来源"
            style="width: 150px"
            allow-clear
            @change="handleFilterChange"
          >
            <a-select-option :value="true">引导式</a-select-option>
            <a-select-option :value="false">标准化</a-select-option>
          </a-select>
          <a-input-search
            v-model:value="publicSearchKeyword"
            placeholder="搜索数据集组名称"
            style="width: 300px"
            allow-clear
            @search="handlePublicSearch"
          />
        </a-space>
      </div>

      <!-- 数据集组卡片列表 -->
      <a-spin :spinning="publicGroupLoading">
        <div class="card-list-wrapper">
          <a-list
            :grid="{ gutter: [16, 16], xs: 1, sm: 1, md: 2, lg: 2, xl: 2, xxl: 2 }"
            :data-source="publicDatasetGroups"
            :pagination="publicGroupPagination"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <a-card hoverable class="dataset-group-card-horizontal">
                  <!-- 卡片头部 -->
                  <template #title>
                    <div class="card-header">
                      <div class="card-title-section">
                        <FolderOpenOutlined class="title-icon" />
                        <span class="group-name">{{ item.name }}</span>
                      </div>
                      <a-space class="card-actions">
                        <a @click="handleViewPublicGroup(item.id)" class="action-link">查看详情</a>
                      </a-space>
                    </div>
                  </template>

                  <!-- 卡片内容区域 -->
                  <div class="card-content-horizontal">
                    <!-- 左侧统计信息区域 -->
                    <div class="stats-section">
                      <div class="stats-row">
                        <a-statistic
                          title="数据集数量"
                          :value="item.datasetCount"
                          :value-style="{
                            color: '#1890ff',
                            fontSize: '20px',
                            fontWeight: 'bold',
                          }"
                          suffix="个"
                          class="stat-item"
                        />
                        <a-statistic
                          title="版本总数"
                          :value="item.versionCount"
                          :value-style="{
                            color: '#52c41a',
                            fontSize: '20px',
                            fontWeight: 'bold',
                          }"
                          suffix="个"
                          class="stat-item"
                        />
                        <a-statistic
                          title="总图片数"
                          :value="item.totalFiles"
                          :value-style="{
                            color: '#722ed1',
                            fontSize: '20px',
                            fontWeight: 'bold',
                          }"
                          class="stat-item"
                        />
                        <a-statistic
                          title="标签种类数"
                          :value="item.labelCount"
                          :value-style="{
                            color: '#f8a349',
                            fontSize: '20px',
                            fontWeight: 'bold',
                          }"
                          suffix="种"
                          class="stat-item"
                        />
                      </div>
                    </div>

                    <!-- 右侧信息区域 -->
                    <div class="info-section">
                      <!-- 描述信息 -->
                      <div class="description-area">
                        <div class="description-title">描述信息</div>
                        <Tooltip :title="item.description">
                          <p class="description-text">
                            {{ item.description || '暂无描述信息' }}
                          </p>
                        </Tooltip>
                      </div>

                      <!-- 最新数据集 -->
                      <div class="latest-datasets-area">
                        <div class="latest-datasets-title">
                          <FileTextOutlined class="section-icon" />
                          最新数据集
                        </div>
                        <div class="latest-datasets-content">
                          <template v-if="Object.keys(item.latestDatasets || {}).length > 0">
                            <div
                              v-for="(createTime, datasetName, index) in item.latestDatasets"
                              :key="datasetName"
                              class="dataset-item-horizontal"
                              v-show="index < 2"
                            >
                              <a-typography-link
                                @click="handlePublicDatasetLinkClick(item.id, datasetName)"
                                class="dataset-link"
                              >
                                <div class="dataset-info-horizontal">
                                  <span class="dataset-name">{{ datasetName }}</span>
                                  <span class="dataset-date">{{ parseDate(createTime) }}</span>
                                </div>
                              </a-typography-link>
                            </div>
                          </template>
                          <template v-else>
                            <a-empty
                              :image="simpleImage"
                              description="暂无数据集"
                              :imageStyle="{ height: '32px' }"
                              class="empty-datasets"
                            />
                          </template>
                        </div>
                      </div>
                      <div class="meta-info">
                        <span class="create-time">创建于: {{ parseDate(item.creationTime) }}</span>
                      </div>
                    </div>
                  </div>
                </a-card>
              </a-list-item>
            </template>
            <template #empty>
              <a-empty :image="simpleImage" description="暂无公开数据集组" />
            </template>
          </a-list>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import {
    Card as ACard,
    Empty as AEmpty,
    InputSearch as AInputSearch,
    List as AList,
    ListItem as AListItem,
    Select as ASelect,
    SelectOption as ASelectOption,
    Space as ASpace,
    Spin as ASpin,
    Statistic as AStatistic,
    Tooltip,
    TypographyLink as ATypographyLink,
  } from 'ant-design-vue';
  import { FileTextOutlined, FolderOpenOutlined } from '@ant-design/icons-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { computed, onMounted, reactive, ref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { useUserStore } from '/@/store/modules/user';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { useGo } from '/@/hooks/web/usePage';

  const userStore = useUserStore();
  const roles = userStore.getUserInfo.roles;
  const { hasPermission } = usePermission();
  const { createMessage } = useMessage();
  const router = useRouter();
  const go = useGo();

  // 公有数据集组状态
  const publicGroupLoading = ref(false);
  const publicDatasetGroups = ref<any[]>([]);
  const publicSearchKeyword = ref('');
  const isGuidedFilter = ref<boolean | undefined>(undefined);
  const publicGroupPagination = reactive({
    current: 1,
    pageSize: 8,
    total: 0,
    showSizeChanger: false,
    onChange: (page, pageSize) => {
      publicGroupPagination.current = page;
      publicGroupPagination.pageSize = pageSize;
      fetchPublicDatasetGroups();
    },
  });

  // 搜索处理函数
  const handlePublicSearch = () => {
    publicGroupPagination.current = 1;
    fetchPublicDatasetGroups();
  };

  // 筛选处理函数
  const handleFilterChange = () => {
    publicGroupPagination.current = 1;
    fetchPublicDatasetGroups();
  };

  // API函数
  const fetchPublicDatasetGroups = async () => {
    publicGroupLoading.value = true;
    try {
      const params: any = {
        current: publicGroupPagination.current ?? 1,
        size: publicGroupPagination.pageSize ?? 8,
      };
      if (publicSearchKeyword.value && publicSearchKeyword.value.trim() !== '') {
        params.name = publicSearchKeyword.value.trim();
      }
      if (isGuidedFilter.value !== undefined && isGuidedFilter.value !== null) {
        params.isGuided = isGuidedFilter.value;
      }

      const result = await maHttp.get(
        {
          url: 'datasets/group/getPublicDatasetGroupByPageTYX',
          params,
        },
        {
          urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
        },
      );

      const items = result.result || [];
      const total = result.page?.total || 0;

      publicDatasetGroups.value = items;
      publicGroupPagination.total = total;
    } catch (e) {
      createMessage.error('获取公开数据集组失败');
    } finally {
      publicGroupLoading.value = false;
    }
  };

  // 跳转逻辑
  const handleViewPublicGroup = (groupId: number) => {
    sessionStorage.setItem('publicGroupCurrentPage', publicGroupPagination.current.toString());
    sessionStorage.setItem('publicGroupSearchKeyword', publicSearchKeyword.value);
    sessionStorage.setItem('publicGroupIsGuidedFilter', String(isGuidedFilter.value));
    sessionStorage.setItem(
      'PublicGroupName',
      publicDatasetGroups.value.find((g) => g.id === groupId).name,
    );
    router.push(`/maData/labelGroupPublicDetails/${groupId}`);
  };

  // 数据集链接点击逻辑
  const handlePublicDatasetLinkClick = (groupId: number, datasetName: string) => {
    handleViewPublicGroup(groupId);
  };

  // 工具函数
  const parseDate = (dateStr) => {
    return new Date(dateStr).toLocaleString();
  };

  const simpleImage = AEmpty.PRESENTED_IMAGE_SIMPLE;

  // 监听搜索关键词清空
  watch(publicSearchKeyword, (newVal, oldVal) => {
    if (oldVal && !newVal) {
      handlePublicSearch();
    }
  });

  // 监听筛选条件清空
  watch(isGuidedFilter, (newVal, oldVal) => {
    if (oldVal !== undefined && newVal === undefined) {
      handleFilterChange();
    }
  });

  // 恢复所有状态
  const restoreAllState = () => {
    const route = useRoute();
    const currentTab = route.query.tab as string;

    if (currentTab === '4') {
      // 恢复页码
      const savedPage = sessionStorage.getItem('publicGroupCurrentPage');
      if (savedPage) {
        publicGroupPagination.current = parseInt(savedPage, 10);
      }

      // 恢复搜索关键词
      const savedKeyword = sessionStorage.getItem('publicGroupSearchKeyword');
      if (savedKeyword !== null) {
        publicSearchKeyword.value = savedKeyword;
      }

      // 恢复筛选条件
      const savedIsGuided = sessionStorage.getItem('publicGroupIsGuidedFilter');
      if (savedIsGuided !== null) {
        isGuidedFilter.value =
          savedIsGuided === 'true' ? true : savedIsGuided === 'false' ? false : undefined;
      }

      // 恢复后清除保存的状态，避免影响其他操作
      cleanupSavedState();
    }
  };

  // 清理保存的状态
  const cleanupSavedState = () => {
    sessionStorage.removeItem('publicGroupCurrentPage');
    sessionStorage.removeItem('publicGroupSearchKeyword');
    sessionStorage.removeItem('publicGroupIsGuidedFilter');
  };

  onMounted(() => {
    restoreAllState();
    fetchPublicDatasetGroups();
  });
</script>

<style lang="less" scoped>
  .dataset-group-card-horizontal {
    height: 280px;
    transition: all 0.3s ease;
    border: 1px solid rgba(24, 144, 255, 0.15);
    border-radius: 8px;
    background: transparent !important;

    &:hover {
      box-shadow: 0 8px 24px rgba(24, 144, 255, 0.25);
      transform: translateY(-2px);
      border-color: rgba(24, 144, 255, 0.5);
    }

    :deep(.ant-card-head) {
      background: transparent;
      border-bottom: 1px solid rgba(24, 144, 255, 0.15);
      padding: 0 20px;
      min-height: 60px;
    }

    :deep(.ant-card-head-title) {
      padding: 12px 0;
    }

    :deep(.ant-card-body) {
      background: transparent;
      padding: 20px;
      height: calc(280px - 60px);
      overflow: hidden;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      width: 100%;

      .card-title-section {
        display: flex;
        align-items: center;
        flex: 1;

        .title-icon {
          font-size: 18px;
          color: #1890ff;
          margin-right: 8px;
        }

        .group-name {
          font-size: 16px;
          font-weight: 600;
          color: inherit;
          max-width: 70%;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .card-actions {
        .action-link {
          color: #1890ff;
          font-size: 14px;
          transition: color 0.3s;

          &:hover {
            color: #40a9ff;
          }
        }
      }
    }

    .card-content-horizontal {
      display: flex;
      height: 100%;
      gap: 24px;

      .stats-section {
        flex: 0 0 60%;
        display: flex;
        flex-direction: column;
        justify-content: center;

        .stats-row {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 16px;

          .stat-item {
            text-align: center;
            padding: 16px 12px;
            border-radius: 8px;
            border: 1px solid rgba(24, 144, 255, 0.2);
            transition: all 0.3s ease;
            background: transparent;

            &:hover {
              transform: translateY(-2px);
              box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
              border-color: rgba(24, 144, 255, 0.4);
            }

            :deep(.ant-statistic-title) {
              font-size: 12px;
              color: rgba(255, 255, 255, 0.65);
              font-weight: 500;
              margin-bottom: 4px;
            }

            :deep(.ant-statistic-content) {
              display: flex;
              justify-content: center;
              align-items: center;
            }

            &:nth-child(1) {
              border-color: rgba(24, 144, 255, 0.3);

              :deep(.ant-statistic-content-value) {
                color: #1890ff !important;
              }
            }

            &:nth-child(2) {
              border-color: rgba(64, 169, 255, 0.3);

              :deep(.ant-statistic-content-value) {
                color: #40a9ff !important;
              }
            }

            &:nth-child(3) {
              border-color: rgba(135, 208, 104, 0.3);

              :deep(.ant-statistic-content-value) {
                color: #87d068 !important;
              }
            }

            &:nth-child(4) {
              border-color: rgba(114, 46, 209, 0.3);

              :deep(.ant-statistic-content-value) {
                color: #722ed1 !important;
              }
            }
          }
        }
      }

      .info-section {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 16px;

        .description-area {
          flex: 1;

          .description-title {
            font-size: 13px;
            color: rgba(255, 255, 255, 0.65);
            font-weight: 500;
            margin-bottom: 8px;
          }

          .description-text {
            color: rgba(255, 255, 255, 0.85);
            font-size: 14px;
            line-height: 1.5;
            margin: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            -webkit-line-clamp: 3;
            -webkit-box-orient: vertical;
            max-height: 63px;
          }
        }

        .latest-datasets-area {
          .latest-datasets-title {
            display: flex;
            align-items: center;
            font-size: 13px;
            color: rgba(255, 255, 255, 0.65);
            font-weight: 500;
            margin-bottom: 8px;

            .section-icon {
              margin-right: 4px;
              color: #1890ff;
            }
          }

          .latest-datasets-content {
            .dataset-item-horizontal {
              margin-bottom: 8px;

              &:last-child {
                margin-bottom: 0;
              }

              .dataset-link {
                display: block;

                &:hover {
                  color: #1890ff;
                }

                .dataset-info-horizontal {
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  padding: 8px 12px;
                  background: transparent;
                  border-radius: 6px;
                  border: 1px solid rgba(24, 144, 255, 0.15);
                  transition: all 0.3s;

                  &:hover {
                    border-color: rgba(24, 144, 255, 0.4);
                    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
                  }

                  .dataset-name {
                    flex: 1;
                    font-size: 14px;
                    color: inherit;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    margin-right: 8px;
                  }

                  .dataset-date {
                    font-size: 12px;
                    color: rgba(255, 255, 255, 0.45);
                    white-space: nowrap;
                  }
                }
              }
            }

            .empty-datasets {
              :deep(.ant-empty-description) {
                font-size: 12px;
                color: rgba(255, 255, 255, 0.45);
              }
            }
          }
        }
      }
    }
  }

  .dataset-group-container {
    .card-list-wrapper {
      padding: 16px;
    }
  }

  .meta-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 12px;
    border-top: 1px solid rgba(24, 144, 255, 0.15);

    .create-time {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.45);
    }
  }

  @media (max-width: 768px) {
    .dataset-group-card-horizontal {
      height: auto;

      .card-content-horizontal {
        flex-direction: column;
        gap: 16px;

        .stats-section {
          flex: none;

          .stats-row {
            grid-template-columns: 1fr 1fr;
          }
        }
      }

      .card-header {
        .card-title-section {
          .group-name {
            max-width: 60%;
          }
        }
      }
    }
  }

  @media (max-width: 480px) {
    .dataset-group-card-horizontal {
      .card-header {
        .card-title-section {
          .group-name {
            max-width: 50%;
          }
        }
      }
    }
  }
</style>
