<!-- PublicDatasetCascade.vue -->
<template>
  <div class="public-dataset-cascade">
    <!-- 级联选择面板 -->
    <div class="dataset-cascade-panel">
      <!-- 第一级：数据集组 -->
      <div class="cascade-level">
        <div class="level-header">
          <h4>数据集组</h4>
          <AInputSearch
            v-model:value="searchGroup"
            placeholder="搜索数据集组"
            style="width: 200px"
            :loading="groupLoading"
            @search="onSearchGroup"
          />
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
              <div v-else class="empty-wrapper">
                <AEmpty :image="AEmpty.PRESENTED_IMAGE_SIMPLE" description="该组下暂无数据集" />
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
                    <span
                      v-if="version.labelCountMap && Object.keys(version.labelCountMap).length > 0"
                    >
                      标签种类数: {{ Object.keys(version.labelCountMap).length }}
                    </span>
                    <span v-if="version.createTime">{{ formatDate(version.createTime) }}</span>
                  </div>
                </div>
              </template>
              <div v-else class="empty-wrapper">
                <AEmpty :image="AEmpty.PRESENTED_IMAGE_SIMPLE" description="该数据集暂无包含训练目标的版本" />
              </div>
            </div>
          </ASpin>
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
                        >{{
                          version.importableImageCount ||
                          version.imageCount ||
                          version.imageCounts ||
                          0
                        }}
                        张</span
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
            <span class="item-text" :title="getVersionDisplayName(version)">{{
              getVersionDisplayName(version)
            }}</span>
            <span class="status-badge">
              <span v-if="isBoundVersion(version.id)" class="bound-badge">原绑定</span>
              <span v-else class="new-badge">新增</span>
            </span>
            <span class="remove-icon">×</span>
          </div>
        </ATooltip>
      </div>
    </ACard>
    <!-- 显示变更摘要 -->
    <div v-if="hasChanges" style="margin-top: 20px" class="changes-summary">
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

    <div
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
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { defineProps, defineEmits, ref, computed, onMounted, watch } from 'vue';
  import {
    Button as AButton,
    Card as ACard,
    InputSearch as AInputSearch,
    Modal,
    Pagination as APagination,
    Spin as ASpin,
    Tooltip as ATooltip,
    Empty as AEmpty,
  } from 'ant-design-vue';
  import { useMessage } from '/src/hooks/web/useMessage';
  import {
    bindDatasetVersions,
    getGuidedGenerationLabels,
    getPublicDatasetGroupByPage,
    getPublicDatasetsByDatasetGroupId,
    getPublicDatasetsByDatasetGroupIdAndAnnotateType,
    getPublicDatasetVersionsByDatasetIdAll,
    searchPublicDatasetGroupByName,
  } from '/src/views/mineai/train/generationGuideList/detail/dataset/component/api';

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
    annotateType?: number;
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
    annotateType?: number; // 标注类型：102=目标检测，103=语义分割
    annotationFormat?: string; // 标注格式：YOLO, Segment-YOLO, COCO等
  }>();

  const emits = defineEmits<{
    (e: 'bind-success'): void;
  }>();

  // 级联选择相关状态
  const searchGroup = ref('');
  const searchDataset = ref('');
  const searchVersion = ref('');

  const selectedGroup = ref<DatasetGroup | null>(null);
  const selectedDataset = ref<Dataset | null>(null);

  // 临时选择状态
  const tempSelectedVersions = ref<DatasetVersion[]>([]);

  // 数据状态
  const datasetGroups = ref<DatasetGroup[]>([]);
  const datasets = ref<Dataset[]>([]);
  const versions = ref<DatasetVersion[]>([]);
  const allVersions = ref<DatasetVersion[]>([]);

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
    if (!searchVersion.value) {
      return versions.value;
    }
    // 仅执行搜索过滤
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
      102: '标注中',
      105: '已标注',
      302: '抽帧中',
      401: '数据增强中',
      403: '导入中',
      103: '自动标注中',
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

  // API调用方法
  const fetchDatasetGroups = async (page = 1, pageSize = 10, search?: string) => {
    try {
      groupLoading.value = true;
      let response;
      if (search && search.trim()) {
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
      console.log(props);
      // 如果提供了annotateType，使用带annotateType过滤的公开数据集接口
      if (props.annotateType) {
        response = await getPublicDatasetsByDatasetGroupIdAndAnnotateType(
          groupId,
          props.annotateType,
        );
      } else {
        // 如果没有annotateType，使用原有的公开数据集接口
        response = await getPublicDatasetsByDatasetGroupId(groupId);
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
        datasets.value = [];
      }
    } catch (error) {
      console.error('获取数据集失败:', error);
      datasets.value = [];
    } finally {
      datasetLoading.value = false;
    }
  };

  const fetchVersions = async (datasetId: number, labelList?: string[]) => {
    try {
      versionLoading.value = true;
      const response = await getPublicDatasetVersionsByDatasetIdAll({
        datasetId,
        labelList: labelList,
        isPublic: true,
      });

      if (response && Array.isArray(response)) {
        versions.value = response;
      } else {
        versions.value = [];
      }
    } catch (error) {
      console.error('获取数据集版本失败:', error);
      versions.value = [];
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
    let labels = await getGuidedGenerationLabels(props.modelGenerationId);
    const labelNameList = labels.map((label) => {
      return label.name;
    });
    await fetchVersions(Number(dataset.id), labelNameList);
  };

  // 搜索方法
  const onSearchGroup = async () => {
    groupPage.value = 1;
    await fetchDatasetGroups(groupPage.value, groupPageSize.value, searchGroup.value);
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
    await fetchDatasetGroups(page, groupPageSize.value, searchGroup.value);
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
      createMessage.warning('没有需要保存的变更');
      return;
    }

    try {
      const changesSummary = getChangesSummaryText();
      Modal.confirm({
        title: '确认保存变更',
        content: `确认要保存这些变更吗？(${changesSummary})`,
        okText: '确定',
        cancelText: '取消',
        onOk: async () => {
          const versionIds = tempSelectedVersions.value.map((v) => v.id);
          const params = {
            datasetVersionIds: versionIds,
            modelGenerationId: props.modelGenerationId,
          };
          await bindDatasetVersions(params);
          createMessage.success('变更保存成功');
          emits('bind-success');
        },
        onCancel: () => {},
      });
    } catch (e) {
      console.error(e);
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
  });
</script>

<style scoped lang="scss">
  /* 级联选择面板样式 - 统一配色方案 */
  .dataset-cascade-panel {
    display: flex;
    gap: 16px;
    height: 400px;
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

  /* 选择结果样式 - 主要修改部分 */
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
    max-width: 300px; /* 固定最大宽度 */
    min-width: 200px; /* 设置最小宽度 */
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

  /* 修改文字部分的样式 */
  .item-text {
    flex: 1; /* 占用剩余空间 */
    min-width: 0; /* 允许缩小 */
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: inherit;
  }

  .status-badge {
    display: flex;
    gap: 4px;
    flex-shrink: 0; /* 不允许压缩 */
  }

  .bound-badge,
  .new-badge {
    font-size: 10px;
    padding: 3px 8px;
    border-radius: 4px;
    font-weight: 500;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    flex-shrink: 0;
    white-space: nowrap; /* 防止换行 */
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
    margin-left: 4px;
    font-weight: bold;
    color: #a0a0a0;
    transition: color 0.2s ease;
    flex-shrink: 0; /* 不允许压缩 */
    width: 16px;
    height: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    line-height: 1;
  }

  .selected-item:hover .remove-icon {
    color: #ff4d4f;
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

  .version-tooltip .version-details {
    color: #e0e0e0;
    font-size: 13px;
    line-height: 1.6;
  }

  .version-tooltip .version-details p {
    margin: 8px 0;
  }

  .version-tooltip .version-details strong {
    color: #ffffff;
  }

  .version-tooltip .bound-status {
    color: #fa8c16;
    font-weight: 600;
    background: rgba(250, 140, 22, 0.2);
    padding: 2px 6px;
    border-radius: 4px;
  }

  .version-tooltip .new-status {
    color: #52c41a;
    font-weight: 600;
    background: rgba(82, 196, 26, 0.2);
    padding: 2px 6px;
    border-radius: 4px;
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

    .selected-item {
      max-width: 250px;
      min-width: 180px;
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
</style>
