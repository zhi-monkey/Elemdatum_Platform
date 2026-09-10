<template>
  <div class="video-app">
    <!-- 鼠标悬停区域 -->
    <div
      class="hover-trigger-area"
      :class="{ locked: isSidebarLocked }"
      @mouseenter="showSidebar"
      @mouseleave="tryHideSidebar"
    >
      <div class="hover-indicator" v-if="!isSidebarVisible && !isSidebarLocked">
        <span>▶</span>
      </div>
    </div>

    <!-- 侧边栏容器 -->
    <div
      class="video-sidebar"
      :class="{
        visible: isSidebarVisible || isSidebarLocked,
        locked: isSidebarLocked,
      }"
      @mouseenter="handleSidebarEnter"
      @mouseleave="handleSidebarLeave"
    >
      <!-- 标题和锁定按钮 -->
      <div class="sidebar-header">
        <h3>视频目录</h3>
        <div class="header-controls">
          <button
            class="refresh-btn"
            @click="loadVideos"
            :disabled="loadingVideos"
            :title="'刷新视频列表'"
          >
            <span class="refresh-icon">{{ loadingVideos ? '🔄' : '🔄' }}</span>
            <span v-if="loadingVideos" class="loading-text">刷新中...</span>
          </button>
          <button
            class="lock-btn"
            @click="toggleLock"
            :class="{ active: isSidebarLocked }"
            :title="isSidebarLocked ? '解锁侧边栏' : '锁定侧边栏'"
          >
            <span class="lock-icon" v-if="isSidebarLocked">🔒</span>
            <span class="lock-icon" v-else>🔓</span>
          </button>
        </div>
      </div>

      <!-- 视频列表 -->
      <div v-if="videos.length > 0" class="sidebar-content">
        <div
          v-for="(video, index) in videos"
          :key="video.url"
          class="video-item"
          :class="{ active: currentVideoIndex === index }"
          @click="playVideo(video, index)"
        >
          <!-- 视频封面 -->
          <div class="video-cover">
            <img v-if="video.cover" :src="video.cover" :alt="video.title" loading="lazy" />
            <div v-else class="default-cover">
              <div class="play-icon">▶</div>
            </div>
            <!-- 当前播放标识 -->
            <div v-if="currentVideoIndex === index" class="playing-indicator">
              <div class="playing-dot"></div>
            </div>
          </div>

          <!-- 视频信息 -->
          <div class="video-info">
            <div class="video-name">
              {{ video.title }}
            </div>
            <div v-if="currentVideoIndex === index" class="current-playing"> 正在播放 </div>
          </div>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-else-if="loadingVideos" class="loading-videos">
        <div class="loading-spinner"></div>
        <p>加载视频列表中...</p>
      </div>

      <!-- 无视频提示 -->
      <div v-else class="no-videos">
        <div class="no-data-icon">📹</div>
        <p>暂无视频</p>
        <button class="retry-btn" @click="loadVideos">重新加载</button>
      </div>

      <!-- 错误信息 -->
      <div v-if="error" class="error-message">
        <div class="error-icon">❌</div>
        <p>加载失败: {{ error }}</p>
        <button class="retry-btn" @click="loadVideos">重试</button>
      </div>
    </div>

    <!-- 主播放区域 -->
    <div class="video-main" :class="{ 'sidebar-visible': isSidebarVisible || isSidebarLocked }">
      <!-- 视频标题显示区域 -->
      <div v-if="currentVideo.title" class="video-title-wrapper">
        <h1 class="video-current-title">{{ currentVideo.title }}</h1>
        <div class="video-divider"></div>
      </div>

      <!-- 视频播放器 -->
      <div class="video-player-container">
        <VideoPlayer
          ref="videoPlayerRef"
          :src="currentVideo.src"
          :poster="currentVideo.poster"
          :controls="playerOptions.controls"
          :autoplay="playerOptions.autoplay"
          :fluid="playerOptions.fluid"
          :playback-rates="playerOptions.playbackRates"
          :control-bar="playerOptions.controlBar"
          :language="playerOptions.language"
          :muted="playerOptions.muted"
          :loop="playerOptions.loop"
          :preload="playerOptions.preload"
          :aspect-ratio="playerOptions.aspectRatio"
        />
      </div>

      <!-- 视频切换控制条 -->
      <div class="video-control-bar">
        <button
          class="control-btn prev-btn"
          @click="playPrevVideo"
          :disabled="!hasPrevVideo || loadingVideos"
          :class="{ disabled: !hasPrevVideo || loadingVideos }"
        >
          <span class="btn-icon">◀</span>
          <span class="btn-text">上一个</span>
        </button>

        <div class="current-video-info">
          <span class="video-index">{{ currentVideoIndex + 1 }} / {{ videos.length }}</span>
        </div>

        <button
          class="control-btn next-btn"
          @click="playNextVideo"
          :disabled="!hasNextVideo || loadingVideos"
          :class="{ disabled: !hasNextVideo || loadingVideos }"
        >
          <span class="btn-text">下一个</span>
          <span class="btn-icon">▶</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from 'vue';
  import { VideoPlayer } from '@videojs-player/vue';
  import 'video.js/dist/video-js.css';
  import { getMinIOAuth } from '/@/views/mineai/data/dataset-details2/api';
  import { minIOConfig } from '/@/utils/dubhe';
  import { decrypt } from '/@/utils/dubhe/rsaEncrypt';
  import { Minio } from 'minio-js';

  // 视频播放器引用
  const videoPlayerRef = ref(null);

  // 侧边栏状态
  const isSidebarVisible = ref(false);
  const isSidebarLocked = ref(false);
  let sidebarHideTimeout: number | null = null;

  // 视频数据接口
  interface VideoItem {
    filename: string;
    title: string;
    cover?: string;
    url: string;
    fullPath: string;
  }

  // 响应式数据
  const videos = ref<VideoItem[]>([]);
  const currentVideoIndex = ref(-1);
  const loadingVideos = ref(false);
  const error = ref<string>('');

  // 计算属性：是否有上一个视频
  const hasPrevVideo = computed(() => {
    return currentVideoIndex.value > 0;
  });

  // 计算属性：是否有下一个视频
  const hasNextVideo = computed(() => {
    return currentVideoIndex.value >= 0 && currentVideoIndex.value < videos.value.length - 1;
  });

  // 当前视频数据
  const currentVideo = reactive({
    src: '',
    poster: '',
    title: '',
  });

  // 播放器选项
  const playerOptions = reactive({
    controls: true,
    autoplay: false,
    fluid: true,
    loop: false,
    muted: false,
    preload: 'auto',
    aspectRatio: '16:9',
    language: 'zh-CN',
    playbackRates: [0.5, 0.75, 1, 1.25, 1.5, 2],
    controlBar: {
      remainingTimeDisplay: false,
      playToggle: true,
      progressControl: true,
      volumePanel: true,
      fullscreenToggle: true,
      playbackRateMenuButton: true,
      currentTimeDisplay: true,
      timeDivider: true,
      durationDisplay: true,
    },
  });

  // 显示侧边栏
  const showSidebar = () => {
    if (sidebarHideTimeout) {
      clearTimeout(sidebarHideTimeout);
      sidebarHideTimeout = null;
    }
    isSidebarVisible.value = true;
  };

  // 尝试隐藏侧边栏
  const tryHideSidebar = () => {
    if (!isSidebarLocked.value) {
      sidebarHideTimeout = window.setTimeout(() => {
        isSidebarVisible.value = false;
      }, 200);
    }
  };

  // 处理侧边栏鼠标进入
  const handleSidebarEnter = () => {
    if (sidebarHideTimeout) {
      clearTimeout(sidebarHideTimeout);
      sidebarHideTimeout = null;
    }
  };

  // 处理侧边栏鼠标离开
  const handleSidebarLeave = () => {
    if (!isSidebarLocked.value) {
      isSidebarVisible.value = false;
    }
  };

  // 切换锁定状态
  const toggleLock = () => {
    isSidebarLocked.value = !isSidebarLocked.value;
    if (isSidebarLocked.value) {
      isSidebarVisible.value = true;
    }
  };

  // 自然排序函数：支持数字和字母混合排序
  const naturalSort = (a: string, b: string): number => {
    const regex = /(\d+)|(\D+)/g;
    const aParts = a.match(regex) || [];
    const bParts = b.match(regex) || [];

    for (let i = 0; i < Math.max(aParts.length, bParts.length); i++) {
      const aPart = aParts[i] || '';
      const bPart = bParts[i] || '';

      // 如果都是数字，按数值比较
      if (/^\d+$/.test(aPart) && /^\d+$/.test(bPart)) {
        const diff = parseInt(aPart, 10) - parseInt(bPart, 10);
        if (diff !== 0) return diff;
      } else {
        // 否则按字符串比较
        const diff = aPart.localeCompare(bPart, 'zh-CN', { numeric: true, sensitivity: 'base' });
        if (diff !== 0) return diff;
      }
    }
    return 0;
  };

  // 从视频文件获取封面
  const getVideoThumbnail = (videoUrl: string): Promise<string> => {
    return new Promise((resolve) => {
      const video = document.createElement('video');
      video.crossOrigin = 'anonymous';
      video.preload = 'metadata';
      video.currentTime = 1;

      video.onloadeddata = () => {
        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;

        const ctx = canvas.getContext('2d');
        if (ctx) {
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          const thumbnail = canvas.toDataURL('image/jpeg', 0.8);
          resolve(thumbnail);
        } else {
          resolve('');
        }
      };

      video.onerror = () => {
        resolve('');
      };

      video.src = videoUrl;
    });
  };

  // 从MinIO获取视频文件
  const loadVideos = async () => {
    try {
      loadingVideos.value = true;
      error.value = '';
      videos.value = [];
      currentVideoIndex.value = -1;
      const authInfo = await getMinIOAuth();
      if (!authInfo) {
        throw new Error('获取MinIO认证失败');
      }

      const { accessKey, privateKey, secretKey } = authInfo;
      const rawAccessKey = decrypt(accessKey, privateKey);
      const rawSecretKey = decrypt(secretKey, privateKey);
      const config = {
        ...minIOConfig.config,
        accessKey: rawAccessKey,
        secretKey: rawSecretKey,
      };

      const client = new Minio.Client(config);
      const bucket = minIOConfig.bucketName;
      const folder = 'demo-videos/'; // 只查找videos文件夹下的文件
      const mp4Files: any[] = [];

      await new Promise((resolve, reject) => {
        const stream = client.listObjects(bucket, folder, true);

        stream.on('data', (obj) => {
          if (obj.name && obj.name.toLowerCase().endsWith('.mp4')) {
            mp4Files.push({
              name: obj.name,
              size: obj.size,
              fullPath: obj.name,
            });
          }
        });

        stream.on('end', () => {
          resolve(mp4Files);
        });

        stream.on('error', (err) => {
          reject(err);
        });
      });

      // 按文件名进行自然排序
      mp4Files.sort((a, b) => {
        const fileNameA = a.name.split('/').pop() || a.name;
        const fileNameB = b.name.split('/').pop() || b.name;
        return naturalSort(fileNameA, fileNameB);
      });

      console.log(`找到 ${mp4Files.length} 个视频文件`);

      if (mp4Files.length === 0) {
        videos.value = [];
        return;
      }

      const videoItems: VideoItem[] = [];

      for (const file of mp4Files) {
        try {
          // 获取带签名的URL（7天有效期）
          const url = await client.presignedGetObject(bucket, file.fullPath, 7 * 24 * 60 * 60);

          // 从文件名中提取标题
          const fileName = file.name.split('/').pop() || file.name;
          const title = fileName
            .replace(/\.[^/.]+$/, '')
            .replace(/_/g, ' ')
            .replace(/-/g, ' ');

          videoItems.push({
            filename: fileName,
            title: title,
            url: url,
            fullPath: file.fullPath,
            cover: '', // 封面稍后异步生成
          });
        } catch (err) {
          console.error(`获取视频 ${file.name} 的URL失败:`, err);
        }
      }

      videos.value = videoItems;
      if (videoItems.length > 0) {
        currentVideoIndex.value = 0;
        const firstVideo = videoItems[0];
        currentVideo.src = firstVideo.url;
        currentVideo.title = firstVideo.title;
        currentVideo.poster = '';

        // 异步生成封面
        generateVideoCovers();
      }
    } catch (err) {
      console.error('加载MinIO视频列表失败:', err);
      error.value = (err as Error).message || '加载视频列表失败';
    } finally {
      loadingVideos.value = false;
    }
  };

  // 异步生成所有视频的封面
  const generateVideoCovers = async () => {
    for (let i = 0; i < videos.value.length; i++) {
      const video = videos.value[i];
      if (!video.cover) {
        try {
          const thumbnail = await getVideoThumbnail(video.url);
          if (thumbnail) {
            video.cover = thumbnail;
            // 如果当前视频正在播放，也更新封面
            if (currentVideoIndex.value === i) {
              currentVideo.poster = thumbnail;
            }
          }
        } catch (err) {
          console.error(`生成视频封面失败: ${video.title}`, err);
        }
      }
    }
  };

  // 播放视频
  const playVideo = (video: VideoItem, index?: number) => {
    currentVideo.src = video.url;
    currentVideo.poster = video.cover || '';
    currentVideo.title = video.title;

    // 更新当前视频索引
    if (index !== undefined) {
      currentVideoIndex.value = index;
    } else {
      const foundIndex = videos.value.findIndex((v) => v.url === video.url);
      if (foundIndex !== -1) {
        currentVideoIndex.value = foundIndex;
      }
    }
  };

  // 播放上一个视频
  const playPrevVideo = () => {
    if (hasPrevVideo.value && !loadingVideos.value) {
      const prevIndex = currentVideoIndex.value - 1;
      const prevVideo = videos.value[prevIndex];
      playVideo(prevVideo, prevIndex);
    }
  };

  // 播放下一个视频
  const playNextVideo = () => {
    if (hasNextVideo.value && !loadingVideos.value) {
      const nextIndex = currentVideoIndex.value + 1;
      const nextVideo = videos.value[nextIndex];
      playVideo(nextVideo, nextIndex);
    }
  };

  // 清理播放器资源
  onUnmounted(() => {
    const player = videoPlayerRef.value?.player;
    if (player) {
      player.dispose();
    }
    if (sidebarHideTimeout) {
      clearTimeout(sidebarHideTimeout);
    }
  });

  // 组件挂载时加载
  onMounted(() => {
    loadVideos();
  });
