<!-- PublicDatasetCascade.vue -->
<template>
  <div class="public-dataset-cascade">
    <!-- 级联选择面板 -->
    <div class="dataset-cascade-panel">
      <!-- 第一级：数据集组 -->
      <div class="cascade-level">
        <div class="level-header">
          <h4>数据集组</h4>
          <div class="search-controls" :class="{ 'vertical-layout': isVerticalLayout }">
            <div class="privacy-filter">
              <ACheckboxGroup v-model:value="privacyFilter" @change="onPrivacyFilterChange">
                <ACheckbox value="public">公开</ACheckbox>
                <ACheckbox value="private">私有</ACheckbox>
              </ACheckboxGroup>
            </div>
            <div class="search-box">
              <AInputSearch
                v-model:value="searchGroup"
                placeholder="搜索数据集组"
                :loading="groupLoading"
                @search="onSearchGroup"
              />
            </div>
          </div>
        </div>
        <div class="level-content">
          <ASpin :spinning="groupLoading" tip="加载数据集组中...">
            <div class="item-container">
              <template v-if="datasetGroups.length > 0">
                <div
                  v-for="group in datasetGroups"
                  :key="group.id"
                  class="cascade-item"
                  :class="{
                    active: selectedGroup?.id === group.id,
                    'has-selected': getGroupSelectedCount(group.id) > 0,
                  }"
                  @click="selectGroup(group)"
                >
                  <div class="item-name">
                    {{ group.name }}
                    <span v-if="getGroupSelectedCount(group.id) > 0" class="selection-count">
                      (已选 {{ getGroupSelectedCount(group.id) }})
                    </span>
                    <span v-if="isBoundGroup(group.id)" class="bound-indicator">包含已绑定</span>
                  </div>
                  <div class="item-desc">
                    {{ group.description || group.remark || '暂无描述' }}
                  </div>
                  <div class="item-meta">
                    <span v-if="group.createTime">{{ formatDate(group.createTime) }}</span>
                  </div>
                </div>
              </template>
              <div v-else class="empty-wrapper">
                <AEmpty :image="AEmpty.PRESENTED_IMAGE_SIMPLE" description="暂无公开数据集组" />
              </div>
            </div>
          </ASpin>
          <div class="pagination-wrapper" v-if="totalGroups > groupPageSize">
            <APagination
              v-model:current="groupPage"
              :total="totalGroups"
              :page-size="groupPageSize"
              size="small"
              @change="onGroupPageChange"
            />
          </div>
        </div>
      </div>

      <!-- 第二级：数据集 -->
      <div class="cascade-level" v-if="selectedGroup">
        <div class="level-header">
          <h4>数据集</h4>
          <AInputSearch
            v-model:value="searchDataset"
            placeholder="搜索数据集"
            style="width: 200px"
            :loading="datasetLoading"
            @search="onSearchDataset"
          />
        </div>
        <div class="level-content">
          <ASpin :spinning="datasetLoading" tip="加载数据集中...">
            <div class="item-container">
              <template v-if="filteredDatasets.length > 0">
                <div
                  v-for="dataset in filteredDatasets"
                  :key="dataset.id"
                  class="cascade-item"
                  :class="{
                    active: selectedDataset?.id === dataset.id,
                    'has-selected': getDatasetSelectedCount(dataset.id) > 0,
                  }"
                  @click="selectDataset(dataset)"
                >
                  <div class="item-name">
                    {{ dataset.name }}
                    <span v-if="getDatasetSelectedCount(dataset.id) > 0" class="selection-count">
                      (已选 {{ getDatasetSelectedCount(dataset.id) }})
                    </span>
                    <span v-if="isBoundDataset(dataset.id)" class="bound-indicator"
                      >包含已绑定版本</span
                    >
                  </div>
                  <div class="item-desc">{{ dataset.remark || '暂无描述' }}</div>
                  <div class="item-meta">
                    <span>类型: {{ getDatasetTypeText(dataset.annotateType) }}</span>
                    <span>状态: {{ getDatasetStatusText(dataset.status) }}</span>
                    <span v-if="dataset.createTime">{{ formatDate(dataset.createTime) }}</span>
                  </div>
                </div>
              </template>
              <div v-else class="empty-tip">
                <div class="empty-tip-content">
                  当前数据集组内，暂无算法所需要的{{
                    getAnnotateTypeTextForTip()
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
          </ASpin>
        </div>
      </div>

      <!-- 第三级：数据集版本 -->
      <div class="cascade-level" v-if="selectedDataset">
        <div class="level-header">
          <h4>数据集版本</h4>
          <AInputSearch
            v-model:value="searchVersion"
            placeholder="搜索版本"
            style="width: 200px"
            :loading="versionLoading"
            @search="onSearchVersion"
          />
        </div>
        <div class="level-content">
          <ASpin :spinning="versionLoading" tip="加载版本中...">
            <div class="item-container">
              <template v-if="filteredVersions.length > 0">
                <div
                  v-for="version in filteredVersions"
                  :key="version.id"
                  class="cascade-item"
                  :class="{
                    selected: isVersionInTempSelection(version.id),
                    'originally-bound': isBoundVersion(version.id),
                  }"
                  @click="toggleVersionSelection(version)"
                >
                  <div class="item-name">
                    {{ version.versionName || version.name || `版本 ${version.id}` }}
                    <span v-if="version.isCurrent" class="current-badge">当前版本</span>
                    <span v-if="isBoundVersion(version.id)" class="bound-indicator">原已绑定</span>
                    <span
                      v-if="isVersionInTempSelection(version.id)"
                      class="temp-selected-indicator"
                      >已选中</span
                    >
                  </div>
                  <div class="item-desc">
                    {{ version.versionNote || version.remark || '暂无描述' }}
                  </div>
                  <div class="item-meta">
                    <span v-if="version.imageCount">图片数: {{ version.imageCount }}</span>
                    <span v-if="version.totalFileSize">
                      标签种类数: {{ Object.keys(version.labelCountMap).length }}
                    </span>
                    <span v-if="version.createTime">{{ formatDate(version.createTime) }}</span>
                  </div>
                </div>
              </template>
              <div v-else class="empty-tip">
                <div class="empty-tip-content">
                  当前数据集暂未保存发布版本，请先前往数据集管理中保存数据集版本后才能用于训练。
                  <a
                    v-if="
                      selectedDataset?.groupId &&
                      selectedDataset?.id &&
                      canVisitDataset(selectedDataset.id)
                    "
                    @click.stop="goToGroupDetail(selectedDataset.groupId, selectedGroup?.name)"
                    class="link-text"
                  >
                    前往数据集
                  </a>
                </div>
              </div>
            </div>
          </ASpin>
          <div class="pagination-wrapper" v-if="totalVersions > versionPageSize">
            <APagination
              v-model:current="versionPage"
              :total="totalVersions"
              :page-size="versionPageSize"
              size="small"
              @change="onVersionPageChange"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 当前选择结果显示 -->
    <ACard
      v-if="finalSelectedVersions.length > 0"
      title="当前选择的数据集版本"
      style="margin-top: 20px"
    >
      <div class="selected-items">
        <ATooltip
          v-for="version in finalSelectedVersions"
          :key="`temp-${version.id}`"
          placement="top"
          :overlay-style="{ maxWidth: '450px' }"
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
                      <span class="info-value"
                        >{{ version.importableImageCount || version.imageCount }} 张</span
                      >
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
                        getAnnotateTypeText(version.annotateType)
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
          <div
            class="selected-item"
            :class="{
              'originally-bound': isBoundVersion(version.id),
              'newly-added': !isBoundVersion(version.id),
            }"
            @click="removeVersionFromTempSelection(version)"
          >
            {{ getVersionDisplayName(version) }}
            <span class="status-badge">
              <span v-if="isBoundVersion(version.id)" class="bound-badge">原绑定</span>
              <span v-else class="new-badge">新增</span>
            </span>
            <span class="remove-icon">×</span>
          </div>
        </ATooltip>
      </div>

      <!-- 显示变更摘要 -->
      <div v-if="hasChanges" class="changes-summary">
        <ADivider />
        <div class="summary-title">变更摘要：</div>
        <div class="summary-content">
          <div v-if="addedVersions.length > 0" class="change-item">
            <span class="change-label add">新增 ({{ addedVersions.length }})：</span>
            <span class="change-versions">{{
              addedVersions.map((v) => getVersionDisplayName(v)).join(', ')
            }}</span>
          </div>
          <div v-if="removedVersions.length > 0" class="change-item">
            <span class="change-label remove">移除 ({{ removedVersions.length }})：</span>
            <span class="change-versions">{{
              removedVersions.map((v) => getBoundVersionDisplayName(v)).join(', ')
            }}</span>
          </div>
        </div>
      </div>
    </ACard>

    <!-- 标签筛选 -->
    <ACard v-if="finalSelectedVersions.length > 0" title="标签筛选" style="margin-top: 20px">
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
              <AButton size="small" @click="selectAllLabels" class="action-btn">全选</AButton>
              <AButton size="small" @click="clearSelectedLabels" class="action-btn">清空</AButton>
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
    </ACard>

    <!-- <div
    style="
        text-align: right;
        margin-top: 20px;
        display: flex;
        gap: 12px;
        justify-content: flex-end;
      "
  >
    <AButton v-if="hasChanges" @click="resetTempSelection">重置</AButton>
    <AButton type="primary" @click="handleSubmit" :disabled="isImportDisabled || !hasChanges">
      保存变更{{ hasChanges ? `(${getChangesSummaryText()})` : '' }}
    </AButton>
  </div> -->
  </div>
