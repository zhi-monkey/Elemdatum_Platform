<template>
  <ACard
    v-if="boundVersions && boundVersions.length > 0"
    style="margin-top: 20px; padding: 20px; background: transparent !important"
  >
    <template #title>
      <h2 style="font-size: 18px; margin: 0">已绑定数据集版本</h2>
    </template>
    <div class="bound-versions-list">
      <template v-for="(item, index) in boundVersions" :key="index">
        <div v-for="version in item.datasetVersions" :key="version.id" class="bound-version-row">
          <ATooltip
            placement="top"
            :overlay-style="{ maxWidth: '450px' }"
            :overlay-class-name="'custom-tooltip'"
          >
            <template #title>
              <div class="version-tooltip">
                <div class="tooltip-header">
                  <div class="tooltip-title">{{ getBoundVersionDisplayName(item, version) }}</div>
                </div>
                <div class="tooltip-content">
                  <div class="info-section">
                    <div class="info-grid">
                      <div class="info-item">
                        <span class="info-icon">📊</span>
                        <span class="info-label">图片数量</span>
                        <span class="info-value">{{ version.importableImageCount || 0 }} 张</span>
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
                      完整标签统计
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

            <!-- 主要展示区域 -->
            <div class="bound-version-main">
              <ATag class="version-tag" color="blue">
                {{ getBoundVersionDisplayName(item, version) }}
              </ATag>
              <span class="bound-indicator">已绑定</span>
            </div>
          </ATooltip>

          <!-- 前三个标签展示 -->
          <div v-if="getTopThreeLabels(version).length > 0" class="preview-labels">
            <span class="labels-prefix">标签:</span>
            <ATag
              v-for="(labelInfo, labelIndex) in getTopThreeLabels(version)"
              :key="labelInfo.name"
              :color="getLabelPreviewColor(labelIndex)"
              size="small"
              class="preview-label-tag"
            >
              {{ labelInfo.name }} ({{ labelInfo.count }})
            </ATag>
            <span v-if="getTotalLabelsCount(version) > 3" class="more-labels-indicator">
              +{{ getTotalLabelsCount(version) - 3 }}个标签
            </span>
          </div>
        </div>
      </template>
    </div>
  </ACard>
</template>

<script lang="ts" setup>
  import { defineProps } from 'vue';
  import { Card as ACard, Tooltip as ATooltip, Tag as ATag } from 'ant-design-vue';

  const props = defineProps<{
    boundVersions: object[];
  }>();

  // 格式化日期的方法
  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '';
    try {
      return new Date(dateStr).toLocaleDateString();
    } catch {
      return dateStr;
    }
  };

  // 获取标注类型文本
  const getAnnotateTypeText = (type?: number) => {
    const typeMap: Record<number, string> = {
      101: '分类',
      102: '目标检测',
      103: '目标分割',
      104: '实例分割',
    };
    return type ? typeMap[type] || '未知' : '未知';
  };

  // 获取已绑定版本的显示名称
  const getBoundVersionDisplayName = (item: any, version: any) => {
    const versionDisplay = version.versionName || version.name || `版本 ${version.id}`;
    return `${item.datasetGroup.name} / ${item.dataset.name} / ${versionDisplay}`;
  };

  // 获取前三个标签
  const getTopThreeLabels = (version: any) => {
    if (!version.labelCountMap || Object.keys(version.labelCountMap).length === 0) {
      return [];
    }

    const labels = Object.entries(version.labelCountMap)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => (b.count as number) - (a.count as number))
      .slice(0, 3);

    return labels;
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };
  // 获取标签总数
  const getTotalLabelsCount = (version: any) => {
    if (!version.labelCountMap) return 0;
    return Object.keys(version.labelCountMap).length;
  };

  // 获取预览标签颜色
  const getLabelPreviewColor = (index: number) => {
    const colors = ['green', 'orange', 'purple'];
    return colors[index] || 'default';
  };
</script>

<style scoped lang="scss">
  .bound-versions-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .bound-version-row {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 12px 16px;
    background: linear-gradient(135deg, rgba(64, 150, 255, 0.08) 0%, rgba(64, 150, 255, 0.05) 100%);
    border: 1px solid rgba(64, 150, 255, 0.2);
    border-radius: 8px;
    transition: all 0.2s ease;
  }

  .bound-version-row:hover {
    background: linear-gradient(135deg, rgba(64, 150, 255, 0.12) 0%, rgba(64, 150, 255, 0.08) 100%);
    border-color: rgba(64, 150, 255, 0.3);
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(64, 150, 255, 0.15);
  }

  .bound-version-main {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .version-tag {
    font-size: 13px;
    font-weight: 500;
    margin: 0;
    max-width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .bound-indicator {
    font-size: 10px;
    color: #52c41a;
    background: linear-gradient(135deg, rgba(82, 196, 26, 0.2) 0%, rgba(82, 196, 26, 0.1) 100%);
    padding: 3px 8px;
    border-radius: 4px;
    border: 1px solid #52c41a;
    font-weight: 500;
    box-shadow: 0 1px 3px rgba(82, 196, 26, 0.2);
    flex-shrink: 0;
  }

  .preview-labels {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    margin-left: 8px;
  }

  .labels-prefix {
    font-size: 12px;
    color: #666;
    font-weight: 500;
    flex-shrink: 0;
  }

  .preview-label-tag {
    font-size: 11px;
    margin: 0;
    padding: 2px 6px;
    border-radius: 4px;
    font-weight: 500;
  }

  .more-labels-indicator {
    font-size: 11px;
    color: #8c8c8c;
    font-style: italic;
    padding: 2px 6px;
    background: rgba(140, 140, 140, 0.1);
    border-radius: 4px;
    border: 1px dashed #d9d9d9;
  }

  /* 复用tooltip样式 */
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
  }

  .version-tooltip .tooltip-header {
    background: linear-gradient(135deg, #4096ff 0%, #1677ff 100%);
    padding: 16px 20px;
  }

  .version-tooltip .tooltip-header .tooltip-title {
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
    border-radius: 8px;
    transition: all 0.2s ease;
  }

  .version-tooltip .info-item:hover {
    border-color: rgba(64, 150, 255, 0.3);
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

  /* 响应式设计 */
  @media (max-width: 768px) {
    .bound-versions-list {
      grid-template-columns: 1fr;
    }

    .bound-version-main {
      flex-direction: column;
      align-items: flex-start;
      gap: 8px;
    }

    .preview-labels {
      margin-left: 0;
      flex-wrap: wrap;
    }

    .version-tag {
      max-width: 100%;
    }
  }

  @media (max-width: 1024px) and (min-width: 769px) {
    .bound-versions-list {
      gap: 12px;
    }

    .bound-version-row {
      padding: 10px 14px;
    }
  }
</style>
