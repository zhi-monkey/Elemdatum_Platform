<template>
  <BasicModal
    title="数据集详情"
    :destroyOnClose="true"
    :draggable="true"
    :showOkBtn="false"
    :showCancelBtn="false"
    :canFullscreen="false"
    @register="register"
    class="dataset-detail-modal"
  >
    <template #default>
      <a-spin :spinning="spinning" class="spinning-container">
        <div class="modal-content">
          <!-- 头部信息卡片 -->
          <div class="header-card">
            <div class="dataset-title">
              <h3>{{ datasetName }}</h3>
              <div class="dataset-id">ID: {{ datasetId }}</div>
            </div>
            <div class="version-badge">
              <span class="version-text">{{ currentVersion }}</span>
            </div>
          </div>

          <!-- 主要信息网格 -->
          <div class="info-grid">
            <div class="info-card">
              <div class="info-icon">
                <span class="icon">📊</span>
              </div>
              <div class="info-content">
                <div class="info-label">图片总数</div>
                <div class="info-value">{{ imageCounts }}</div>
              </div>
            </div>

            <div class="info-card">
              <div class="info-icon">
                <span class="icon">💾</span>
              </div>
              <div class="info-content">
                <div class="info-label">文件大小</div>
                <div class="info-value">{{ dataSetTotalSize }}</div>
              </div>
            </div>

            <div class="info-card">
              <div class="info-icon">
                <span class="icon">🏷️</span>
              </div>
              <div class="info-content">
                <div class="info-label">标注类型</div>
                <div class="info-value type-badge">{{ annotationType }}</div>
              </div>
            </div>

            <div class="info-card">
              <div class="info-icon">
                <span class="icon">📤</span>
              </div>
              <div class="info-content">
                <div class="info-label">导出格式</div>
                <div class="info-value">{{ publicationType }}</div>
              </div>
            </div>
          </div>

          <!-- 详细信息 -->
          <div class="detail-section">
            <div class="detail-item">
              <span class="detail-label">创建时间</span>
              <span class="detail-value">{{ createTime }}</span>
            </div>

            <div class="detail-item description-item">
              <span class="detail-label">版本描述</span>
              <span class="detail-value description-text">{{ datasetDescription }}</span>
            </div>
          </div>
        </div>
      </a-spin>
    </template>
  </BasicModal>
</template>
<script lang="ts" setup>
  import { ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { Spin as ASpin } from 'ant-design-vue';
  import { formatDateTime } from '/@/utils';

  // 文件大小格式化函数
  const formatFileSize = (bytes: number) => {
    if (!bytes || bytes === 0) return '0 B';

    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const k = 1024;
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + units[i];
  };

  const datasetId = ref(0);
  const datasetName = ref('');
  const createTime = ref('');
  const imageCounts = ref(0);
  const annotationType = ref('');
  const publicationType = ref('');
  const datasetDescription = ref('');
  const dataSetTotalSize = ref('1200kB');
  const currentVersion = ref('');
  const spinning = ref(true);

  const [register] = useModalInner(async (data) => {
    spinning.value = true;
    console.log(data.record);
    datasetId.value = data.record.datasetId;
    datasetName.value = data.record.name;
    createTime.value = formatDateTime(data.record.createTime);
    // 使用从API获取的imageCount字段
    imageCounts.value = data.record.imageCount ?? 0;
    // finished.value = data.record.progressVO.finished;
    // unfinished.value = data.record.progressVO.unfinished;
    // console.log(data.record);
    annotationType.value = data.record.annotateType === 102 ? '目标检测' : '目标分割';
    publicationType.value = data.record.format === 'COCO' ? 'CreateML' : data.record.format;
    // 明确判断 datasetDescription
    if (data.record.versionNote && data.record.versionNote.trim() !== '') {
      datasetDescription.value = data.record.versionNote;
    } else {
      datasetDescription.value = '无';
    }
    // isCurrentVersion.value = data.record.isCurrent ? '是' : '否';
    currentVersion.value = data.record.versionName;
    // 使用从API获取的totalFileSize字段并格式化
    dataSetTotalSize.value = formatFileSize(data.record.totalFileSize ?? 0);
    spinning.value = false;
  });
</script>
<style scoped>
  .dataset-detail-modal {
    :deep(.ant-modal-body) {
      padding: 0;
    }
  }

  .spinning-container {
    min-height: 400px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .modal-content {
    padding: 24px;
    background: #1a1a1a;
    min-height: 400px;
  }

  /* 头部信息卡片 */
  .header-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: linear-gradient(135deg, #2d3748 0%, #4a5568 100%);
    color: white;
    padding: 20px 24px;
    border-radius: 12px;
    margin-bottom: 24px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    border: 1px solid #2d3748;
  }

  .dataset-title h3 {
    margin: 0 0 8px 0;
    font-size: 20px;
    font-weight: 600;
    color: white;
  }

  .dataset-id {
    font-size: 14px;
    opacity: 0.8;
    color: rgba(255, 255, 255, 0.9);
  }

  .version-badge {
    background: rgba(255, 255, 255, 0.15);
    padding: 8px 16px;
    border-radius: 20px;
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .version-text {
    font-size: 14px;
    font-weight: 500;
    color: white;
  }

  /* 主要信息网格 */
  .info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 16px;
    margin-bottom: 24px;
  }

  .info-card {
    background: #2d2d2d;
    border-radius: 12px;
    padding: 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    border: 1px solid #404040;
    transition: all 0.3s ease;
  }

  .info-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
    border-color: #555555;
  }

  .info-icon {
    width: 48px;
    height: 48px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .info-icon .icon {
    font-size: 20px;
  }

  .info-content {
    flex: 1;
  }

  .info-label {
    font-size: 12px;
    color: #a0a0a0;
    margin-bottom: 4px;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .info-value {
    font-size: 18px;
    font-weight: 600;
    color: #e0e0e0;
    line-height: 1.2;
  }

  .type-badge {
    background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
    color: white;
    padding: 4px 12px;
    border-radius: 16px;
    font-size: 14px;
    font-weight: 500;
  }

  /* 详细信息部分 */
  .detail-section {
    background: #2d2d2d;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    border: 1px solid #404040;
  }

  .detail-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #404040;
  }

  .detail-item:last-child {
    border-bottom: none;
  }

  .detail-item.description-item {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .detail-label {
    font-size: 14px;
    color: #a0a0a0;
    font-weight: 500;
    min-width: 80px;
  }

  .detail-value {
    font-size: 14px;
    color: #e0e0e0;
    font-weight: 500;
  }

  .description-text {
    background: #1a1a1a;
    padding: 12px;
    border-radius: 8px;
    border-left: 4px solid #667eea;
    width: 100%;
    line-height: 1.5;
    color: #e0e0e0;
  }

  /* 响应式设计 */
  @media (max-width: 768px) {
    .info-grid {
      grid-template-columns: 1fr;
    }

    .header-card {
      flex-direction: column;
      gap: 16px;
      text-align: center;
    }

    .info-card {
      padding: 16px;
    }
  }
</style>