</template>

<script lang="ts" setup>
  import { computed, defineEmits, defineProps, onMounted, ref, watch } from 'vue';
  import {
    Button as AButton,
    Card as ACard,
    Checkbox as ACheckbox,
    CheckboxGroup as ACheckboxGroup,
    Divider as ADivider,
    Empty as AEmpty,
    InputSearch as AInputSearch,
    Modal,
    Pagination as APagination,
    Spin as ASpin,
    Tooltip as ATooltip,
  } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useGo } from '/@/hooks/web/usePage';
  import { useUserStore } from '/@/store/modules/user';
  // 导入API方法
  import {
    getDatasetsByDatasetGroupId,
    getDatasetsByDatasetGroupIdAndAnnotateType,
    getDatasetVersions,
  } from '/@/views/mineai/train/generationEdit/data';
  import {
    bindDatasetVersions,
    getPublicDatasetsByDatasetGroupId,
    getPublicDatasetsByDatasetGroupIdAndAnnotateType,
  } from '/@/views/mineai/train/generationGuideList/detail/dataset/component/api';
  import {
    getPrivateAndPublicDatasetGroupByPage,
    getPrivateDatasetGroupByPage,
    getPublicDatasetGroupByPage,
    searchPrivateAndPublicDatasetGroupByName,
    searchPrivateDatasetGroupByName,
    searchPublicDatasetGroupByName,
  } from '/@/views/mineai/train/generationEdit/data';

  const { createMessage } = useMessage();

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
    type?: number;
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
    imageCounts?: number;
    totalFileSize?: number;
    labelCountMap?: Record<string, number>;
    remark?: string;
    versionNumber?: string;
    fileCount?: number;
    dataSize?: string;
    status?: number;
    createUserId?: number;
    currentVersionName?: string;
    format?: string;
    _groupName?: string;
    _datasetName?: string;
    _groupId?: string | number;
  }

  interface BoundVersionItem {
    datasetGroup: DatasetGroup;
    dataset: Dataset;
    datasetVersions: DatasetVersion[];
  }

  const props = defineProps<{
    modelGenerationId: number;
    boundVersions: BoundVersionItem[];
    isImportDisabled?: boolean;
    initialSelectedLabels?: Record<string | number, string>; // 新增prop
    annotateType?: number; // 标注类型：102=目标检测，103=语义分割
    annotationFormat?: string; // 标注格式：YOLO, Segment-YOLO, COCO等
  }>();

  const emits = defineEmits<{
    (e: 'bind-success'): void;
  }>();

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
      // 如果没有提供名称，尝试从allGroups中获取
      const group = allGroups.value.get(groupId);
      if (group?.name) {
        sessionStorage.setItem('GroupName', group.name);
      }
    }
    go(`/maData/labelGroupPrivateDetails/${groupId}`);
  };

  // 级联选择相关状态
  const searchGroup = ref('');
  const searchDataset = ref('');
  const searchVersion = ref('');
  const privacyFilter = ref<string[]>(['public', 'private']); // 隐私筛选：public, private，默认全选

  const selectedGroup = ref<DatasetGroup | null>(null);
  const selectedDataset = ref<Dataset | null>(null);

  // 临时选择状态
  const tempSelectedVersions = ref<DatasetVersion[]>([]);

  // 标签筛选相关状态
  const selectedLabels = ref<string[]>([]);

  // 拖拽相关状态
  const dragOverLabel = ref<string | null>(null);
  const dragOverPosition = ref<'before' | 'after' | null>(null);

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

  // 分页相关
  const groupPage = ref(1);
  const groupPageSize = ref(10);
  const totalGroups = ref(0);
  const versionPage = ref(1);
  const versionPageSize = ref(10);
  const totalVersions = ref(0);

  // 已绑定版本相关计算属性
  const boundVersionsFlat = computed(() => {
    const flat: DatasetVersion[] = [];
    if (props.boundVersions && Array.isArray(props.boundVersions)) {
      props.boundVersions.forEach((item: BoundVersionItem) => {
        if (item.datasetVersions && Array.isArray(item.datasetVersions)) {
          item.datasetVersions.forEach((version: DatasetVersion) => {
            flat.push({
              ...version,
              _groupName: item.datasetGroup.name,
              _datasetName: item.dataset.name,
              _groupId: item.datasetGroup.id,
            });
          });
        }
      });
    }
    return flat;
  });

  const boundGroupIds = computed(() => {
    return new Set(
      props.boundVersions?.map((item: BoundVersionItem) => item.datasetGroup.id) || [],
    );
  });

  const boundDatasetIds = computed(() => {
    return new Set(props.boundVersions?.map((item: BoundVersionItem) => item.dataset.id) || []);
  });

  const boundVersionIds = computed(() => {
    return new Set(boundVersionsFlat.value.map((v) => v.id));
  });

  // 最终选择的版本
  const finalSelectedVersions = computed(() => {
    return tempSelectedVersions.value;
  });

  // 新增的版本
  const addedVersions = computed(() => {
    return tempSelectedVersions.value.filter((v) => !boundVersionIds.value.has(v.id));
  });

  // 被移除的版本
  const removedVersions = computed(() => {
    const selectedIds = new Set(tempSelectedVersions.value.map((v) => v.id));
    return boundVersionsFlat.value.filter((v) => !selectedIds.has(v.id));
  });

  // 是否有变更
  const hasChanges = computed(() => {
    return addedVersions.value.length > 0 || removedVersions.value.length > 0;
  });

  // 标签相关计算属性
  const mergedLabelCountMap = computed(() => {
    const labelMap: Record<string, number> = {};

    finalSelectedVersions.value.forEach((version) => {
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

  // 2. 添加设置初始标签的方法
  const setInitialLabels = (labelsMap: Record<string | number, string>) => {
    if (!labelsMap || Object.keys(labelsMap).length === 0) {
      selectedLabels.value = [];
      return;
    }

    // 将 {0: 'person', 1: 'car'} 转换为按顺序排列的数组 ['person', 'car']
    const sortedEntries = Object.entries(labelsMap).sort(([a], [b]) => Number(a) - Number(b));
    const orderedLabels = sortedEntries.map(([_, label]) => label);

    // 只选择在当前可用标签中存在的标签
    const availableLabels = Object.keys(mergedLabelCountMap.value);
    const validLabels = orderedLabels.filter((label) =>
      availableLabels.some(
        (availableLabel) => availableLabel.toLowerCase() === label.toLowerCase(),
      ),
    );

    // 将标签转换为小写匹配的实际标签名
    const normalizedLabels = validLabels.map((label) => {
      const matchedLabel = availableLabels.find(
        (availableLabel) => availableLabel.toLowerCase() === label.toLowerCase(),
      );
      return matchedLabel || label;
    });

    selectedLabels.value = normalizedLabels;
  };

  // 4. 监听 mergedLabelCountMap 的变化，当标签数据加载完成时应用初始选择
  watch(
    mergedLabelCountMap,
    (newLabelMap) => {
      if (
        Object.keys(newLabelMap).length > 0 &&
        props.initialSelectedLabels &&
        Object.keys(props.initialSelectedLabels).length > 0
      ) {
        setInitialLabels(props.initialSelectedLabels);
      }
    },
    { deep: true },
  );

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
          version.remark.toLowerCase().includes(searchVersion.value.toLowerCase())),
    );
  });

  // 判断是否为已绑定项目的方法
  const isBoundGroup = (groupId: string | number) => {
    return boundGroupIds.value.has(groupId);
  };

  const isBoundDataset = (datasetId: string | number) => {
    return boundDatasetIds.value.has(datasetId);
  };

  const isBoundVersion = (versionId: string | number) => {
    return boundVersionIds.value.has(versionId);
  };

  // 判断版本是否在临时选择中
  const isVersionInTempSelection = (versionId: string | number) => {
    return tempSelectedVersions.value.some((v) => v.id === versionId);
  };

  // 检查是否存在大小写冲突
  const hasCaseConflict = (normalizedLabel: string): boolean => {
    const originalLabels: string[] = [];

    finalSelectedVersions.value.forEach((version) => {
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

  // 初始化临时选择状态
  const initTempSelection = () => {
    tempSelectedVersions.value = [...boundVersionsFlat.value];
  };

  // 重置临时选择状态
  const resetTempSelection = () => {
    tempSelectedVersions.value = [...boundVersionsFlat.value];
  };

  // 切换版本选择状态
  const toggleVersionSelection = (version: DatasetVersion) => {
    const isCurrentlySelected = isVersionInTempSelection(version.id);
    const existingVersionFromSameDataset = tempSelectedVersions.value.find(
      (v) => v.datasetId === version.datasetId && v.id !== version.id,
    );

    if (isCurrentlySelected) {
      tempSelectedVersions.value = tempSelectedVersions.value.filter((v) => v.id !== version.id);
    } else if (existingVersionFromSameDataset) {
      tempSelectedVersions.value = tempSelectedVersions.value.filter(
        (v) => v.datasetId !== version.datasetId,
      );
      const enhancedVersion = {
        ...version,
        _groupName: selectedGroup.value?.name,
        _datasetName: selectedDataset.value?.name,
        _groupId: selectedGroup.value?.id,
      };
      tempSelectedVersions.value.push(enhancedVersion);
    } else {
      const enhancedVersion = {
        ...version,
        _groupName: selectedGroup.value?.name,
        _datasetName: selectedDataset.value?.name,
        _groupId: selectedGroup.value?.id,
      };
      tempSelectedVersions.value.push(enhancedVersion);
    }
  };

  // 从临时选择中移除版本
  const removeVersionFromTempSelection = (version: DatasetVersion) => {
    tempSelectedVersions.value = tempSelectedVersions.value.filter((v) => v.id !== version.id);
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

  // 获取标签顺序号的方法
  const getLabelOrderNumber = (label: string) => {
    const index = selectedLabels.value.indexOf(label);
    return index > -1 ? index + 1 : '';
  };

  // 全选标签
  const selectAllLabels = () => {
    selectedLabels.value = Object.keys(mergedLabelCountMap.value);
  };

  // 清空标签选择
  const clearSelectedLabels = () => {
    selectedLabels.value = [];
  };

  // 拖拽相关方法
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

  const handleLabelDragLeave = (event: DragEvent, label: string) => {
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

  // 获取变更摘要文本
  const getChangesSummaryText = () => {
    const parts: string[] = [];
    if (addedVersions.value.length > 0) {
      parts.push(`新增${addedVersions.value.length}个`);
    }
    if (removedVersions.value.length > 0) {
      parts.push(`移除${removedVersions.value.length}个`);
    }
    return parts.join('，');
  };

  // 工具方法
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '';
    try {
      return new Date(dateStr).toLocaleDateString();
    } catch {
      return dateStr;
    }
  };

  const getDatasetTypeText = (type?: number) => {
    const typeMap: Record<number, string> = {
      101: '图像分类',
      102: '目标检测',
      103: '目标分割',
      201: '目标跟踪',
      301: '文本分类',
      302: '文本分词',
      303: '命名实体识别',
      401: '声音分类',
      402: '语音识别',
      10001: '自定义导入',
    };
    return type ? typeMap[type] || '未知' : '未知';
  };

  const getDatasetStatusText = (status?: number) => {
    const statusMap: Record<number, string> = {
      101: '未标注',
      102: '手动标注中',
      103: '自动标注中',
      104: '自动标注完成',
      105: '标注完成',

      201: '目标跟踪中',
      202: '目标跟踪完成',
      203: '目标跟踪失败',

      301: '未采样',
      302: '采样中',
      303: '采样失败',

      401: '增强中',
      402: '导入中',
      403: '压缩包导入中',
    };

    return status ? statusMap[status] || '未知' : '未知';
  };

  const getAnnotateTypeText = (type?: number) => {
    const typeMap: Record<number, string> = {
      101: '分类',
      102: '目标检测',
      103: '目标分割',
      104: '实例分割',
    };
    return type ? typeMap[type] || '未知' : '未知';
  };

  // 获取标注类型文本（用于空状态提示）
  const getAnnotateTypeTextForTip = () => {
    if (props.annotateType === 102) {
      return '目标检测';
    } else if (props.annotateType === 103) {
      return '目标分割';
    }
    return '目标检测/目标分割';
  };

  const getVersionDisplayName = (version: DatasetVersion) => {
    if (version._groupName && version._datasetName) {
      const versionDisplay = version.versionName || version.name || `版本 ${version.id}`;
      return `${version._groupName} / ${version._datasetName} / ${versionDisplay}`;
    }
    const dataset = allDatasets.value.get(version.datasetId);
    const group = allGroups.value.get(dataset?.groupId || '');
    const versionDisplay = version.versionName || version.name || `版本 ${version.id}`;
    return `${group?.name || '未知组'} / ${dataset?.name || '未知数据集'} / ${versionDisplay}`;
  };

  const getBoundVersionDisplayName = (version: DatasetVersion) => {
    const versionDisplay = version.versionName || version.name || `版本 ${version.id}`;
    return `${version._groupName} / ${version._datasetName} / ${versionDisplay}`;
  };

  const getGroupNameForVersion = (version: DatasetVersion) => {
    if (version._groupName) return version._groupName;
    const dataset = allDatasets.value.get(version.datasetId);
    const group = allGroups.value.get(dataset?.groupId || '');
    return group?.name || '未知组';
  };

  const getDatasetNameForVersion = (version: DatasetVersion) => {
    if (version._datasetName) return version._datasetName;
    const dataset = allDatasets.value.get(version.datasetId);
    return dataset?.name || '未知数据集';
  };

  // 判断是否需要垂直布局（当三个框都显示时）
  const isVerticalLayout = computed(() => {
    // 当数据集组和数据集都选择了时，就启用垂直布局
    return selectedGroup.value && selectedDataset.value;
  });

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
      // 如果提供了annotateType，使用带annotateType过滤的公开数据集接口
      if (props.annotateType) {
        response = await getDatasetsByDatasetGroupIdAndAnnotateType(groupId, props.annotateType);
      } else {
        // 如果没有annotateType，使用原有的公开数据集接口
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

        const datasetIds = new Set(datasetsWithGroupId.map((d: Dataset) => d.id));
        groupDatasetMap.value.set(groupId, datasetIds);
      } else {
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
        //isPublic: true,
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

  // 搜索方法
  const onSearchGroup = async () => {
    groupPage.value = 1;
    await fetchDatasetGroups(groupPage.value, groupPageSize.value, searchGroup.value, privacyFilter.value);
  };

  // 隐私筛选变化处理
  const onPrivacyFilterChange = async () => {
    groupPage.value = 1;
    await fetchDatasetGroups(groupPage.value, groupPageSize.value, searchGroup.value, privacyFilter.value);
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

  const onVersionPageChange = async (page: number) => {
    if (!selectedDataset.value) return;
    versionPage.value = page;
    await fetchVersions(Number(selectedDataset.value.id), page, versionPageSize.value);
  };

  // 统计方法
  const getGroupSelectedCount = (groupId: string | number) => {
    const datasetIds = groupDatasetMap.value.get(groupId);
    if (!datasetIds || datasetIds.size === 0) return 0;
    return tempSelectedVersions.value.filter((version) => {
      return datasetIds.has(version.datasetId);
    }).length;
  };

  const getDatasetSelectedCount = (datasetId: string | number) => {
    return tempSelectedVersions.value.filter((version) => version.datasetId === datasetId).length;
  };

  // 提交方法
  const handleSubmit = async () => {
    if (!hasChanges.value) {
      //createMessage.warning('没有需要保存的变更');
      return Promise.resolve();
    }

    try {
      const changesSummary = getChangesSummaryText();

      // 将 Modal.confirm 包装成 Promise
      return new Promise((resolve, reject) => {
        Modal.confirm({
          title: '确认保存变更',
          content: `确认要保存这些变更吗？(${changesSummary})`,
          okText: '确定',
          cancelText: '取消',
          onOk: async () => {
            try {
              const versionIds = tempSelectedVersions.value.map((v) => v.id);
              const params = {
                datasetVersionIds: versionIds,
                modelGenerationId: props.modelGenerationId,
              };
              await bindDatasetVersions(params);
              createMessage.success('变更保存成功');
              emits('bind-success');
              resolve(); // 成功时resolve
            } catch (error) {
              console.error('数据集绑定失败:', error);
              createMessage.error('数据集绑定失败');
              reject(error); // 失败时reject
            }
          },
          onCancel: () => {
            // 用户取消时也resolve，表示操作完成（虽然用户选择了取消）
            resolve();
          },
        });
      });
    } catch (e) {
      console.error(e);
      return Promise.reject(e);
    }
  };

  // 监听searchGroup变化
  watch(searchGroup, (newVal) => {
    if (!newVal) {
      fetchDatasetGroups(groupPage.value, groupPageSize.value);
    }
  });

  // 监听boundVersions变化
  watch(
    () => props.boundVersions,
    () => {
      initTempSelection();
    },
    { deep: true, immediate: true },
  );

  // 组件挂载时初始化
  onMounted(async () => {
    initTempSelection();
    await fetchDatasetGroups();
  });

  // 暴露重置方法供父组件调用
  defineExpose({
    resetTempSelection,
    initTempSelection,
    handleSubmit,
    // 新增暴露标签相关方法
    selectedLabels,
    mergedLabelCountMap,
    clearSelectedLabels,
    getSelectedLabels: () => selectedLabels.value,
    getLabelOrder: () => [...selectedLabels.value],
  });
</script>

<style scoped lang="scss">
  /* 级联选择面板样式 - 统一配色方案 */
  .dataset-cascade-panel {
    display: flex;
    gap: 16px;
    height: 600px;
    margin-bottom: 20px;
  }

  .cascade-level {
    flex: 1;
    display: flex;
    flex-direction: column;
    border: 1px solid #444;
    border-radius: 8px;
    overflow: hidden;
    min-height: 0;
    background: linear-gradient(135deg, rgba(40, 40, 50, 0.4) 0%, rgba(30, 30, 40, 0.3) 100%);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  .level-header {
    padding: 16px 20px;
    background: linear-gradient(135deg, rgba(30, 30, 40, 0.8) 0%, rgba(25, 25, 35, 0.6) 100%);
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;
  }

  .level-header h4 {
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
  }

  .search-controls.vertical-layout {
    flex-direction: column;
    align-items: flex-end;
    gap: 8px;
  }

  .privacy-filter {
    display: flex;
    align-items: center;
  }

  .privacy-filter :deep(.ant-checkbox-wrapper) {
    color: #e0e0e0;
    margin-right: 8px;
  }

  .privacy-filter :deep(.ant-checkbox-wrapper:last-child) {
    margin-right: 0;
  }

  .search-box {
    min-width: 200px;
  }

  .level-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    min-height: 0;
  }

  :deep(.ant-spin-nested-loading) {
    flex: 1;
    min-height: 0;
    display: flex;
  }

  :deep(.ant-spin-container) {
    height: 100%;
    width: 100%;
    display: flex;
  }

  .item-container {
    flex: 1;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 12px;
    min-height: 0;
  }

  .empty-wrapper {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    width: 100%;
    color: #888;
  }

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

  .cascade-item {
    padding: 14px 16px;
    margin-bottom: 10px;
    border: 1px solid #555;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s ease;
    background: linear-gradient(135deg, rgba(50, 50, 60, 0.4) 0%, rgba(45, 45, 55, 0.3) 100%);
    position: relative;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .cascade-item:hover {
    border-color: #4096ff;
    background: linear-gradient(135deg, rgba(64, 150, 255, 0.15) 0%, rgba(64, 150, 255, 0.1) 100%);
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(64, 150, 255, 0.2);
  }

  .cascade-item.active {
    border-color: #4096ff;
    background: linear-gradient(135deg, rgba(64, 150, 255, 0.2) 0%, rgba(64, 150, 255, 0.15) 100%);
    box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.3);
  }

  .cascade-item.has-selected {
    border-color: #52c41a;
    background: linear-gradient(135deg, rgba(82, 196, 26, 0.15) 0%, rgba(82, 196, 26, 0.1) 100%);
  }

  .cascade-item.active.has-selected {
    border-color: #4096ff;
    background: linear-gradient(135deg, rgba(64, 150, 255, 0.25) 0%, rgba(82, 196, 26, 0.15) 100%);
    box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.4), inset 0 0 0 1px rgba(82, 196, 26, 0.3);
  }

  .cascade-item.selected {
    border-color: #52c41a;
    background: linear-gradient(135deg, rgba(82, 196, 26, 0.25) 0%, rgba(82, 196, 26, 0.2) 100%);
    box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.3);
  }

  .cascade-item.originally-bound {
    border-color: #fa8c16;
    background: linear-gradient(135deg, rgba(250, 140, 22, 0.15) 0%, rgba(250, 140, 22, 0.1) 100%);
  }

  .item-name {
    font-size: 14px;
    font-weight: 600;
    color: #e0e0e0;
    margin-bottom: 6px;
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .selection-count,
  .bound-indicator,
  .temp-selected-indicator,
  .current-badge {
    font-size: 10px;
    padding: 3px 8px;
    border-radius: 4px;
    font-weight: 500;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  }

  .selection-count {
    color: #52c41a;
    background: rgba(82, 196, 26, 0.2);
    border: 1px solid rgba(82, 196, 26, 0.3);
  }

  .bound-indicator {
    color: #fa8c16;
    background: linear-gradient(135deg, rgba(250, 140, 22, 0.2) 0%, rgba(250, 140, 22, 0.1) 100%);
    border: 1px solid #fa8c16;
  }

  .temp-selected-indicator {
    color: #52c41a;
    background: linear-gradient(135deg, rgba(82, 196, 26, 0.2) 0%, rgba(82, 196, 26, 0.1) 100%);
    border: 1px solid #52c41a;
  }

  .current-badge {
    color: #52c41a;
    background: linear-gradient(135deg, rgba(82, 196, 26, 0.2) 0%, rgba(82, 196, 26, 0.1) 100%);
    border: 1px solid #52c41a;
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
  }

  .item-meta span {
    margin-right: 12px;
  }

  .pagination-wrapper {
    padding: 16px 20px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    background: linear-gradient(135deg, rgba(30, 30, 40, 0.8) 0%, rgba(25, 25, 35, 0.6) 100%);
    display: flex;
    justify-content: center;
  }

  /* 选择结果样式 */
  .selected-items {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

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
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .selected-item:hover {
    background: linear-gradient(135deg, rgba(255, 77, 79, 0.25) 0%, rgba(255, 77, 79, 0.2) 100%);
    border-color: #ff4d4f;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
  }

  .selected-item.originally-bound {
    background: linear-gradient(135deg, rgba(250, 140, 22, 0.15) 0%, rgba(250, 140, 22, 0.1) 100%);
    border: 1px solid #fa8c16;
  }

  .status-badge {
    display: flex;
    gap: 4px;
  }

  .bound-badge,
  .new-badge {
    font-size: 10px;
    padding: 3px 8px;
    border-radius: 4px;
    font-weight: 500;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    flex-shrink: 0;
  }

  .bound-badge {
    color: #fa8c16;
    background: linear-gradient(135deg, rgba(250, 140, 22, 0.2) 0%, rgba(250, 140, 22, 0.1) 100%);
    border: 1px solid #fa8c16;
  }

  .new-badge {
    color: #52c41a;
    background: linear-gradient(135deg, rgba(82, 196, 26, 0.2) 0%, rgba(82, 196, 26, 0.1) 100%);
    border: 1px solid #52c41a;
  }

  .remove-icon {
    margin-left: 8px;
    font-weight: bold;
    color: #a0a0a0;
    transition: color 0.2s ease;
    flex-shrink: 0;
  }

  /* 变更摘要样式 */
  .changes-summary {
    margin-top: 16px;
  }

  .summary-title {
    font-size: 14px;
    font-weight: 600;
    color: #e0e0e0;
    margin-bottom: 12px;
  }

  .summary-content {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .change-item {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 8px 12px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 6px;
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .change-label {
    font-size: 12px;
    font-weight: 600;
    padding: 2px 6px;
    border-radius: 4px;
    flex-shrink: 0;
  }

  .change-label.add {
    color: #52c41a;
    background: rgba(82, 196, 26, 0.2);
    border: 1px solid rgba(82, 196, 26, 0.3);
  }

  .change-label.remove {
    color: #ff4d4f;
    background: rgba(255, 77, 79, 0.2);
    border: 1px solid rgba(255, 77, 79, 0.3);
  }

  .change-versions {
    font-size: 12px;
    color: #b0b0b0;
    line-height: 1.4;
    flex: 1;
  }

  /* 标签筛选样式 */
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

        &.drag-over-before {
          border-left: 3px solid #4caf50 !important;
          box-shadow: -3px 0 0 0 #4caf50;
        }

        &.drag-over-after {
          border-right: 3px solid #4caf50 !important;
          box-shadow: 3px 0 0 0 #4caf50;
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
  }

  /* Tooltip样式 */
  :deep(.custom-tooltip .ant-tooltip-inner) {
    padding: 0;
    background: transparent;
  }

  .version-tooltip {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
    overflow: hidden;
    min-width: 380px;
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .version-tooltip .tooltip-header {
    background: linear-gradient(135deg, #4096ff 0%, #1677ff 100%);
    padding: 16px 20px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .version-tooltip .tooltip-title {
    color: #ffffff;
    font-size: 15px;
    font-weight: 600;
    line-height: 1.4;
    margin: 0;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  }

  .version-tooltip .tooltip-content {
    padding: 20px;
  }

  .version-tooltip .info-section {
    margin-bottom: 16px;
  }

  .version-tooltip .info-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .version-tooltip .info-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 8px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    transition: all 0.2s ease;
  }

  .version-tooltip .info-icon {
    font-size: 16px;
    flex-shrink: 0;
  }

  .version-tooltip .info-label {
    color: #b3b3b3;
    font-size: 12px;
    margin-right: auto;
    min-width: 0;
    flex: 1;
  }

  .version-tooltip .info-value {
    color: #ffffff;
    font-size: 13px;
    font-weight: 500;
    flex-shrink: 0;
  }

  .version-tooltip .labels-section {
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    padding-top: 16px;
  }

  .version-tooltip .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    color: #ffffff;
    font-size: 14px;
    font-weight: 600;
  }

  .version-tooltip .section-icon {
    font-size: 16px;
  }

  .version-tooltip .labels-container {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    max-height: 120px;
    overflow-y: auto;
  }

  .version-tooltip .label-chip {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 10px;
    background: linear-gradient(135deg, rgba(64, 150, 255, 0.2) 0%, rgba(64, 150, 255, 0.1) 100%);
    border: 1px solid rgba(64, 150, 255, 0.3);
    border-radius: 16px;
    font-size: 12px;
    transition: all 0.2s ease;
  }

  .version-tooltip .label-chip:hover {
    background: linear-gradient(135deg, rgba(64, 150, 255, 0.3) 0%, rgba(64, 150, 255, 0.2) 100%);
    border-color: rgba(64, 150, 255, 0.5);
    transform: translateY(-1px);
  }

  .version-tooltip .label-name {
    color: #ffffff;
    font-weight: 500;
  }

  .version-tooltip .label-count {
    color: #4096ff;
    font-weight: 600;
    background: rgba(64, 150, 255, 0.2);
    padding: 2px 6px;
    border-radius: 8px;
    font-size: 11px;
    min-width: 24px;
    text-align: center;
  }

  /* 滚动条样式 */
  .item-container::-webkit-scrollbar {
    width: 4px;
    height: 4px;
  }

  .item-container::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.05);
    border-radius: 2px;
  }

  .item-container::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.2);
    border-radius: 2px;
  }

  .item-container::-webkit-scrollbar-thumb:hover {
    background: rgba(255, 255, 255, 0.3);
  }

  /* 响应式设计 */
  @media (max-width: 1200px) {
    .dataset-cascade-panel {
      flex-direction: column;
      height: auto;
    }

    .cascade-level {
      height: 400px;
    }

    .version-tooltip {
      min-width: 280px;
      max-width: 320px;
    }

    .version-tooltip .info-grid {
      grid-template-columns: 1fr;
    }

    .filter-header {
      flex-direction: column;
      gap: 16px;
      align-items: flex-start !important;

      .filter-info {
        width: 100%;
        justify-content: space-between;

        .info-stats {
          flex-wrap: wrap;
        }
      }
    }
  }
</style>
