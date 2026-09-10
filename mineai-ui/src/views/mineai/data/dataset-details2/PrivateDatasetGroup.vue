<template>
  <div class="dataset-group-container">
    <!-- 工具栏 -->
    <div
      class="p-4 bg-white"
      style="display: flex; justify-content: space-between; align-items: center"
    >
      <a-button type="primary" @click="handleCreateGroup">新增数据集组</a-button>
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
          v-model:value="searchKeyword"
          placeholder="搜索数据集组名称"
          style="width: 300px"
          allow-clear
          @search="handleSearch"
        />
      </a-space>
    </div>

    <!-- 数据集组卡片列表 -->
    <a-spin :spinning="groupLoading">
      <div class="card-list-wrapper">
        <a-list
          :grid="{ gutter: [16, 16], xs: 1, sm: 1, md: 2, lg: 2, xl: 2, xxl: 2 }"
          :data-source="datasetGroups"
          :pagination="groupPagination"
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
                      <a @click="handleViewGroup(item.id)" class="action-link">查看详情</a>
                      <a @click="handleEditGroup(item)" class="action-link">编辑</a>
                      <a @click="handleDelGroup(item)" class="action-link">删除</a>
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
                              @click="handleDatasetLinkClick(item.id, datasetName)"
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
            <a-empty :image="simpleImage" description="暂无数据集组" />
          </template>
        </a-list>
      </div>
    </a-spin>
  </div>

  <!-- 数据集组创建/编辑弹窗 -->
  <DatasetGroupCreateModal
    @register="registerDatasetGroupCreateModal"
    @success="handleDatasetGroupModalSuccess"
  />
</template>

