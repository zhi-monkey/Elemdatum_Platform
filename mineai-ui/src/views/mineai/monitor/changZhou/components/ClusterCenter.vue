<template>
  <div
    class="cluster-card"
    @mouseenter="
      showControls = true;
      stopAutoScroll();
    "
    @mouseleave="
      showControls = false;
      startAutoScroll();
    "
  >
    <!-- 添加轮播控制 -->
    <div class="carousel-controls" v-show="showControls && nodes.length > 1">
      <div class="control-prev" @click="prevCluster">
        <LeftOutlined />
      </div>
      <div class="control-next" @click="nextCluster">
        <RightOutlined />
      </div>
    </div>

    <!-- 添加指示器 -->
    <div class="carousel-indicators" v-if="nodes.length > 1">
      <span
        v-for="(node, index) in nodes"
        :key="index"
        :class="{ active: index === currentClusterIndex }"
        @click="goToCluster(index)"
      ></span>
    </div>

    <!-- 集群信息内容 -->
    <div class="cluster-content" v-if="nodes.length > 0">
      <div class="resource-usage">
        <div class="usage-section">
          <h3>集群信息</h3>
          <div class="cpu-memory-container">
            <div class="usage-item cpu-usage">
              <div class="usage-header">
                <div class="usage-icon">
                  <DesktopOutlined />
                </div>
                <div class="usage-info">
                  <span class="usage-label">CPU 算力</span>
                  <span class="usage-value"
                    >{{ currentNode.cpuUsed }}/{{ currentNode.cpuTotal }} 核心</span
                  >
                </div>
                <span class="usage-percent">{{ currentNode.cpuUsage }}%</span>
              </div>
              <div class="tech-progress-container">
                <div class="progress-bg">
                  <div
                    class="progress-fill cpu-fill"
                    :style="{ width: currentNode.cpuUsage + '%' }"
                  >
                    <div class="progress-glow"></div>
                  </div>
                </div>
                <div class="progress-indicators">
                  <span class="indicator" :class="{ active: currentNode.cpuUsage >= 25 }"></span>
                  <span class="indicator" :class="{ active: currentNode.cpuUsage >= 50 }"></span>
                  <span class="indicator" :class="{ active: currentNode.cpuUsage >= 75 }"></span>
                  <span class="indicator" :class="{ active: currentNode.cpuUsage >= 90 }"></span>
                </div>
              </div>
            </div>

            <div class="usage-item memory-usage">
              <div class="usage-header">
                <div class="usage-icon">
                  <DatabaseOutlined />
                </div>
                <div class="usage-info">
                  <span class="usage-label">内存占用</span>
                  <span class="usage-value"
                    >{{ currentNode.memoryUsed }}/{{ currentNode.memoryTotal }} GB</span
                  >
                </div>
                <span class="usage-percent">{{ currentNode.memoryUsage }}%</span>
              </div>
              <div class="tech-progress-container">
                <div class="progress-bg">
                  <div
                    class="progress-fill memory-fill"
                    :style="{ width: currentNode.memoryUsage + '%' }"
                  >
                    <div class="progress-glow"></div>
                  </div>
                </div>
                <div class="progress-indicators">
                  <span class="indicator" :class="{ active: currentNode.memoryUsage >= 25 }"></span>
                  <span class="indicator" :class="{ active: currentNode.memoryUsage >= 50 }"></span>
                  <span class="indicator" :class="{ active: currentNode.memoryUsage >= 75 }"></span>
                  <span class="indicator" :class="{ active: currentNode.memoryUsage >= 90 }"></span>
                </div>
              </div>
            </div>
          </div>

          <div class="gpu-container">
            <h3>显卡占用情况</h3>
            <div class="gpu-list" v-if="currentNode.gpus && currentNode.gpus.length > 0">
              <div v-for="gpu in paginatedGpus" :key="gpu.name + gpu.memoryTotal" class="gpu-item">
                <div class="gpu-header">
                  <div class="gpu-icon">
                    <svg viewBox="0 0 1024 1024" width="20" height="20" fill="currentColor">
                      <path d="M608 768h32v60h-32z"></path><path d="M1010.07 243l-100-100.77A48 48 0 0 0 876 128H24a24 24 0 0 0-24 24v720a24 24 0 0 0 48 0V768h80v84h178v-84h32v84h448v-84h90.12a48 48 0 0 0 33.94-14.06l99.88-99.88a48 48 0 0 0 14.06-33.94V276.77a48 48 0 0 0-13.93-33.77zM282 828H152v-60h130z m480 0H362v-60h400z m214-207.88L876.12 720H48V176h828l100 100.77zM479.78 438.77a192.13 192.13 0 0 0-103.9-161.51 188 188 0 0 0-20.34-9A191.5 191.5 0 0 0 288 256q-4.63 0-9.23 0.22a192.13 192.13 0 0 0-161.51 103.9 188 188 0 0 0-9 20.34A191.5 191.5 0 0 0 96 448q0 4.63 0.22 9.23a192.13 192.13 0 0 0 103.9 161.51 188 188 0 0 0 20.34 9A191.5 191.5 0 0 0 288 640q4.65 0 9.24-0.22a192.14 192.14 0 0 0 161.5-103.9 188 188 0 0 0 9-20.34A191.5 191.5 0 0 0 480 448q0-4.63-0.22-9.23z m-14.3-4.43a116.45 116.45 0 0 1-119.77 39 62.32 62.32 0 0 0 4.84-17.77 130.52 130.52 0 0 0 94.8-90.93 176.42 176.42 0 0 1 20.13 69.7z m-51.62-112.2a180.49 180.49 0 0 1 21 25.22q-0.94 5.77-2.48 11.56a116.54 116.54 0 0 1-81.79 82.15 62.09 62.09 0 0 0-4.59-17.82 130.56 130.56 0 0 0 36.63-126.12 179.88 179.88 0 0 1 31.23 25.01z m-49-34.79q2.08 5.52 3.64 11.3a116.47 116.47 0 0 1-29.75 112 63.71 63.71 0 0 0-12.95-13.08 130.54 130.54 0 0 0-31.28-127.48 177 177 0 0 1 70.35 17.26zM335 448a46.77 46.77 0 0 1-1.18 10.46 45.82 45.82 0 0 1-3.37 9.7 47.09 47.09 0 0 1-15.78 18.52 46.62 46.62 0 0 1-12.82 6.23 45.59 45.59 0 0 1-10.1 1.94q-1.86 0.15-3.75 0.15a46.77 46.77 0 0 1-10.46-1.18 45.82 45.82 0 0 1-9.7-3.37 47.14 47.14 0 0 1-24.75-28.6 45.59 45.59 0 0 1-1.94-10.1q-0.15-1.86-0.15-3.75a46.77 46.77 0 0 1 1.18-10.46 45.82 45.82 0 0 1 3.37-9.7 47.14 47.14 0 0 1 28.6-24.75 45.59 45.59 0 0 1 10.1-1.94q1.86-0.15 3.75-0.15a46.77 46.77 0 0 1 10.46 1.18 45.82 45.82 0 0 1 9.7 3.37 47.14 47.14 0 0 1 24.75 28.6 45.59 45.59 0 0 1 1.94 10.1q0.15 1.86 0.15 3.75z m-60.66-177.48a116.45 116.45 0 0 1 39 119.77 61.91 61.91 0 0 0-17.76-4.83 130.53 130.53 0 0 0-90.94-94.81 176.42 176.42 0 0 1 69.7-20.13z m-112.2 51.62a180.49 180.49 0 0 1 25.22-21q5.77 0.94 11.56 2.48a116.54 116.54 0 0 1 82.15 81.79 62.09 62.09 0 0 0-17.82 4.66 130.56 130.56 0 0 0-126.12-36.63 179.88 179.88 0 0 1 25.01-31.3z m-23.49 45.35a116.47 116.47 0 0 1 112 29.75 63.71 63.71 0 0 0-13.08 12.95 130.54 130.54 0 0 0-127.48 31.28 177 177 0 0 1 17.23-70.34q5.55-2.13 11.33-3.64z m-28.13 94.17a116.45 116.45 0 0 1 119.77-39 61.91 61.91 0 0 0-4.83 17.76 130.5 130.5 0 0 0-94.81 90.94 176.42 176.42 0 0 1-20.13-69.7z m51.62 112.2a180.49 180.49 0 0 1-21-25.22q0.94-5.77 2.48-11.56a116.54 116.54 0 0 1 81.79-82.15 62.09 62.09 0 0 0 4.66 17.82 130.56 130.56 0 0 0-36.63 126.12 179.88 179.88 0 0 1-31.3-25.01z m49 34.79q-2.08-5.52-3.64-11.3a116.47 116.47 0 0 1 29.75-112 63.71 63.71 0 0 0 12.95 13.08 130.54 130.54 0 0 0 31.28 127.48 177 177 0 0 1-70.35-17.26z m90.53 16.83q-4.5-3.69-8.71-7.88a116.49 116.49 0 0 1-30.26-111.89 61.91 61.91 0 0 0 17.76 4.83 130.53 130.53 0 0 0 90.94 94.81 176.42 176.42 0 0 1-69.74 20.13z m112.2-51.62a180.49 180.49 0 0 1-25.22 21q-5.79-0.95-11.56-2.48a116.54 116.54 0 0 1-82.15-81.79 62.09 62.09 0 0 0 17.81-4.59 130.28 130.28 0 0 0 126.12 36.63 179.88 179.88 0 0 1-25.01 31.23z m23.49-45.35a116.47 116.47 0 0 1-112-29.75 63.71 63.71 0 0 0 13.08-12.95 130.78 130.78 0 0 0 127.48-31.28 177 177 0 0 1-17.23 70.34q-5.56 2.13-11.34 3.64z m490.43-89.74a192.13 192.13 0 0 0-103.9-161.51 188 188 0 0 0-20.34-9A191.5 191.5 0 0 0 736 256q-4.63 0-9.23 0.22a192.13 192.13 0 0 0-161.51 103.9 188 188 0 0 0-9 20.34A191.5 191.5 0 0 0 544 448q0 4.63 0.22 9.23a192.13 192.13 0 0 0 103.9 161.51 188 188 0 0 0 20.34 9A191.5 191.5 0 0 0 736 640q4.65 0 9.24-0.22a192.14 192.14 0 0 0 161.5-103.9 188 188 0 0 0 9-20.34A191.5 191.5 0 0 0 928 448q0-4.63-0.22-9.23z m-14.3-4.43a116.45 116.45 0 0 1-119.77 39 62.32 62.32 0 0 0 4.84-17.77 130.52 130.52 0 0 0 94.8-90.93 176.42 176.42 0 0 1 20.12 69.7z m-51.62-112.2a180.49 180.49 0 0 1 21 25.22q-0.95 5.77-2.48 11.56a116.54 116.54 0 0 1-81.79 82.15 62.09 62.09 0 0 0-4.6-17.82 130.56 130.56 0 0 0 36.63-126.12 179.88 179.88 0 0 1 31.23 25.01z m-49-34.79q2.08 5.52 3.64 11.3a116.47 116.47 0 0 1-29.75 112 63.71 63.71 0 0 0-13-13.08 130.54 130.54 0 0 0-31.28-127.48 177 177 0 0 1 70.39 17.26zM783 448a46.77 46.77 0 0 1-1.18 10.46 45.82 45.82 0 0 1-3.37 9.7 47.09 47.09 0 0 1-15.78 18.52 46.62 46.62 0 0 1-12.82 6.23 45.59 45.59 0 0 1-10.1 1.94q-1.86 0.15-3.75 0.15a46.77 46.77 0 0 1-10.46-1.18 45.82 45.82 0 0 1-9.7-3.37 47.14 47.14 0 0 1-24.75-28.6 45.59 45.59 0 0 1-1.94-10.1q-0.15-1.86-0.15-3.75a46.77 46.77 0 0 1 1.18-10.46 45.82 45.82 0 0 1 3.37-9.7 47.14 47.14 0 0 1 28.6-24.75 45.59 45.59 0 0 1 10.1-1.94q1.86-0.15 3.75-0.15a46.77 46.77 0 0 1 10.46 1.18 45.82 45.82 0 0 1 9.7 3.37 47.14 47.14 0 0 1 24.75 28.6 45.59 45.59 0 0 1 1.94 10.1q0.15 1.86 0.15 3.75z m-60.66-177.48a116.45 116.45 0 0 1 39 119.77 61.91 61.91 0 0 0-17.76-4.83 130.53 130.53 0 0 0-90.94-94.81 176.42 176.42 0 0 1 69.7-20.13z m-112.2 51.62a180.49 180.49 0 0 1 25.22-21q5.77 0.94 11.56 2.48a116.54 116.54 0 0 1 82.15 81.79 62.09 62.09 0 0 0-17.82 4.66 130.56 130.56 0 0 0-126.12-36.63 179.88 179.88 0 0 1 25.01-31.3z m-23.49 45.35a116.47 116.47 0 0 1 112 29.75 63.71 63.71 0 0 0-13.08 12.95 130.54 130.54 0 0 0-127.48 31.28 177 177 0 0 1 17.23-70.34q5.55-2.13 11.33-3.64z m-28.13 94.17a116.45 116.45 0 0 1 119.77-39 61.91 61.91 0 0 0-4.83 17.76 130.5 130.5 0 0 0-94.81 90.94 176.42 176.42 0 0 1-20.13-69.7z m51.62 112.2a180.49 180.49 0 0 1-21-25.22q0.95-5.77 2.48-11.56a116.54 116.54 0 0 1 81.79-82.15 62.09 62.09 0 0 0 4.66 17.82 130.56 130.56 0 0 0-36.63 126.12 179.88 179.88 0 0 1-31.3-25.01z m49 34.79q-2.08-5.52-3.64-11.3a116.47 116.47 0 0 1 29.75-112 63.71 63.71 0 0 0 13 13.08 130.54 130.54 0 0 0 31.28 127.48 177 177 0 0 1-70.4-17.26z m90.53 16.83q-4.5-3.69-8.71-7.88a116.49 116.49 0 0 1-30.26-111.89 61.91 61.91 0 0 0 17.76 4.83 130.53 130.53 0 0 0 90.94 94.81 176.42 176.42 0 0 1-69.74 20.13z m112.2-51.62a180.49 180.49 0 0 1-25.22 21q-5.79-0.95-11.56-2.48a116.54 116.54 0 0 1-82.15-81.79 62.09 62.09 0 0 0 17.81-4.59 130.28 130.28 0 0 0 126.12 36.63 179.88 179.88 0 0 1-25.01 31.23z m23.49-45.35a116.47 116.47 0 0 1-112-29.75 63.71 63.71 0 0 0 13.08-12.95 130.78 130.78 0 0 0 127.48-31.28 177 177 0 0 1-17.23 70.34q-5.56 2.13-11.34 3.64z"></path>
                    </svg>
                  </div>
                  <div class="gpu-info">
                    <span class="gpu-name">{{ gpu.name }}</span>
                  </div>
                </div>

                <div class="gpu-metrics">
                  <div class="metric-item">
                    <div class="metric-header">
                      <span class="metric-label">算力使用</span>
                      <span class="metric-value">{{ gpu.computeUsage }}%</span>
                    </div>
                    <div class="tech-progress-container">
                      <div class="progress-bg">
                        <div
                          class="progress-fill gpu-fill"
                          :style="{ width: gpu.computeUsage + '%' }"
                        >
                          <div class="progress-glow"></div>
                        </div>
                      </div>
                      <div class="progress-indicators">
                        <span class="indicator" :class="{ active: gpu.computeUsage >= 25 }"></span>
                        <span class="indicator" :class="{ active: gpu.computeUsage >= 50 }"></span>
                        <span class="indicator" :class="{ active: gpu.computeUsage >= 75 }"></span>
                        <span class="indicator" :class="{ active: gpu.computeUsage >= 90 }"></span>
                      </div>
                    </div>
                  </div>

                  <div class="metric-item">
                    <div class="metric-header">
                      <span class="metric-label">显存占用</span>
                      <span class="metric-value"
                        >{{ gpu.memoryUsed }}/{{ gpu.memoryTotal }} GB</span
                      >
                    </div>
                    <div class="tech-progress-container">
                      <div class="progress-bg">
                        <div
                          class="progress-fill memory-fill"
                          :style="{ width: gpu.memoryUsage + '%' }"
                        >
                          <div class="progress-glow"></div>
                        </div>
                      </div>
                      <div class="progress-indicators">
                        <span class="indicator" :class="{ active: gpu.memoryUsage >= 25 }"></span>
                        <span class="indicator" :class="{ active: gpu.memoryUsage >= 50 }"></span>
                        <span class="indicator" :class="{ active: gpu.memoryUsage >= 75 }"></span>
                        <span class="indicator" :class="{ active: gpu.memoryUsage >= 90 }"></span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 没有GPU的提示 -->
            <div v-else class="no-gpu">
              <div class="no-gpu-icon">
                <svg viewBox="0 0 24 24" width="48" height="48" fill="currentColor">
                  <path d="M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z" />
                </svg>
              </div>
              <div class="no-gpu-text">该节点没有GPU设备</div>
            </div>

            <!-- GPU分页控件 -->
            <div
              class="gpu-pagination"
              v-if="currentNode.gpus && currentNode.gpus.length > itemsPerPage"
            >
              <button class="page-btn" :disabled="currentPage === 0" @click="prevGpuPage">
                <LeftOutlined />
              </button>
              <span class="page-info"> {{ currentPage + 1 }} / {{ totalPages }} </span>
              <button
                class="page-btn"
                :disabled="currentPage === totalPages - 1"
                @click="nextGpuPage"
              >
                <RightOutlined />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-else class="loading-container">
      <div class="loading-spinner"></div>
      <div class="loading-text">加载集群数据中...</div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import {
    DesktopOutlined,
    DatabaseOutlined,
    LeftOutlined,
    RightOutlined,
  } from '@ant-design/icons-vue';
  import { ref, computed, onMounted, onUnmounted } from 'vue';
  import { getClusterData } from '/@/views/mineai/monitor/changZhou/data';

  interface GpuInfo {
    computeUsage: number;
    memoryUsage: number;
    computeUsed: number;
    computeTotal: number;
    memoryTotal: number;
    name: string;
    memoryUsed: number;
    status: string;
  }

  interface NodeInfo {
    cpuUsage: number;
    role: string;
    memoryUsage: number;
    cpuUsed: number;
    nodeIp: string;
    memoryUsed: number;
    cpuTotal: number;
    memoryTotal: number;
    gpus: GpuInfo[];
    name: string;
    key: string;
    status: string;
    lastUpdateTime: string;
  }

  interface ClusterInfo {
    readyNodeCount: number;
    clusterName: string;
    clusterPort: number;
    nodeCount: number;
    clusterIp: string;
    lastUpdateTime: string;
  }

  interface ApiResponse {
    nodes: NodeInfo[];
    cluster: ClusterInfo;
  }

  // 响应式数据
  const nodes = ref<NodeInfo[]>([]);
  const clusterInfo = ref<ClusterInfo | null>(null);
  const loading = ref(true);

  // 当前显示的节点索引
  const currentClusterIndex = ref(0);
  // 自动轮播定时器
  const autoScrollTimer = ref<ReturnType<typeof setInterval> | null>(null);
  // 是否显示控制按钮
  const showControls = ref(false);
  // GPU分页相关
  const itemsPerPage = 4;
  const currentPage = ref(0);

  // 获取当前节点信息
  const currentNode = computed(() => nodes.value[currentClusterIndex.value]);

  // 计算当前页的GPU列表
  const paginatedGpus = computed(() => {
    if (!currentNode.value.gpus) return [];
    const start = currentPage.value * itemsPerPage;
    const end = start + itemsPerPage;
    return currentNode.value.gpus.slice(start, end);
  });

  // 总页数
  const totalPages = computed(() => {
    if (!currentNode.value.gpus) return 0;
    return Math.ceil(currentNode.value.gpus.length / itemsPerPage);
  });

  // 加载数据
  const loadData = async () => {
    try {
      loading.value = true;
      const data = await getClusterData();
      nodes.value = data.nodes;
      clusterInfo.value = data.cluster;

      // 根据节点数量设置轮播
      if (data.cluster.nodeCount > 0) {
        currentClusterIndex.value = 0;
        currentPage.value = 0;
      }
    } catch (error) {
      console.error('加载集群数据失败:', error);
    } finally {
      loading.value = false;
    }
  };

  // 开始自动轮播
  const startAutoScroll = () => {
    if (autoScrollTimer.value || nodes.value.length <= 1) {
      return;
    }

    autoScrollTimer.value = setInterval(() => {
      nextCluster();
    }, 5000); // 每5秒切换一次
  };

  // 停止自动轮播
  const stopAutoScroll = () => {
    if (autoScrollTimer.value) {
      clearInterval(autoScrollTimer.value);
      autoScrollTimer.value = null;
    }
  };

  // 切换到下一个节点
  const nextCluster = () => {
    if (nodes.value.length === 0) return;
    currentClusterIndex.value = (currentClusterIndex.value + 1) % nodes.value.length;
    // 重置GPU分页
    currentPage.value = 0;
  };

  // 切换到上一个节点
  const prevCluster = () => {
    if (nodes.value.length === 0) return;
    currentClusterIndex.value =
      currentClusterIndex.value === 0 ? nodes.value.length - 1 : currentClusterIndex.value - 1;
    // 重置GPU分页
    currentPage.value = 0;
  };

  // 跳转到指定节点
  const goToCluster = (index: number) => {
    if (index >= 0 && index < nodes.value.length) {
      currentClusterIndex.value = index;
      // 重置GPU分页
      currentPage.value = 0;
    }
  };

  // 上一页GPU
  const prevGpuPage = () => {
    if (currentPage.value > 0) {
      currentPage.value--;
    }
  };

  // 下一页GPU
  const nextGpuPage = () => {
    if (currentPage.value < totalPages.value - 1) {
      currentPage.value++;
    }
  };

  // 组件挂载时加载数据并启动自动轮播
  onMounted(() => {
    loadData().then(() => {
      startAutoScroll();
    });
  });

  // 组件销毁时清理定时器
  onUnmounted(() => {
    stopAutoScroll();
  });