</script>

<style scoped>
  /* 视频切换控制条样式 */
  .video-control-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
    padding: 0 10px;
  }

  .control-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 8px 16px;
    border: 1px solid #e0e0e0;
    border-radius: 6px;
    background: #fff;
    color: #333;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
    min-width: 100px;
  }

  .control-btn:hover:not(.disabled) {
    background: #f5f5f5;
    border-color: #1890ff;
    color: #1890ff;
  }

  .control-btn:active:not(.disabled) {
    transform: translateY(1px);
  }

  .control-btn.disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background: #f9f9f9;
  }

  .btn-icon {
    font-size: 12px;
  }

  .btn-text {
    font-size: 13px;
  }

  .current-video-info {
    font-size: 14px;
    color: #666;
    font-weight: 500;
  }

  .video-index {
    background: #f0f0f0;
    padding: 4px 12px;
    border-radius: 4px;
  }

  .video-app {
    display: flex;
    height: 100vh;
    background: white !important;
    position: relative;
  }

  /* 鼠标悬停触发区域 */
  .hover-trigger-area {
    position: fixed;
    left: 0;
    top: 0;
    width: 50px;
    height: 100%;
    z-index: 100;
    transition: background-color 0.3s ease;
  }

  .hover-trigger-area:hover {
    background-color: rgba(24, 144, 255, 0.1);
  }

  .hover-indicator {
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    background: rgba(24, 144, 255, 0.9);
    color: white;
    width: 20px;
    height: 40px;
    border-radius: 0 4px 4px 0;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0.8;
    transition: all 0.3s ease;
  }

  .hover-trigger-area:hover .hover-indicator {
    opacity: 1;
    transform: translateY(-50%) translateX(5px);
  }

  /* 侧边栏样式 */
  .video-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    width: 280px;
    height: 100vh;
    background: white !important;
    border-right: 1px solid #e0e0e0;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    box-shadow: 2px 0 5px rgba(0, 0, 0, 0.05);
    transform: translateX(-100%);
    transition: transform 0.3s ease;
    z-index: 999;
  }

  .video-sidebar.visible {
    transform: translateX(0);
  }

  .video-sidebar.locked {
    box-shadow: 2px 0 10px rgba(0, 0, 0, 0.15);
  }

  .sidebar-header {
    padding: 20px 20px 15px;
    border-bottom: 1px solid #f0f0f0;
    background: white !important;
  }

  .sidebar-header h3 {
    margin: 0 0 10px 0;
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }

  .header-controls {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  /* 刷新按钮样式 */
  .refresh-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 12px;
    background: #f5f5f5;
    border: 1px solid #e0e0e0;
    border-radius: 4px;
    font-size: 12px;
    color: #666;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .refresh-btn:hover:not(:disabled) {
    background: #e6f7ff;
    border-color: #1890ff;
    color: #1890ff;
  }

  .refresh-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .refresh-icon {
    font-size: 14px;
  }

  .loading-text {
    color: #999;
  }

  /* 锁定按钮样式 */
  .lock-btn {
    background: none;
    border: none;
    width: 32px;
    height: 32px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 16px;
    color: #666;
    transition: all 0.2s ease;
  }

  .lock-btn:hover {
    background: #f5f5f5;
    color: #1890ff;
  }

  .lock-btn.active {
    background: #e6f7ff;
    color: #1890ff;
  }

  .sidebar-content {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .video-item {
    display: flex;
    align-items: center;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s ease;
    border: 1px solid #f0f0f0;
    background: #fff;
    gap: 12px;
  }

  /* 高亮当前播放视频的样式 */
  .video-item.active {
    background: linear-gradient(135deg, rgba(24, 144, 255, 0.1) 0%, rgba(24, 144, 255, 0.05) 100%);
    border-color: #1890ff;
    border-left: 3px solid #1890ff;
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
  }

  .video-item:hover {
    background: #f8f9fa;
    border-color: #1890ff;
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
  }

  .video-item.active:hover {
    background: linear-gradient(135deg, rgba(24, 144, 255, 0.15) 0%, rgba(24, 144, 255, 0.08) 100%);
  }

  .video-cover {
    width: 80px;
    height: 60px;
    border-radius: 6px;
    overflow: hidden;
    flex-shrink: 0;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
  }

  .video-cover img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s ease;
  }

  .video-item:hover .video-cover img {
    transform: scale(1.05);
  }

  .video-item.active .video-cover img {
    box-shadow: 0 0 0 2px #1890ff;
  }

  /* 当前播放标识 */
  .playing-indicator {
    position: absolute;
    top: 5px;
    right: 5px;
    width: 16px;
    height: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .playing-dot {
    width: 8px;
    height: 8px;
    background: #52c41a;
    border-radius: 50%;
    animation: pulse 1.5s infinite;
    box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.3);
  }

  @keyframes pulse {
    0% {
      box-shadow: 0 0 0 0 rgba(82, 196, 26, 0.5);
    }
    70% {
      box-shadow: 0 0 0 8px rgba(82, 196, 26, 0);
    }
    100% {
      box-shadow: 0 0 0 0 rgba(82, 196, 26, 0);
    }
  }

  .default-cover {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    color: white;
    font-size: 20px;
  }

  .play-icon {
    opacity: 0.8;
    font-size: 16px;
  }

  .video-info {
    flex: 1;
    min-width: 0;
  }

  .video-name {
    font-size: 14px;
    color: #333;
    font-weight: 500;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  /* 正在播放标签样式 */
  .current-playing {
    font-size: 12px;
    color: #52c41a;
    font-weight: 500;
    margin-top: 4px;
    padding: 2px 6px;
    background: rgba(82, 196, 26, 0.1);
    border-radius: 4px;
    display: inline-block;
    animation: fadeIn 0.3s ease;
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(-5px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .video-item.active .video-name {
    color: #1890ff;
    font-weight: 600;
  }

  /* 加载状态样式 */
  .loading-videos {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    color: #666;
    font-size: 14px;
    padding: 20px;
  }

  .loading-spinner {
    width: 40px;
    height: 40px;
    border: 3px solid #f0f0f0;
    border-top-color: #1890ff;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(360deg);
    }
  }

  /* 无视频提示 */
  .no-videos {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    color: #999;
    font-size: 14px;
    padding: 20px;
    text-align: center;
  }

  /* 错误信息样式 */
  .error-message {
    padding: 20px;
    text-align: center;
    color: #f5222d;
  }

  .error-icon {
    font-size: 40px;
    margin-bottom: 10px;
  }

  .retry-btn {
    margin-top: 10px;
    padding: 8px 16px;
    background: #1890ff;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 13px;
  }

  .retry-btn:hover {
    background: #40a9ff;
  }

  .no-data-icon {
    font-size: 40px;
    opacity: 0.6;
  }

  /* 主播放区域样式 */
  .video-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background: white !important;
    transition: margin-left 0.3s ease;
  }

  .video-main.sidebar-visible {
    margin-left: 280px;
  }

  .video-player-container {
    width: 100%;
    max-width: 1000px;
    background: white !important;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  /* 视频标题显示样式 */
  .video-title-wrapper {
    width: 100%;
    max-width: 1000px;
    margin-top: 20px;
    text-align: center;
  }

  .video-current-title {
    font-size: 24px;
    font-weight: 600;
    color: #333;
    margin: 0;
    padding: 0 20px;
    line-height: 1.4;
    word-break: break-word;
  }

  .video-divider {
    height: 2px;
    width: 60px;
    background: linear-gradient(to right, #1890ff, #52c41a);
    margin: 10px auto 0;
    border-radius: 1px;
  }

  /* 强制显示当前时间和总时间 */
  .video-player-container :deep(.vjs-current-time) {
    display: inline-block !important;
    visibility: visible !important;
    opacity: 1 !important;
    padding-right: 2px !important;
  }

  .video-player-container :deep(.vjs-duration) {
    display: inline-block !important;
    visibility: visible !important;
    opacity: 1 !important;
    padding-left: 2px !important;
  }

  .video-player-container :deep(.vjs-time-divider) {
    display: inline-block !important;
    visibility: visible !important;
    opacity: 1 !important;
    min-width: 8px !important;
    width: auto !important;
    padding: 0 2px !important;
  }

  .video-player-container :deep(.vjs-time-control) {
    display: inline-block !important;
    min-width: auto !important;
    width: auto !important;
    flex: none !important;
  }

  .video-player-container :deep(.vjs-time-control.vjs-current-time),
  .video-player-container :deep(.vjs-time-control.vjs-duration) {
    font-size: 1em !important;
    line-height: 3em !important;
    width: auto !important;
    min-width: 3.5em !important;
  }

  .video-player-container :deep(.vjs-time-control.vjs-time-divider) {
    min-width: 6px !important;
    max-width: 10px !important;
    padding: 0 1px !important;
  }

  .video-player-container :deep(.vjs-control-bar) {
    display: flex;
    align-items: center;
  }

  .video-player-container :deep(.vjs-control-bar .vjs-time-control) {
    margin: 0 2px !important;
  }

  .video-player-container :deep(.vjs-time-divider) span {
    display: inline-block;
    width: 6px;
    text-align: center;
  }

  /* 滚动条样式 */
  .sidebar-content::-webkit-scrollbar {
    width: 6px;
  }

  .sidebar-content::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 3px;
  }

  .sidebar-content::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 3px;
  }

  .sidebar-content::-webkit-scrollbar-thumb:hover {
    background: #a8a8a8;
  }

  /* 响应式设计 */
  @media (max-width: 768px) {
    .video-app {
      flex-direction: column;
    }

    .hover-trigger-area {
      width: 30px;
    }

    .video-sidebar {
      width: 250px;
    }

    .video-main.sidebar-visible {
      margin-left: 250px;
    }

    .video-main {
      padding: 10px;
      margin-left: 0 !important;
    }

    .video-player-container {
      padding: 15px;
    }

    .video-current-title {
      font-size: 20px;
      padding: 0 10px;
    }

    .video-title-wrapper {
      margin-top: 15px;
    }

    .control-btn {
      min-width: 80px;
      padding: 6px 12px;
      font-size: 12px;
    }

    .btn-icon {
      font-size: 10px;
    }

    .btn-text {
      font-size: 11px;
    }

    .video-index {
      font-size: 12px;
      padding: 3px 8px;
    }
  }
</style>