<script lang="ts" setup>
  import { useModal } from '/@/components/Modal';
  import {
    Button as AButton,
    Card as ACard,
    Empty as AEmpty,
    InputSearch as AInputSearch,
    List as AList,
    ListItem as AListItem,
    Modal,
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
  import { onMounted, onUnmounted, reactive, ref, watch } from 'vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { deleteDatasetGroup } from './api';
  import DatasetGroupCreateModal from './dataset-group/DatasetGroupCreateModal.vue';
  import { useRoute, useRouter } from 'vue-router';

  const { createMessage } = useMessage();

  // Modal注册
  const [registerDatasetGroupCreateModal, { openModal: openDatasetGroupCreateModal }] = useModal();

  // 数据集组列表状态
  const groupLoading = ref(false);
  const datasetGroups = ref<any[]>([]);
  const searchKeyword = ref('');
  const isGuidedFilter = ref<boolean | undefined>(undefined);
  const groupPagination = reactive({
    current: 1,
    pageSize: 8,
    total: 0,
    showSizeChanger: false,
    onChange: (page, pageSize) => {
      groupPagination.current = page;
      groupPagination.pageSize = pageSize;
      fetchDatasetGroups();
    },
  });

  // 搜索处理函数
  const handleSearch = () => {
    // 搜索时重置到第一页并重新请求
    groupPagination.current = 1;
    fetchDatasetGroups();
  };

  // 筛选处理函数
  const handleFilterChange = () => {
    // 筛选时重置到第一页并重新请求
    groupPagination.current = 1;
    fetchDatasetGroups();
  };

  // 视图切换状态
  const activeGroupId = ref<number | null>(null);
  const activeGroupData = ref<any>(null);

  // API函数
  const fetchGroupsAPI = (current: number, size: number, name?: string, isGuided?: boolean) => {
    const params: any = {
      current,
      size,
    };
    if (name && name.trim() !== '') {
      params.name = name.trim();
    }
    if (isGuided !== undefined && isGuided !== null) {
      params.isGuided = isGuided;
    }
    return maHttp
      .get(
        {
          url: 'datasets/group/getAllDatasetGroupPage',
          params,
        },
        {
          urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
        },
      )
      .then((res) => {
        return {
          items: res.result,
          total: res.page.total,
        };
      });
  };

  // 数据集组列表逻辑
  const fetchDatasetGroups = async () => {
    groupLoading.value = true;
    try {
      const { items, total } = await fetchGroupsAPI(
        groupPagination.current ?? 1,
        groupPagination.pageSize ?? 8,
        searchKeyword.value,
        isGuidedFilter.value,
      );
      datasetGroups.value = items;
      groupPagination.total = total;
    } catch (e) {
      createMessage.error('获取数据集组失败');
    } finally {
      groupLoading.value = false;
    }
  };

  // 数据集组操作
  const handleCreateGroup = () => {
    openDatasetGroupCreateModal(true, { isUpdate: false });
  };

  const handleEditGroup = (record) => {
    openDatasetGroupCreateModal(true, { isUpdate: true, record });
  };

  const handleDelGroup = async (record) => {
    const model = Modal.confirm({
      title: '删除数据集组',
      content: `确定要删除数据集组【${record.name}】吗？`,
      okText: '删除',
      okType: 'danger',
      onOk() {
        return deleteDatasetGroup(record.id).then(async () => {
          createMessage.success('删除成功！');
          model.destroy();
          await fetchDatasetGroups();
        });
      },
      onCancel() {
        model.destroy();
      },
    });
  };

  const handleDatasetGroupModalSuccess = async (data: { isUpdate: boolean }) => {
    if (data.isUpdate) {
      createMessage.success('数据集组编辑成功');
    } else {
      createMessage.success('数据集组创建成功');
    }
    await fetchDatasetGroups();
  };

  // 跳转逻辑
  const router = useRouter();
  const handleViewGroup = (groupId: number) => {
    activeGroupId.value = groupId;
    activeGroupData.value = datasetGroups.value.find((g) => g.id === groupId);
    sessionStorage.setItem('privateGroupCurrentPage', groupPagination.current.toString());
    sessionStorage.setItem('privateGroupSearchKeyword', searchKeyword.value);
    sessionStorage.setItem('privateGroupIsGuidedFilter', String(isGuidedFilter.value));
    sessionStorage.setItem('GroupName', activeGroupData.value.name);
    router.push(`/maData/labelGroupPrivateDetails/${groupId}`);
  };

  // 数据集链接点击和高亮逻辑
  const handleDatasetLinkClick = async (groupId: number, datasetName: string) => {
    handleViewGroup(groupId);
  };

  // 工具函数
  const parseDate = (dateStr) => {
    return new Date(dateStr).toLocaleString();
  };

  const simpleImage = AEmpty.PRESENTED_IMAGE_SIMPLE;

  // 监听搜索关键词清空
  watch(searchKeyword, (newVal, oldVal) => {
    // 当搜索框被清空时（从有值变为空），自动触发查询
    if (oldVal && !newVal) {
      handleSearch();
    }
  });

  // 恢复所有状态
  const restoreAllState = () => {
    const route = useRoute();
    const currentTab = route.query.tab as string;

    if (currentTab === '1') {
      // 恢复页码
      const savedPage = sessionStorage.getItem('privateGroupCurrentPage');
      if (savedPage) {
        groupPagination.current = parseInt(savedPage, 10);
      }

      // 恢复搜索关键词
      const savedKeyword = sessionStorage.getItem('privateGroupSearchKeyword');
      if (savedKeyword !== null) {
        searchKeyword.value = savedKeyword;
      }

      // 恢复筛选条件
      const savedIsGuided = sessionStorage.getItem('privateGroupIsGuidedFilter');
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
    sessionStorage.removeItem('privateGroupCurrentPage');
    sessionStorage.removeItem('privateGroupSearchKeyword');
    sessionStorage.removeItem('privateGroupIsGuidedFilter');
  };

  onMounted(() => {
    restoreAllState();
    fetchDatasetGroups();
  });

  onUnmounted(() => {});
</script>

<style lang="less" scoped>
  .group-title-link {
    display: flex;
    align-items: center;
    font-size: 14px !important;
    font-weight: 500;
    color: #1890ff;

    &:hover {
      color: #40a9ff;
      text-decoration: underline;
    }

    .anticon {
      margin-right: 6px;
    }
  }

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
        gap: 12px;
        min-height: 0;
        min-width: 0;
        width: 100%;
        overflow: hidden;

        .description-area {
          flex: 0 0 auto;
          height: 80px;
          overflow: hidden;

          .description-title {
            font-size: 13px;
            color: rgba(255, 255, 255, 0.65);
            font-weight: 500;
            margin-bottom: 6px;
          }

          .description-text {
            color: rgba(255, 255, 255, 0.85);
            font-size: 13px;
            line-height: 1.4;
            margin: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            word-break: break-word;
            height: 37px;
          }
        }

        .latest-datasets-area {
          flex: 1;
          display: flex;
          flex-direction: column;
          min-height: 0;

          .latest-datasets-title {
            display: flex;
            align-items: center;
            font-size: 13px;
            color: rgba(255, 255, 255, 0.65);
            font-weight: 500;
            margin-bottom: 6px;
            flex-shrink: 0;

            .section-icon {
              margin-right: 4px;
              color: #1890ff;
            }
          }

          .latest-datasets-content {
            flex: 1;
            overflow: hidden;
            width: 100%;

            .dataset-item-horizontal {
              margin-bottom: 6px;
              width: 100%;

              &:last-child {
                margin-bottom: 0;
              }

              .dataset-link {
                display: block;
                width: 100%;

                &:hover {
                  color: #1890ff;
                }

                .dataset-info-horizontal {
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  padding: 6px 10px;
                  background: transparent;
                  border-radius: 6px;
                  border: 1px solid rgba(24, 144, 255, 0.15);
                  transition: all 0.3s;
                  min-height: 32px;
                  width: 100%;
                  box-sizing: border-box;

                  &:hover {
                    border-color: rgba(24, 144, 255, 0.4);
                    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
                  }

                  .dataset-name {
                    flex: 1;
                    font-size: 13px;
                    color: inherit;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    margin-right: 8px;
                    min-width: 0;
                    max-width: 100%;
                  }

                  .dataset-date {
                    font-size: 11px;
                    color: rgba(255, 255, 255, 0.45);
                    white-space: nowrap;
                    flex-shrink: 0;
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

  .meta-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 8px;
    margin-top: auto;
    border-top: 1px solid rgba(24, 144, 255, 0.15);
    flex-shrink: 0;

    .create-time {
      font-size: 11px;
      color: rgba(255, 255, 255, 0.45);
    }
  }
</style>
