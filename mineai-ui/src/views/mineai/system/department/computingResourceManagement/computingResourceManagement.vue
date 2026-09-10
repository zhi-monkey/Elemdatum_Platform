<template>
  <a-card>
    <a-row :gutter="24">
      <!--   服务器图标占位   -->
      <a-col :span="2" />
      <a-col :span="8">节点</a-col>
      <a-col :span="3">CPU负载</a-col>
      <a-col :span="3">内存用量</a-col>
      <a-col :span="3">磁盘用量</a-col>
      <a-col :span="5">GPU管理</a-col>
    </a-row>
  </a-card>
  <a-card v-for="item in data" :key="item.id">
    <a-row :gutter="24">
      <!--   图标   -->
      <a-col :span="2">
        <div style="display: flex; align-items: center; justify-content: center">
          <HddOutlined style="font-size: 48px" />
        </div>
      </a-col>
      <!--  节点名称    -->
      <a-col :span="8">
        <a-row>
          <p class="lineHeight" style="font-size: 24px; margin: 0">{{ item.ip || item.instance }}</p>
        </a-row>
      </a-col>
      <!--    CPU 负载    -->
      <a-col :span="3">
        <a-row>
          <strong>{{ parseFloat(item.cpuUsage).toFixed(2) + '%' }}</strong>
        </a-row>
        <a-row>
          <p style="font-size: 10px">Usage: {{ item.cpuUsed }} / {{ item.cpuTotal }}</p>
        </a-row>
      </a-col>
      <!--   内存 用量   -->
      <a-col :span="3">
        <a-row>
          <strong>{{ ((item.memoryUsed / item.memoryTotal) * 100).toFixed(2) + '%' }}</strong>
        </a-row>
        <a-row>
          <p style="font-size: 10px">
            {{ (item.memoryUsed / 1000 / 1000 / 1000).toFixed(2) }} /
            {{ (item.memoryTotal / 1000 / 1000 / 1000).toFixed(2) }} GB
          </p>
        </a-row>
      </a-col>
      <!--   磁盘 用量   -->
      <a-col :span="3">
        <a-row>
          <strong>{{ ((item.diskUsed / item.diskTotal) * 100).toFixed(2) + '%' }}</strong>
        </a-row>
        <a-row>
          <p style="font-size: 10px">
            {{ (item.diskUsed / 1000 / 1000 / 1000).toFixed(2) }} /
            {{ (item.diskTotal / 1000 / 1000 / 1000).toFixed(2) }} GB
          </p>
        </a-row>
      </a-col>
      <!--   GPU 管理   -->
      <a-col :span="5">
        <div
          v-if="item.nodeGpuInfo && item.nodeGpuInfo.gpus && item.nodeGpuInfo.gpus.length > 0"
          class="gpu-action-wrapper"
        >
          <a-button size="small" @click="showGpuModal(item)" class="gpu-manage-btn">
            <template #icon>
              <VideoCameraOutlined />
            </template>
            管理GPU ({{ item.nodeGpuInfo.totalGpuCount }}个)
          </a-button>
        </div>
        <div v-else>
          <span style="font-size: 12px; color: #999">无GPU</span>
        </div>
      </a-col>
    </a-row>
  </a-card>

  <!-- GPU管理弹窗 -->
  <a-modal
    v-model:visible="gpuModalVisible"
    title="GPU资源管理"
    width="1100px"
    :footer="null"
    @cancel="handleModalClose"
    class="gpu-modal"
  >
    <div class="gpu-modal-content">
      <div class="gpu-stats-summary">
        <div class="stat-card">
          <div class="stat-label">GPU总数</div>
          <div class="stat-value">{{ currentGpus.length }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">可用</div>
          <div class="stat-value available">
            {{ currentGpus.filter((gpu) => !isGpuDisabled(gpu.gpuUUID)).length }}
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">已禁用</div>
          <div class="stat-value disabled">
            {{ currentGpus.filter((gpu) => isGpuDisabled(gpu.gpuUUID)).length }}
          </div>
        </div>
      </div>

      <a-table
        :columns="gpuColumns"
        :data-source="currentGpus"
        :pagination="false"
        :row-key="(record) => record.gpuUUID || record.gpuIndex"
        size="middle"
        class="gpu-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'gpuIndex'">
            <div class="gpu-uuid">
              <VideoCameraOutlined class="gpu-icon" />
              <span class="uuid-text">{{ record.gpuUUID }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'gpuModel'">
            <div class="gpu-model">{{ record.gpuModel }}</div>
          </template>
          <template v-else-if="column.key === 'health'">
            <div class="status-tag">{{ record.health || 'Unknown' }}</div>
          </template>
        </template>
        <template #action="{ record }">
          <a-button
            v-if="isGpuDisabled(record.gpuUUID)"
            size="small"
            @click="handleEnableGpu(record.gpuUUID)"
            class="action-btn enable-btn"
          >
            <template #icon><CheckCircleOutlined /></template>
            启用
          </a-button>

          <!-- 禁用按钮 -->
          <a-button
            v-else
            size="small"
            @click="handleDisableGpu(record.gpuUUID)"
            class="action-btn disable-btn"
          >
            <template #icon><StopOutlined /></template>
            禁用
          </a-button>
        </template>
      </a-table>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
  import {
    Button as AButton,
    Card as ACard,
    Col as ACol,
    message,
    Modal as AModal,
    Row as ARow,
    Table as ATable,
    Tag as ATag,
  } from 'ant-design-vue';
  import {
    HddOutlined,
    VideoCameraOutlined,
    CheckCircleOutlined,
    StopOutlined,
  } from '@ant-design/icons-vue';
  import { h, onMounted, ref } from 'vue';
  import {
    disableGpu,
    enableGpu,
    getDisabledGpus,
    getResourceData,
  } from '/@/views/mineai/system/department/computingResourceManagement/resourceData';

  const data = ref<any[]>([]);
  const gpuModalVisible = ref(false);
  const currentGpus = ref<any[]>([]);
  const disabledGpuSet = ref(new Set<string>());

  // GPU表格列定义
  const gpuColumns = [
    {
      title: 'GPU UUID',
      dataIndex: 'gpuUUID',
      key: 'gpuIndex',
      width: 250,
      slots: { customRender: 'gpuIndex' },
    },
    {
      title: '型号',
      dataIndex: 'gpuModel',
      key: 'gpuModel',
      width: 200,
      slots: { customRender: 'gpuModel' },
    },
    {
      title: '状态',
      dataIndex: 'health',
      key: 'health',
      width: 100,
      customRender: ({ record }) => {
        return h(
          ATag,
          { color: record.health && record.health.toLowerCase() === 'healthy' ? 'green' : 'red' },
          record.health,
        );
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      slots: { customRender: 'action' },
    },
  ];

  const getData = async () => {
    data.value = await getResourceData();
    console.log('data', data.value);
  };

  // 获取禁用的GPU列表
  const loadDisabledGpus = async () => {
    try {
      const result = await getDisabledGpus();
      disabledGpuSet.value = new Set(result || []);
    } catch (error) {
      console.error('获取禁用GPU列表失败:', error);
    }
  };

  // 检查GPU是否被禁用
  const isGpuDisabled = (gpuUuid: string) => {
    return disabledGpuSet.value.has(gpuUuid);
  };

  // 显示GPU管理弹窗
  const showGpuModal = (node: any) => {
    console.log('showGpuModal called with node:', node);
    console.log('node.nodeGpuInfo:', node.nodeGpuInfo);

    if (node.nodeGpuInfo && node.nodeGpuInfo.gpus) {
      console.log('node.nodeGpuInfo.gpus:', node.nodeGpuInfo.gpus);
      currentGpus.value = node.nodeGpuInfo.gpus;
      console.log('currentGpus.value after assignment:', currentGpus.value);
      gpuModalVisible.value = true;
    } else {
      console.warn('No GPU info found in node');
    }
  };

  // 关闭弹窗
  const handleModalClose = () => {
    gpuModalVisible.value = false;
    currentGpus.value = [];
  };

  // 禁用GPU
  const handleDisableGpu = async (gpuUuid: string) => {
    try {
      await disableGpu(gpuUuid);
      disabledGpuSet.value.add(gpuUuid);
      message.success('GPU已禁用');
    } catch (error) {
      message.error('禁用GPU失败');
      console.error('禁用GPU失败:', error);
    }
  };

  // 启用GPU
  const handleEnableGpu = async (gpuUuid: string) => {
    try {
      await enableGpu(gpuUuid);
      disabledGpuSet.value.delete(gpuUuid);
      message.success('GPU已启用');
    } catch (error) {
      message.error('启用GPU失败');
      console.error('启用GPU失败:', error);
    }
  };

  onMounted(async () => {
    await getData();
    await loadDisabledGpus();
  });
</script>

<style scoped lang="scss">
  /* 通用辅助类 */
  .lineHeight {
    line-height: 48px;
  }

  .gpu-action-wrapper {
    display: flex;
    align-items: center;
  }

  .gpu-manage-btn {
    /* 基础布局 */
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 28px; /* 固定高度，显精致 */
    padding: 0 16px;
    border-radius: 4px; /* 微圆角，硬朗感 */
    font-size: 12px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    /* 默认状态：低调的半透明蓝 */
    background: rgba(59, 130, 246, 0.1) !important; /* 强制覆盖 AntD */
    border: 1px solid rgba(59, 130, 246, 0.3) !important;
    color: #60a5fa !important; /* 亮蓝色文字 */
    box-shadow: 0 0 0 rgba(0, 0, 0, 0);

    /* 图标微调 */
    :deep(.anticon) {
      margin-right: 6px;
      font-size: 14px;
      transition: all 0.3s;
    }

    /* 悬停状态：点亮荧光 */
    &:hover {
      background: rgba(59, 130, 246, 0.25) !important;
      border-color: #3b82f6 !important;
      color: #ffffff !important; /* 文字变白，增强对比 */
      /* 核心：添加蓝色外发光，营造科技感 */
      box-shadow: 0 0 15px rgba(59, 130, 246, 0.5), inset 0 0 10px rgba(59, 130, 246, 0.1);
      transform: translateY(-1px);

      /* 图标跟随变亮 */
      :deep(.anticon) {
        color: #fff;
        transform: scale(1.1); /* 图标微放大 */
      }
    }

    /* 点击状态 */
    &:active {
      transform: translateY(0);
      box-shadow: 0 0 5px rgba(59, 130, 246, 0.4);
    }
  }

  :deep(.gpu-modal) {
    .ant-modal-content {
      /* 核心背景色：纯粹的深灰，不偏色 */
      background-color: #1f1f1f;
      /* 边框：极细的微光边框 */
      border: 1px solid rgba(60, 130, 246, 0.15);
      box-shadow: 0 0 40px rgba(0, 0, 0, 0.6);
      border-radius: 12px;
    }

    .ant-modal-header {
      background: #1f1f1f; /* 与内容一致 */
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      padding: 20px 24px;
      border-radius: 12px 12px 0 0;

      .ant-modal-title {
        font-size: 18px;
        font-weight: 600;
        letter-spacing: 0.5px;
        /* 标题渐变色，呼应蓝色主题 */
        background: linear-gradient(90deg, #e8eaed, #93c5fd);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
      }
    }

    .ant-modal-body {
      background: #1f1f1f;
      padding: 0;
    }

    .ant-modal-close {
      color: #6b7280;
      transition: all 0.3s;

      &:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.1);
        border-radius: 50%;
      }
    }
  }

  .gpu-modal-content {
    padding: 24px;
  }

  /*
   顶部统计卡片区域
   设计思路：悬浮在深色背景上的半透明磨砂块
*/
  .gpu-stats-summary {
    display: flex;
    gap: 20px;
    margin-bottom: 24px;
    padding: 24px;
    /* 背景：极低透明度的蓝灰，保持通透 */
    background: linear-gradient(180deg, rgba(30, 41, 59, 0.4) 0%, rgba(30, 41, 59, 0.2) 100%);
    border-radius: 12px;
    /* 边框：顶部亮，底部暗，营造立体感 */
    border: 1px solid rgba(255, 255, 255, 0.06);
    box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.2);
  }

  .stat-card {
    flex: 1;
    text-align: center;
    padding: 16px;
    position: relative;
    background: rgba(255, 255, 255, 0.02);
    border-radius: 8px;
    border: 1px solid transparent;
    transition: all 0.3s ease;

    /* 装饰线：左侧蓝色光条 */
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 20%;
      bottom: 20%;
      width: 3px;
      background: #3b82f6;
      border-radius: 0 2px 2px 0;
      opacity: 0.5;
      transition: all 0.3s;
    }

    &:hover {
      background: rgba(59, 130, 246, 0.05); /* 极淡的蓝色背景 */
      border-color: rgba(59, 130, 246, 0.3);
      transform: translateY(-2px);

      &::before {
        opacity: 1;
        box-shadow: 0 0 8px #3b82f6;
      }
    }
  }

  .stat-label {
    font-size: 13px;
    color: #94a3b8; /* 偏冷灰色 */
    margin-bottom: 8px;
    text-transform: uppercase;
    letter-spacing: 1px;
  }

  .stat-value {
    font-size: 36px;
    font-weight: 700;
    font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
    color: #f1f5f9;

    /* 普通数值：带一点点蓝色光晕 */
    text-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);

    &.available {
      color: #34d399;
      /* 绿色荧光感 */
      text-shadow: 0 0 15px rgba(52, 211, 153, 0.3);
    }

    &.disabled {
      color: #f87171;
      /* 红色荧光感 */
      text-shadow: 0 0 15px rgba(248, 113, 113, 0.3);
    }
  }

  /*
   Table 样式深度定制
   去除 Ant Design 默认的线条感，改用色块区分
*/
  :deep(.gpu-table) {
    .ant-table {
      background: transparent;
      border: 1px solid rgba(255, 255, 255, 0.05);
      border-radius: 8px;
    }

    /* 表头：深黑底，底部亮线 */
    .ant-table-thead > tr > th {
      background: #141414;
      color: #60a5fa; /* 亮蓝色文字 */
      font-weight: 600;
      border-bottom: 1px solid rgba(60, 130, 246, 0.2);
      padding: 16px 24px;
    }

    /* 表体 */
    .ant-table-tbody > tr > td {
      background: #1f1f1f; /* 与 Modal 背景一致 */
      border-bottom: 1px solid rgba(255, 255, 255, 0.04);
      color: #e2e8f0;
      padding: 16px 24px;
      transition: background 0.2s;
    }

    /* Hover 效果：深蓝高亮 */
    .ant-table-tbody > tr:hover > td {
      background: rgba(59, 130, 246, 0.08) !important;
    }

    /* 去掉最后一行边框 */
    .ant-table-tbody > tr:last-child > td {
      border-bottom: none;
    }
  }

  /* GPU UUID 样式优化 */
  .gpu-uuid {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .gpu-icon {
    font-size: 18px;
    color: #60a5fa;
    /* 图标微发光 */
    filter: drop-shadow(0 0 2px rgba(96, 165, 250, 0.5));
  }

  .uuid-text {
    font-family: 'JetBrains Mono', 'Courier New', monospace; /* 代码字体 */
    font-size: 13px;
    color: #e2e8f0;
    opacity: 0.9;
  }

  .gpu-model {
    color: #94a3b8;
    background: rgba(255, 255, 255, 0.03);
    padding: 4px 8px;
    border-radius: 4px;
    display: inline-block;
    font-size: 12px;
  }

  /* 状态标签微调 */
  .status-tag {
    border: none;
    padding: 4px 10px;
  }

  /* 按钮高级感样式 */
  .action-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    border-radius: 4px;
    font-weight: 500;
    font-size: 12px;
    height: 28px;
    padding: 0 12px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    /* 默认无边框，靠背景色区分 */
    border: 1px solid transparent;
  }

  /* 启用按钮：幽灵绿 */
  .enable-btn {
    background: rgba(16, 185, 129, 0.1);
    color: #34d399;
    border-color: rgba(16, 185, 129, 0.2);

    &:hover {
      background: rgba(16, 185, 129, 0.2);
      box-shadow: 0 0 12px rgba(16, 185, 129, 0.25);
      color: #6ee7b7;
    }
  }

  /* 禁用按钮：幽灵红 */
  .disable-btn {
    background: rgba(239, 68, 68, 0.1);
    color: #f87171;
    border-color: rgba(239, 68, 68, 0.2);

    &:hover {
      background: rgba(239, 68, 68, 0.2);
      box-shadow: 0 0 12px rgba(239, 68, 68, 0.25);
      color: #fca5a5;
    }
  }

  /* 滚动条美化（适配 Webkit） */
  ::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }
  ::-webkit-scrollbar-track {
    background: #1f1f1f;
  }
  ::-webkit-scrollbar-thumb {
    background: #374151;
    border-radius: 3px;
  }
  ::-webkit-scrollbar-thumb:hover {
    background: #4b5563;
  }

  :deep(.ant-card-body) {
    background: transparent !important;
  }
</style>