</script>

<style scoped lang="less">
  .cluster-card {
    position: relative;
    padding: 20px;
    background: rgba(26, 30, 48, 0.2);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border-radius: 12px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    width: 100%;
    height: 100%;
    color: #e6e6e6;
    display: flex;
    flex-direction: column;
  }

  .loading-container {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    height: 100%;
    gap: 20px;
  }

  .loading-spinner {
    width: 40px;
    height: 40px;
    border: 4px solid rgba(64, 158, 255, 0.3);
    border-left: 4px solid #409eff;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  .loading-text {
    color: #409eff;
    font-size: 16px;
  }

  .no-gpu {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
    color: #bfbfbf;

    .no-gpu-icon {
      margin-bottom: 16px;
      opacity: 0.5;
    }

    .no-gpu-text {
      font-size: 14px;
    }
  }

  .metric-detail {
    font-size: 11px;
    color: #bfbfbf;
    margin-top: 4px;
    text-align: center;
  }

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(360deg);
    }
  }

  /* 其他样式保持不变 */
  .carousel-controls {
    position: absolute;
    top: 30vh;
    transform: translateY(-50%);
    width: 100%;
    display: flex;
    justify-content: space-between;
    z-index: 10;
    left: 0;
    padding: 0 10px;
    box-sizing: border-box;

    .control-prev,
    .control-next {
      width: 36px;
      height: 36px;
      background-color: rgba(255, 255, 255, 0.15);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        background-color: rgba(64, 158, 255, 0.4);
        transform: scale(1.1);
      }

      svg {
        color: #e6e6e6;
        font-size: 18px;
      }
    }
  }

  .carousel-indicators {
    position: absolute;
    bottom: 15px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 10px;
    z-index: 10;

    span {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background-color: rgba(255, 255, 255, 0.4);
      cursor: pointer;
      transition: all 0.3s ease;

      &.active {
        background-color: #409eff;
        transform: scale(1.2);
      }
    }
  }

  .cluster-content {
    transition: transform 0.5s ease;
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
    overflow-y: auto;
  }

  .resource-usage {
    display: flex;
    flex-direction: column;
    min-height: 100%;

    .usage-section {
      display: flex;
      flex-direction: column;

      h3 {
        font-size: 16px;
        font-weight: bold;
        margin-bottom: 15px;
        color: #ffffff;
      }

      .cpu-memory-container {
        display: flex;
        gap: 20px;
        margin-bottom: 20px;

        .usage-item {
          flex: 1;
          padding: 20px;
          background: rgba(26, 30, 48, 0.4);
          backdrop-filter: blur(10px);
          -webkit-backdrop-filter: blur(10px);
          border-radius: 12px;
          border: 1px solid rgba(64, 158, 255, 0.2);
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
          position: relative;
          overflow: hidden;

          &::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 2px;
            background: linear-gradient(90deg, transparent, #409eff, transparent);
            opacity: 0.6;
          }

          .usage-header {
            display: flex;
            align-items: center;
            margin-bottom: 16px;

            .usage-icon {
              width: 40px;
              height: 40px;
              background: linear-gradient(135deg, #409eff, #1890ff);
              border-radius: 8px;
              display: flex;
              align-items: center;
              justify-content: center;
              margin-right: 12px;
              box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);

              svg {
                color: #ffffff;
                font-size: 18px;
              }
            }

            .usage-info {
              flex: 1;
              display: flex;
              flex-direction: column;

              .usage-label {
                font-size: 14px;
                font-weight: 600;
                color: #e6f7ff;
                margin-bottom: 2px;
              }

              .usage-value {
                font-size: 12px;
                color: #bfbfbf;
              }
            }

            .usage-percent {
              font-size: 20px;
              font-weight: bold;
              color: #409eff;
              text-shadow: 0 0 10px rgba(64, 158, 255, 0.5);
            }
          }

          .tech-progress-container {
            position: relative;

            .progress-bg {
              height: 8px;
              background: rgba(0, 0, 0, 0.3);
              border-radius: 4px;
              overflow: hidden;
              position: relative;

              .progress-fill {
                height: 100%;
                border-radius: 4px;
                position: relative;
                transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);

                &.cpu-fill {
                  background: linear-gradient(90deg, #409eff, #1890ff, #40a9ff);
                }

                &.memory-fill {
                  background: linear-gradient(90deg, #52c41a, #73d13d, #95de64);
                }

                &.gpu-fill {
                  background: linear-gradient(90deg, #722ed1, #9254de, #b37feb);
                }

                .progress-glow {
                  position: absolute;
                  top: 0;
                  right: 0;
                  width: 20px;
                  height: 100%;
                  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.6));
                  animation: progressGlow 2s ease-in-out infinite;
                }
              }
            }

            .progress-indicators {
              display: flex;
              justify-content: space-between;
              margin-top: 8px;

              .indicator {
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background: rgba(255, 255, 255, 0.2);
                transition: all 0.3s ease;

                &.active {
                  background: #409eff;
                  box-shadow: 0 0 8px rgba(64, 158, 255, 0.6);
                  transform: scale(1.2);
                }
              }
            }
          }
        }
      }

      .gpu-container {
        display: flex;
        flex-direction: column;

        h3 {
          font-size: 16px;
          font-weight: bold;
          margin-bottom: 15px;
          color: #ffffff;
        }

        .gpu-list {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 12px;
          margin-bottom: 10px;

          .gpu-item {
            padding: 16px;
            background: rgba(26, 30, 48, 0.4);
            backdrop-filter: blur(10px);
            -webkit-backdrop-filter: blur(10px);
            border-radius: 12px;
            border: 1px solid rgba(64, 158, 255, 0.2);
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
            position: relative;
            overflow: hidden;

            &::before {
              content: '';
              position: absolute;
              top: 0;
              left: 0;
              right: 0;
              height: 2px;
              background: linear-gradient(90deg, transparent, #722ed1, transparent);
              opacity: 0.6;
            }

            .gpu-header {
              display: flex;
              align-items: center;
              margin-bottom: 12px;

              .gpu-icon {
                width: 36px;
                height: 36px;
                background: linear-gradient(135deg, #722ed1, #9254de);
                border-radius: 8px;
                display: flex;
                align-items: center;
                justify-content: center;
                margin-right: 12px;
                box-shadow: 0 4px 12px rgba(114, 46, 209, 0.3);

                svg {
                  color: #ffffff;
                  width: 18px;
                  height: 18px;
                }
              }

              .gpu-info {
                flex: 1;

                .gpu-name {
                  display: block;
                  font-weight: 600;
                  color: #e6f7ff;
                  font-size: 14px;
                }
              }
            }

            .gpu-metrics {
              .metric-item {
                margin-bottom: 8px;

                &:last-child {
                  margin-bottom: 0;
                }

                .metric-header {
                  display: flex;
                  justify-content: space-between;
                  margin-bottom: 6px;

                  .metric-label {
                    font-size: 12px;
                    color: #bfbfbf;
                    font-weight: 500;
                  }

                  .metric-value {
                    font-size: 12px;
                    color: #409eff;
                    font-weight: 600;
                  }
                }

                .tech-progress-container {
                  position: relative;

                  .progress-bg {
                    height: 6px;
                    background: rgba(0, 0, 0, 0.3);
                    border-radius: 3px;
                    overflow: hidden;
                    position: relative;

                    .progress-fill {
                      height: 100%;
                      border-radius: 3px;
                      position: relative;
                      transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);

                      &.gpu-fill {
                        background: linear-gradient(90deg, #722ed1, #9254de, #b37feb);
                      }

                      &.memory-fill {
                        background: linear-gradient(90deg, #52c41a, #73d13d, #95de64);
                      }

                      .progress-glow {
                        position: absolute;
                        top: 0;
                        right: 0;
                        width: 15px;
                        height: 100%;
                        background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.6));
                        animation: progressGlow 2s ease-in-out infinite;
                      }
                    }
                  }

                  .progress-indicators {
                    display: flex;
                    justify-content: space-between;
                    margin-top: 6px;

                    .indicator {
                      width: 6px;
                      height: 6px;
                      border-radius: 50%;
                      background: rgba(255, 255, 255, 0.2);
                      transition: all 0.3s ease;

                      &.active {
                        background: #722ed1;
                        box-shadow: 0 0 8px rgba(114, 46, 209, 0.6);
                        transform: scale(1.2);
                      }
                    }
                  }
                }
              }
            }
          }
        }

        .gpu-pagination {
          display: flex;
          justify-content: center;
          align-items: center;
          gap: 15px;
          margin-top: 10px;

          .page-btn {
            width: 36px;
            height: 36px;
            border: none;
            background: rgba(42, 46, 66, 0.3);
            backdrop-filter: blur(5px);
            -webkit-backdrop-filter: blur(5px);
            border-radius: 6px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.3s ease;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);

            &:hover:not(:disabled) {
              background: rgba(58, 62, 82, 0.4);
              backdrop-filter: blur(8px);
              -webkit-backdrop-filter: blur(8px);
              transform: translateY(-2px);
            }

            &:disabled {
              opacity: 0.5;
              cursor: not-allowed;
            }

            svg {
              font-size: 14px;
              color: #ffffff;
            }
          }

          .page-info {
            font-size: 14px;
            color: #bfbfbf;
          }
        }
      }
    }
  }

  @keyframes progressGlow {
    0%,
    100% {
      opacity: 0.3;
    }
    50% {
      opacity: 1;
    }
  }

  @media (max-width: 768px) {
    .cpu-memory-container {
      flex-direction: column;
    }

    .gpu-list {
      grid-template-columns: 1fr !important;
    }

    .cluster-card {
      width: 100%;
    }
  }
</style>
