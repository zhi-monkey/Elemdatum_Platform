<template>
  <div>
    <a-card title="训练数据集选择" style="margin-bottom: 16px; padding: 16px">
      <div class="cascade-panel">
        <!-- 第一级：数据集组 -->
        <div class="cascade-level">
          <div class="level-header">
            <h4>数据集组</h4>
            <div class="search-controls" :class="{ 'vertical-layout': isVerticalLayout }">
              <div class="privacy-filter">
                <a-checkbox-group v-model:value="privacyFilter" @change="onPrivacyFilterChange">
                  <a-checkbox value="public">公开</a-checkbox>
                  <a-checkbox value="private">私有</a-checkbox>
                </a-checkbox-group>
              </div>
              <div class="search-box">
                <a-input-search
                  v-model:value="searchGroup"
                  placeholder="搜索数据集组"
                  :loading="groupLoading"
                  @search="onSearchGroup"
                />
              </div>
            </div>
          </div>
          <div class="level-content">
            <div class="item-list">
              <a-spin :spinning="groupLoading" tip="加载数据集组中...">
                <div class="item-container">
                  <template v-if="datasetGroups.length > 0">
                    <div
                      v-for="group in datasetGroups"
                      :key="group.id"
                      class="item"
                      :class="{
                        active: selectedGroup?.id === group.id,
                        'has-selected': getGroupSelectedCount(group.id) > 0,
                        'all-selected': isGroupAllSelected(group.id),
                      }"
                      @click="selectGroup(group)"
                    >
                      <div class="item-name">
                        {{ group.name }}
                        <span v-if="getGroupSelectedCount(group.id) > 0" class="selection-count">
                          (已选 {{ getGroupSelectedCount(group.id) }})
                        </span>
                      </div>
                      <div class="item-desc"
                        >{{ group.description || group.remark || '暂无描述' }}
                      </div>
                      <div class="item-meta">
                        <span v-if="group.createTime"
                          >创建时间: {{ formatDate(group.createTime) }}</span
                        >
                      </div>
                    </div>
                  </template>
                  <div v-else class="empty-tip">
                    <div class="empty-tip-content">
                      当前无对应的{{ getAnnotateTypeText() }}的数据集组
                    </div>
                  </div>
                </div>
              </a-spin>
            </div>
            <div class="pagination-wrapper">
              <a-pagination
                v-model:current="groupPage"
                :total="totalGroups"
                :page-size="groupPageSize"
                size="small"
                show-size-changer
                @change="onGroupPageChange"
                @show-size-change="onGroupSizeChange"
              />
            </div>
          </div>
        </div>

        <!-- 第二级：数据集 -->
        <div class="cascade-level" v-if="selectedGroup">
          <div class="level-header">
            <h4>数据集</h4>
            <div class="search-box">
              <a-input-search
                v-model:value="searchDataset"
                placeholder="搜索数据集"
                style="width: 200px"
                :loading="datasetLoading"
                @search="onSearchDataset"
              />
            </div>
          </div>
          <div class="level-content">
            <div class="item-list">
              <a-spin :spinning="datasetLoading" tip="加载数据集中...">
                <div class="item-container">
                  <template v-if="filteredDatasets.length > 0">
                    <div
                      v-for="dataset in filteredDatasets"
                      :key="dataset.id"
                      class="item"
                      :class="{
                        active: selectedDataset?.id === dataset.id,
                        'has-selected': getDatasetSelectedCount(dataset.id) > 0,
                        'all-selected': isDatasetAllSelected(dataset.id),
                      }"
                      @click="selectDataset(dataset)"
                    >
                      <div class="item-name">
                        {{ dataset.name }}
                        <span
                          v-if="getDatasetSelectedCount(dataset.id) > 0"
                          class="selection-count"
                        >
                          (已选 {{ getDatasetSelectedCount(dataset.id) }})
                        </span>
                      </div>
                      <div class="item-meta">
                        <span>类型: {{ getDatasetAnnoTypeText(dataset.annotateType) }}</span>
                        <span>状态: {{ getDatasetStatusText(dataset.status) }}</span>
                        <span v-if="dataset.fileCount">文件数: {{ dataset.fileCount }}</span>
                        <span v-if="dataset.createTime"
                          >创建时间: {{ formatDate(dataset.createTime) }}</span
                        >
                      </div>
                    </div>
                  </template>
                  <div v-else class="empty-tip">
                    <div class="empty-tip-content">
                      当前数据集组内，暂无算法所需要的{{
                        getAnnotateTypeText()
                      }}的数据集，请使用和算法类型相匹配的数据集。
                      <a
                        v-if="selectedGroup?.id && canVisitGroup(selectedGroup.id)"
                        @click.stop="goToGroupDetail(selectedGroup.id, selectedGroup.name)"
                        class="link-text"
                      >
                        前往数据集组
                      </a>
                    </div>
                  </div>
                </div>
              </a-spin>
            </div>
          </div>
        </div>

        <!-- 第三级：数据集版本 -->
        <div class="cascade-level" v-if="selectedDataset">
          <div class="level-header">
            <h4>数据集版本</h4>
            <div class="search-box">
              <a-input-search
                v-model:value="searchVersion"
                placeholder="搜索版本"
                style="width: 200px"
                :loading="versionLoading"
                @search="onSearchVersion"
              />
            </div>
          </div>
          <div class="level-content">
            <div class="item-list">
              <a-spin :spinning="versionLoading" tip="加载版本中...">
                <div class="item-container">
                  <template v-if="filteredVersions.length > 0">
                    <div
                      v-for="version in filteredVersions"
                      :key="version.id"
                      class="item"
                      :class="{
                        selected: selectedVersions.some((v) => v.id === version.id),
                      }"
                      @click="toggleVersion(version)"
                    >
                      <div class="item-name">
                        {{ version.versionName || version.name || `版本 ${version.id}` }}
                        <span v-if="version.isCurrent" class="current-badge">当前版本</span>
                      </div>
                      <div class="item-desc">
                        {{ (version.versionNote && version.versionNote.trim()) || '暂无描述' }}
                      </div>
                      <div class="item-meta">
                        <span v-if="version.imageCount">图片数: {{ version.imageCount }}</span>
                        <span v-if="version.labelCountMap"
                          >标签种类数: {{ Object.keys(version.labelCountMap).length }}</span
                        >
                        <span v-if="version.createTime">
                          创建时间: {{ formatDate(version.createTime) }}
                        </span>
                      </div>
                    </div>
                  </template>
                  <div v-else class="empty-tip">
                    <div class="empty-tip-content">
                      当前数据集暂未保存发布版本，请先前往数据集管理中保存数据集版本后才能用于训练。
                      <a
                        v-if="selectedDataset?.groupId && selectedDataset?.id && canVisitDataset(selectedDataset.id)"
                        @click.stop="goToGroupDetail(selectedDataset.groupId, selectedGroup?.name)"
                        class="link-text"
                      >
                        前往数据集
                      </a>
                    </div>
                  </div>
                </div>
              </a-spin>
            </div>
            <div class="pagination-wrapper">
              <a-pagination
                v-model:current="versionPage"
                :total="totalVersions"
                :page-size="versionPageSize"
                size="small"
                show-size-changer
                @change="onVersionPageChange"
                @show-size-change="onVersionSizeChange"
              />
            </div>
          </div>
        </div>
      </div>
    </a-card>

    <!-- 选择结果显示 -->
    <a-card
      v-if="selectedVersions.length > 0"
      title="已选择的数据集版本"
      style="margin-bottom: 16px"
    >
      <div class="selected-items">
        <a-tooltip
          v-for="version in selectedVersions"
          :key="version.id"
          placement="top"
          :overlay-style="{ maxWidth: '500px' }"
          :overlay-class-name="'custom-tooltip'"
        >
          <template #title>
            <div class="version-tooltip">
              <div class="tooltip-header">
                <div class="tooltip-title">{{ getVersionDisplayName(version) }}</div>
              </div>
              <div class="tooltip-content">
                <div class="info-section">
                  <div class="info-grid">
                    <div class="info-item">
                      <span class="info-icon">📊</span>
                      <span class="info-label">图片数量</span>
                      <span class="info-value">{{ version.imageCount || 0 }} 张</span>
                    </div>
                    <div class="info-item">
                      <span class="info-icon">💾</span>
                      <span class="info-label">文件大小</span>
                      <span class="info-value">{{
                        formatFileSize(version.totalFileSize || 0)
                      }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-icon">🏷️</span>
                      <span class="info-label">标注类型</span>
                      <span class="info-value">{{
                        getDatasetAnnoTypeText(version.annotateType) || '未知'
                      }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-icon">📅</span>
                      <span class="info-label">创建时间</span>
                      <span class="info-value">{{ formatDate(version.createTime) }}</span>
                    </div>
                  </div>
                </div>
                <div
                  v-if="version.labelCountMap && Object.keys(version.labelCountMap).length > 0"
                  class="labels-section"
                >
                  <div class="section-title">
                    <span class="section-icon">🔖</span>
                    标签统计
                  </div>
                  <div class="labels-container">
                    <div
                      v-for="(count, label) in version.labelCountMap"
                      :key="label"
                      class="label-chip"
                    >
                      <span class="label-name">{{ label }}</span>
                      <span class="label-count">{{ count }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <div class="selected-item" @click="removeVersion(version)">
            {{ getVersionDisplayName(version) }}
            <span class="remove-icon">×</span>
          </div>
        </a-tooltip>
      </div>
    </a-card>

    <!-- 标签筛选 -->
    <a-card v-if="selectedVersions.length > 0" title="标签筛选" style="margin-bottom: 16px">
      <div class="label-filter-section">
        <div class="filter-header">
          <div class="section-title">
            <span class="section-icon">🔖</span>
            <span class="section-text">标签选择</span>
          </div>
          <div class="filter-info">
            <div class="info-stats">
              <div class="stat-item">
                <span class="stat-label">总标签数</span>
                <span class="stat-value">{{ Object.keys(mergedLabelCountMap).length }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">已选择</span>
                <span class="stat-value highlight">{{ selectedLabels.length }}</span>
              </div>
              <div v-if="selectedLabels.length > 0" class="stat-item">
                <span class="stat-label">标签数量</span>
                <span class="stat-value">{{ selectedLabelTotalCount }}</span>
              </div>
            </div>
            <div class="filter-actions">
              <a-button size="small" @click="selectAllLabels" class="action-btn">全选</a-button>
              <a-button size="small" @click="clearSelectedLabels" class="action-btn">清空</a-button>
            </div>
          </div>
        </div>

        <div class="enhanced-labels-container" @dragover="handleContainerDragOver">
          <div
            v-for="(count, label) in mergedLabelCountMap"
            :key="label"
            class="enhanced-label-chip"
            :class="{
              selected: selectedLabels.includes(label),
              'has-case-conflict': hasCaseConflict(label),
              'drag-over-before': dragOverLabel === label && dragOverPosition === 'before',
              'drag-over-after': dragOverLabel === label && dragOverPosition === 'after',
            }"
            @click="toggleLabel(label)"
            draggable="false"
            @dragstart="handleDragStart($event, label)"
            @dragover="handleLabelDragOver($event, label)"
            @dragleave="handleLabelDragLeave($event, label)"
            @drop="handleDrop($event, label)"
          >
            <span v-if="selectedLabels.includes(label)" class="label-order">
              {{ getLabelOrderNumber(label) }}
            </span>
            <span class="label-name">{{ label }}</span>
            <span class="label-count">{{ count }}</span>
            <span v-if="hasCaseConflict(label)" class="case-conflict-icon" title="存在大小写重复"
              >⚠</span
            >
          </div>
        </div>
        <!-- 添加标签顺序说明 -->
        <div class="label-order-info" v-if="selectedLabels.length > 0">
          标签顺序：
          <span v-for="(label, index) in selectedLabels" :key="label" class="label-order-item">
            {{ index + 1 }}. {{ label }}
          </span>
        </div>
      </div>
    </a-card>

    <!-- 数据集划分配置 -->
    <DatasetSplitConfig v-model="splitSize" />
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref, watch } from 'vue';
  import {
    Button as AButton,
    Card as ACard,
    Checkbox as ACheckbox,
    CheckboxGroup as ACheckboxGroup,
    InputSearch as AInputSearch,
    Pagination as APagination,
    Spin as ASpin,
    Tooltip as ATooltip,
  } from 'ant-design-vue';
  import {
    getDatasetsByDatasetGroupId,
    getDatasetsByDatasetGroupIdAndAnnotateType,
    getDatasetVersions,
    getPrivateAndPublicDatasetGroupByPage,
    getPrivateDatasetGroupByPage,
    getPublicDatasetGroupByPage,
    searchPrivateAndPublicDatasetGroupByName,
    searchPrivateDatasetGroupByName,
    searchPublicDatasetGroupByName,
  } from '/@/views/mineai/train/generationCreate/api';
  import DatasetSplitConfig from '/@/views/mineai/train/generationCreate/DatasetSplitConfig.vue';
  import { useGo } from '/@/hooks/web/usePage';
  import { useUserStore } from '/@/store/modules/user';

  // Props定义
  const props = defineProps<{
    annotateType?: number; // 标注类型：102=目标检测，103=语义分割
    annotationFormat?: string; // 标注格式：YOLO, Segment-YOLO, COCO等
  }>();

  // 数据类型定义
  interface DatasetGroup {
    id: string | number;
    name: string;
    description?: string;
    remark?: string;
    createTime?: string;
    createUserId?: number;
  }

  interface Dataset {
    id: string | number;
    name: string;
    remark?: string;
    annotateType?: number;
    status?: number;
    createTime?: string;
    fileCount?: number;
    currentVersionName?: string;
    createUserId?: number;
    groupId?: string | number;
  }

  interface DatasetVersion {
    id: string | number;
    datasetId: string | number;
    annotateType?: number;
    name?: string;
    versionName?: string;
    createTime?: string;
    versionNote?: string;
    isCurrent?: boolean;
    dataConversion?: number;
    isPublic?: number;
    imageCount?: number;
    totalFileSize?: number;
    labelCountMap?: Record<string, number>;
    remark?: string;
    versionNumber?: string;
    fileCount?: number;
    imageCounts?: number;
    dataSize?: string;
    status?: number;
    createUserId?: number;
    currentVersionName?: string;
    format?: string;
  }

  // 导航函数
  const go = useGo();

  const userStore = useUserStore();
  const userData = userStore.getUserInfo;
  const roles = (userData && userData.roles) || [];
  const isAdmin = computed(() => {
    // id=1 是管理员角色, id=89是管理人员角色
    return roles[0] && (roles[0].id === 1 || roles[0].id === 89);
  });

  const canVisitGroup = (groupId: string | number) => {
    if (isAdmin.value) return true;

    const group = allGroups.value.get(groupId);
    const createUserId = group?.createUserId;

    if (!createUserId || !userData?.id) return false;

    return createUserId === userData.id;
  };

  const canVisitDataset = (datasetId: string | number) => {
    if (isAdmin.value) return true;

    const dataset = allDatasets.value.get(datasetId);
    const createUserId = dataset?.createUserId;

    if (!createUserId || !userData?.id) return false;

    return createUserId === userData.id;
  };

  // 跳转到数据集组详情页（权限在模板中通过 canVisitGroup 控制）
  const goToGroupDetail = (groupId: string | number, groupName?: string) => {

    if (groupName) {
      sessionStorage.setItem('GroupName', groupName);
    } else {
      const groupFromMap = allGroups.value.get(groupId);
      if (groupFromMap?.name) {
        sessionStorage.setItem('GroupName', groupFromMap.name);
      }
    }
    go(`/maData/labelGroupPrivateDetails/${groupId}`);
  };

  // 响应式状态
  const searchGroup = ref('');
  const searchDataset = ref('');
  const searchVersion = ref('');
  const privacyFilter = ref<string[]>(['public', 'private']); // 隐私筛选：public, private，默认全选

  const selectedGroup = ref<DatasetGroup | null>(null);
  const selectedDataset = ref<Dataset | null>(null);
  const selectedVersions = ref<DatasetVersion[]>([]);
  const selectedLabels = ref<string[]>([]);

  // 数据集划分配置
  const splitSize = ref([70, 80]);

  // 数据状态
  const datasetGroups = ref<DatasetGroup[]>([]);
  const datasets = ref<Dataset[]>([]);
  const versions = ref<DatasetVersion[]>([]);

  // 维护完整的数据映射关系
  const allDatasets = ref<Map<string | number, Dataset>>(new Map());
  const allGroups = ref<Map<string | number, DatasetGroup>>(new Map());
  const groupDatasetMap = ref<Map<string | number, Set<string | number>>>(new Map());

  // 加载状态
  const groupLoading = ref(false);
  const datasetLoading = ref(false);
  const versionLoading = ref(false);

  // 分页相关 - 数据集组
  const groupPage = ref(1);
  const groupPageSize = ref(10);
  const totalGroups = ref(0);

  // 分页相关 - 数据集版本
  const versionPage = ref(1);
  const versionPageSize = ref(10);
  const totalVersions = ref(0);

  // 处理滑块变化，确保每个集合至少10%
  const handleSliderChange = (value: number[]) => {
    let [first, second] = value;

    // 确保第一个值至少是10，最多是80（这样测试集至少有10%）
    first = Math.max(10, Math.min(80, first));

    // 确保第二个值至少比第一个值大10（测试集至少10%），最多是90（验证集至少10%）
    second = Math.max(first + 10, Math.min(90, second));

    splitSize.value = [first, second];
  };

  // 计算属性
  const filteredDatasets = computed(() => {
    if (!searchDataset.value) return datasets.value;

    return datasets.value.filter(
      (dataset) =>
        dataset.name.toLowerCase().includes(searchDataset.value.toLowerCase()) ||
        (dataset.remark &&
          dataset.remark.toLowerCase().includes(searchDataset.value.toLowerCase())),
    );
  });

  const filteredVersions = computed(() => {
    if (!searchVersion.value) return versions.value;

    return versions.value.filter(
      (version) =>
        (version.versionName &&
          version.versionName.toLowerCase().includes(searchVersion.value.toLowerCase())) ||
        (version.name && version.name.toLowerCase().includes(searchVersion.value.toLowerCase())) ||
        (version.remark &&
          version.remark.toLowerCase().includes(searchVersion.value.toLowerCase())) ||
        (version.versionNote &&
          version.versionNote.toLowerCase().includes(searchVersion.value.toLowerCase())) ||
        (version.currentVersionName &&
          version.currentVersionName.toLowerCase().includes(searchVersion.value.toLowerCase())),
    );
  });

  // 标签相关计算属性
  const mergedLabelCountMap = computed(() => {
    const labelMap: Record<string, number> = {};

    selectedVersions.value.forEach((version) => {
      if (version.labelCountMap) {
        Object.entries(version.labelCountMap).forEach(([label, count]) => {
          const normalizedLabel = label.toLowerCase();
          labelMap[normalizedLabel] = (labelMap[normalizedLabel] || 0) + count;
        });
      }
    });

    return labelMap;
  });

  const totalLabelCount = computed(() => {
    return Object.values(mergedLabelCountMap.value).reduce((sum, count) => sum + count, 0);
  });

  const selectedLabelTotalCount = computed(() => {
    return selectedLabels.value.reduce((sum, label) => {
      return sum + (mergedLabelCountMap.value[label] || 0);
    }, 0);
  });

  // 判断是否需要垂直布局（当三个框都显示时）
  const isVerticalLayout = computed(() => {
    // 当数据集组和数据集都选择了时，就启用垂直布局
    return selectedGroup.value && selectedDataset.value;
  });

  // 检查是否存在大小写冲突
  const hasCaseConflict = (normalizedLabel: string): boolean => {
    const originalLabels: string[] = [];

    selectedVersions.value.forEach((version) => {
      if (version.labelCountMap) {
        Object.keys(version.labelCountMap).forEach((label) => {
          if (label.toLowerCase() === normalizedLabel) {
            originalLabels.push(label);
          }
        });
      }
    });

    return new Set(originalLabels).size > 1;
  };

  // 工具方法
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B';

    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  // API调用方法
  const fetchDatasetGroups = async (
    page = 1,
    pageSize = 10,
    search?: string,
    privacyTypes?: string[],
  ) => {
    try {
      groupLoading.value = true;

      // 确保至少选择一个类型
      const selectedTypes =
        privacyTypes && privacyTypes.length > 0 ? privacyTypes : ['public', 'private'];

      let response;
      const hasSearch = search && search.trim();

      // 根据选择的类型和是否有搜索条件来决定调用哪个接口
      if (selectedTypes.length === 2) {
        // 选择了公开和私有，使用全部接口
        if (hasSearch) {
          response = await searchPrivateAndPublicDatasetGroupByName({
            current: page,
            size: pageSize,
            name: search.trim(),
          });
        } else {
          response = await getPrivateAndPublicDatasetGroupByPage({
            current: page,
            size: pageSize,
          });
        }
      } else if (selectedTypes.includes('public') && !selectedTypes.includes('private')) {
        // 只选择公开
        if (hasSearch) {
          response = await searchPublicDatasetGroupByName({
            current: page,
            size: pageSize,
            name: search.trim(),
          });
        } else {
          response = await getPublicDatasetGroupByPage({
            current: page,
            size: pageSize,
          });
        }
      } else if (selectedTypes.includes('private') && !selectedTypes.includes('public')) {
        // 只选择私有
        if (hasSearch) {
          response = await searchPrivateDatasetGroupByName({
            current: page,
            size: pageSize,
            name: search.trim(),
          });
        } else {
          response = await getPrivateDatasetGroupByPage({
            current: page,
            size: pageSize,
          });
        }
      } else {
        // 默认情况，使用全部接口
        if (hasSearch) {
          response = await searchPrivateAndPublicDatasetGroupByName({
            current: page,
            size: pageSize,
            name: search.trim(),
          });
        } else {
          response = await getPrivateAndPublicDatasetGroupByPage({
            current: page,
            size: pageSize,
          });
        }
      }

      if (response) {
        datasetGroups.value = response.records || [];
        totalGroups.value = response.total || 0;

        datasetGroups.value.forEach((group) => {
          allGroups.value.set(group.id, group);
        });
      } else {
        console.error('获取数据集组失败:', response?.msg);
        datasetGroups.value = [];
        totalGroups.value = 0;
      }
    } catch (error) {
      console.error('获取数据集组失败:', error);
      datasetGroups.value = [];
      totalGroups.value = 0;
    } finally {
      groupLoading.value = false;
    }
  };

  const fetchDatasets = async (groupId: number) => {
    try {
      datasetLoading.value = true;
      let response;

      // 如果提供了annotateType，使用新接口过滤
      if (props.annotateType) {
        response = await getDatasetsByDatasetGroupIdAndAnnotateType(groupId, props.annotateType);
      } else {
        response = await getDatasetsByDatasetGroupId(groupId);
      }

      if (response) {
        const datasetsWithGroupId = (response || []).map((dataset: Dataset) => ({
          ...dataset,
          groupId: groupId,
        }));

        datasets.value = datasetsWithGroupId;

        datasetsWithGroupId.forEach((dataset: Dataset) => {
          allDatasets.value.set(dataset.id, dataset);
        });

        const datasetIds = new Set<string | number>(datasetsWithGroupId.map((d: Dataset) => d.id));
        groupDatasetMap.value.set(groupId, datasetIds);
      } else {
        console.error('获取数据集失败:', response?.msg);
        datasets.value = [];
      }
    } catch (error) {
      console.error('获取数据集失败:', error);
      datasets.value = [];
    } finally {
      datasetLoading.value = false;
    }
  };

  const fetchVersions = async (datasetId: number, page = 1, pageSize = 10) => {
    try {
      versionLoading.value = true;
      const params: any = {
        datasetId,
        page,
        pageSize,
        current: page,
        size: pageSize,
      };

      // 如果提供了annotationFormat，添加到查询参数中
      if (props.annotationFormat) {
        params.format = props.annotationFormat;
      }

      const response = await getDatasetVersions(params);

      if (response && response.result) {
        versions.value = response.result || [];
        totalVersions.value = response.page?.total || 0;
      } else {
        console.error('获取数据集版本失败:', response?.msg);
        versions.value = [];
        totalVersions.value = 0;
      }
    } catch (error) {
      console.error('获取数据集版本失败:', error);
      versions.value = [];
      totalVersions.value = 0;
    } finally {
      versionLoading.value = false;
    }
  };

  // 交互方法
  const selectGroup = async (group: DatasetGroup) => {
    selectedGroup.value = group;
    selectedDataset.value = null;
    datasets.value = [];
    versions.value = [];
    searchDataset.value = '';
    searchVersion.value = '';

    await fetchDatasets(Number(group.id));
  };

  const selectDataset = async (dataset: Dataset) => {
    selectedDataset.value = dataset;
    versions.value = [];
    searchVersion.value = '';
    versionPage.value = 1;

    await fetchVersions(Number(dataset.id), versionPage.value, versionPageSize.value);
  };

  const toggleVersion = (version: DatasetVersion) => {
    const index = selectedVersions.value.findIndex((v) => v.id === version.id);
    if (index > -1) {
      selectedVersions.value.splice(index, 1);
    } else {
      selectedVersions.value = selectedVersions.value.filter(
        (v) => v.datasetId !== version.datasetId,
      );
      selectedVersions.value.push(version);
    }
  };

  const removeVersion = (version: DatasetVersion) => {
    const index = selectedVersions.value.findIndex((v) => v.id === version.id);
    if (index > -1) {
      selectedVersions.value.splice(index, 1);
    }
  };

  // 标签筛选方法
  const toggleLabel = (label: string) => {
    const index = selectedLabels.value.indexOf(label);
    if (index > -1) {
      selectedLabels.value.splice(index, 1);
    } else {
      selectedLabels.value.push(label);
    }
  };

  // 添加获取标签顺序号的方法
  const getLabelOrderNumber = (label: string) => {
    const index = selectedLabels.value.indexOf(label);
    return index > -1 ? index + 1 : '';
  };

  // 添加拖拽相关的响应式变量
  const dragOverLabel = ref<string | null>(null);
  const dragOverPosition = ref<'before' | 'after' | null>(null);

  // 添加拖拽相关方法
  let dragSourceLabel = '';

  const handleDragStart = (event: DragEvent, label: string) => {
    dragSourceLabel = label;
    event.dataTransfer!.effectAllowed = 'move';

    // 添加自定义拖拽图像
    const dragElement = event.target as HTMLElement;
    event.dataTransfer!.setDragImage(dragElement, 0, 0);
  };

  const handleContainerDragOver = (event: DragEvent) => {
    event.preventDefault();
  };

  const handleLabelDragOver = (event: DragEvent, label: string) => {
    event.preventDefault();
    const target = event.target as HTMLElement;
    const rect = target.getBoundingClientRect();
    const midpoint = (rect.left + rect.right) / 2;

    dragOverLabel.value = label;
    dragOverPosition.value = event.clientX <= midpoint ? 'before' : 'after';
  };

  const handleLabelDragLeave = (_event: DragEvent, label: string) => {
    // 只有当真正离开元素时才清除状态
    if (dragOverLabel.value === label) {
      dragOverLabel.value = null;
      dragOverPosition.value = null;
    }
  };

  const handleDrop = (event: DragEvent, targetLabel: string) => {
    event.preventDefault();

    // 清除拖拽状态
    dragOverLabel.value = null;
    dragOverPosition.value = null;

    if (dragSourceLabel === targetLabel) return;

    const sourceIndex = selectedLabels.value.indexOf(dragSourceLabel);
    const targetIndex = selectedLabels.value.indexOf(targetLabel);

    if (sourceIndex > -1 && targetIndex > -1) {
      // 交换两个标签的位置
      [selectedLabels.value[sourceIndex], selectedLabels.value[targetIndex]] = [
        selectedLabels.value[targetIndex],
        selectedLabels.value[sourceIndex],
      ];
    }
  };

  const removeSelectedLabel = (label: string) => {
    const index = selectedLabels.value.indexOf(label);
    if (index > -1) {
      selectedLabels.value.splice(index, 1);
    }
  };

  const selectAllLabels = () => {
    selectedLabels.value = Object.keys(mergedLabelCountMap.value);
  };

  const clearSelectedLabels = () => {
    selectedLabels.value = [];
  };

  // 搜索方法
  const onSearchGroup = async () => {
    groupPage.value = 1;
    await fetchDatasetGroups(
      groupPage.value,
      groupPageSize.value,
      searchGroup.value,
      privacyFilter.value,
    );
  };

  // 隐私筛选变化处理
  const onPrivacyFilterChange = async (checkedValues: string[]) => {
    // 确保至少选择一个
    if (checkedValues.length === 0) {
      privacyFilter.value = ['public', 'private'];
      return;
    }

    groupPage.value = 1;
    await fetchDatasetGroups(
      groupPage.value,
      groupPageSize.value,
      searchGroup.value,
      privacyFilter.value,
    );
  };

  const onSearchDataset = () => {
    // 数据集搜索使用前端过滤
  };

  const onSearchVersion = () => {
    // 版本搜索使用前端过滤
  };

  // 分页方法
  const onGroupPageChange = async (page: number) => {
    groupPage.value = page;
    await fetchDatasetGroups(page, groupPageSize.value, searchGroup.value, privacyFilter.value);
  };

  const onGroupSizeChange = async (_current: number, size: number) => {
    groupPageSize.value = size;
    groupPage.value = 1;
    await fetchDatasetGroups(1, size, searchGroup.value, privacyFilter.value);
  };

  const onVersionPageChange = async (page: number) => {
    if (!selectedDataset.value) return;
    versionPage.value = page;
    await fetchVersions(Number(selectedDataset.value.id), page, versionPageSize.value);
  };

  const onVersionSizeChange = async (_current: number, size: number) => {
    if (!selectedDataset.value) return;
    versionPageSize.value = size;
    versionPage.value = 1;
    await fetchVersions(Number(selectedDataset.value.id), 1, size);
  };

  // 统计方法
  const getGroupSelectedCount = (groupId: string | number) => {
    const datasetIds = groupDatasetMap.value.get(groupId);
    if (!datasetIds || datasetIds.size === 0) {
      return 0;
    }

    return selectedVersions.value.filter((v) => datasetIds.has(v.datasetId)).length;
  };

  const getDatasetSelectedCount = (datasetId: string | number) => {
    return selectedVersions.value.filter((v) => v.datasetId === datasetId).length;
  };

  const isGroupAllSelected = (groupId: string | number) => {
    const datasetIds = groupDatasetMap.value.get(groupId);
    if (!datasetIds || datasetIds.size === 0) {
      return false;
    }

    const selectedDatasetIds = new Set(selectedVersions.value.map((v) => v.datasetId));

    for (const datasetId of datasetIds) {
      if (!selectedDatasetIds.has(datasetId)) {
        return false;
      }
    }

    return true;
  };

  const isDatasetAllSelected = (datasetId: string | number) => {
    return selectedVersions.value.some((v) => v.datasetId === datasetId);
  };

  const getVersionDisplayName = (version: DatasetVersion) => {
    const dataset = allDatasets.value.get(version.datasetId);
    const group = allGroups.value.get(dataset?.groupId || '');
    const versionDisplay = version.versionName || version.name || `版本 ${version.id}`;
    return `${group?.name || '未知组'} / ${dataset?.name || '未知数据集'} / ${versionDisplay}`;
  };

  // 工具方法
  const getDatasetAnnoTypeText = (annoType?: number) => {
    const typeMap: Record<number, string> = {
      102: '目标检测',
      103: '目标分割',
    };
    return annoType ? typeMap[annoType] || '未知' : '未知';
  };

  const getDatasetStatusText = (status?: number) => {
    const statusMap: Record<number, string> = {
      101: '未标注',
      102: '标注中',
      105: '已标注',
      302: '抽帧中',
      401: '数据增强中',
      403: '导入中',
      103: '自动标注中',
    };
    return status ? statusMap[status] || '未知' : '未知';
  };

  // 获取标注类型文本（用于空状态提示）
  const getAnnotateTypeText = () => {
    if (props.annotateType === 102) {
      return '目标检测';
    } else if (props.annotateType === 103) {
      return '目标分割';
    }
    return '目标检测/目标分割';
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '';
    try {
      return new Date(dateStr).toLocaleDateString();
    } catch {
      return dateStr;
    }
  };

  // 监听搜索框变化
  watch(searchGroup, (newVal) => {
    if (!newVal) {
      fetchDatasetGroups(groupPage.value, groupPageSize.value, '', privacyFilter.value);
    }
  });

  // 监听选中的版本变化，自动清空不相关的标签选择
  watch(
    selectedVersions,
    () => {
      // 清空之前选择的标签，因为标签列表可能已经改变
      selectedLabels.value = [];
    },
    { deep: true },
  );

  // 组件挂载时初始化数据
  onMounted(async () => {
    await fetchDatasetGroups();
  });

  // 暴露选中的版本数据和标签数据给父组件使用
  defineExpose({
    selectedVersions,
    selectedLabels,
    mergedLabelCountMap,
    splitSize,
    clearSelection: () => {
      selectedVersions.value = [];
      selectedLabels.value = [];
    },
    getSelectedVersionIds: () => selectedVersions.value.map((v) => v.id),
    getSelectedLabels: () => selectedLabels.value,
    // 添加获取标签顺序的方法
    getLabelOrder: () => [...selectedLabels.value],
    getSplitSize: () => splitSize.value,
  });
</script>

<style scoped lang="less">
  .cascade-panel {
    display: flex;
    gap: 24px;
    height: 400px;

    .cascade-level {
      flex: 1;
      display: flex;
      flex-direction: column;
      border: 1px solid #444;
      border-radius: 8px;
      overflow: hidden;
      background: linear-gradient(135deg, rgba(40, 40, 50, 0.4) 0%, rgba(30, 30, 40, 0.3) 100%);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

      .level-header {
        padding: 16px 20px;
        background: linear-gradient(135deg, rgba(30, 30, 40, 0.8) 0%, rgba(25, 25, 35, 0.6) 100%);
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        display: flex;
        justify-content: space-between;
        align-items: center;

        h4 {
          margin: 0;
          font-size: 16px;
          font-weight: 600;
          color: #e0e0e0;
          text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
        }

        .search-controls {
          display: flex;
          align-items: center;
          gap: 12px;
          flex-shrink: 0;
          min-width: 0; // 允许flex子项收缩
          transition: all 0.3s ease; // 添加过渡动画

          // 垂直布局样式
          &.vertical-layout {
            flex-direction: column;
            align-items: stretch;
            gap: 8px;

            .privacy-filter {
              .ant-checkbox-group {
                justify-content: center;
                gap: 16px;
              }
            }

            .search-box {
              width: 100%;
              max-width: none;
              min-width: auto;

              .ant-input-search {
                width: 100%;
              }
            }
          }

          .privacy-filter {
            flex-shrink: 1; // 允许收缩
            min-width: 0;

            .ant-checkbox-group {
              display: flex;
              flex-wrap: nowrap;
              gap: 8px;

              .ant-checkbox-wrapper {
                color: #e0e0e0;
                font-size: 12px;
                white-space: nowrap;
                flex-shrink: 0;

                &:hover {
                  color: #4096ff;
                }

                .ant-checkbox {
                  .ant-checkbox-inner {
                    border-color: rgba(255, 255, 255, 0.3);
                    background-color: transparent;
                  }

                  &.ant-checkbox-checked .ant-checkbox-inner {
                    border-color: #4096ff;
                    background-color: #4096ff;
                  }
                }
              }
            }
          }

          .search-box {
            flex-shrink: 0;
            min-width: 180px; // 设置最小宽度
            max-width: 250px; // 设置最大宽度
            flex: 1; // 允许搜索框占据剩余空间

            .ant-input-search {
              width: 100%;
            }
          }
        }
      }

      .level-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: hidden;

        .item-list {
          flex: 1;
          overflow-y: scroll;
          position: relative;

          .item-container {
            height: 100%;
            overflow-y: scroll;
            padding: 12px;
            display: flex;
            flex-direction: column;

            .empty-tip {
              flex: 1;
              display: flex;
              align-items: center;
              justify-content: center;
              min-height: 200px;

              .empty-tip-content {
                text-align: center;
                color: #888;
                font-size: 14px;
                line-height: 1.6;
                padding: 20px;
                background: rgba(255, 255, 255, 0.03);
                border-radius: 8px;
                border: 1px dashed rgba(255, 255, 255, 0.1);

                .link-text {
                  color: #4096ff;
                  text-decoration: none;
                  margin-left: 8px;
                  cursor: pointer;
                  transition: all 0.2s ease;

                  &:hover {
                    color: #1677ff;
                    text-decoration: underline;
                  }
                }
              }
            }

            .item {
              padding: 14px 16px;
              margin-bottom: 10px;
              border: 1px solid #555;
              border-radius: 8px;
              cursor: pointer;
              transition: all 0.3s ease;
              background: linear-gradient(
                135deg,
                rgba(50, 50, 60, 0.4) 0%,
                rgba(45, 45, 55, 0.3) 100%
              );
              position: relative;
              box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

              &:hover {
                border-color: #4096ff;
                background: linear-gradient(
                  135deg,
                  rgba(64, 150, 255, 0.15) 0%,
                  rgba(64, 150, 255, 0.1) 100%
                );
                transform: translateY(-2px);
                box-shadow: 0 4px 16px rgba(64, 150, 255, 0.2);
              }

              &.active {
                border-color: #4096ff;
                background: linear-gradient(
                  135deg,
                  rgba(64, 150, 255, 0.2) 0%,
                  rgba(64, 150, 255, 0.15) 100%
                );
                box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.3);
              }

              &.has-selected {
                border-color: #52c41a;
                background: linear-gradient(
                  135deg,
                  rgba(82, 196, 26, 0.15) 0%,
                  rgba(82, 196, 26, 0.1) 100%
                );
              }

              &.active.has-selected {
                border-color: #4096ff;
                background: linear-gradient(
                  135deg,
                  rgba(64, 150, 255, 0.25) 0%,
                  rgba(82, 196, 26, 0.15) 100%
                );
                box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.4),
                  inset 0 0 0 1px rgba(82, 196, 26, 0.3);

                &::before {
                  content: '';
                  position: absolute;
                  top: 6px;
                  right: 6px;
                  width: 10px;
                  height: 10px;
                  background: radial-gradient(circle, #4096ff 0%, rgba(64, 150, 255, 0.8) 100%);
                  border-radius: 50%;
                  box-shadow: 0 0 6px rgba(64, 150, 255, 0.8);
                }
              }

              &.all-selected {
                border-color: #52c41a;
                background: linear-gradient(
                  135deg,
                  rgba(82, 196, 26, 0.25) 0%,
                  rgba(82, 196, 26, 0.2) 100%
                );
              }

              &.selected {
                border-color: #52c41a;
                background: linear-gradient(
                  135deg,
                  rgba(82, 196, 26, 0.25) 0%,
                  rgba(82, 196, 26, 0.2) 100%
                );
                box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.3);
              }

              .item-name {
                font-size: 14px;
                font-weight: 600;
                color: #e0e0e0;
                margin-bottom: 6px;
                display: flex;
                align-items: center;
                gap: 8px;

                .selection-count {
                  font-size: 12px;
                  color: #52c41a;
                  font-weight: normal;
                  background: rgba(82, 196, 26, 0.2);
                  padding: 2px 6px;
                  border-radius: 4px;
                  border: 1px solid rgba(82, 196, 26, 0.3);
                }

                .current-badge {
                  font-size: 10px;
                  color: #52c41a;
                  background: linear-gradient(
                    135deg,
                    rgba(82, 196, 26, 0.2) 0%,
                    rgba(82, 196, 26, 0.1) 100%
                  );
                  padding: 3px 8px;
                  border-radius: 4px;
                  border: 1px solid #52c41a;
                  font-weight: normal;
                  box-shadow: 0 1px 3px rgba(82, 196, 26, 0.2);
                }
              }

              .item-desc {
                font-size: 12px;
                color: #b0b0b0;
                line-height: 1.5;
                margin-bottom: 6px;
              }

              .item-meta {
                font-size: 11px;
                color: #888;

                span {
                  margin-right: 12px;
                }
              }
            }
          }
        }

        .pagination-wrapper {
          padding: 16px 20px;
          border-top: 1px solid rgba(255, 255, 255, 0.1);
          background: linear-gradient(135deg, rgba(30, 30, 40, 0.8) 0%, rgba(25, 25, 35, 0.6) 100%);
          display: flex;
          justify-content: center;
        }
      }
    }
  }

  .selected-items {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;

    .selected-item {
      padding: 10px 14px;
      background: linear-gradient(135deg, rgba(82, 196, 26, 0.25) 0%, rgba(82, 196, 26, 0.2) 100%);
      border: 1px solid #52c41a;
      border-radius: 6px;
      font-size: 13px;
      color: #e0e0e0;
      cursor: pointer;
      transition: all 0.3s ease;
      max-width: 300px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      box-shadow: 0 2px 8px rgba(82, 196, 26, 0.2);

      &:hover {
        background: linear-gradient(
          135deg,
          rgba(255, 77, 79, 0.25) 0%,
          rgba(255, 77, 79, 0.2) 100%
        );
        border-color: #ff4d4f;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);

        .remove-icon {
          color: #ff4d4f;
        }
      }

      .remove-icon {
        margin-left: 8px;
        font-weight: bold;
        color: #a0a0a0;
        transition: color 0.2s ease;
      }
    }
  }

  // 修复 Tooltip 背景问题
  :global(.custom-tooltip .ant-tooltip-inner) {
    padding: 0 !important;
    background: transparent !important;
    box-shadow: none !important;
    border: none !important;
  }

  :global(.custom-tooltip .ant-tooltip-arrow) {
    display: none !important;
  }

  .version-tooltip {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
    overflow: hidden;
    min-width: 380px;
    border: 1px solid rgba(255, 255, 255, 0.1);

    .tooltip-header {
      background: linear-gradient(135deg, #4096ff 0%, #1677ff 100%);
      padding: 16px 20px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);

      .tooltip-title {
        color: #ffffff;
        font-size: 15px;
        font-weight: 600;
        line-height: 1.4;
        margin: 0;
        text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
      }
    }

    .tooltip-content {
      padding: 20px;

      .info-section {
        margin-bottom: 16px;

        .info-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 12px;

          .info-item {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 10px 12px;
            background: rgba(255, 255, 255, 0.05);
            border-radius: 8px;
            border: 1px solid rgba(255, 255, 255, 0.1);
            transition: all 0.2s ease;

            &:hover {
              background: rgba(255, 255, 255, 0.08);
              border-color: rgba(64, 150, 255, 0.3);
            }

            .info-icon {
              font-size: 16px;
              flex-shrink: 0;
            }

            .info-label {
              color: #b3b3b3;
              font-size: 12px;
              margin-right: auto;
              min-width: 0;
              flex: 1;
            }

            .info-value {
              color: #ffffff;
              font-size: 13px;
              font-weight: 500;
              flex-shrink: 0;
            }
          }
        }
      }

      .labels-section {
        border-top: 1px solid rgba(255, 255, 255, 0.1);
        padding-top: 16px;

        .section-title {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 12px;
          color: #ffffff;
          font-size: 14px;
          font-weight: 600;

          .section-icon {
            font-size: 16px;
          }
        }

        .labels-container {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;
          max-height: 120px;
          overflow-y: auto;

          .label-chip {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 6px 10px;
            background: linear-gradient(
              135deg,
              rgba(64, 150, 255, 0.2) 0%,
              rgba(64, 150, 255, 0.1) 100%
            );
            border: 1px solid rgba(64, 150, 255, 0.3);
            border-radius: 16px;
            font-size: 12px;
            transition: all 0.2s ease;

            &:hover {
              background: linear-gradient(
                135deg,
                rgba(64, 150, 255, 0.3) 0%,
                rgba(64, 150, 255, 0.2) 100%
              );
              border-color: rgba(64, 150, 255, 0.5);
              transform: translateY(-1px);
            }

            .label-name {
              color: #ffffff;
              font-weight: 500;
            }

            .label-count {
              color: #4096ff;
              font-weight: 600;
              background: rgba(64, 150, 255, 0.2);
              padding: 2px 6px;
              border-radius: 8px;
              font-size: 11px;
              min-width: 24px;
              text-align: center;
            }
          }
        }
      }
    }
  }

  .label-filter-section {
    .filter-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      padding: 20px;
      background: linear-gradient(135deg, rgba(40, 40, 50, 0.4) 0%, rgba(30, 30, 40, 0.3) 100%);
      border-radius: 12px;
      border: 1px solid rgba(255, 255, 255, 0.1);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);

      .section-title {
        display: flex;
        align-items: center;
        gap: 10px;
        color: #ffffff;
        font-size: 16px;
        font-weight: 600;

        .section-icon {
          font-size: 18px;
        }

        .section-text {
          text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
        }
      }

      .filter-info {
        display: flex;
        align-items: center;
        gap: 24px;

        .info-stats {
          display: flex;
          gap: 16px;

          .stat-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 8px 12px;
            background: rgba(255, 255, 255, 0.05);
            border-radius: 8px;
            border: 1px solid rgba(255, 255, 255, 0.1);
            min-width: 80px;

            .stat-label {
              font-size: 11px;
              color: #b3b3b3;
              margin-bottom: 2px;
            }

            .stat-value {
              font-size: 14px;
              color: #ffffff;
              font-weight: 600;

              &.highlight {
                color: #4096ff;
              }
            }
          }
        }

        .filter-actions {
          display: flex;
          gap: 8px;

          .action-btn {
            background: linear-gradient(
              135deg,
              rgba(64, 150, 255, 0.2) 0%,
              rgba(64, 150, 255, 0.1) 100%
            );
            border: 1px solid rgba(64, 150, 255, 0.3);
            color: #4096ff;
            transition: all 0.2s ease;

            &:hover {
              background: linear-gradient(
                135deg,
                rgba(64, 150, 255, 0.3) 0%,
                rgba(64, 150, 255, 0.2) 100%
              );
              border-color: rgba(64, 150, 255, 0.5);
              color: #ffffff;
              transform: translateY(-1px);
            }
          }
        }
      }
    }

    .enhanced-labels-container {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin-bottom: 16px;
      min-height: 50px;
      padding: 5px;
      border-radius: 8px;
      transition: background-color 0.2s;

      .enhanced-label-chip {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 10px 14px;
        background: linear-gradient(135deg, rgba(60, 60, 75, 0.4) 0%, rgba(50, 50, 65, 0.3) 100%);
        border: 1px solid rgba(255, 255, 255, 0.15);
        border-radius: 18px;
        cursor: pointer;
        transition: all 0.3s ease;
        position: relative;
        min-width: 120px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

        &:hover {
          border-color: rgba(64, 150, 255, 0.4);
          background: linear-gradient(
            135deg,
            rgba(64, 150, 255, 0.15) 0%,
            rgba(64, 150, 255, 0.1) 100%
          );
          transform: translateY(-2px);
          box-shadow: 0 4px 16px rgba(64, 150, 255, 0.2);
        }

        &.selected {
          border-color: rgba(64, 150, 255, 0.6);
          background: linear-gradient(
            135deg,
            rgba(64, 150, 255, 0.25) 0%,
            rgba(64, 150, 255, 0.2) 100%
          );
          box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.3);

          .label-name {
            color: #ffffff;
          }

          .label-count {
            background: linear-gradient(
              135deg,
              rgba(64, 150, 255, 0.4) 0%,
              rgba(64, 150, 255, 0.3) 100%
            );
            color: #ffffff;
            border-color: rgba(64, 150, 255, 0.5);
          }
        }

        &.has-case-conflict {
          &::after {
            content: '';
            position: absolute;
            top: 4px;
            right: 4px;
            width: 8px;
            height: 8px;
            background: radial-gradient(circle, #faad14 0%, rgba(250, 173, 20, 0.8) 100%);
            border-radius: 50%;
            box-shadow: 0 0 4px rgba(250, 173, 20, 0.6);
          }
        }

        /* 拖拽悬停效果 */

        &.drag-over-before {
          border-left: 3px solid #4caf50 !important;
          box-shadow: -3px 0 0 0 #4caf50;
        }

        &.drag-over-after {
          border-right: 3px solid #4caf50 !important;
          box-shadow: 3px 0 0 0 #4caf50;
        }

        .label-name {
          flex: 1;
          font-size: 13px;
          color: #e0e0e0;
          font-weight: 500;
        }

        .label-count {
          font-size: 12px;
          color: #4096ff;
          font-weight: 600;
          background: linear-gradient(
            135deg,
            rgba(64, 150, 255, 0.2) 0%,
            rgba(64, 150, 255, 0.1) 100%
          );
          padding: 4px 8px;
          border-radius: 10px;
          border: 1px solid rgba(64, 150, 255, 0.3);
          min-width: 32px;
          text-align: center;
          transition: all 0.2s ease;
        }

        .case-conflict-icon {
          margin-left: 4px;
          color: #faad14;
          font-size: 12px;
          filter: drop-shadow(0 0 2px rgba(250, 173, 20, 0.6));
        }

        .label-order {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 20px;
          height: 20px;
          border-radius: 50%;
          background-color: #1890ff;
          color: white;
          font-size: 12px;
          font-weight: bold;
          margin-right: 6px;
        }

        .label-name {
          flex: 1;
          font-size: 13px;
          color: #e0e0e0;
          font-weight: 500;
        }
      }
    }
  }

  // 响应式设计
  @media (max-width: 1200px) {
    .cascade-panel {
      flex-direction: column;
      height: auto;

      .cascade-level {
        height: 350px;
      }
    }

    .filter-header,
    .config-header {
      flex-direction: column;
      gap: 16px;
      align-items: flex-start !important;

      .filter-info,
      .config-info {
        width: 100%;
        justify-content: space-between;

        .info-stats,
        .split-stats {
          flex-wrap: wrap;
        }
      }
    }
  }

  // 针对搜索控件的响应式处理
  @media (max-width: 800px) {
    .search-controls {
      flex-direction: column;
      align-items: stretch;
      gap: 8px;

      .privacy-filter {
        .ant-checkbox-group {
          justify-content: center;
        }
      }

      .search-box {
        width: 100%;

        .ant-input-search {
          width: 100% !important;
        }
      }
    }
  }

  // 当空间非常紧张时的处理
  @media (max-width: 600px) {
    .search-controls {
      .privacy-filter {
        .ant-checkbox-group {
          flex-direction: column;
          align-items: flex-start;
          gap: 4px;
        }
      }
    }
  }

  // 深色主题下的滚动条样式
  .item-container::-webkit-scrollbar,
  .labels-container::-webkit-scrollbar,
  .enhanced-labels-container::-webkit-scrollbar {
    width: 6px;
  }

  .item-container::-webkit-scrollbar-track,
  .labels-container::-webkit-scrollbar-track,
  .enhanced-labels-container::-webkit-scrollbar-track {
    background: rgba(50, 50, 60, 0.3);
    border-radius: 3px;
  }

  .item-container::-webkit-scrollbar-thumb,
  .labels-container::-webkit-scrollbar-thumb,
  .enhanced-labels-container::-webkit-scrollbar-thumb {
    background: linear-gradient(135deg, rgba(100, 100, 120, 0.6) 0%, rgba(80, 80, 100, 0.4) 100%);
    border-radius: 3px;

    &:hover {
      background: linear-gradient(
        135deg,
        rgba(120, 120, 140, 0.8) 0%,
        rgba(100, 100, 120, 0.6) 100%
      );
    }
  }

  // Spin 组件在深色主题下的样式
  :deep(.ant-spin-container) {
    min-height: 200px;
  }

  :deep(.ant-spin-tip) {
    color: #e0e0e0;
  }

  // 分页组件样式优化
  :deep(.ant-pagination) {
    .ant-pagination-item {
      background: rgba(255, 255, 255, 0.05);
      border-color: rgba(255, 255, 255, 0.1);

      &:hover {
        border-color: #4096ff;
      }

      &.ant-pagination-item-active {
        background: #4096ff;
        border-color: #4096ff;
      }
    }

    .ant-pagination-prev,
    .ant-pagination-next {
      background: rgba(255, 255, 255, 0.05);
      border-color: rgba(255, 255, 255, 0.1);

      &:hover {
        border-color: #4096ff;
      }
    }
  }

  .label-order-info {
    margin-top: 16px;
    padding: 12px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.1);

    .label-order-item {
      display: inline-block;
      margin-right: 12px;
      padding: 4px 8px;
      background: rgba(24, 144, 255, 0.2);
      border-radius: 4px;
      font-size: 12px;
    }
  }
</style>
