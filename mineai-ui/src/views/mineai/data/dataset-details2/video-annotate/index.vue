<template>
  <div class="video-annotate">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <a-button type="primary" @click="goBack">返回</a-button>
      <span class="title">{{ datasetName }}</span>
      <div class="label-picker">
        <span>标签：</span>
        <el-select
          v-model="selectedLabelId"
          placeholder="选择标签"
          filterable
          allow-create
          clearable
          :disabled="!!selectedTrackId"
          style="width: 180px"
        >
          <el-option v-for="l in labels" :key="l.id" :label="l.displayName || l.name" :value="l.id" />
        </el-select>
      </div>
      <el-select v-model="exportFormat" style="width: 110px">
        <el-option label="JSON" value="json" />
        <el-option label="MOT" value="mot" />
      </el-select>
      <a-button type="primary" :disabled="!currentFileId" @click="doExport">导出</a-button>
      <span class="draw-hint">画框需先选择标签</span>
    </div>

    <div class="body">
      <!-- 左侧：视频文件列表 -->
      <div class="file-list">
        <div class="file-list-title">视频文件</div>
        <div
          v-for="f in files"
          :key="f.id"
          class="file-item"
          :class="{ active: f.id === currentFileId }"
          @click="selectFile(f)"
        >
          <div class="file-name">{{ f.name }}</div>
          <div class="file-status">{{ f.annotationStatus || 'READY' }}</div>
        </div>
      </div>

      <!-- 中间：播放器 + 画框 -->
      <div class="player-area">
        <div
          class="video-box"
          ref="videoBoxRef"
          @mousedown="onMouseDown"
          @mousemove="onMouseMove"
          @mouseup="onMouseUp"
        >
          <video
            ref="videoRef"
            class="video-el"
            :src="videoUrl"
            :width="displayW"
            :height="displayH"
            preload="auto"
            @loadedmetadata="onLoadedMetadata"
            @timeupdate="onTimeUpdate"
            @play="startFrameLoop"
            @pause="stopFrameLoop"
            @error="onVideoError"
            @waiting="buffering = true"
            @canplay="buffering = false"
          ></video>
          <svg class="overlay" :width="displayW" :height="displayH">
            <polyline
              v-for="l in trackLines"
              v-show="showTrackLine"
              :key="'line' + l.id"
              :points="l.points"
              :stroke="l.color"
              fill="none"
              stroke-width="2"
              stroke-dasharray="4,4"
            />
            <rect
              v-for="(b, i) in currentBoxes"
              :key="'b' + i"
              v-show="!editState || editState.trackId !== b.trackId"
              :x="b.x * displayW"
              :y="b.y * displayH"
              :width="b.width * displayW"
              :height="b.height * displayH"
              :style="{ stroke: b.color, fill: b.color + '33' }"
              class="bbox"
            />
            <g
              v-for="(b, i) in currentBoxes"
              :key="'h' + i"
              v-show="!editState || editState.trackId !== b.trackId"
            >
              <rect
                v-for="c in boxCorners(b)"
                :key="c.dir"
                :x="c.x - 4"
                :y="c.y - 4"
                width="8"
                height="8"
                :style="{ fill: b.color }"
                stroke="#fff"
                stroke-width="1"
              />
            </g>
            <rect
              v-if="editingBox"
              :x="editingBox.x * displayW"
              :y="editingBox.y * displayH"
              :width="editingBox.width * displayW"
              :height="editingBox.height * displayH"
              :style="{ stroke: editingBox.color, fill: editingBox.color + '33' }"
              class="bbox"
            />
            <rect
              v-if="drawingRect"
              :x="drawingRect.x"
              :y="drawingRect.y"
              :width="drawingRect.w"
              :height="drawingRect.h"
              :style="{ stroke: drawingColor, fill: drawingColor + '33' }"
              class="bbox drawing"
            />
          </svg>
          <div v-if="!currentFileId" class="placeholder">请在左侧选择视频文件</div>
          <div v-if="videoError" class="placeholder video-error">{{ videoError }}</div>
          <div v-if="buffering" class="placeholder">视频加载中...</div>
        </div>
        <div class="controls">
          <a-button size="small" @click="prevFrame">上一帧</a-button>
          <a-button size="small" @click="playPause">{{ playing ? '暂停' : '播放' }}</a-button>
          <a-button size="small" @click="nextFrame">下一帧</a-button>
          <a-button size="small" @click="prevKeyframe">上一关键帧</a-button>
          <a-button size="small" @click="nextKeyframe">下一关键帧</a-button>
          <el-select
            v-model="playbackRate"
            size="small"
            style="width: 80px"
            @change="setPlaybackRate"
          >
            <el-option v-for="r in [0.5, 1, 1.5, 2, 3, 4]" :key="r" :label="r + 'x'" :value="r" />
          </el-select>
          <input
            type="number"
            class="frame-input"
            min="1"
            :max="frameCount || 1"
            v-model="frameInput"
            @change="jumpToInput"
          />
          <span class="frame-info">帧 {{ currentFrame }} / {{ frameCount }}</span>
          <span v-if="currentFrameStatus" class="frame-status">{{ currentFrameStatus }}</span>
          <a-button size="small" @click="doMarkOccluded">遮挡</a-button>
          <a-button size="small" @click="doMarkOutside">消失</a-button>
          <a-button size="small" @click="doMarkRestore">恢复</a-button>
          <span class="trackline-toggle">轨迹线 <el-switch v-model="showTrackLine" /></span>
        </div>
        <div class="frame-slider-row">
          <input
            type="range"
            class="frame-slider"
            min="1"
            :max="frameCount || 1"
            :value="currentFrame"
            @input="onSeekInput"
          />
          <div class="frame-ruler">
            <span
              v-for="tick in rulerTicks"
              :key="tick"
              class="ruler-tick"
              :style="{ left: pct(tick) + '%' }"
              >{{ tick }}</span
            >
          </div>
        </div>
      </div>

      <!-- 右侧：Track 列表 -->
      <div class="track-list">
        <div class="track-list-title">目标 Track</div>
        <el-select
          v-model="selectedTrackId"
          placeholder="全部 Track"
          clearable
          size="small"
          class="track-select"
        >
          <el-option
            v-for="t in tracks"
            :key="t.id"
            :label="'#' + t.trackNo + ' ' + labelName(t.labelId)"
            :value="t.id"
          />
        </el-select>
        <div
          v-for="t in filteredTracks"
          :key="t.id"
          class="track-item"
          :class="{ selected: t.id === selectedTrackId }"
          @click="selectTrack(t)"
        >
          <div class="track-info">
            <div class="track-no"
              >#{{ t.trackNo }} <span class="track-label">{{ labelName(t.labelId) }}</span></div
            >
            <div class="track-meta">帧 {{ t.startFrame }}~{{ t.endFrame }}</div>
            <div class="keyframe-list">
              <span v-for="kf in t.keyframes || []" :key="kf.id" class="keyframe-chip">
                <span class="kf-frame" @click.stop="seekFrame(kf.frameIndex)"
                  >帧{{ kf.frameIndex }}</span
                >
                <span class="kf-del" @click.stop="deleteKeyframe(t, kf)">×</span>
              </span>
            </div>
          </div>
          <a-button size="small" type="link" danger @click.stop="deleteTrack(t)">删除</a-button>
        </div>
        <a-empty v-if="!tracks.length" description="暂无 Track" />
      </div>
    </div>

    <!-- 底部：Timeline（简化版） -->
    <div class="timeline" v-if="currentFileId">
      <div v-for="t in filteredTracks" :key="t.id" class="timeline-row">
        <div class="timeline-label">#{{ t.trackNo }}</div>
        <div class="timeline-bar">
          <div
            v-for="seg in trackSegments(t)"
            :key="'seg-' + seg.start"
            class="timeline-seg"
            :class="'seg-' + seg.status"
            :style="{
              left: pct(seg.start) + '%',
              width: Math.max(pct(seg.end - seg.start), 1) + '%',
            }"
          ></div>
          <div
            v-for="kf in t.keyframes || []"
            :key="kf.id"
            class="keyframe-dot"
            :style="{ left: pct(kf.frameIndex) + '%' }"
            :title="'帧 ' + kf.frameIndex"
            @click="seekFrame(kf.frameIndex)"
            >◆</div
          >
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, ref, computed } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { ElMessageBox } from 'element-plus';
  import {
    getVideoDatasetFiles,
    loadVideoAnnotation,
    openVideoAnnotationTask,
    createVideoTrack,
    createVideoKeyframe,
    updateVideoKeyframe,
    deleteVideoKeyframe,
    deleteVideoTrack,
    exportVideoAnnotation,
    getAllLabelTemplateVideo,
  } from '../api/index';
  import { bucketHost, bucketName } from '/@/utils/dubhe';

  const route = useRoute();
  const router = useRouter();
  const { createMessage } = useMessage();

  const datasetId = parseInt(route.params?.id as string);
  const datasetName = (route.params?.name as string) || '';

  const files = ref<any[]>([]);
  const currentFileId = ref<number | null>(null);
  const currentFileName = ref('');
  const labels = ref<{ id: number; name: string; displayName?: string; annotationName?: string; color?: string }[]>([]);
  const selectedLabelId = ref<number | null>(null);

  const videoRef = ref<HTMLVideoElement | null>(null);
  const videoBoxRef = ref<HTMLDivElement | null>(null);
  const videoUrl = ref('');
  const fps = ref(30);
  const frameCount = ref(0);
  const currentFrame = ref(0);
  const playing = ref(false);

  const displayW = ref(0);
  const displayH = ref(0);

  const tracks = ref<any[]>([]);
  const taskId = ref<number | null>(null);
  const selectedTrackId = ref<number | null>(null);
  const showTrackLine = ref(true);
  const exportFormat = ref('json');
  const videoError = ref('');
  const playbackRate = ref(1);
  const pendingFrame = ref(0);
  const buffering = ref(false);

  // 画框状态
  const drawing = ref(false);
  const drawStart = ref({ x: 0, y: 0 });
  const drawingRect = ref<{ x: number; y: number; w: number; h: number } | null>(null);
  // bbox 编辑状态：mode=move/resize，keyframeId 为 null 表示当前帧是插值帧（编辑时改为新增关键帧）
  const editState = ref<{
    mode: 'move' | 'resize';
    trackId: number;
    keyframeId: number | null;
    dir: string;
    color: string;
    origX: number;
    origY: number;
    origW: number;
    origH: number;
    startPx: number;
    startPy: number;
  } | null>(null);
  const editingBox = ref<{
    x: number;
    y: number;
    width: number;
    height: number;
    color: string;
  } | null>(null);

  // 标签库配置的颜色映射（标注名 -> #RRGGBB），loadLabels 加载视频标签库时填充；
  // 未配置颜色的标签不写入，走哈希兜底（与点云标注同一套算法）。
  const labelColorMap: Record<string, string> = {};

  /** 根据标注名获取类别颜色：优先库色，未配置则按名称确定性哈希兜底（与点云标注一致） */
  function getLabelColor(label: string): string {
    const configured = labelColorMap[label];
    if (configured) return configured;
    let h1 = 0x811c9dc5;
    let h2 = 5381;
    for (let i = 0; i < label.length; i++) {
      const c = label.charCodeAt(i);
      h1 ^= c;
      h1 = Math.imul(h1, 0x01000193) >>> 0;
      h2 = ((h2 << 5) + h2 + c) >>> 0;
    }
    const hue = (h1 + h2 * 137.508) % 360;
    return hslToHex(hue, 70, 55);
  }

  function hslToHex(h: number, s: number, l: number): string {
    s /= 100;
    l /= 100;
    const a = s * Math.min(l, 1 - l);
    const f = (n: number) => {
      const k = (n + h / 30) % 12;
      return l - a * Math.max(-1, Math.min(k - 3, Math.min(9 - k, 1)));
    };
    const toHex = (x: number) => Math.round(255 * x).toString(16).padStart(2, '0');
    return `#${toHex(f(0))}${toHex(f(8))}${toHex(f(4))}`;
  }

  /** labelId -> 标注名（annotationName 优先），作为颜色键 */
  function labelColorKey(labelId: number | null): string {
    if (labelId == null) return '';
    const l = labels.value.find((x) => Number(x.id) === Number(labelId));
    return l ? l.annotationName || l.name || '' : '';
  }

  /** 当前选中标签对应的颜色（画新框预览用） */
  const drawingColor = computed(() => getLabelColor(labelColorKey(selectedLabelId.value)));

  // 选中某个 Track 时只显示该 Track，否则显示全部
  const filteredTracks = computed(() => {
    if (!selectedTrackId.value) return tracks.value;
    return tracks.value.filter((t) => t.id === selectedTrackId.value);
  });

  const currentBoxes = computed(() => {
    const boxes: any[] = [];
    for (const t of filteredTracks.value) {
      const b = evaluateTrackAtFrame(t, currentFrame.value);
      if (b) {
        const kf = (t.keyframes || []).find((k: any) => k.frameIndex === currentFrame.value);
        boxes.push({
          ...b,
          trackId: t.id,
          trackNo: t.trackNo,
          labelId: t.labelId,
          keyframeId: kf ? kf.id : null,
          color: getLabelColor(labelColorKey(t.labelId)),
        });
      }
    }
    return boxes;
  });

  // 每条 Track 的轨迹线（连接关键帧中心点，展示运动路径）
  const trackLines = computed(() => {
    const lines: any[] = [];
    for (const t of filteredTracks.value) {
      const kfs = (t.keyframes || []).slice().sort((a: any, b: any) => a.frameIndex - b.frameIndex);
      if (kfs.length < 2) continue;
      const color = getLabelColor(labelColorKey(t.labelId));
      // 按 outside 分段：连续的非 outside 关键帧连成一段，outside 处断开
      let segPoints: string[] = [];
      const flush = () => {
        if (segPoints.length >= 2) {
          lines.push({ id: `${t.id}-${lines.length}`, color, points: segPoints.join(' ') });
        }
        segPoints = [];
      };
      for (const kf of kfs) {
        if (kf.outside) {
          flush();
        } else {
          const cx = (kf.x + kf.width / 2) * displayW.value;
          const cy = (kf.y + kf.height / 2) * displayH.value;
          segPoints.push(`${cx},${cy}`);
        }
      }
      flush();
    }
    return lines;
  });

  // 当前帧（选中 Track）的状态：正常 / 遮挡 / 消失
  const currentFrameStatus = computed(() => {
    if (!selectedTrackId.value) return '';
    const t = tracks.value.find((x) => x.id === selectedTrackId.value);
    if (!t) return '';
    const kfs = (t.keyframes || []).slice().sort((a: any, b: any) => a.frameIndex - b.frameIndex);
    let status = '';
    for (const kf of kfs) {
      if (kf.frameIndex <= currentFrame.value) {
        status = kf.outside ? '消失' : kf.occluded ? '遮挡' : '正常';
      } else {
        break;
      }
    }
    return status;
  });

  onMounted(async () => {
    await loadFiles();
    await loadLabels();
  });

  async function loadFiles() {
    try {
      const res = await getVideoDatasetFiles(datasetId, { current: 1, size: 100 });
      files.value = res?.result || res?.items || [];
      if (files.value.length > 0) {
        selectFile(files.value[0]);
      }
    } catch (e) {
      createMessage.error('加载视频列表失败');
    }
  }

  async function loadLabels() {
    try {
      labels.value = (await getAllLabelTemplateVideo()) || [];
      // 重建库色映射：管理页配置过颜色的类别优先用库色，未配置的走哈希兜底
      Object.keys(labelColorMap).forEach((k) => delete labelColorMap[k]);
      labels.value.forEach((l) => {
        const key = l.annotationName || l.name;
        if (key && l.color) labelColorMap[key] = l.color;
      });
    } catch (e) {
      // 标签加载失败不阻塞
    }
  }

  async function selectFile(file: any) {
    stopFrameLoop();
    currentFileId.value = file.id;
    currentFileName.value = file.name || '';
    videoError.value = '';
    tracks.value = [];
    taskId.value = null;
    const playKey = file.convertedUrl || file.url || file.objectKey;
    videoUrl.value = `${bucketHost}/${bucketName}/${playKey}`;
    try {
      const data = await openVideoAnnotationTask(datasetId, file.id);
      taskId.value = data?.taskId ?? null;
      fps.value = data?.fps || 30;
      frameCount.value = data?.frameCount || 0;
      tracks.value = data?.tracks || [];
      if (data?.width && data?.height) {
        // 用视频原始宽高比约束画布，避免坐标拉伸
        syncDisplaySize(data.width, data.height);
      }
      pendingFrame.value = data?.lastFrameIndex || 0;
    } catch (e) {
      createMessage.error('加载标注失败');
    }
  }

  function onLoadedMetadata() {
    const v = videoRef.value;
    if (v) {
      if (!frameCount.value) {
        frameCount.value = Math.round((v.duration || 0) * fps.value);
      }
      syncDisplaySize(v.videoWidth, v.videoHeight);
      if (pendingFrame.value > 0) {
        seekFrame(pendingFrame.value);
        pendingFrame.value = 0;
      }
    }
  }

  function syncDisplaySize(vw: number, vh: number) {
    const box = videoBoxRef.value;
    if (!box || !vw || !vh) return;
    const boxW = box.clientWidth;
    const boxH = box.clientHeight;
    const scale = Math.min(boxW / vw, boxH / vh);
    displayW.value = vw * scale;
    displayH.value = vh * scale;
  }

  function onTimeUpdate() {
    const v = videoRef.value;
    if (v) {
      currentFrame.value = Math.min(
        Math.round(v.currentTime * fps.value) + 1,
        frameCount.value || 1,
      );
      frameInput.value = currentFrame.value;
    }
  }

  function onVideoError() {
    if (!videoUrl.value) return;
    videoError.value =
      '视频播放失败：可能是 H.265(HEVC) 编码，浏览器不支持，请转码为 H.264 后重新上传。';
  }

  // 播放时用 requestAnimationFrame 每帧精确更新，避免 timeupdate 250ms 一跳导致框跟不上
  let rafId: number | null = null;
  function startFrameLoop() {
    playing.value = true;
    const tick = () => {
      const v = videoRef.value;
      if (v) {
        currentFrame.value = Math.min(
          Math.round(v.currentTime * fps.value) + 1,
          frameCount.value || 1,
        );
        frameInput.value = currentFrame.value;
      }
      rafId = requestAnimationFrame(tick);
    };
    rafId = requestAnimationFrame(tick);
  }
  function stopFrameLoop() {
    playing.value = false;
    if (rafId !== null) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
  }

  function seekFrame(frame: number) {
    const v = videoRef.value;
    if (!v) return;
    const max = frameCount.value || 1;
    const clamped = Math.max(1, Math.min(max, frame));
    v.currentTime = (clamped - 1) / fps.value;
  }

  const frameInput = ref(1);
  function onSeekInput(e: Event) {
    const f = Number((e.target as HTMLInputElement).value);
    seekFrame(f);
  }
  function jumpToInput() {
    if (frameInput.value) seekFrame(Number(frameInput.value));
  }

  function prevFrame() {
    seekFrame(Math.max(1, currentFrame.value - 1));
  }
  function nextFrame() {
    seekFrame(Math.min(frameCount.value || 1, currentFrame.value + 1));
  }
  function allKeyframes() {
    const frames: number[] = [];
    for (const t of filteredTracks.value) {
      for (const kf of t.keyframes || []) {
        frames.push(kf.frameIndex);
      }
    }
    return [...new Set(frames)].sort((a, b) => a - b);
  }
  function prevKeyframe() {
    const frames = allKeyframes().filter((f) => f < currentFrame.value);
    if (frames.length) seekFrame(frames[frames.length - 1]);
  }
  function nextKeyframe() {
    const frames = allKeyframes().filter((f) => f > currentFrame.value);
    if (frames.length) seekFrame(frames[0]);
  }
  function setPlaybackRate(rate: number) {
    playbackRate.value = rate;
    const v = videoRef.value;
    if (v) v.playbackRate = rate;
  }
  function playPause() {
    const v = videoRef.value;
    if (!v) return;
    if (v.paused) {
      v.play();
      playing.value = true;
    } else {
      v.pause();
      playing.value = false;
    }
  }

  function pct(frame: number) {
    if (!frameCount.value) return 0;
    return (frame / frameCount.value) * 100;
  }

  // 帧进度条刻度尺：50 帧一个刻度
  const rulerTicks = computed(() => {
    if (!frameCount.value) return [];
    const ticks: number[] = [];
    for (let f = 50; f <= frameCount.value; f += 50) {
      ticks.push(f);
    }
    return ticks;
  });

  // ===== 画框 / 编辑 bbox =====
  function onMouseDown(e: MouseEvent) {
    if (!currentFileId.value || !taskId.value) return;
    const rect = videoRef.value?.getBoundingClientRect();
    if (!rect) return;
    const px = e.clientX - rect.left;
    const py = e.clientY - rect.top;

    // 从后往前找（后画的在上层），判断是否点在已有 bbox 的手柄或内部
    const handle = 8;
    for (let i = currentBoxes.value.length - 1; i >= 0; i--) {
      const b = currentBoxes.value[i];
      const bx = b.x * displayW.value;
      const by = b.y * displayH.value;
      const bw = b.width * displayW.value;
      const bh = b.height * displayH.value;
      const corners = [
        { dir: 'tl', x: bx, y: by },
        { dir: 'tr', x: bx + bw, y: by },
        { dir: 'bl', x: bx, y: by + bh },
        { dir: 'br', x: bx + bw, y: by + bh },
      ];
      const corner = corners.find(
        (c) => Math.abs(px - c.x) <= handle && Math.abs(py - c.y) <= handle,
      );
      if (corner) {
        editState.value = {
          mode: 'resize',
          trackId: b.trackId,
          keyframeId: b.keyframeId,
          dir: corner.dir,
          color: b.color,
          origX: b.x,
          origY: b.y,
          origW: b.width,
          origH: b.height,
          startPx: px,
          startPy: py,
        };
        return;
      }
      if (px >= bx && px <= bx + bw && py >= by && py <= by + bh) {
        editState.value = {
          mode: 'move',
          trackId: b.trackId,
          keyframeId: b.keyframeId,
          dir: '',
          color: b.color,
          origX: b.x,
          origY: b.y,
          origW: b.width,
          origH: b.height,
          startPx: px,
          startPy: py,
        };
        return;
      }
    }

    // 画新框
    if (!selectedLabelId.value) {
      createMessage.warning('请先选择标签');
      return;
    }
    drawStart.value = { x: px, y: py };
    drawing.value = true;
    drawingRect.value = { x: px, y: py, w: 0, h: 0 };
  }

  function onMouseMove(e: MouseEvent) {
    const rect = videoRef.value?.getBoundingClientRect();
    if (!rect) return;
    const px = e.clientX - rect.left;
    const py = e.clientY - rect.top;

    if (editState.value) {
      const s = editState.value;
      const dx = (px - s.startPx) / displayW.value;
      const dy = (py - s.startPy) / displayH.value;
      let x = s.origX;
      let y = s.origY;
      let w = s.origW;
      let h = s.origH;
      if (s.mode === 'move') {
        x = s.origX + dx;
        y = s.origY + dy;
      } else {
        if (s.dir.includes('l')) {
          x = s.origX + dx;
          w = s.origW - dx;
        }
        if (s.dir.includes('r')) {
          w = s.origW + dx;
        }
        if (s.dir.includes('t')) {
          y = s.origY + dy;
          h = s.origH - dy;
        }
        if (s.dir.includes('b')) {
          h = s.origH + dy;
        }
        if (w < 0.01) w = 0.01;
        if (h < 0.01) h = 0.01;
      }
      x = Math.max(0, Math.min(1 - w, x));
      y = Math.max(0, Math.min(1 - h, y));
      editingBox.value = { x, y, width: w, height: h, color: s.color };
      return;
    }

    if (drawing.value) {
      drawingRect.value = {
        x: Math.min(drawStart.value.x, px),
        y: Math.min(drawStart.value.y, py),
        w: Math.abs(px - drawStart.value.x),
        h: Math.abs(py - drawStart.value.y),
      };
    }
  }

  async function onMouseUp() {
    if (editState.value) {
      const s = editState.value;
      editState.value = null;
      const box = editingBox.value;
      editingBox.value = null;
      if (box) {
        const changed =
          Math.abs(box.x - s.origX) > 0.001 ||
          Math.abs(box.y - s.origY) > 0.001 ||
          Math.abs(box.width - s.origW) > 0.001 ||
          Math.abs(box.height - s.origH) > 0.001;
        if (changed) await saveEditedBox(s, box);
      }
      return;
    }
    if (!drawing.value) return;
    drawing.value = false;
    const r = drawingRect.value;
    drawingRect.value = null;
    if (!r || r.w < 5 || r.h < 5 || !displayW.value || !displayH.value) return;
    const norm = {
      x: r.x / displayW.value,
      y: r.y / displayH.value,
      width: r.w / displayW.value,
      height: r.h / displayH.value,
    };
    await doCreateBox(norm);
  }

  async function saveEditedBox(
    s: any,
    box: { x: number; y: number; width: number; height: number },
  ) {
    try {
      if (s.keyframeId) {
        await updateVideoKeyframe(s.keyframeId, {
          x: box.x,
          y: box.y,
          width: box.width,
          height: box.height,
        });
      } else {
        await createVideoKeyframe(s.trackId, {
          frameIndex: currentFrame.value,
          x: box.x,
          y: box.y,
          width: box.width,
          height: box.height,
        });
      }
      await reloadAnnotation();
    } catch (err: any) {
      createMessage.error(err?.message || '保存失败');
    }
  }

  function boxCorners(b: any) {
    const bx = b.x * displayW.value;
    const by = b.y * displayH.value;
    const bw = b.width * displayW.value;
    const bh = b.height * displayH.value;
    return [
      { dir: 'tl', x: bx, y: by },
      { dir: 'tr', x: bx + bw, y: by },
      { dir: 'bl', x: bx, y: by + bh },
      { dir: 'br', x: bx + bw, y: by + bh },
    ];
  }

  function trackSegments(t: any) {
    const kfs = (t.keyframes || []).slice().sort((a: any, b: any) => a.frameIndex - b.frameIndex);
    if (!kfs.length) return [];
    const segments: any[] = [];
    for (let i = 0; i < kfs.length; i++) {
      const kf = kfs[i];
      const start = kf.frameIndex;
      const end = i < kfs.length - 1 ? kfs[i + 1].frameIndex : t.endFrame || kf.frameIndex;
      const status = kf.outside ? 'outside' : kf.occluded ? 'occluded' : 'normal';
      segments.push({ start, end, status });
    }
    return segments;
  }

  function selectTrack(t: any) {
    if (selectedTrackId.value === t.id) {
      // 取消选中：回到"新建 Track"模式，清空标签
      selectedTrackId.value = null;
      selectedLabelId.value = null;
    } else {
      // 选中：回显该 Track 的标签并锁定（label 挂在 Track 上，避免误改）
      selectedTrackId.value = t.id;
      selectedLabelId.value = t.labelId;
    }
  }

  function labelName(labelId: number | null) {
    if (labelId == null) return '';
    const l = labels.value.find((x) => Number(x.id) === Number(labelId));
    return l ? l.displayName || l.name : '';
  }

  async function deleteTrack(t: any) {
    try {
      await ElMessageBox.confirm(
        `确定要删除 Track #${t.trackNo} 吗？删除后其所有关键帧也会一并删除。`,
        '删除确认',
        { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
      );
    } catch {
      return; // 用户取消
    }
    try {
      await deleteVideoTrack(t.id);
      if (selectedTrackId.value === t.id) {
        selectedTrackId.value = null;
        selectedLabelId.value = null;
      }
      await reloadAnnotation();
      createMessage.success('已删除 Track');
    } catch (err: any) {
      createMessage.error(err?.message || '删除失败');
    }
  }

  async function deleteKeyframe(t: any, kf: any) {
    try {
      await deleteVideoKeyframe(kf.id);
      await reloadAnnotation();
    } catch (err: any) {
      createMessage.error(err?.message || '删除关键帧失败');
    }
  }

  async function doMarkOccluded() {
    const t = tracks.value.find((x) => x.id === selectedTrackId.value);
    if (!t) {
      createMessage.warning('请先选中一个 Track');
      return;
    }
    const b = evaluateTrackAtFrame(t, currentFrame.value);
    if (!b) {
      createMessage.warning('当前帧该 Track 无框，无法标记遮挡');
      return;
    }
    try {
      await createVideoKeyframe(t.id, {
        frameIndex: currentFrame.value,
        x: b.x,
        y: b.y,
        width: b.width,
        height: b.height,
        occluded: true,
        outside: false,
      });
      await reloadAnnotation();
    } catch (err: any) {
      createMessage.error(err?.message || '标记遮挡失败');
    }
  }

  async function doMarkOutside() {
    const t = tracks.value.find((x) => x.id === selectedTrackId.value);
    if (!t) {
      createMessage.warning('请先选中一个 Track');
      return;
    }
    const b = evaluateTrackAtFrame(t, currentFrame.value);
    try {
      await createVideoKeyframe(t.id, {
        frameIndex: currentFrame.value,
        x: b ? b.x : 0,
        y: b ? b.y : 0,
        width: b ? b.width : 0,
        height: b ? b.height : 0,
        occluded: false,
        outside: true,
      });
      await reloadAnnotation();
    } catch (err: any) {
      createMessage.error(err?.message || '标记消失失败');
    }
  }

  async function doMarkRestore() {
    const t = tracks.value.find((x) => x.id === selectedTrackId.value);
    if (!t) {
      createMessage.warning('请先选中一个 Track');
      return;
    }
    // 当前帧无框（outside 区间）时，沿用该 Track 最后一个非 outside 关键帧的 bbox
    let b = evaluateTrackAtFrame(t, currentFrame.value);
    if (!b) {
      const normalKfs = (t.keyframes || []).filter((k: any) => !k.outside);
      if (normalKfs.length) {
        const last = normalKfs[normalKfs.length - 1];
        b = { x: last.x, y: last.y, width: last.width, height: last.height };
      }
    }
    try {
      await createVideoKeyframe(t.id, {
        frameIndex: currentFrame.value,
        x: b ? b.x : 0,
        y: b ? b.y : 0,
        width: b ? b.width : 0,
        height: b ? b.height : 0,
        occluded: false,
        outside: false,
      });
      await reloadAnnotation();
    } catch (err: any) {
      createMessage.error(err?.message || '恢复失败');
    }
  }

  async function doCreateBox(box: { x: number; y: number; width: number; height: number }) {
    if (!taskId.value) {
      createMessage.error('标注任务未创建');
      return;
    }
    try {
      const frameIndex = currentFrame.value || 1;
      const normBox = {
        x: Number(box.x),
        y: Number(box.y),
        width: Number(box.width),
        height: Number(box.height),
      };
      if (selectedTrackId.value) {
        await createVideoKeyframe(selectedTrackId.value, {
          frameIndex,
          ...normBox,
        });
        createMessage.success('已在该 Track 追加关键帧');
      } else {
        const trackNo = tracks.value.length + 1;
        const data = {
          taskId: Number(taskId.value),
          trackNo: Number(trackNo),
          labelId: Number(selectedLabelId.value),
          frameIndex,
          ...normBox,
        };
        await createVideoTrack(data);
        createMessage.success('已创建 Track #' + trackNo);
      }
      await reloadAnnotation();
    } catch (err: any) {
      createMessage.error(err?.message || '操作失败');
    }
  }

  async function reloadAnnotation() {
    if (!currentFileId.value) return;
    const data = await loadVideoAnnotation(datasetId, currentFileId.value);
    tracks.value = data?.tracks || [];
    taskId.value = data?.taskId ?? taskId.value;
  }

  async function doExport() {
    if (!currentFileId.value) return;
    try {
      const format = exportFormat.value;
      const data = await exportVideoAnnotation(currentFileId.value, format);
      const isMot = format === 'mot';
      const content = isMot ? data : JSON.stringify(data, null, 2);
      const mime = isMot ? 'text/plain' : 'application/json';
      const ext = isMot ? 'txt' : 'json';
      const blob = new Blob([content], { type: mime });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${currentFileName.value || 'video_' + currentFileId.value}_annotation.${ext}`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err: any) {
      createMessage.error(err?.message || '导出失败');
    }
  }

  // ===== 插值 =====
  function evaluateTrackAtFrame(track: any, frame: number) {
    const kfs = (track.keyframes || [])
      .slice()
      .sort((a: any, b: any) => a.frameIndex - b.frameIndex);
    if (!kfs.length) return null;
    if (frame < kfs[0].frameIndex || frame > kfs[kfs.length - 1].frameIndex) return null;
    const exact = kfs.find((k: any) => k.frameIndex === frame);
    if (exact) {
      if (exact.outside) return null;
      return {
        x: exact.x,
        y: exact.y,
        width: exact.width,
        height: exact.height,
        occluded: !!exact.occluded,
      };
    }
    let prev = kfs[0];
    let next = kfs[kfs.length - 1];
    for (let i = 0; i < kfs.length - 1; i++) {
      if (frame > kfs[i].frameIndex && frame < kfs[i + 1].frameIndex) {
        prev = kfs[i];
        next = kfs[i + 1];
        break;
      }
    }
    if (prev.outside) return null;
    const t = (frame - prev.frameIndex) / (next.frameIndex - prev.frameIndex);
    return {
      x: prev.x + t * (next.x - prev.x),
      y: prev.y + t * (next.y - prev.y),
      width: prev.width + t * (next.width - prev.width),
      height: prev.height + t * (next.height - prev.height),
      occluded: !!prev.occluded,
    };
  }

  function goBack() {
    router.go(-1);
  }
</script>

<style scoped lang="less">
  .video-annotate {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background: #181d31;

    .toolbar {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 8px 16px;
      background: #181d31;
      color: #fff;

      .title {
        font-size: 16px;
        font-weight: 600;
      }

      .label-picker {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .draw-hint {
        margin-left: auto;
        color: #e6a23c;
        font-size: 16px;
      }
    }

    .body {
      display: flex;
      flex: 1;
      min-height: 0;

      .file-list {
        width: 220px;
        border-right: 1px solid #2a2d3e;
        overflow-y: auto;
        color: #ccc;

        .file-list-title {
          padding: 8px 12px;
          font-weight: 600;
          color: #fff;
          border-bottom: 1px solid #2a2d3e;
        }

        .file-item {
          padding: 8px 12px;
          cursor: pointer;

          &:hover {
            background: #2a2d3e;
          }

          &.active {
            background: #409eff;
            color: #fff;
          }

          .file-name {
            font-size: 13px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .file-status {
            font-size: 12px;
            color: #888;
          }
        }
      }

      .player-area {
        flex: 1;
        display: flex;
        flex-direction: column;
        min-width: 0;

        .video-box {
          flex: 1;
          position: relative;
          display: flex;
          align-items: center;
          justify-content: center;
          overflow: hidden;

          .video-el {
            max-width: 100%;
            max-height: 100%;
          }

          .overlay {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            pointer-events: none;
          }

          .bbox {
            stroke-width: 2;
          }

          .placeholder {
            color: #666;
          }

          .video-error {
            color: #f56c6c;
            font-size: 13px;
            padding: 8px 16px;
          }
        }

        .controls {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 8px 12px;
          background: #181d31;

          .frame-info {
            color: #fff;
            font-size: 13px;
          }

          .frame-status {
            color: #e6a23c;
            font-size: 13px;
            font-weight: 600;
          }

          .hint {
            color: #888;
            font-size: 12px;
          }

          .frame-input {
            width: 64px;
            background: #2a2d3e;
            color: #fff;
            border: 1px solid #3a3f55;
            border-radius: 4px;
            padding: 2px 6px;
          }
        }

        .frame-slider-row {
          padding: 4px 12px 8px;
          background: #181d31;

          .frame-slider {
            width: 100%;
            display: block;
          }

          .frame-ruler {
            position: relative;
            height: 18px;
            margin-top: 2px;

            .ruler-tick {
              position: absolute;
              transform: translateX(-50%);
              font-size: 10px;
              color: #888;
            }
          }
        }
      }

      .track-list {
        width: 220px;
        border-left: 1px solid #2a2d3e;
        overflow-y: auto;
        color: #ccc;

        .track-list-title {
          padding: 8px 12px;
          font-weight: 600;
          color: #fff;
          border-bottom: 1px solid #2a2d3e;
        }

        .track-select {
          width: calc(100% - 24px);
          margin: 8px 12px;
        }

        .track-item {
          padding: 8px 12px;
          cursor: pointer;
          border-bottom: 1px solid #2a2d3e;
          display: flex;
          align-items: center;
          justify-content: space-between;

          &:hover {
            background: #2a2d3e;
          }

          &.selected {
            background: #409eff;
          }

          .track-info {
            flex: 1;
            min-width: 0;
          }

          .track-no {
            color: #fff;
            font-weight: 600;
          }

          .track-label {
            color: #409eff;
            font-weight: 400;
            font-size: 12px;
          }

          .track-meta {
            font-size: 12px;
            color: #888;
          }

          .keyframe-list {
            margin-top: 4px;
            display: flex;
            flex-wrap: wrap;
            gap: 4px;
          }

          .keyframe-chip {
            display: inline-flex;
            align-items: center;
            gap: 2px;
            background: #2a2d3e;
            border: 1px solid #3a3f55;
            border-radius: 4px;
            padding: 0 4px;
            font-size: 12px;

            .kf-frame {
              color: #409eff;
              cursor: pointer;
            }

            .kf-del {
              color: #f56c6c;
              cursor: pointer;
              padding: 0 2px;
            }
          }
        }
      }
    }

    .timeline {
      height: 140px;
      border-top: 1px solid #2a2d3e;
      overflow-y: auto;
      background: #1a1d2e;

      .timeline-row {
        display: flex;
        align-items: center;
        height: 30px;
        padding: 0 8px;

        .timeline-label {
          width: 60px;
          color: #fff;
          font-size: 12px;
        }

        .timeline-bar {
          position: relative;
          flex: 1;
          height: 12px;
          background: #2a2d3e;
          border-radius: 6px;

          .timeline-seg {
            position: absolute;
            top: 0;
            height: 100%;
            border-radius: 2px;

            &.seg-normal {
              background: #67c23a;
            }

            &.seg-occluded {
              background: #e6a23c;
            }

            &.seg-outside {
              background: #f56c6c;
            }
          }

          .keyframe-dot {
            position: absolute;
            top: -2px;
            color: #1890ff;
            font-size: 14px;
            cursor: pointer;
            transform: translateX(-50%);
          }
        }
      }
    }
  }
</style>
