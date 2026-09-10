<template>
  <div ref="pageRef" class="pc-annotate-page" tabindex="-1" @keydown="handleKeydown">
    <!-- ================= 顶部导航栏 ================= -->
    <header class="annotate-header">
      <div class="header-title">
        <Button type="text" class="back-button" @click="goBack">
          <template #icon><ArrowLeftOutlined /></template>
        </Button>
        <div>
          <div class="title">点云标注</div>
          <div class="subtitle">数据集：{{ datasetName }}<template v-if="currentFile"> · {{ currentFile.name }}</template></div>
        </div>
      </div>
      <div class="header-actions">
        <Tooltip title="撤销 (Ctrl+Z)">
          <Button :disabled="!canUndo" @click="undo">
            <template #icon><UndoOutlined /></template>
          </Button>
        </Tooltip>
        <Tooltip title="重做 (Ctrl+Y)">
          <Button :disabled="!canRedo" @click="redo">
            <template #icon><RedoOutlined /></template>
          </Button>
        </Tooltip>
        <Tooltip title="切换查看模式（隐藏标注，仅看原始点云）(Q)">
          <Button :type="viewMode === 'view' ? 'primary' : 'default'" @click="toggleViewMode">
            <template #icon><EyeOutlined /></template>
            {{ viewMode === 'view' ? '查看' : '编辑' }}
          </Button>
        </Tooltip>
        <Button @click="resetView">重置视角</Button>
        <Dropdown :disabled="!currentFile">
          <Button type="primary" :disabled="!currentFile">
            <template #icon><DownloadOutlined /></template>
            导出标注
            <DownOutlined />
          </Button>
          <template #overlay>
            <Menu @click="handleExportClick">
              <MenuItem key="platform">
                <span>平台内部 JSON</span>
                <span class="export-menu-desc">（对象统一存储模型）</span>
              </MenuItem>
              <MenuItem key="kitti">
                <span>KITTI 格式</span>
                <span class="export-menu-desc">（自动驾驶 label）</span>
              </MenuItem>
              <MenuItem key="nuscenes">
                <span>nuScenes 格式</span>
                <span class="export-menu-desc">（sample_annotation）</span>
              </MenuItem>
            </Menu>
          </template>
        </Dropdown>
      </div>
    </header>

    <!-- ================= 主体：中栏工作区 + 右栏设置 ================= -->
    <main class="annotate-main">
      <!-- 中栏：工作区 -->
      <section class="workspace panel">
        <div class="workspace-toolbar">
          <div class="toolbar-group">
            <Tooltip title="选择/旋转视角 (Esc 返回)">
              <Button :type="activeTool === 'select' ? 'primary' : 'default'" @click="setTool('select')">
                <template #icon><DragOutlined /></template>
              </Button>
            </Tooltip>
            <Tooltip title="平移视角（左键拖拽移动视口）">
              <Button :type="activeTool === 'pan' ? 'primary' : 'default'" @click="setTool('pan')">
                <template #icon><HolderOutlined /></template>
              </Button>
            </Tooltip>
            <Tooltip title="新建 3D Box：需先创建并选中分割对象，按住鼠标拖拽框选范围，松手创建">
              <Button :type="activeTool === 'box' ? 'primary' : 'default'" :disabled="!pointCloudReady || !currentObjectId" @click="setTool('box')">
                <template #icon><BorderOutlined /></template>
                3D Box
              </Button>
            </Tooltip>
          </div>
          <div class="toolbar-right">
            <span class="toolbar-label">点大小</span>
            <Slider v-model:value="pointSize" :min="0.1" :max="5" :step="0.1" class="point-size-slider" />
            <span class="toolbar-label">着色</span>
            <Select v-model:value="colorMode" size="small" class="color-mode-select">
              <SelectOption value="height">按高度</SelectOption>
              <SelectOption value="intensity">按强度</SelectOption>
              <SelectOption value="solid">纯色</SelectOption>
            </Select>
          </div>
        </div>

        <div ref="renderRef" class="render-container" @mousedown="handleCanvasMouseDown" @mousemove="handleCanvasMouseMove" @mouseup="handleCanvasMouseUp" @dblclick="handleCanvasDblClick">
          <Spin :spinning="pointLoading" size="large" tip="正在从 MinIO 加载并解析 PCD...">
            <div class="render-fill"></div>
          </Spin>
          <div v-if="loadError" class="load-error">
            <WarningOutlined />
            <span>{{ loadError }}</span>
            <Button size="small" @click="currentFile && selectFile(currentFile)">重新加载</Button>
          </div>
          <div class="render-stats">
            <span>点数 {{ formatCount(pointCount) }}</span>
            <span>3D Box {{ boxes.length }}</span>
            <span>分割对象 {{ segObjects.length }}</span>
            <span v-if="selection.length > 0">选区 {{ selection.length }} 点</span>
          </div>
          <!-- 分割对象图例（左下角悬浮） -->
          <div v-if="segObjects.length > 0" class="legend-panel">
            <div class="legend-title">分割对象图例</div>
            <div v-for="obj in segObjects" :key="obj.id" class="legend-item" @click="setCurrentObject(obj.id)">
              <span class="legend-dot" :style="{ backgroundColor: obj.color }"></span>
              <span class="legend-name" :class="{ active: obj.id === currentObjectId }">{{ obj.label }}</span>
              <span class="legend-count">{{ formatCount(obj.indices.length) }}点</span>
            </div>
          </div>
          </div>
      </section>

      <!-- 右栏：设置面板 -->
      <aside class="setting-panel panel">
        <div class="panel-title">标注设置</div>
        <Form layout="vertical" size="small">
          <FormItem label="目标类别（数字键 1-9 快速选择）">
            <div class="label-select-row">
              <Select v-model:value="draft.label" :options="labelOptions" size="small" />
              <Button size="small" :disabled="!pointCloudReady" @click="createSegObject">新建对象</Button>
            </div>
          </FormItem>
          <FormItem label="3D 框选区操作">
            <div v-if="currentObjectId" class="current-object-bar">
              <span class="co-dot" :style="{ backgroundColor: getCurrentObject()?.color || '#888' }"></span>
              <span class="co-label">当前操作对象：{{ getCurrentObject()?.label || '-' }}</span>
              <span class="co-count">{{ formatCount(getCurrentObject()?.indices.length || 0) }} 点</span>
            </div>
            <div v-else class="current-object-bar co-empty">当前未选择分割对象，请点击下方列表选择</div>
            <div class="number-row">
              <Button size="small" type="primary" :disabled="selectedBoxIndex < 0 || !currentObjectId" @click="addSelectionToObject">
                <template #icon><PlusOutlined /></template>
                加入对象
              </Button>
              <Button size="small" danger :disabled="selectedBoxIndex < 0 || !currentObjectId" @click="eraseSelectionFromObject">
                <template #icon><ClearOutlined /></template>
                擦除
              </Button>
            </div>
          </FormItem>
        </Form>

        <Divider>分割对象（{{ segObjects.length }}）</Divider>
        <div class="box-list seg-object-list">
          <button
            v-for="obj in segObjects"
            :key="obj.id"
            type="button"
            class="box-item"
            :class="{ active: obj.id === currentObjectId }"
            @click="setCurrentObject(obj.id)"
          >
            <span class="box-color" :style="{ backgroundColor: obj.color }"></span>
            <span class="box-label">{{ obj.label }}</span>
            <span class="box-size">{{ formatCount(obj.indices.length) }} 点</span>
            <DeleteOutlined class="item-delete" @click.stop="removeSegObject(obj.id)" />
          </button>
          <Empty v-if="segObjects.length === 0" description="新建分割对象后，框选点云加入" />
        </div>

        <Divider>3D Box（{{ boxes.length }}）</Divider>
        <Form layout="vertical" size="small">
          <FormItem label="框尺寸（长 / 宽 / 高）">
            <div class="size-input-row">
              <span class="size-input-label">长</span>
              <InputNumber v-model:value="draft.length" :min="0.1" :step="0.1" class="size-input" />
            </div>
            <div class="size-input-row">
              <span class="size-input-label">宽</span>
              <InputNumber v-model:value="draft.width" :min="0.1" :step="0.1" class="size-input" />
            </div>
            <div class="size-input-row">
              <span class="size-input-label">高</span>
              <InputNumber v-model:value="draft.height" :min="0.1" :step="0.1" class="size-input" />
            </div>
          </FormItem>
          <FormItem label="航向角 Yaw（度，绕 Z 轴真旋转）">
            <Slider v-model:value="draft.yaw" :min="-180" :max="180" />
          </FormItem>
          <div v-if="selectedBoxIndex >= 0" class="box-inline-info">
            框内点云数：<b>{{ selectedBoxPointCount }}</b> 个
          </div>
        </Form>
        <div class="box-list">
          <button
            v-for="(box, index) in boxes"
            :key="box.id"
            type="button"
            class="box-item"
            :class="{ active: index === selectedBoxIndex }"
            @click="selectBox(index)"
          >
            <span class="box-color" :style="{ backgroundColor: box.color }"></span>
            <span class="box-label">{{ box.label }} #{{ index + 1 }}</span>
            <span class="box-size">{{ formatBoxSize(box) }}</span>
            <span v-if="box.objectId" class="box-object-tag" :style="{ backgroundColor: getObjectColor(box.objectId) }">{{ getObjectLabel(box.objectId) }}</span>
            <DeleteOutlined class="item-delete" @click.stop="removeBoxByIndex(index)" />
          </button>
          <Empty v-if="boxes.length === 0" description="暂无标注框" />
        </div>

        <!-- 三视图：选中 3D 框后展示，可通过 tab 切换俯/正/侧三个正交投影，支持拖拽角点改尺寸、拖框内移动、俯视图旋转（用户手册 6.1.4.9）
            受右栏宽度（294px）限制，三个 canvas 同时显示会过小且需滚动，故采用 tab 切换：一次只显示一个视图，更聚焦交互 -->
        <Divider v-if="tvVisible">三视图（选中框）</Divider>
        <div v-if="tvVisible" class="three-view-wrap">
          <div class="tv-tabs">
            <button
              type="button"
              class="tv-tab"
              :class="{ active: tvActiveView === 'top' }"
              @click="setTvActiveView('top')"
              title="俯视图 Top（X-Y）"
            >
              <span class="tv-tab-icon">Ⓣ</span>俯视
            </button>
            <button
              type="button"
              class="tv-tab"
              :class="{ active: tvActiveView === 'front' }"
              @click="setTvActiveView('front')"
              title="正视图 Front（X-Z）"
            >
              <span class="tv-tab-icon">Ⓕ</span>正视
            </button>
            <button
              type="button"
              class="tv-tab"
              :class="{ active: tvActiveView === 'side' }"
              @click="setTvActiveView('side')"
              title="侧视图 Side（Y-Z）"
            >
              <span class="tv-tab-icon">Ⓢ</span>侧视
            </button>
          </div>
          <div class="tv-canvas-area">
            <div v-show="tvActiveView === 'top'" class="tv-item">
              <div class="tv-label">俯视图 Top（X-Y，黄色环=旋转手柄）</div>
              <canvas ref="tvTopRef" class="tv-canvas" width="262" height="198" @mousedown="onTvMouseDown('top', $event)"></canvas>
            </div>
            <div v-show="tvActiveView === 'front'" class="tv-item">
              <div class="tv-label">正视图 Front（X-Z）</div>
              <canvas ref="tvFrontRef" class="tv-canvas" width="262" height="198" @mousedown="onTvMouseDown('front', $event)"></canvas>
            </div>
            <div v-show="tvActiveView === 'side'" class="tv-item">
              <div class="tv-label">侧视图 Side（Y-Z）</div>
              <canvas ref="tvSideRef" class="tv-canvas" width="262" height="198" @mousedown="onTvMouseDown('side', $event)"></canvas>
            </div>
          </div>
        </div>
      </aside>
    </main>

    <!-- ================= 底部帧播放器（时序帧切换，视频播放器式） ================= -->
    <div class="frame-player">
      <div class="fp-controls">
        <Tooltip title="首帧">
          <Button size="small" :disabled="!canFrameNav || currentFrameNumber <= 1" @click="seekToFrame(1)">
            <template #icon><FastBackwardOutlined /></template>
          </Button>
        </Tooltip>
        <Tooltip title="上一帧 (←)">
          <Button size="small" :disabled="!canFrameNav || currentFrameNumber <= 1" @click="prevFrame">
            <template #icon><StepBackwardOutlined /></template>
          </Button>
        </Tooltip>
        <Button type="primary" size="small" :disabled="!canFrameNav" @click="togglePlay">
          <template #icon><PauseOutlined v-if="isPlaying" /><CaretRightOutlined v-else /></template>
          {{ isPlaying ? '暂停' : '播放' }}
        </Button>
        <Tooltip title="下一帧 (→)">
          <Button size="small" :disabled="!canFrameNav || currentFrameNumber >= files.length" @click="nextFrame">
            <template #icon><StepForwardOutlined /></template>
          </Button>
        </Tooltip>
        <Tooltip title="末帧">
          <Button size="small" :disabled="!canFrameNav || currentFrameNumber >= files.length" @click="seekToFrame(files.length)">
            <template #icon><FastForwardOutlined /></template>
          </Button>
        </Tooltip>
      </div>
      <div class="fp-progress">
        <Slider
          :min="1"
          :max="Math.max(files.length, 1)"
          :value="currentFrameNumber"
          :disabled="!canFrameNav"
          :tooltip-open="false"
          class="fp-slider"
          @change="onFrameSeek"
        />
        <span class="fp-frame-label">{{ files.length ? currentFrameNumber + ' / ' + files.length : '—' }}</span>
      </div>
      <div class="fp-info">
        <span class="fp-filename" :title="currentFile?.name">{{ currentFile ? currentFile.name : '无帧' }}</span>
        <span class="fp-stat"><span class="fp-dot-green"></span>{{ annotatedCountAll }} 已标</span>
        <span class="fp-stat"><span class="fp-dot-gray"></span>{{ unannotatedCountAll }} 未标</span>
      </div>
      <div class="fp-speed">
        <span class="fp-speed-label">速度</span>
        <Select v-model:value="playFps" size="small" class="fp-speed-select">
          <SelectOption :value="1">1 帧/秒</SelectOption>
          <SelectOption :value="2">2 帧/秒</SelectOption>
          <SelectOption :value="5">5 帧/秒</SelectOption>
          <SelectOption :value="10">10 帧/秒</SelectOption>
        </Select>
        <Tooltip title="跳转至最近未标注帧">
          <Button size="small" :disabled="unannotatedCountAll === 0" @click="jumpToUnannotated">
            <template #icon><AimOutlined /></template>跳未标
          </Button>
        </Tooltip>
      </div>
    </div>
    <!-- 帧点阵：全部帧状态条（绿=已标 灰=未标 蓝=当前），点击跳转 -->
    <div v-if="files.length > 1" class="frame-dots">
      <span
        v-for="(f, i) in files"
        :key="f.id"
        class="frame-dot"
        :class="{ annotated: f.annotated, active: i + 1 === currentFrameNumber }"
        :title="`${i + 1}. ${f.name}`"
        @click="seekToFrame(i + 1)"
      ></span>
    </div>
  </div>
</template>

<script setup lang="ts">
  /**
   * ============================================================
   *  pointCloudAnnotate/index.vue —— 点云标注工作流（3D 检测 + 3D 分割）
   * ============================================================
   *  说明：
   *    在原有 3D Box 标注基础上，参照 DotCloudTest 参考实现与平台用户手册完善工作流：
   *    - 3D Box：真旋转修复（Box3Helper 不支持旋转，改用 Group 包裹）、框内点数统计
   *    - 3D Cuboid：锚点 → 长宽 → 滚轮高度 → 确认（简化交互）
   *    - 点级对象化打标：新建对象 → 矩形/多边形选区 → 加入/擦除 → 对象着色层 + 3D 悬浮标签
   *    - 文件工作流：上/下一文件、跳转未标注、已标/未标状态标记
   *    - 撤销/重做：HistoryManager（命令模式），Ctrl+Z / Ctrl+Y
   *    - 着色模式：高度 / 强度 / 纯色（PCD 内含 intensity 字段则强度模式可用）
   *    - 导出：标注 JSON（含 Box 与分割对象，兼容后端 pcdMetadata.annotations 结构）
   *  说明：本页为前端仿真模式，标注结果通过「导出 JSON」留存；
   *    「已标/未标」由文件 pcdMetadata.annotations 是否非空前端判定。
   * ============================================================
   */
  import { computed, markRaw, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import {
    AimOutlined,
    ArrowLeftOutlined,
    BorderOutlined,
    CaretRightOutlined,
    ClearOutlined,
    DeleteOutlined,
    DownloadOutlined,
    DownOutlined,
    DragOutlined,
    EyeOutlined,
    FastBackwardOutlined,
    FastForwardOutlined,
    HolderOutlined,
    PauseOutlined,
    PlusOutlined,
    RedoOutlined,
    StepBackwardOutlined,
    StepForwardOutlined,
    UndoOutlined,
    WarningOutlined,
  } from '@ant-design/icons-vue';
  import {
    Button,
    Divider,
    Dropdown,
    Empty,
    Form,
    FormItem,
    InputNumber,
    Menu,
    MenuItem,
    Select,
    Slider,
    Spin,
    Tooltip,
  } from 'ant-design-vue';
  import * as THREE from 'three';
  import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
  import { PCDLoader } from 'three/examples/jsm/loaders/PCDLoader.js';
  import { getPcDatasetFiles, getAllLabelTemplatePointcloud, queryLabels, savePcDatasetAnnotations } from '../api';
  import { buildExportContent, type ExportAnnotationSource, type ExportFormat } from './pcAnnotationFormats';
  import { bucketHost, bucketName } from '/@/utils/dubhe';
  import { useMessage } from '/@/hooks/web/useMessage';

  /** ==================== 类型定义 ==================== */

  /** PCD 文件（annotated 为前端根据 pcdMetadata.annotations 判定） */
  interface PcFile {
    id: number;
    name: string;
    objectKey?: string;
    url?: string;
    pointCount?: number;
    pcdMetadata?: string;
    annotated?: boolean;
  }

  /** 3D Box 标注（helper/groupId 用于场景管理，不参与导出） */
  interface BoxAnnotation {
    id: string;
    label: string;
    labelId?: number;
    center: [number, number, number];
    size: [number, number, number];
    yaw: number; // 绕 Z 轴旋转角（度）
    color: string;
    group: THREE.Group; // 线框 + hitProxy 统一挂在 Group 下，Group 旋转即真旋转
    labelSprite?: { sprite: THREE.Sprite; texture: THREE.CanvasTexture };
    objectId: string | null; // 关联的分割对象 ID（工作流：先建对象再建 3D Box）
  }

  /** 点级分割对象（indices 为原始点云 position 属性的点索引集合） */
  interface SegObject {
    id: string;
    label: string;
    color: string;
    indices: number[];
    overlay?: THREE.Points; // 对象着色层（独立 Points，显示对象色）
    sprite?: { sprite: THREE.Sprite; texture: THREE.CanvasTexture }; // 3D 悬浮标签
  }

  /** 标签信息（后端 data_dataset_label） */
  interface LabelInfo {
    id: number;
    name: string;
    color?: string;
  }

  /** 工具模式：select 选择/旋转 / pan 平移视角 / box 3D Box / rect 矩形选区 / polygon 多边形选区 */
  type ToolMode = 'select' | 'pan' | 'box' | 'rect' | 'polygon';

  /** 历史记录条目（命令模式：undo/redo 成对闭包） */
  interface HistoryEntry {
    description: string;
    undo: () => void;
    redo: () => void;
  }

  const SelectOption = Select.Option;

  /** ==================== 路由与消息 ==================== */
  const route = useRoute();
  const router = useRouter();
  const { createMessage } = useMessage();
  const datasetId = Number(route.params.id);
  const datasetName = String(route.params.name || '');

  /** ==================== 响应式状态 ==================== */
  const pageRef = ref<HTMLElement>();
  const renderRef = ref<HTMLElement>();
  const files = ref<PcFile[]>([]);
  const currentFile = ref<PcFile>();
  const fileLoading = ref(false);
  const pointLoading = ref(false);
  // 点云就绪标记：points 是普通变量不具备响应性，模板按钮 :disabled 必须依赖响应式 ref，
  // 否则 PCD 加载完成后工具栏按钮仍保持禁用（暗病：模板绑定不到非响应式变量）
  const pointCloudReady = ref(false);
  const loadError = ref('');
  const pointCount = ref(0);
  const pointSize = ref(0.1);
  const boxes = ref<BoxAnnotation[]>([]);
  const segObjects = ref<SegObject[]>([]);
  const currentObjectId = ref<string | null>(null);
  const selection = ref<number[]>([]); // 当前选区的点索引
  const selectedBoxIndex = ref(-1);
  const viewMode = ref<'edit' | 'view'>('edit'); // 编辑/查看模式
  const colorMode = ref<'height' | 'intensity' | 'solid'>('height');
  const activeTool = ref<ToolMode>('select');

  const labelOptions = ref<{ label: string; value: string }[]>([]);
  const backendLabels = ref<LabelInfo[]>([]);
  const draft = reactive({ label: '车辆', length: 4, width: 2, height: 1.8, yaw: 0 });

  /**
   * 标签库配置的颜色映射（label -> #RRGGBB），loadLabels 加载点云标签库时填充；
   * 未配置颜色的标签不写入，走哈希兜底。
   */
  const labelColorMap: Record<string, string> = {};

  /**
   * 根据 label 获取类别颜色：
   * 1) 优先返回点云标签库（label_template_pointcloud，管理页可用颜色选择器配置）配置的颜色；
   * 2) 未配置时按 label 字符串确定性哈希生成（FNV-1a + djb2 双重哈希，映射到色相环），
   *    保证同一 label 恒为同一种颜色、不同 label 色相尽量分散。
   * 不依赖标签顺序/数据库，任何场景（点云标签库 / 数据集标签回退）都稳定一致。
   */
  function getLabelColor(label: string): string {
    const configured = labelColorMap[label];
    if (configured) return configured;
    // FNV-1a 32bit
    let h1 = 0x811c9dc5;
    // djb2 32bit
    let h2 = 5381;
    for (let i = 0; i < label.length; i++) {
      const c = label.charCodeAt(i);
      h1 ^= c;
      h1 = Math.imul(h1, 0x01000193) >>> 0;
      h2 = ((h2 << 5) + h2 + c) >>> 0; // h2 * 33 + c
    }
    // 混合双哈希映射到 0-360 色相（h2 乘黄金角无理数 137.508 打破同余分布）
    const hue = (h1 + h2 * 137.508) % 360;
    return hslToHex(hue, 70, 55);
  }

  /** HSL(h,s,l) -> #RRGGBB，s/l 为 0-100 */
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

  /** 历史栈（撤销/重做） */
  const historyStack = ref<HistoryEntry[]>([]);
  const historyIndex = ref(-1);
  const canUndo = computed(() => historyIndex.value >= 0);
  const canRedo = computed(() => historyIndex.value < historyStack.value.length - 1);

  /** 当前选中框内点云数（点击选中时统计） */
  const selectedBoxPointCount = ref(0);

  /** 已标注文件数 / 未标注文件数（用于跳转与提示） */
  const annotatedCount = computed(() => files.value.filter((f) => f.annotated).length);
  const unannotatedCount = computed(() => files.value.length - annotatedCount.value);

  /** 统计：已标/未标/总数（基于全量 files） */
  const annotatedCountAll = computed(() => files.value.filter((f) => f.annotated).length);
  const unannotatedCountAll = computed(() => files.value.length - annotatedCountAll.value);

  /** 当前文件在列表中的索引 */
  const currentFileIndex = computed(() =>
    files.value.findIndex((f) => f.id === currentFile.value?.id),
  );

  /** ==================== 底部帧播放器（时序帧切换）==================== */
  /** 当前帧序号（1 起） */
  const currentFrameNumber = computed(() =>
    files.value.length === 0 ? 0 : currentFileIndex.value + 1,
  );
  /** 是否有帧可切换 */
  const canFrameNav = computed(() => files.value.length > 0);
  /** 是否正在自动播放 */
  const isPlaying = ref(false);
  /** 自动播放速度（帧/秒） */
  const playFps = ref(2);
  /** 自动播放定时器 */
  let playTimer: ReturnType<typeof setTimeout> | null = null;

  /** 播放/暂停：逐帧自动切换（模拟视频播放） */
  function togglePlay() {
    if (isPlaying.value) {
      stopPlay();
      return;
    }
    // 已到最后一帧时从头播放
    if (currentFileIndex.value >= files.value.length - 1 && files.value.length > 0) {
      selectFile(files.value[0]);
    }
    isPlaying.value = true;
    playTimer = setTimeout(playNextTick, 1000 / playFps.value);
  }

  /** 停止自动播放 */
  function stopPlay() {
    isPlaying.value = false;
    if (playTimer) {
      clearTimeout(playTimer);
      playTimer = null;
    }
  }

  /** 播放下一帧（供定时器驱动；到底停止） */
  function playNextTick() {
    if (!isPlaying.value) return;
    if (currentFileIndex.value >= files.value.length - 1) {
      stopPlay();
      return;
    }
    selectFile(files.value[currentFileIndex.value + 1]);
    playTimer = setTimeout(playNextTick, 1000 / playFps.value);
  }

  /** 进度条/点阵跳转：frameNo 为 1 起的帧号 */
  function seekToFrame(frameNo: number) {
    if (!canFrameNav.value) return;
    const index = Math.max(1, Math.min(frameNo, files.value.length)) - 1;
    if (index !== currentFileIndex.value) {
      stopPlay();
      selectFile(files.value[index]);
    }
  }

  /** Slider 回调（antd 返回 number） */
  function onFrameSeek(value: number | number[]) {
    const v = Array.isArray(value) ? value[0] : value;
    seekToFrame(Math.round(v));
  }

  /** 上一帧 */
  function prevFrame() {
    if (currentFileIndex.value <= 0) return;
    stopPlay();
    selectFile(files.value[currentFileIndex.value - 1]);
  }

  /** 下一帧 */
  function nextFrame() {
    if (currentFileIndex.value >= files.value.length - 1) return;
    stopPlay();
    selectFile(files.value[currentFileIndex.value + 1]);
  }

  /** ==================== Three.js 场景变量 ==================== */
  let scene: THREE.Scene | undefined;
  let camera: THREE.PerspectiveCamera | undefined;
  let renderer: THREE.WebGLRenderer | undefined;
  let controls: OrbitControls | undefined;
  let points: THREE.Points | undefined;
  let intensityAttribute: THREE.BufferAttribute | null = null; // 自解析的强度数据
  let animationId = 0;
  let resizeObserver: ResizeObserver | undefined;
  const raycaster = new THREE.Raycaster();
  const pointer = new THREE.Vector2();

  /** 选区交互临时状态（屏幕坐标） */
  let rectStart: { x: number; y: number } | null = null;
  let rectCurrent: { x: number; y: number } | null = null;
  let polygonPoints: { x: number; y: number }[] = [];
  let selectionOverlay: THREE.Points | null = null; // 选区黄色高亮层
  let selectionMoveListener: ((e: MouseEvent) => void) | null = null;
  let selectionUpListener: ((e: MouseEvent) => void) | null = null;

  /** 3D Box 拖拽绘制临时状态（世界坐标，参考 DotCloud drawBoxPreview 交互） */
  let boxAnchor: THREE.Vector3 | null = null; // 按下时拾取的锚点（世界坐标）
  let boxPreview: THREE.LineSegments | null = null; // 拖拽中的黄色线框预览

  /** 三视图（俯/正/侧）状态：refs 由模板注入，绘制逻辑见「三视图」一节 */
  const tvTopRef = ref<HTMLCanvasElement>();
  const tvFrontRef = ref<HTMLCanvasElement>();
  const tvSideRef = ref<HTMLCanvasElement>();

  /** ==================== 生命周期 ==================== */
  onMounted(async () => {
    await nextTick();
    initScene();
    await Promise.all([loadFiles(), loadLabels()]);
    pageRef.value?.focus();
    console.info('[点云标注] 页面初始化完成', { datasetId, datasetName });
  });

  onBeforeUnmount(() => {
    stopPlay();
    cancelAnimationFrame(animationId);
    resizeObserver?.disconnect();
    controls?.dispose();
    renderer?.dispose();
    renderer?.domElement.removeEventListener('click', handleCanvasClick);
    document.removeEventListener('mousemove', handleWindowMouseMove);
    document.removeEventListener('mouseup', handleWindowMouseUp);
    clearSceneObjects();
  });

  /** ==================== 数据加载 ==================== */

  /** 加载文件列表并判定标注状态（pcdMetadata.annotations 非空 → 已标注） */
  async function loadFiles() {
    fileLoading.value = true;
    try {
      const response = await getPcDatasetFiles(datasetId, { current: 1, size: 1000 });
      const list: PcFile[] = (response?.result || []) as PcFile[];
      list.forEach((file) => {
        file.annotated = hasStoredAnnotations(file.pcdMetadata);
      });
      files.value = list;
      console.info('[点云标注] 文件列表加载完成', {
        datasetId,
        total: list.length,
        annotated: list.filter((f) => f.annotated).length,
      });
      if (!currentFile.value && files.value.length > 0) {
        await selectFile(files.value[0]);
      }
    } catch (error) {
      console.error('[点云标注] 加载 PCD 文件列表失败', { datasetId, error });
      createMessage.error('加载 PCD 文件列表失败');
    } finally {
      fileLoading.value = false;
    }
  }

  /** 判定文件是否已有后端保存的标注（pcdMetadata.annotations 数组非空） */
  function hasStoredAnnotations(pcdMetadata?: string): boolean {
    if (!pcdMetadata) return false;
    try {
      const meta = JSON.parse(pcdMetadata);
      return Array.isArray(meta?.annotations) && meta.annotations.length > 0;
    } catch {
      return false;
    }
  }

  /**
   * 加载标签：优先使用「标签管理 → 点云标签」选项卡（labelTemplate type=1）中定义的标签；
   * 若未配置则回退到数据集自身标签（data_dataset_label），再未配置则提示。
   */
  async function loadLabels() {
    try {
      // 每次重载前清空库色映射（标签可能改名/改色，避免旧键残留）
      Object.keys(labelColorMap).forEach((k) => delete labelColorMap[k]);
      // 1. 从点云标签库（label_template_pointcloud）取标签
      const templates = await getAllLabelTemplatePointcloud();
      const templateList: LabelInfo[] = Array.isArray(templates) ? templates : [];
      if (templateList.length > 0) {
        // 展示中文名称（displayName），标注值用标注名称（annotationName）
        backendLabels.value = templateList.map((t) => ({
          ...t,
          name: t.annotationName || t.name,
        }));
        labelOptions.value = templateList.map((t) => ({
          label: t.displayName || t.annotationName || t.name,
          value: t.annotationName || t.name,
        }));
        // 填充库色映射：管理页配置过颜色的类别优先使用库色，未配置的走哈希兜底
        templateList.forEach((t) => {
          const key = t.annotationName || t.name;
          if (t.color) labelColorMap[key] = t.color;
        });
      } else {
        // 2. 回退：数据集自身标签
        const response = await queryLabels(datasetId);
        const list: LabelInfo[] = Array.isArray(response) ? response : response?.result || [];
        backendLabels.value = list;
        labelOptions.value = list.map((item) => ({ label: item.name, value: item.name }));
      }
      if (labelOptions.value.length === 0) {
        draft.label = '';
        createMessage.info('未配置标签，请先在「标签管理」的「点云标签」选项卡中添加目标类别');
      } else if (!draft.label || !labelOptions.value.find((opt) => opt.value === draft.label)) {
        draft.label = labelOptions.value[0].value;
      }
      console.info('[点云标注] 标签加载完成', { datasetId, labels: labelOptions.value });
    } catch (error) {
      console.error('[点云标注] 加载标签失败', { datasetId, error });
      createMessage.warning('加载标签失败');
    }
  }

  /** ==================== Three.js 场景初始化 ==================== */

  function initScene() {
    const container = renderRef.value;
    if (!container) return;
    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x0b1120);
    camera = new THREE.PerspectiveCamera(60, container.clientWidth / container.clientHeight, 0.01, 100000);
    camera.position.set(0, 0, 30);
    renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(container.clientWidth, container.clientHeight);
    // canvas 由 JS 动态创建，scoped 样式选择器(.render-container canvas[data-v-x])匹配不到，
    // 必须用内联样式强制 absolute 定位，否则 canvas 是 static、被 relative 的 Spin 容器盖住而不可见。
    const glCanvas = renderer.domElement;
    glCanvas.style.position = 'absolute';
    glCanvas.style.inset = '0';
    glCanvas.style.width = '100%';
    glCanvas.style.height = '100%';
    container.appendChild(glCanvas);
    controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.08;
    raycaster.params.Points.threshold = 0.35;
    renderer.domElement.addEventListener('click', handleCanvasClick);
    resizeObserver = new ResizeObserver(resizeScene);
    resizeObserver.observe(container);
    animate();
    console.info('[点云标注] Three.js 场景初始化完成', { datasetId });
  }

  /** 选择文件并加载 PCD */
  async function selectFile(file: PcFile) {
    currentFile.value = file;
    pointLoading.value = true;
    loadError.value = '';
    cancelSelectionInteraction();
    clearAnnotations();
    clearAllSegObjects(false);
    clearSelection();
    // 历史栈按文件隔离：切换文件后旧文件的操作不可撤销（避免跨文件误撤销）
    historyStack.value = [];
    historyIndex.value = -1;
    try {
      const objectKey = file.objectKey || file.url;
      if (!objectKey) throw new Error('文件缺少 MinIO 对象路径');
      // MinIO 直链必须带上 bucket 名（例如 /cz-dev/），否则返回 403；兼容已含 bucket 前缀的旧数据
      const url = /^https?:\/\//i.test(objectKey)
        ? objectKey
        : objectKey.startsWith(`${bucketName}/`)
          ? `${bucketHost}/${objectKey}`
          : `${bucketHost}/${bucketName}/${objectKey}`;
      const response = await fetch(url);
      if (!response.ok) throw new Error(`MinIO 返回 HTTP ${response.status}`);
      const buffer = await response.arrayBuffer();
      const loaded = new PCDLoader().parse(buffer, file.name);
      replacePoints(loaded);
      pointCount.value = loaded.geometry.getAttribute('position')?.count || file.pointCount || 0;
      // 自解析强度（PCD ASCII 含 intensity 字段，PCDLoader 不解析该列）
      intensityAttribute = extractIntensity(buffer);
      if (intensityAttribute) {
        loaded.geometry.setAttribute('intensity', intensityAttribute);
        console.info('[点云标注] 强度字段解析完成', { datasetId, count: intensityAttribute.count });
      }
      applyColorMode();
      fitCameraToPoints(loaded);
      console.info('[点云标注] PCD 加载完成', {
        datasetId,
        fileId: file.id,
        objectKey,
        bytes: buffer.byteLength,
        pointCount: pointCount.value,
        annotated: file.annotated,
      });
    } catch (error) {
      loadError.value = error instanceof Error ? error.message : '点云加载失败';
      console.error('[点云标注] 从 MinIO 加载 PCD 失败', { datasetId, fileId: file.id, error });
    } finally {
      pointLoading.value = false;
    }
  }

  /**
   * 从 PCD 二进制中解析 intensity 列（仅支持 ASCII 编码，兼容 FIELDS 含 intensity 的场景）。
   * PCDLoader 只解析 x/y/z 与 rgb，intensity 需自行提取以支持「按强度着色」。
   */
  function extractIntensity(buffer: ArrayBuffer): THREE.BufferAttribute | null {
    try {
      const text = new TextDecoder('utf-8').decode(buffer);
      const lines = text.split('\n');
      let fieldsLine = '';
      let dataAscii = false;
      let pointCount = 0;
      for (const line of lines) {
        const trimmed = line.trim();
        if (trimmed.startsWith('FIELDS')) fieldsLine = trimmed;
        else if (trimmed.startsWith('POINTS')) pointCount = parseInt(trimmed.split(/\s+/)[1], 10);
        else if (trimmed.startsWith('DATA')) dataAscii = trimmed.toUpperCase().includes('ASCII');
      }
      if (!fieldsLine || !dataAscii || pointCount <= 0) return null;
      const fields = fieldsLine.split(/\s+/).slice(1);
      const intensityIndex = fields.findIndex((f) => f.toLowerCase() === 'intensity');
      if (intensityIndex < 0) return null;
      // 跳过头部（含 DATA 行），逐行解析 intensity 列
      const values = new Float32Array(pointCount);
      const dataStart = text.indexOf('DATA');
      const firstDataLine = text.indexOf('\n', dataStart) + 1;
      const body = text.slice(firstDataLine).trim();
      const rows = body.split(/\r?\n/);
      let count = 0;
      for (const row of rows) {
        if (count >= pointCount) break;
        const cols = row.trim().split(/\s+/);
        if (cols.length <= intensityIndex) continue;
        values[count] = parseFloat(cols[intensityIndex]);
        count++;
      }
      return new THREE.BufferAttribute(values, 1);
    } catch (error) {
      console.warn('[点云标注] 强度字段解析失败，按强度着色不可用', { error });
      return null;
    }
  }

  /** 替换点云对象（释放旧资源） */
  function replacePoints(loaded: THREE.Points) {
    if (!scene) return;
    if (points) {
      scene.remove(points);
      points.geometry.dispose();
      (points.material as THREE.Material).dispose();
    }
    points = loaded;
    pointCloudReady.value = true;
    const material = points.material as THREE.PointsMaterial;
    material.size = pointSize.value;
    material.sizeAttenuation = true;
    scene.add(points);
  }

  /** 相机适配至点云包围盒 */
  function fitCameraToPoints(target: THREE.Points) {
    if (!camera || !controls) return;
    target.geometry.computeBoundingBox();
    const bounds = target.geometry.boundingBox;
    if (!bounds) return;
    const center = bounds.getCenter(new THREE.Vector3());
    const size = bounds.getSize(new THREE.Vector3()).length();
    // raycast 命中半径必须与点云尺度联动：
    // 固定 0.35 在 LiDAR 大尺度点云（包围盒对角线可达数百米）上过小，
    // 相机拉远后射线会从点隙穿过导致「未拾取到点云」（暗病：点击无法放置 3D Box）。
    // 取包围盒对角线的 0.5%，兼顾命中率与点击精度。
    raycaster.params.Points.threshold = Math.max(size * 0.005, 0.1);
    console.info('[点云标注] raycast 阈值已按点云尺度校准', {
      boundingBoxDiagonal: size,
      threshold: raycaster.params.Points.threshold,
    });
    controls.target.copy(center);
    // 默认视角：相机距离减半 ≈ 初始缩放放大 2 倍
    camera.position.set(center.x, center.y - Math.max(size, 10) / 2, center.z + Math.max(size * 0.6, 6) / 2);
    camera.near = Math.max(size / 10000, 0.01);
    camera.far = Math.max(size * 100, 1000);
    camera.updateProjectionMatrix();
    controls.update();
  }

  /** ==================== 工具切换 ==================== */

  function setTool(tool: ToolMode) {
    // 取消进行中的选区/3D Box 拖拽交互
    cancelSelectionInteraction();
    removeBoxPreview();
    boxAnchor = null;
    if (tool === 'box') {
      createMessage.info('在点云上按住鼠标拖拽框选范围，松手创建 3D Box（点一下用默认尺寸）');
    }
    activeTool.value = tool;
    // 绘制/选区类工具下禁用 OrbitControls（左键拖拽/滚轮会与选区、Cuboid 交互冲突），
    // 选择/平移工具下恢复视角控制（与图片标注页"绘制/移动模式互斥"的设计一致）
    const needsOrbit = tool === 'select' || tool === 'pan';
    if (controls) controls.enabled = needsOrbit;
    // 平移工具：左键拖拽 = 平移视口（OrbitControls 左键默认旋转，这里切换为 PAN）
    if (controls && tool === 'pan') {
      controls.mouseButtons = {
        LEFT: THREE.MOUSE.PAN,
        MIDDLE: THREE.MOUSE.DOLLY,
        RIGHT: THREE.MOUSE.ROTATE,
      };
    } else if (controls && tool === 'select') {
      controls.mouseButtons = {
        LEFT: THREE.MOUSE.ROTATE,
        MIDDLE: THREE.MOUSE.DOLLY,
        RIGHT: THREE.MOUSE.PAN,
      };
    }
    // 绘制/选区类工具显示十字光标，提示用户当前处于"绘制/打标"状态（选择/平移工具恢复默认指针）
    if (renderer) {
      renderer.domElement.style.cursor =
        tool === 'select' || tool === 'pan' ? 'default' : 'crosshair';
    }
    console.info('[点云标注] 切换工具', { tool, orbitEnabled: needsOrbit });
  }

  /** ==================== 画布鼠标事件 ==================== */

  /** 鼠标按下：矩形选区起点 / 3D Box 拖拽锚点 / Cuboid 交互起点 */
  function handleCanvasMouseDown(event: MouseEvent) {
    if (viewMode.value === 'view') return;
    if (activeTool.value === 'rect') {
      // 矩形选区：左键按下记录起点（右键不处理，保留 OrbitControls 平移）
      if (event.button !== 0) return;
      rectStart = getCanvasRelativePos(event);
      rectCurrent = rectStart;
      document.addEventListener('mousemove', handleWindowMouseMove);
      document.addEventListener('mouseup', handleWindowMouseUp);
      event.preventDefault();
    } else if (activeTool.value === 'box' && event.button === 0) {
      // 3D Box：按下拾取世界坐标作为锚点（raycast miss 时回退到射线远端，参考 DotCloud pickFallbackPoint）
      const world = pickWorldPoint(event) || pickFallbackPoint(event);
      if (world) {
        boxAnchor = world.clone();
        document.addEventListener('mousemove', handleWindowMouseMove);
        document.addEventListener('mouseup', handleWindowMouseUp);
        event.preventDefault();
        console.info('[点云标注] 3D Box 锚点已设', { x: world.x.toFixed(2), y: world.y.toFixed(2), z: world.z.toFixed(2) });
      }
    }
  }

  /** 画布内鼠标移动：矩形选区实时预览 / 3D Box 拖拽预览 / Cuboid 预览 */
  function handleCanvasMouseMove(event: MouseEvent) {
    if (viewMode.value === 'view') return;
    if (activeTool.value === 'rect' && rectStart) {
      rectCurrent = getCanvasRelativePos(event);
      drawRectOverlay();
    } else if (activeTool.value === 'box' && boxAnchor) {
      const cur = pickWorldPoint(event) || pickFallbackPoint(event);
      if (cur) drawBoxPreview(boxAnchor, cur);
    }
  }

  /** 鼠标释放：矩形选区完成 / 3D Box 按 AABB 创建 */
  function handleCanvasMouseUp(event: MouseEvent) {
    if (viewMode.value === 'view') return;
    if (activeTool.value === 'rect' && rectStart && event.button === 0) {
      rectCurrent = getCanvasRelativePos(event);
      finishRectSelection();
      document.removeEventListener('mousemove', handleWindowMouseMove);
      document.removeEventListener('mouseup', handleWindowMouseUp);
      rectStart = null;
      rectCurrent = null;
    } else if (activeTool.value === 'box' && boxAnchor && event.button === 0) {
      const cur = pickWorldPoint(event) || pickFallbackPoint(event);
      if (cur) finishBoxByDrag(boxAnchor, cur);
      document.removeEventListener('mousemove', handleWindowMouseMove);
      document.removeEventListener('mouseup', handleWindowMouseUp);
      boxAnchor = null;
      removeBoxPreview();
    }
  }

  /** 双击：多边形选区闭合（去掉双击自动触发的最后两个重复点击点） */
  function handleCanvasDblClick(event: MouseEvent) {
    if (viewMode.value === 'view') return;
    if (activeTool.value === 'polygon') {
      // 浏览器在 dblclick 前会触发两次 click，每个 click 已在 handleCanvasClick 中加了一个点，
      // 这里去掉最后两个重复点，保证多边形顶点是用户实际单击的位置
      if (polygonPoints.length >= 3) {
        polygonPoints = polygonPoints.slice(0, -2);
        finishPolygonSelection();
      } else {
        polygonPoints = [];
        removePolygonOverlay();
      }
    }
  }

  /** 窗口级监听（选区/3D Box 拖拽移出画布也能收尾） */
  function handleWindowMouseMove(event: MouseEvent) {
    if (activeTool.value === 'rect' && rectStart) {
      rectCurrent = getCanvasRelativePos(event);
      drawRectOverlay();
    } else if (activeTool.value === 'box' && boxAnchor) {
      const cur = pickWorldPoint(event) || pickFallbackPoint(event);
      if (cur) drawBoxPreview(boxAnchor, cur);
    }
  }
  function handleWindowMouseUp(event: MouseEvent) {
    if (activeTool.value === 'rect' && rectStart) {
      rectCurrent = getCanvasRelativePos(event);
      finishRectSelection();
      document.removeEventListener('mousemove', handleWindowMouseMove);
      document.removeEventListener('mouseup', handleWindowMouseUp);
      rectStart = null;
      rectCurrent = null;
    } else if (activeTool.value === 'box' && boxAnchor) {
      const cur = pickWorldPoint(event) || pickFallbackPoint(event);
      if (cur) finishBoxByDrag(boxAnchor, cur);
      document.removeEventListener('mousemove', handleWindowMouseMove);
      document.removeEventListener('mouseup', handleWindowMouseUp);
      boxAnchor = null;
      removeBoxPreview();
    }
  }

  /** canvas 相对坐标 */
  function getCanvasRelativePos(event: MouseEvent): { x: number; y: number } {
    const rect = renderer!.domElement.getBoundingClientRect();
    return { x: event.clientX - rect.left, y: event.clientY - rect.top };
  }

  /**
   * 画布单击：按当前工具分发。
   * - cuboid：状态机推进（锚点 → 长宽 → 确认）
   * - polygon：多边形选区逐点描边
   * - select：拾取选中 3D Box / 分割对象
   * 注意：3D Box 已改为"按下拖拽 → 松手按 AABB 创建"（参考 DotCloud main.js drawBoxPreview），
   * 不再走 click 分支，避免与拖拽完成重复创建。
   */
  function handleCanvasClick(event: MouseEvent) {
    if (viewMode.value === 'view') return;
    const rect = renderer!.domElement.getBoundingClientRect();
    pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
    pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;

    if (activeTool.value === 'polygon') {
      polygonPoints.push(getCanvasRelativePos(event));
      drawPolygonOverlay();
      return;
    }

    // select 模式：拾取 3D Box（hitProxy）或分割对象（overlay）
    if (activeTool.value === 'select') {
      selectByRaycast(pointer);
    }
  }

  /** 射线拾取：优先 3D Box（不透明 hitProxy），其次分割对象 overlay */
  function selectByRaycast(ndc: THREE.Vector2) {
    if (!scene) return;
    // 注意：Group 本身不可被 Raycaster 命中，必须递归收集其下的 Mesh(hitProxy) 与 Points(overlay)
    const interactMeshes: THREE.Object3D[] = [];
    boxes.value.forEach((box) => {
      box.group.traverse((child) => {
        const mesh = child as THREE.Mesh;
        if (mesh.isMesh && (mesh.material as THREE.MeshBasicMaterial).opacity === 0) {
          interactMeshes.push(mesh);
        }
      });
    });
    segObjects.value.forEach((obj) => obj.overlay && interactMeshes.push(obj.overlay));
    if (interactMeshes.length === 0) return;
    raycaster.setFromCamera(ndc, camera!);
    const hits = raycaster.intersectObjects(interactMeshes, false);
    if (hits.length === 0) {
      // 未命中任何标注 → 取消选中
      selectedBoxIndex.value = -1;
      setCurrentObject(null);
      return;
    }
    const hitObject = hits[0].object;
    // 命中 Box 的 hitProxy（属于某个 Box 的 Group）
    const boxIndex = boxes.value.findIndex((box) => box.group.children.includes(hitObject));
    if (boxIndex >= 0) {
      selectBox(boxIndex);
      // 框属于对象：点击框时聚焦其所属分割对象（只显示该对象区域，避免其他对象巨大标签/着色层全部冒出）
      const box = boxes.value[boxIndex];
      setCurrentObject(box?.objectId ?? null);
      return;
    }
    // 命中分割对象 overlay
    const seg = segObjects.value.find((obj) => obj.overlay === hitObject);
    if (seg) {
      setCurrentObject(seg.id);
      selectedBoxIndex.value = -1;
    }
  }

  /** ==================== 3D Box 拖拽绘制 ==================== */

  /**
   * 拾取鼠标射线命中的点云世界坐标。
   * raycast miss（点隙/空白区域）时返回 null，由调用方回退到 pickFallbackPoint。
   * 参考 DotCloud main.js pickWorldPoint。
   */
  function pickWorldPoint(event: MouseEvent): THREE.Vector3 | null {
    if (!points || !camera) return null;
    const rect = renderer!.domElement.getBoundingClientRect();
    const ndc = new THREE.Vector2(
      ((event.clientX - rect.left) / rect.width) * 2 - 1,
      -((event.clientY - rect.top) / rect.height) * 2 + 1,
    );
    raycaster.setFromCamera(ndc, camera);
    const hit = raycaster.intersectObject(points, false)[0];
    return hit?.point ?? null;
  }

  /**
   * raycast miss 时的回退：把鼠标射线延伸到点云包围盒中心距离处，得到一个可用的世界坐标点，
   * 保证在点稀疏/空白区域也能拖拽出 3D Box。参考 DotCloud main.js pickFallbackPoint。
   */
  function pickFallbackPoint(event: MouseEvent): THREE.Vector3 | null {
    if (!points || !camera || !points.geometry.boundingBox) return null;
    const rect = renderer!.domElement.getBoundingClientRect();
    const ndc = new THREE.Vector2(
      ((event.clientX - rect.left) / rect.width) * 2 - 1,
      -((event.clientY - rect.top) / rect.height) * 2 + 1,
    );
    raycaster.setFromCamera(ndc, camera);
    const bbox = points.geometry.boundingBox;
    const center = new THREE.Vector3();
    bbox.getCenter(center);
    const t = camera.position.distanceTo(center);
    return camera.position.clone().add(raycaster.ray.direction.clone().normalize().multiplyScalar(t));
  }

  /**
   * 拖拽过程中实时绘制 3D Box 线框预览（AABB：两角世界坐标）。
   * 用 LineSegments + depthTest:false + 高 renderOrder 保证线框始终盖在点云上，参考 DotCloud drawBoxPreview。
   */
  function drawBoxPreview(a: THREE.Vector3, b: THREE.Vector3) {
    if (!scene) return;
    const lx = Math.min(a.x, b.x), ux = Math.max(a.x, b.x);
    const ly = Math.min(a.y, b.y), uy = Math.max(a.y, b.y);
    const lz = Math.min(a.z, b.z), uz = Math.max(a.z, b.z);
    const corners = [
      [lx, ly, lz], [ux, ly, lz], [ux, uy, lz], [lx, uy, lz],
      [lx, ly, uz], [ux, ly, uz], [ux, uy, uz], [lx, uy, uz],
    ];
    const edges = [
      [0, 1], [1, 2], [2, 3], [3, 0],
      [4, 5], [5, 6], [6, 7], [7, 4],
      [0, 4], [1, 5], [2, 6], [3, 7],
    ];
    const pts: number[] = [];
    for (const [i, j] of edges) {
      pts.push(corners[i][0], corners[i][1], corners[i][2]);
      pts.push(corners[j][0], corners[j][1], corners[j][2]);
    }
    const geom = new THREE.BufferGeometry();
    geom.setAttribute('position', new THREE.BufferAttribute(new Float32Array(pts), 3));
    const mat = new THREE.LineBasicMaterial({ color: 0xffeb3b, depthTest: false });
    if (boxPreview) {
      boxPreview.geometry.dispose();
      boxPreview.geometry = geom;
      boxPreview.material = mat;
    } else {
      boxPreview = new THREE.LineSegments(geom, mat);
      boxPreview.renderOrder = 998;
      scene.add(boxPreview);
    }
  }

  /** 清除 3D Box 拖拽预览 */
  function removeBoxPreview() {
    if (!boxPreview) return;
    scene?.remove(boxPreview);
    boxPreview.geometry.dispose();
    boxPreview.material.dispose();
    boxPreview = null;
  }

  /**
   * 松手完成：按 AABB（两角世界坐标）创建 3D Box。
   * 拖拽距离过小（≈单击）时按点击位置用默认尺寸创建，兼容"点一下放默认框"的旧交互。
   */
  function finishBoxByDrag(a: THREE.Vector3, b: THREE.Vector3) {
    if (!points) return;
    const lx = Math.min(a.x, b.x), ux = Math.max(a.x, b.x);
    const ly = Math.min(a.y, b.y), uy = Math.max(a.y, b.y);
    const lz = Math.min(a.z, b.z), uz = Math.max(a.z, b.z);
    const dx = ux - lx, dy = uy - ly, dz = uz - lz;
    // 阈值：三边都小于 0.5 米视为"点击"而非"拖拽"（点云尺度以米计）
    const CLICK_EPS = 0.5;
    if (dx < CLICK_EPS && dy < CLICK_EPS && dz < CLICK_EPS) {
      createBox(a.clone());
      return;
    }
    const center = new THREE.Vector3((lx + ux) / 2, (ly + uy) / 2, (lz + uz) / 2);
    createBoxAt(center, [dx, dy, dz]);
    console.info('[点云标注] 3D Box 拖拽创建完成', { size: [dx.toFixed(2), dy.toFixed(2), dz.toFixed(2)] });
  }

  /** 按指定中心与尺寸创建 3D Box（覆盖默认尺寸） */
  function createBoxAt(center: THREE.Vector3, size: [number, number, number]) {
    if (!scene) return;
    // 工作流强制：必须先创建/选中分割对象，再给对象添加 3D Box
    if (!currentObjectId.value) {
      createMessage.warning('请先在右侧列表创建并选中一个分割对象，再添加 3D Box');
      return;
    }
    const color = getLabelColor(draft.label);
    const [length, width, height] = size;
    const group = buildBoxGroup(center, length, width, height, draft.yaw, color);
    scene.add(group);
    const annotation: BoxAnnotation = {
      id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
      label: draft.label,
      center: [center.x, center.y, center.z],
      size: [length, width, height],
      yaw: draft.yaw,
      color,
      group: markRaw(group), // THREE 对象禁止被 Vue 响应式代理（否则渲染访问 modelViewMatrix 等只读属性崩溃）
      objectId: currentObjectId.value, // 关联到当前选中的分割对象
    };
    boxes.value.push(annotation);
    selectBox(boxes.value.length - 1);
    renderBoxLabel(annotation, boxes.value.length);
    updateSelectedBoxPointCount();
    // 画框不自动归入对象：框内点云通过右侧「加入对象」按钮显式归入（选区 = 当前选中的 3D 框）
    recordHistory(
      '新建 3D Box',
      () => removeBoxByIndexInternal(boxes.value.indexOf(annotation), false),
      () => {
        scene.add(annotation.group);
        boxes.value.push(annotation);
        renderBoxLabel(annotation, boxes.value.length);
      },
    );
    console.info('[点云标注] 新建 3D Box', { id: annotation.id, center, size, yaw: draft.yaw });
    // 画完一个 3D Box 后自动切回默认"选择"工具（避免继续画框导致重叠或误操作，
    // 与图片标注页"绘制一次后回到选择工具"的交互约定一致）
    setTool('select');
  }

  /** ==================== 三视图（俯/正/侧）==================== */

  /** 三视图类型：top 俯视（X-Y）、front 正视（X-Z）、side 侧视（Y-Z） */
  type TvView = 'top' | 'front' | 'side';
  /** 三视图 tab 当前激活的视图（默认俯视，可点击 tab 切换） */
  const tvActiveView = ref<TvView>('top');
  /** 切换三视图 tab 并立即重绘（保持视图状态一致） */
  function setTvActiveView(view: TvView) {
    if (tvActiveView.value === view) return;
    tvActiveView.value = view;
    nextTick(() => renderThreeViews());
  }

  /**
   * 三视图是否可见：选中 3D 框且点云已加载。
   * 注意：points 是普通 let（非响应式），模板 v-if 不能直接引用它（setup 返回捕获的是初始 undefined，
   * 赋值后不会更新）—— 必须用 pointCloudReady（响应式 ref）+ selectedBoxIndex 组合。
   */
  const tvVisible = computed(() => selectedBoxIndex.value >= 0 && pointCloudReady.value);

  /** 三视图当前选中框的快照（供拖拽历史记录） */
  let tvSnapshot: { size: [number, number, number]; center: [number, number, number]; yaw: number } | null = null;

  /** 三视图拖拽状态 */
  let tvDragState: {
    view: TvView;
    dragging: 'size' | 'move' | 'rotate' | null;
    startClientX: number;
    startClientY: number;
    origSize: [number, number, number];
    origCenter: [number, number, number];
    origYaw: number;
    handleLocal: [number, number, number]; // resize 时被拖角点的局部坐标（决定缩放方向）
  } | null = null;

  const tvCanvasMap = (view: TvView): HTMLCanvasElement | undefined => {
    if (view === 'top') return tvTopRef.value;
    if (view === 'front') return tvFrontRef.value;
    return tvSideRef.value;
  };

  /**
   * 渲染 3D Box 三视图（俯视 X-Y / 正视 X-Z / 侧视 Y-Z，参考用户手册 6.1.4.9）。
   * 三个正交投影视图 + 可拖拽几何修改：
   * - 拖拽框内空白：平移选中框（move）
   * - 拖拽方块手柄：缩放（size）
   * - 俯视图拖拽旋转环：绕 Z 旋转（rotate，仅 top 视图）
   */
  function renderThreeViews() {
    const box = boxes.value[selectedBoxIndex.value];
    if (!box) requestAnimationFrame(() => clearThreeViewCanvases());
    else requestAnimationFrame(() => renderTvForBox(box));
  }

  function renderTvForBox(box: BoxAnnotation) {
    if (!points) return;
    const center = new THREE.Vector3(...box.center);
    // 三视图共用世界→视图的缩放：取框最大边外扩 60%
    const maxDim = Math.max(box.size[0], box.size[1], box.size[2]) * 1.6;
    const halfExtent = maxDim / 2 + 1;
    const views: TvView[] = ['top', 'front', 'side'];
    // 预计算所有点在本框中的相对坐标（世界坐标 - 中心，供投影）
    const positions = points.geometry.getAttribute('position') as THREE.BufferAttribute;
    const count = positions.count;
    // 采样上限：三视图每个 view 展示 ~20k 点，避免大点云卡顿
    const stride = Math.max(1, Math.ceil(count / 20000));
    const rel = new Float32Array(Math.ceil(count / stride) * 3);
    let relCount = 0;
    for (let i = 0; i < count; i += stride) {
      rel[relCount * 3] = positions.getX(i) - center.x;
      rel[relCount * 3 + 1] = positions.getY(i) - center.y;
      rel[relCount * 3 + 2] = positions.getZ(i) - center.z;
      relCount++;
    }
    for (const view of views) {
      const canvas = tvCanvasMap(view);
      if (!canvas) continue;
      const ctx = canvas.getContext('2d');
      if (!ctx) continue;
      const w = canvas.width;
      const h = canvas.height;
      ctx.clearRect(0, 0, w, h);
      // 投影函数：世界坐标 → 画布坐标（居中 + 缩放）
      const sx = (worldX: number) => w / 2 + (worldX / halfExtent) * (w / 2);
      const sy = (worldY: number) => h / 2 - (worldY / halfExtent) * (h / 2);
      const pick = (v: TvView, rx: number, ry: number, rz: number): [number, number] => {
        if (v === 'top') return [rx, ry]; // 俯视：X 右、Y 上
        if (v === 'front') return [rx, rz]; // 正视：X 右、Z 上
        return [ry, rz]; // 侧视：Y 右、Z 上
      };
      // 1) 背景网格（居中十字 + 刻度线）
      ctx.strokeStyle = 'rgba(138, 163, 191, 0.18)';
      ctx.lineWidth = 1;
      for (let g = -3; g <= 3; g++) {
        const off = (g / 3) * (w / 2);
        ctx.beginPath();
        ctx.moveTo(w / 2 + off, 0);
        ctx.lineTo(w / 2 + off, h);
        ctx.stroke();
        ctx.beginPath();
        ctx.moveTo(0, h / 2 + off);
        ctx.lineTo(w, h / 2 + off);
        ctx.stroke();
      }
      // 2) 点云投影（灰色小点）
      ctx.fillStyle = 'rgba(150, 175, 210, 0.5)';
      for (let i = 0; i < relCount; i++) {
        const rx = rel[i * 3];
        const ry = rel[i * 3 + 1];
        const rz = rel[i * 3 + 2];
        const [px, py] = pick(view, rx, ry, rz);
        if (Math.abs(px) > halfExtent || Math.abs(py) > halfExtent) continue;
        ctx.fillRect(sx(px) - 0.5, sy(py) - 0.5, 1.5, 1.5);
      }
      // 3) 框投影（带 yaw 的旋转矩形：局部角点旋转 + 中心平移）
      const yawRad = (box.yaw * Math.PI) / 180;
      const cosY = Math.cos(yawRad);
      const sinY = Math.sin(yawRad);
      const corners = [
        [-box.size[0] / 2, -box.size[1] / 2, -box.size[2] / 2],
        [box.size[0] / 2, -box.size[1] / 2, -box.size[2] / 2],
        [box.size[0] / 2, box.size[1] / 2, -box.size[2] / 2],
        [-box.size[0] / 2, box.size[1] / 2, -box.size[2] / 2],
        [-box.size[0] / 2, -box.size[1] / 2, box.size[2] / 2],
        [box.size[0] / 2, -box.size[1] / 2, box.size[2] / 2],
        [box.size[0] / 2, box.size[1] / 2, box.size[2] / 2],
        [-box.size[0] / 2, box.size[1] / 2, box.size[2] / 2],
      ];
      // 局部坐标 → 世界相对坐标（仅旋转 yaw，中心已减）
      const world = corners.map(([lx, ly, lz]) => [
        lx * cosY - ly * sinY,
        lx * sinY + ly * cosY,
        lz,
      ]);
      // 12 条棱
      const edges: [number, number][] = [
        [0, 1], [1, 2], [2, 3], [3, 0],
        [4, 5], [5, 6], [6, 7], [7, 4],
        [0, 4], [1, 5], [2, 6], [3, 7],
      ];
      ctx.strokeStyle = box.color;
      ctx.lineWidth = 2;
      ctx.strokeRect(-1, -1, 0, 0); // no-op
      edges.forEach(([a, b]) => {
        const [p1x, p1y] = pick(view, world[a][0], world[a][1], world[a][2]);
        const [p2x, p2y] = pick(view, world[b][0], world[b][1], world[b][2]);
        ctx.beginPath();
        ctx.moveTo(sx(p1x), sy(p1y));
        ctx.lineTo(sx(p2x), sy(p2y));
        ctx.stroke();
      });
      // 顶点方块手柄（拖拽缩放；所有角可拖，判断最近角）
      ctx.fillStyle = box.color;
      for (const [wx, wy, wz] of world) {
        const [px, py] = pick(view, wx, wy, wz);
        ctx.fillRect(sx(px) - 3, sy(py) - 3, 6, 6);
      }
      // 中心拖拽手柄（十字，拖拽平移框）
      const centerPx = sx(0);
      const centerPy = sy(0);
      ctx.strokeStyle = '#4fc3f7';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.moveTo(centerPx - 10, centerPy);
      ctx.lineTo(centerPx + 10, centerPy);
      ctx.moveTo(centerPx, centerPy - 10);
      ctx.lineTo(centerPx, centerPy + 10);
      ctx.stroke();
      // 中心圆圈
      ctx.beginPath();
      ctx.arc(centerPx, centerPy, 6, 0, Math.PI * 2);
      ctx.stroke();
      // 标注
      ctx.fillStyle = 'rgba(79, 195, 247, 0.95)';
      ctx.font = '10px sans-serif';
      ctx.fillText('拖拽平移', centerPx + 8, centerPy - 8);
      // 俯视图旋转环（X 轴正方向顶点外围一个圆环）
      if (view === 'top') {
        const rotHandle = world[1]; // +X 角
        const hx = sx(rotHandle[0]) + 22;
        const hy = sy(rotHandle[1]);
        ctx.strokeStyle = '#ffd54f';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(sx(rotHandle[0]), sy(rotHandle[1]));
        ctx.lineTo(hx, hy);
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(hx, hy, 6, 0, Math.PI * 2);
        ctx.stroke();
        ctx.fillStyle = '#ffd54f';
        ctx.fill();
        // 标注
        ctx.fillStyle = 'rgba(255, 213, 79, 0.95)';
        ctx.font = '11px sans-serif';
        ctx.fillText('拖拽旋转', hx - 26, hy - 12);
      }
      // 视图标题（左上角）
      const titles = { top: 'X→  Y↑', front: 'X→  Z↑', side: 'Y→  Z↑' };
      ctx.fillStyle = 'rgba(140, 163, 191, 0.9)';
      ctx.font = '11px monospace';
      ctx.fillText(titles[view], 6, 14);
      // 尺寸标注（右下角）
      ctx.fillStyle = 'rgba(255, 255, 255, 0.75)';
      ctx.font = '11px sans-serif';
      if (view === 'top') ctx.fillText(`长${box.size[0].toFixed(1)} 宽${box.size[1].toFixed(1)}`, w - 110, h - 8);
      else if (view === 'front') ctx.fillText(`长${box.size[0].toFixed(1)} 高${box.size[2].toFixed(1)}`, w - 110, h - 8);
      else ctx.fillText(`宽${box.size[1].toFixed(1)} 高${box.size[2].toFixed(1)}`, w - 110, h - 8);
    }
  }

  /** 清空三个三视图画布（无选中框时） */
  function clearThreeViewCanvases() {
    (['top', 'front', 'side'] as TvView[]).forEach((view) => {
      const canvas = tvCanvasMap(view);
      const ctx = canvas?.getContext('2d');
      if (canvas && ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
    });
  }

  /** 三视图鼠标按下：判断命中（角点/框内/俯视旋转环）并开始拖拽 */
  function onTvMouseDown(view: TvView, event: MouseEvent) {
    const box = boxes.value[selectedBoxIndex.value];
    if (!box) return;
    const canvas = tvCanvasMap(view);
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const cx = event.clientX - rect.left;
    const cy = event.clientY - rect.top;
    tvSnapshot = {
      size: [...box.size] as [number, number, number],
      center: [...box.center] as [number, number, number],
      yaw: box.yaw,
    };
    // 命中判定：距任一角点 < 14px → 缩放；俯视旋转环 → 旋转；框内 → 移动
    const yawRad = (box.yaw * Math.PI) / 180;
    const cosY = Math.cos(yawRad);
    const sinY = Math.sin(yawRad);
    // 8 个角点（局部坐标）+ 绕 Z 旋转 → 世界相对坐标 → 投影到本视图画布坐标
    const cornerLocal: [number, number, number][] = [];
    for (const lx of [-1, 1]) {
      for (const ly of [-1, 1]) {
        for (const lz of [-1, 1]) {
          cornerLocal.push([(lx * box.size[0]) / 2, (ly * box.size[1]) / 2, (lz * box.size[2]) / 2]);
        }
      }
    }
    const maxDim = Math.max(box.size[0], box.size[1], box.size[2]) * 1.6;
    const halfExtent = maxDim / 2 + 1;
    const canvasCorners = cornerLocal.map(([lx, ly, lz]) => {
      const wx = lx * cosY - ly * sinY;
      const wy = lx * sinY + ly * cosY;
      const wz = lz;
      const [px, py] = view === 'top'
        ? [wx, wy]
        : view === 'front'
          ? [wx, wz]
          : [wy, wz];
      return [canvas.width / 2 + (px / halfExtent) * (canvas.width / 2), canvas.height / 2 - (py / halfExtent) * (canvas.height / 2)];
    });
    // 角点命中（记录局部坐标，用于限定缩放方向：沿该角所在轴向反向增长）
    for (let i = 0; i < canvasCorners.length; i++) {
      const [px, py] = canvasCorners[i];
      if (Math.hypot(px - cx, py - cy) < 14) {
        tvDragState = {
          view,
          dragging: 'size',
          startClientX: event.clientX,
          startClientY: event.clientY,
          origSize: [...box.size] as [number, number, number],
          origCenter: [...box.center] as [number, number, number],
          origYaw: box.yaw,
          handleLocal: cornerLocal[i],
        };
        bindTvDragListeners();
        event.preventDefault();
        return;
      }
    }
    // 俯视旋转环命中（环中心在 +X 角外 22px，+X 角 = 8 角点中 (lx=+1, ly=-1, lz=-1) 即 index 4）
    if (view === 'top') {
      const [hx, hy] = canvasCorners[4];
      if (Math.hypot(hx + 22 - cx, hy - cy) < 16) {
        tvDragState = {
          view,
          dragging: 'rotate',
          startClientX: event.clientX,
          startClientY: event.clientY,
          origSize: [...box.size] as [number, number, number],
          origCenter: [...box.center] as [number, number, number],
          origYaw: box.yaw,
          handleLocal: [0, 0, 0],
        };
        bindTvDragListeners();
        event.preventDefault();
        return;
      }
    }
    // 中心手柄命中（圆圈半径 10px，所有视图可用，拖拽平移框）
    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;
    if (Math.hypot(centerX - cx, centerY - cy) < 10) {
      tvDragState = {
        view,
        dragging: 'move',
        startClientX: event.clientX,
        startClientY: event.clientY,
        origSize: [...box.size] as [number, number, number],
        origCenter: [...box.center] as [number, number, number],
        origYaw: box.yaw,
        handleLocal: [0, 0, 0],
      };
      bindTvDragListeners();
      event.preventDefault();
      return;
    }
    // 框内移动：命中判定（近似点在多边形内，用四个顶点）
    const polyX: number[] = [];
    const polyY: number[] = [];
    cornersForPolygon(box, view).forEach(([px, py]) => {
      const maxDim = Math.max(box.size[0], box.size[1], box.size[2]) * 1.6;
      const halfExtent = maxDim / 2 + 1;
      polyX.push(canvas.width / 2 + (px / halfExtent) * (canvas.width / 2));
      polyY.push(canvas.height / 2 - (py / halfExtent) * (canvas.height / 2));
    });
    if (pointInTvPolygon(cx, cy, polyX, polyY)) {
      tvDragState = {
        view,
        dragging: 'move',
        startClientX: event.clientX,
        startClientY: event.clientY,
        origSize: [...box.size] as [number, number, number],
        origCenter: [...box.center] as [number, number, number],
        origYaw: box.yaw,
        handleLocal: [0, 0, 0],
      };
      bindTvDragListeners();
      event.preventDefault();
    }
  }

  /** 获取框投影多边形（视图平面）的顶点（世界相对坐标） */
  function cornersForPolygon(box: BoxAnnotation, view: TvView): [number, number][] {
    const yawRad = (box.yaw * Math.PI) / 180;
    const cosY = Math.cos(yawRad);
    const sinY = Math.sin(yawRad);
    const all: [number, number][] = [];
    for (const lx of [-1, 1]) {
      for (const ly of [-1, 1]) {
        for (const lz of [-1, 1]) {
          const wx = (lx * box.size[0] / 2) * cosY - (ly * box.size[1] / 2) * sinY;
          const wy = (lx * box.size[0] / 2) * sinY + (ly * box.size[1] / 2) * cosY;
          const wz = lz * box.size[2] / 2;
          if (view === 'top') all.push([wx, wy]);
          else if (view === 'front') all.push([wx, wz]);
          else all.push([wy, wz]);
        }
      }
    }
    // 求凸包（近似：取投影外接矩形角即可，用 min/max）
    const xs = all.map((p) => p[0]);
    const ys = all.map((p) => p[1]);
    return [
      [Math.min(...xs), Math.min(...ys)],
      [Math.max(...xs), Math.min(...ys)],
      [Math.max(...xs), Math.max(...ys)],
      [Math.min(...xs), Math.max(...ys)],
    ];
  }

  /** 点是否在多边形内（射线法） */
  function pointInTvPolygon(px: number, py: number, polyX: number[], polyY: number[]): boolean {
    let inside = false;
    for (let i = 0, j = polyX.length - 1; i < polyX.length; j = i++) {
      const xi = polyX[i], yi = polyY[i];
      const xj = polyX[j], yj = polyY[j];
      if (yi > py !== yj > py && px < ((xj - xi) * (py - yi)) / (yj - yi + 1e-9) + xi) {
        inside = !inside;
      }
    }
    return inside;
  }

  /** 绑定三视图拖拽的窗口级监听 */
  function bindTvDragListeners() {
    document.addEventListener('mousemove', handleTvMouseMove);
    document.addEventListener('mouseup', handleTvMouseUp);
  }

  /** 三视图拖拽移动 */
  function handleTvMouseMove(event: MouseEvent) {
    if (!tvDragState) return;
    const box = boxes.value[selectedBoxIndex.value];
    if (!box) return;
    const { view, dragging, startClientX, startClientY, origSize, origCenter, origYaw } = tvDragState;
    const canvas = tvCanvasMap(view);
    if (!canvas) return;
    const scale = Math.max(box.size[0], box.size[1], box.size[2]) * 1.6 / 2 + 1;
    const dxWorld = ((event.clientX - startClientX) / (canvas.width / 2)) * scale;
    const dyWorld = ((event.clientY - startClientY) / (canvas.height / 2)) * scale;
    if (dragging === 'move') {
      // 世界坐标偏移：视图中 X/Y 轴分别对应
      const newCenter = [...origCenter] as [number, number, number];
      if (view === 'top') {
        newCenter[0] = origCenter[0] + dxWorld;
        newCenter[1] = origCenter[1] - dyWorld;
      } else if (view === 'front') {
        newCenter[0] = origCenter[0] + dxWorld;
        newCenter[2] = origCenter[2] - dyWorld;
      } else {
        newCenter[1] = origCenter[1] + dxWorld;
        newCenter[2] = origCenter[2] - dyWorld;
      }
      box.center = newCenter;
      rebuildBoxGroupForTv(box);
    } else if (dragging === 'size') {
      // 被拖角点跟随鼠标：新尺寸 = 2 × |该角相对中心的新坐标|，clamp 下限 0.3
      const { handleLocal } = tvDragState;
      const newSize = [...origSize] as [number, number, number];
      // 视图平面两轴对应的尺寸下标与局部坐标分量
      const axes: [number, 'x' | 'y' | 'z'][] =
        view === 'top' ? [[0, 'x'], [1, 'y']]
          : view === 'front' ? [[0, 'x'], [2, 'z']]
            : [[1, 'y'], [2, 'z']];
      const localVal = (axis: 'x' | 'y' | 'z') => (axis === 'x' ? handleLocal[0] : axis === 'y' ? handleLocal[1] : handleLocal[2]);
      // 轴1（画布横轴，向右为正）使用 +dxWorld；轴2（画布纵轴，向下为正 → 世界 -方向）使用 -dyWorld
      newSize[axes[0][0]] = Math.max(0.3, 2 * Math.abs(localVal(axes[0][1]) + dxWorld));
      newSize[axes[1][0]] = Math.max(0.3, 2 * Math.abs(localVal(axes[1][1]) - dyWorld));
      box.size = newSize;
      rebuildBoxGroupForTv(box);
    } else if (dragging === 'rotate') {
      // 旋转：拖拽旋转环到哪、框的 +X（局部长轴）就指向哪。画布中心即框中心，
      // 鼠标相对中心的世界方向角即为目标 yaw（俯视图 X-Y 平面绕 Z 旋转）。
      const rect = canvas.getBoundingClientRect();
      const canvasX = event.clientX - rect.left;
      const canvasY = event.clientY - rect.top;
      const rx = ((canvasX - canvas.width / 2) / (canvas.width / 2)) * scale; // 世界 X 偏移
      const ry = ((canvas.height / 2 - canvasY) / (canvas.height / 2)) * scale; // 世界 Y 偏移（画布 y 向下 → 世界 y 向上取反）
      const angleDeg = (Math.atan2(ry, rx) * 180) / Math.PI;
      box.yaw = (angleDeg + 360) % 360;
      rebuildBoxGroupForTv(box);
    }
    // 同步 draft（右侧 Yaw 滑块/尺寸输入实时跟随；watch 因 box 值与 draft 已一致会早退，不重复记历史）
    draft.length = box.size[0];
    draft.width = box.size[1];
    draft.height = box.size[2];
    draft.yaw = box.yaw;
    renderThreeViews();
  }

  /** 三视图拖拽中重建选中框（不记录历史，拖拽结束统一记录） */
  function rebuildBoxGroupForTv(box: BoxAnnotation) {
    if (!scene) return;
    const index = boxes.value.indexOf(box);
    if (index < 0) return;
    const center = new THREE.Vector3(...box.center);
    scene.remove(box.group);
    disposeGroup(box.group);
    box.group = markRaw(buildBoxGroup(center, box.size[0], box.size[1], box.size[2], box.yaw, box.color));
    scene.add(box.group);
    // 关键：立即同步 worldMatrix；否则后续 updateSelectedBoxPointCount 读到的 matrixWorld
    // 是身份矩阵（旧值），导致框内点云数与平移无关——用户感觉平移无效
    box.group.updateMatrixWorld(true);
    renderBoxLabel(box, index + 1);
    updateSelectedBoxPointCount();
  }

  /** 三视图拖拽结束：记录历史 */
  function handleTvMouseUp() {
    document.removeEventListener('mousemove', handleTvMouseMove);
    document.removeEventListener('mouseup', handleTvMouseUp);
    if (!tvDragState) return;
    const box = boxes.value[selectedBoxIndex.value];
    const before = tvSnapshot;
    tvDragState = null;
    tvSnapshot = null;
    if (!box || !before) return;
    const after = {
      size: [...box.size] as [number, number, number],
      center: [...box.center] as [number, number, number],
      yaw: box.yaw,
    };
    if (
      before.size[0] === after.size[0] &&
      before.size[1] === after.size[1] &&
      before.size[2] === after.size[2] &&
      before.center[0] === after.center[0] &&
      before.center[1] === after.center[1] &&
      before.center[2] === after.center[2] &&
      before.yaw === after.yaw
    ) {
      return;
    }
    const index = boxes.value.indexOf(box);
    recordHistory(
      '三视图调整 3D Box',
      () => restoreTvBoxState(index, before),
      () => restoreTvBoxState(index, after),
    );
    console.info('[点云标注] 三视图调整完成', { before, after });
  }

  /** 恢复三视图调整前的 Box 状态（供撤销/重做） */
  function restoreTvBoxState(index: number, state: { size: [number, number, number]; center: [number, number, number]; yaw: number }) {
    const box = boxes.value[index];
    if (!box || !scene) return;
    scene.remove(box.group);
    disposeGroup(box.group);
    box.size = [...state.size] as [number, number, number];
    box.center = [...state.center] as [number, number, number];
    box.yaw = state.yaw;
    box.group = markRaw(buildBoxGroup(new THREE.Vector3(...box.center), box.size[0], box.size[1], box.size[2], box.yaw, box.color));
    scene.add(box.group);
    renderBoxLabel(box, index + 1);
    updateSelectedBoxPointCount();
    renderThreeViews();
  }

  /** 完成矩形选区：把屏幕矩形投影为多边形 → 收集框内点索引 → 黄色高亮 */
  function finishRectSelection() {
    if (!rectStart || !rectCurrent || !points) return;
    const minX = Math.min(rectStart.x, rectCurrent.x);
    const maxX = Math.max(rectStart.x, rectCurrent.x);
    const minY = Math.min(rectStart.y, rectCurrent.y);
    const maxY = Math.max(rectStart.y, rectCurrent.y);
    // 过小矩形视为误触
    if (maxX - minX < 4 || maxY - minY < 4) return;
    const polygon = [
      { x: minX, y: minY },
      { x: maxX, y: minY },
      { x: maxX, y: maxY },
      { x: minX, y: maxY },
    ];
    removeRectOverlay();
    doSelect(polygon);
    console.info('[点云标注] 矩形选区完成', { 选中点数: selection.value.length });
  }

  /**
   * 通用选区：把屏幕多边形内的点云点收集为索引集合。
   * 原理（参考 DotCloud selectByScreenPolygon）：把每个点投影到屏幕坐标，
   * 用射线法判定是否落在多边形内。
   */
  function doSelect(polygon: { x: number; y: number }[]) {
    if (!points || !camera) return;
    const positions = points.geometry.getAttribute('position') as THREE.BufferAttribute;
    const hits: number[] = [];
    const v = new THREE.Vector3();
    const rect = renderer!.domElement.getBoundingClientRect();
    for (let i = 0; i < positions.count; i++) {
      // 世界坐标 = 几何体局部坐标 + Points 对象位移（参考 DotCloud selectByScreenPolygon）
      v.fromBufferAttribute(positions, i).add(points.position);
      v.project(camera); // 投影到 NDC
      const sx = (v.x * 0.5 + 0.5) * rect.width;
      const sy = (-v.y * 0.5 + 0.5) * rect.height;
      if (pointInPolygon(sx, sy, polygon)) hits.push(i);
    }
    setSelection(hits);
    drawSelectionOverlay();
  }

  /** 射线法判断点是否在多边形内（yj - yi 加 1e-9 防除零，参考 DotCloud 实现） */
  function pointInPolygon(px: number, py: number, polygon: { x: number; y: number }[]): boolean {
    let inside = false;
    for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
      const xi = polygon[i].x, yi = polygon[i].y;
      const xj = polygon[j].x, yj = polygon[j].y;
      const intersect =
        ((yi > py) !== (yj > py)) && px < ((xj - xi) * (py - yi)) / (yj - yi + 1e-9) + xi;
      if (intersect) inside = !inside;
    }
    return inside;
  }

  /** 设置选区索引集合 */
  function setSelection(indices: number[]) {
    selection.value = indices;
  }

  /** 清空选区并移除高亮层 */
  function clearSelection() {
    console.info('[点云标注] 清空选区', { 原选中点数: selection.value.length });
    selection.value = [];
    removeSelectionOverlay();
  }

  /** ==================== 多边形选区 ==================== */

  /** 完成多边形选区（双击/右键结束） */
  function finishPolygonSelection() {
    doSelect([...polygonPoints]);
    polygonPoints = [];
    removePolygonOverlay();
    console.info('[点云标注] 多边形选区完成', { 选中点数: selection.value.length });
  }

  /** ==================== 选区/多边形屏幕绘制层 ==================== */

  function removeSelectionOverlay() {
    if (selectionOverlay && scene) {
      scene.remove(selectionOverlay);
      selectionOverlay.geometry.dispose();
      (selectionOverlay.material as THREE.Material).dispose();
    }
    selectionOverlay = null;
  }

  /** 绘制选区黄色高亮层（独立 Points，顶点为选中点的世界坐标） */
  function drawSelectionOverlay() {
    if (!scene || !points) return;
    removeSelectionOverlay();
    if (selection.value.length === 0) return;
    const positions = points.geometry.getAttribute('position') as THREE.BufferAttribute;
    const geo = new THREE.BufferGeometry();
    const arr = new Float32Array(selection.value.length * 3);
    selection.value.forEach((index, i) => {
      arr[i * 3] = positions.getX(index) + points.position.x;
      arr[i * 3 + 1] = positions.getY(index) + points.position.y;
      arr[i * 3 + 2] = positions.getZ(index) + points.position.z;
    });
    geo.setAttribute('position', new THREE.BufferAttribute(arr, 3));
    const material = new THREE.PointsMaterial({
      color: 0xffd54f,
      size: pointSize.value * 1.2,
      sizeAttenuation: true,
      transparent: true,
      opacity: 0.5, // 半透明：选完即柔和高亮，避免密集点叠加看起来发白/不透明；点击"加入/擦除"后选区清空 → 仅剩对象着色层（更透明的视觉）
      depthTest: true,
    });
    selectionOverlay = new THREE.Points(geo, material);
    scene.add(selectionOverlay);
  }

  /** 矩形选区实时预览框（SVG 覆盖在 canvas 上，拖拽过程中跟随鼠标） */
  function drawRectOverlay() {
    removeRectOverlay();
    if (!renderer || !rectStart || !rectCurrent) return;
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('class', 'rect-overlay');
    // 必须显式 z-index 提升：canvas 是 WebGL 合成层，某些情况下会盖住无 z-index 的兄弟元素，
    // 导致拖拽预览"看不见、松手后才有结果"的错觉。
    svg.setAttribute('style', 'position:absolute;left:0;top:0;pointer-events:none;z-index:40;');
    const rect = renderer.domElement.getBoundingClientRect();
    svg.setAttribute('width', String(rect.width));
    svg.setAttribute('height', String(rect.height));
    const x = Math.min(rectStart.x, rectCurrent.x);
    const y = Math.min(rectStart.y, rectCurrent.y);
    const w = Math.abs(rectCurrent.x - rectStart.x);
    const h = Math.abs(rectCurrent.y - rectStart.y);
    const el = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
    el.setAttribute('x', String(x));
    el.setAttribute('y', String(y));
    el.setAttribute('width', String(w));
    el.setAttribute('height', String(h));
    el.setAttribute('fill', 'rgba(255,213,79,0.18)');
    el.setAttribute('stroke', '#ffd54f');
    el.setAttribute('stroke-width', '2');
    el.setAttribute('stroke-dasharray', '6,3');
    svg.appendChild(el);
    (renderer.domElement.parentElement as HTMLElement)?.appendChild(svg);
  }

  function removeRectOverlay() {
    if (!renderer) return;
    const parent = renderer.domElement.parentElement as HTMLElement | null;
    parent?.querySelectorAll('svg.rect-overlay').forEach((node) => node.remove());
  }

  /** 多边形描边辅助线（SVG 覆盖在 canvas 上） */
  function drawPolygonOverlay() {
    // 清理旧辅助线
    removePolygonOverlay();
    if (!renderer) return;
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('class', 'polygon-overlay');
    svg.setAttribute('style', 'position:absolute;left:0;top:0;pointer-events:none;z-index:40;');
    const rect = renderer.domElement.getBoundingClientRect();
    svg.setAttribute('width', String(rect.width));
    svg.setAttribute('height', String(rect.height));
    const poly = document.createElementNS('http://www.w3.org/2000/svg', 'polygon');
    poly.setAttribute('points', polygonPoints.map((p) => `${p.x},${p.y}`).join(' '));
    poly.setAttribute('fill', 'rgba(255,213,79,0.18)');
    poly.setAttribute('stroke', '#ffd54f');
    poly.setAttribute('stroke-width', '2');
    svg.appendChild(poly);
    (renderer.domElement.parentElement as HTMLElement)?.appendChild(svg);
  }

  function removePolygonOverlay() {
    if (!renderer) return;
    const parent = renderer.domElement.parentElement as HTMLElement | null;
    parent?.querySelectorAll('svg.polygon-overlay').forEach((node) => node.remove());
  }

  /** 取消进行中的选区交互 */
  function cancelSelectionInteraction() {
    polygonPoints = [];
    removePolygonOverlay();
    removeRectOverlay();
    removeBoxPreview();
    boxAnchor = null;
    document.removeEventListener('mousemove', handleWindowMouseMove);
    document.removeEventListener('mouseup', handleWindowMouseUp);
    rectStart = null;
    rectCurrent = null;
  }

  /** ==================== 点级对象化打标 ==================== */

  /** 新建分割对象（以当前 draft.label 为类别） */
  function createSegObject() {
    if (!draft.label) {
      createMessage.warning('请先在「目标类别」下拉框中选择标签（未配置时请到标签管理添加）');
      return;
    }
    const obj: SegObject = {
      id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
      label: draft.label,
      color: getLabelColor(draft.label),
      indices: [],
    };
    segObjects.value.push(obj);
    setCurrentObject(obj.id);
    createMessage.success(`已新建分割对象「${obj.label}」`);
    console.info('[点云标注] 新建分割对象', { id: obj.id, label: obj.label });
  }

  /** 切换当前激活对象（同时控制各对象区域的显示：只显示当前对象，其他对象区域隐藏） */
  function setCurrentObject(id: string | null) {
    currentObjectId.value = id;
    applySegVisibility();
  }

  /**
   * 统一控制分割对象区域（着色层）的可见性：
   * - 查看模式：全部隐藏（仅显示原始点云）
   * - 编辑模式 + 已选中 3D 框：只显示当前框所属对象的着色层
   * - 编辑模式 + 已选中对象：只显示当前对象的区域（其他对象区域不显示，便于聚焦编辑）
   * - 编辑模式 + 未选中：全部显示（总览）
   * 对象没有悬浮标签（标签只出现在 3D 框上），仅控制着色层。
   */
  function applySegVisibility() {
    const edit = viewMode.value === 'edit';
    const hasCurrent = !!currentObjectId.value;
    segObjects.value.forEach((obj) => {
      const show = edit && (!hasCurrent || obj.id === currentObjectId.value);
      if (obj.overlay) obj.overlay.visible = show;
    });
  }

  /** 把当前选中 3D 框内的点云加入当前分割对象（选区 = 当前选中的 3D 框） */
  function addSelectionToObject() {
    const obj = getCurrentObject();
    if (!obj) {
      createMessage.warning('请先新建或激活一个分割对象');
      return;
    }
    const box = boxes.value[selectedBoxIndex.value];
    if (!box) {
      createMessage.warning('请先选中一个 3D Box（点击框体），再执行「加入对象」');
      return;
    }
    const inside = collectPointsInBoxIndices(box);
    if (inside.length === 0) {
      createMessage.info('该 3D 框内没有点云');
      return;
    }
    // 去重合并
    const existing = new Set(obj.indices);
    const added: number[] = [];
    inside.forEach((index) => {
      if (!existing.has(index)) {
        existing.add(index);
        added.push(index);
      }
    });
    if (added.length === 0) {
      createMessage.info('框内的点已全部属于该对象');
      return;
    }
    const before = [...obj.indices];
    // 用循环追加而非 push(...added)：选区可达数十万点，展开运算符会超出 V8 函数参数上限（~65535）导致栈溢出
    for (let i = 0; i < added.length; i++) {
      obj.indices.push(added[i]);
    }
    renderSegObject(obj);
    createMessage.success(`已向「${obj.label}」加入 ${added.length.toLocaleString()} 点（框内共 ${inside.length.toLocaleString()} 点）`);
    recordHistory(
      `加入选区到「${obj.label}」`,
      () => {
        obj.indices = before;
        renderSegObject(obj);
      },
      () => {
        // 同样避免展开运算符参数上限问题
        obj.indices = [...before];
        for (let i = 0; i < added.length; i++) {
          obj.indices.push(added[i]);
        }
        renderSegObject(obj);
      },
    );
    console.info('[点级标注] 加入选区（3D 框）', { objId: obj.id, 框内点数: inside.length, 新增点数: added.length });
  }

  /** 从当前对象擦除当前选中 3D 框内的点 */
  function eraseSelectionFromObject() {
    const obj = getCurrentObject();
    if (!obj) {
      createMessage.warning('请先新建或激活一个分割对象');
      return;
    }
    const box = boxes.value[selectedBoxIndex.value];
    if (!box) {
      createMessage.warning('请先选中一个 3D Box（点击框体），再执行「擦除」');
      return;
    }
    const target = new Set(collectPointsInBoxIndices(box));
    const before = [...obj.indices];
    const kept = obj.indices.filter((index) => !target.has(index));
    const removedCount = before.length - kept.length;
    if (removedCount === 0) {
      createMessage.info('框内的点不属于该对象');
      return;
    }
    obj.indices = kept;
    renderSegObject(obj);
    createMessage.success(`已从「${obj.label}」擦除 ${removedCount.toLocaleString()} 点`);
    recordHistory(
      `从「${obj.label}」擦除选区`,
      () => {
        obj.indices = before;
        renderSegObject(obj);
      },
      () => {
        obj.indices = kept;
        renderSegObject(obj);
      },
    );
    console.info('[点级标注] 擦除选区（3D 框）', { objId: obj.id, 擦除点数: removedCount });
  }

  /** 获取当前激活对象 */
  function getCurrentObject(): SegObject | null {
    return segObjects.value.find((obj) => obj.id === currentObjectId.value) || null;
  }

  /** 渲染对象着色层（对象不做悬浮标签，标签只出现在 3D 框上） */
  function renderSegObject(obj: SegObject) {
    if (!scene || !points) return;
    // 移除旧 overlay 与标签（清理历史遗留的 sprite，避免场景残留）
    if (obj.overlay) {
      scene.remove(obj.overlay);
      obj.overlay.geometry.dispose();
      (obj.overlay.material as THREE.Material).dispose();
      obj.overlay = undefined;
    }
    if (obj.sprite) {
      scene.remove(obj.sprite.sprite);
      obj.sprite.texture.dispose();
      (obj.sprite.sprite.material as THREE.SpriteMaterial).dispose();
      obj.sprite = undefined;
    }
    if (obj.indices.length === 0) return;
    // 对象着色层：独立 Points（显示对象色，叠加在原始点云之上）
    const positions = points.geometry.getAttribute('position') as THREE.BufferAttribute;
    const geo = new THREE.BufferGeometry();
    const arr = new Float32Array(obj.indices.length * 3);
    obj.indices.forEach((index, i) => {
      arr[i * 3] = positions.getX(index);
      arr[i * 3 + 1] = positions.getY(index);
      arr[i * 3 + 2] = positions.getZ(index);
    });
    geo.setAttribute('position', new THREE.BufferAttribute(arr, 3));
    const material = new THREE.PointsMaterial({
      color: new THREE.Color(obj.color),
      size: pointSize.value * 0.9,
      sizeAttenuation: true,
      transparent: true,
      opacity: 0.95,
      depthWrite: false,
    });
    obj.overlay = markRaw(new THREE.Points(geo, material));
    scene.add(obj.overlay);
    // 渲染后按当前选中态/视图模式统一校正可见性
    applySegVisibility();
  }

  /** 获取分割对象标签（用于 3D Box 列表显示关联对象） */
  function getObjectLabel(objectId: string): string {
    const obj = segObjects.value.find((o) => o.id === objectId);
    return obj ? obj.label : '未知';
  }

  /** 获取分割对象颜色（用于 3D Box 列表显示关联对象标签背景） */
  function getObjectColor(objectId: string): string {
    const obj = segObjects.value.find((o) => o.id === objectId);
    return obj ? obj.color : '#888';
  }

  /** 删除分割对象 */
  function removeSegObject(id: string) {
    const index = segObjects.value.findIndex((obj) => obj.id === id);
    if (index < 0) return;
    const obj = segObjects.value[index];
    if (obj.overlay && scene) {
      scene.remove(obj.overlay);
      obj.overlay.geometry.dispose();
      (obj.overlay.material as THREE.Material).dispose();
    }
    if (obj.sprite && scene) {
      scene.remove(obj.sprite.sprite);
      obj.sprite.texture.dispose();
      (obj.sprite.sprite.material as THREE.SpriteMaterial).dispose();
    }
    segObjects.value.splice(index, 1);
    if (currentObjectId.value === id) currentObjectId.value = null;
    applySegVisibility();
    createMessage.success('已删除分割对象');
    console.info('[点级标注] 删除分割对象', { id, label: obj.label });
  }

  /** 清空所有分割对象（切换文件时调用，undoable=false） */
  function clearAllSegObjects(undoable = true) {
    const before = segObjects.value.map((o) => ({ ...o, overlay: undefined, sprite: undefined }));
    segObjects.value.forEach((obj) => {
      if (obj.overlay && scene) {
        scene.remove(obj.overlay);
        obj.overlay.geometry.dispose();
        (obj.overlay.material as THREE.Material).dispose();
      }
      if (obj.sprite && scene) {
        scene.remove(obj.sprite.sprite);
        obj.sprite.texture.dispose();
        (obj.sprite.sprite.material as THREE.SpriteMaterial).dispose();
      }
    });
    segObjects.value = [];
    currentObjectId.value = null;
    if (undoable) {
      recordHistory(
        '清空分割对象',
        () => {
          segObjects.value = before.map((o) => ({ ...o }));
          segObjects.value.forEach((o) => renderSegObject(o));
        },
        () => {
          segObjects.value.forEach((obj) => {
            if (obj.overlay && scene) scene.remove(obj.overlay);
            if (obj.sprite && scene) scene.remove(obj.sprite.sprite);
          });
          segObjects.value = [];
          currentObjectId.value = null;
        },
      );
    }
  }

  /** ==================== 3D Box 标注 ==================== */

  /**
   * 创建 3D Box：用 Group 包裹 Box3Helper 与 hitProxy 实现真旋转。
   * 关键：Box3Helper 本身不支持 rotation（其顶点由 Box3 计算），
   * 因此把线框与命中体放进 Group，Group 整体绕 Z 旋转 yaw → 显示与导出一致。
   * 委托 createBoxAt（定义在「3D Box 拖拽绘制」一节）以默认尺寸创建。
   */
  function createBox(center: THREE.Vector3) {
    createBoxAt(center, [draft.length, draft.width, draft.height]);
  }

  /** 构建 Box 的 Group（参照 DotCloudTest 示例 drawFinalBox 风格：黄色半透明填充 + 线框 + 命中体）
   *  示例关键参数：opacity: 0.1 + depthTest: false + DoubleSide + renderOrder 高（始终可见，穿透点云） */
  function buildBoxGroup(
    center: THREE.Vector3,
    length: number,
    width: number,
    height: number,
    yaw: number,
    color: string,
  ): THREE.Group {
    const group = new THREE.Group();
    group.position.copy(center);
    group.rotation.z = THREE.MathUtils.degToRad(yaw);
    // 局部坐标包围盒（以原点为中心）
    const half = new THREE.Vector3(length / 2, width / 2, height / 2);
    const box3 = new THREE.Box3(half.clone().negate(), half.clone());
    const helper = new THREE.Box3Helper(box3, new THREE.Color(color));
    // 半透明填充面（参照示例 opacity: 0.1 + depthTest: false，穿透点云始终可见）
    const fillGeo = new THREE.BoxGeometry(length, width, height);
    const fillMat = new THREE.MeshBasicMaterial({
      color: new THREE.Color(color),
      transparent: true,
      opacity: 0.1, // 示例值：极淡填充，仅作视觉提示，不遮挡点云
      depthWrite: false,
      depthTest: false, // 参照示例：始终可见，不被点云遮挡
      side: THREE.DoubleSide,
    });
    const fillMesh = new THREE.Mesh(fillGeo, fillMat);
    fillMesh.renderOrder = 997; // 参照示例 renderOrder
    fillMesh.userData.isFill = true; // 标记用于选中时调整透明度
    // 命中体（透明 Mesh，用于射线选中）
    const proxy = new THREE.Mesh(
      new THREE.BoxGeometry(length, width, height),
      new THREE.MeshBasicMaterial({ transparent: true, opacity: 0, depthWrite: false }),
    );
    proxy.userData.isProxy = true;
    group.add(helper, fillMesh, proxy);
    return group;
  }

  /** 选择 Box（回填表单、高亮） */
  function selectBox(index: number) {
    selectedBoxIndex.value = index;
    const box = boxes.value[index];
    if (!box) return;
    draft.label = box.label;
    [draft.length, draft.width, draft.height] = box.size;
    draft.yaw = box.yaw;
    boxes.value.forEach((item, itemIndex) => {
      item.group.traverse((child) => {
        const lineMat = (child as THREE.LineSegments).material as THREE.LineBasicMaterial | undefined;
        if (lineMat && 'color' in lineMat) {
          // 线框选中态：保持示例的半透明风格（0.6 而非 1.0），避免选中后线框看起来发白发实
          lineMat.opacity = itemIndex === index ? 0.6 : 0.4;
          lineMat.transparent = true;
        }
        // 半透明填充面：选中态适度加深（仍保持示例的"始终半透明"风格，避免不透明遮挡点云）
        if (child.userData.isFill) {
          (child as THREE.Mesh).material.opacity = itemIndex === index ? 0.15 : 0.1;
        }
      });
    });
    updateSelectedBoxPointCount();
    renderThreeViews();
    // 选中框后隐藏对象悬浮标签（框标签已标识类别，避免重复）
    applySegVisibility();
  }

  /** 更新选中 Box（尺寸/yaw/标签变化时重建 Group） */
  function updateSelectedBox() {
    // 捕获操作时的索引，供历史撤销/重做使用（撤销时 selectedBoxIndex.value 可能已变化）
    const editingIndex = selectedBoxIndex.value;
    const box = boxes.value[editingIndex];
    if (!box || !scene) return;
    if (
      box.label === draft.label &&
      box.size[0] === draft.length &&
      box.size[1] === draft.width &&
      box.size[2] === draft.height &&
      box.yaw === draft.yaw
    ) {
      return;
    }
    const before = {
      label: box.label,
      size: [...box.size] as [number, number, number],
      yaw: box.yaw,
      color: box.color,
    };
    const center = new THREE.Vector3(...box.center);
    scene.remove(box.group);
    disposeGroup(box.group);
    box.label = draft.label;
    box.size = [draft.length, draft.width, draft.height];
    box.yaw = draft.yaw;
    box.color = getLabelColor(draft.label);
    box.group = markRaw(buildBoxGroup(center, draft.length, draft.width, draft.height, draft.yaw, box.color));
    scene.add(box.group);
    renderBoxLabel(box, editingIndex + 1);
    updateSelectedBoxPointCount();
    recordHistory(
      '修改 3D Box 属性',
      () => restoreBoxState(editingIndex, before),
      () => restoreBoxState(editingIndex, {
        label: box.label,
        size: [...box.size] as [number, number, number],
        yaw: box.yaw,
        color: box.color,
      }),
    );
    renderThreeViews();
    console.info('[点云标注] 更新 3D Box', { id: box.id, draft });
  }

  /** 恢复 Box 到指定状态（供撤销/重做使用） */
  function restoreBoxState(index: number, state: { label: string; size: [number, number, number]; yaw: number; color: string }) {
    const box = boxes.value[index];
    if (!box || !scene) return;
    scene.remove(box.group);
    disposeGroup(box.group);
    box.label = state.label;
    box.size = [...state.size] as [number, number, number];
    box.yaw = state.yaw;
    box.color = state.color;
    box.group = markRaw(buildBoxGroup(new THREE.Vector3(...box.center), box.size[0], box.size[1], box.size[2], box.yaw, box.color));
    scene.add(box.group);
    renderBoxLabel(box, index + 1);
    updateSelectedBoxPointCount();
    renderThreeViews();
  }

  /** 释放 Group 下所有资源 */
  function disposeGroup(group: THREE.Group) {
    group.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        child.geometry.dispose();
        (child.material as THREE.Material).dispose();
      } else if (child instanceof THREE.LineSegments) {
        child.geometry.dispose();
        (child.material as THREE.Material).dispose();
      }
    });
  }

  /** 统计选中框内点云数（遍历点云，用矩阵逆变换判定局部包围盒） */
  function updateSelectedBoxPointCount() {
    selectedBoxPointCount.value = 0;
    const box = boxes.value[selectedBoxIndex.value];
    if (!box || !points) return;
    const positions = points.geometry.getAttribute('position') as THREE.BufferAttribute;
    const inv = box.group.matrixWorld.clone().invert();
    const local = new THREE.Vector3();
    const half = new THREE.Vector3(box.size[0] / 2, box.size[1] / 2, box.size[2] / 2);
    let count = 0;
    for (let i = 0; i < positions.count; i++) {
      local.set(positions.getX(i), positions.getY(i), positions.getZ(i)).applyMatrix4(inv);
      if (Math.abs(local.x) <= half.x && Math.abs(local.y) <= half.y && Math.abs(local.z) <= half.z) count++;
    }
    selectedBoxPointCount.value = count;
    console.info('[点云标注] 框内点云统计', { boxId: box.id, count });
  }

  /** 删除选中 Box（快捷键/按钮共用） */
  function removeSelectedBox() {
    removeBoxByIndex(selectedBoxIndex.value);
  }

  /** 按索引删除 Box（记录历史） */
  function removeBoxByIndex(index: number) {
    removeBoxByIndexInternal(index, true);
  }

  /** 删除 Box 内部实现（undoable 控制是否记录历史） */
  function removeBoxByIndexInternal(index: number, undoable: boolean) {
    const box = boxes.value[index];
    if (!box || !scene) return;
    scene.remove(box.group);
    disposeGroup(box.group);
    if (box.labelSprite) {
      scene.remove(box.labelSprite.sprite);
      box.labelSprite.texture.dispose();
      (box.labelSprite.sprite.material as THREE.SpriteMaterial).dispose();
    }
    boxes.value.splice(index, 1);
    boxes.value.forEach((remaining, i) => renderBoxLabel(remaining, i + 1));
    selectedBoxIndex.value = -1;
    selectedBoxPointCount.value = 0;
    requestAnimationFrame(() => clearThreeViewCanvases());
    tvDragState = null;
    tvSnapshot = null;
    if (undoable) {
      const annotation = box;
      recordHistory(
        '删除 3D Box',
        () => {
          scene.add(annotation.group);
          const restoreIndex = Math.min(index, boxes.value.length);
          boxes.value.splice(restoreIndex, 0, annotation);
          boxes.value.forEach((b, i) => renderBoxLabel(b, i + 1));
        },
        () => {
          scene.remove(annotation.group);
          disposeGroup(annotation.group);
          const idx = boxes.value.indexOf(annotation);
          if (idx >= 0) boxes.value.splice(idx, 1);
          boxes.value.forEach((b, i) => renderBoxLabel(b, i + 1));
          selectedBoxIndex.value = -1;
        },
      );
    }
    console.info('[点云标注] 删除 3D Box', { index });
  }

  /** 清空所有 Box（切换文件时调用） */
  function clearAnnotations() {
    boxes.value.forEach((box) => {
      scene?.remove(box.group);
      disposeGroup(box.group);
      if (box.labelSprite) {
        scene?.remove(box.labelSprite.sprite);
        box.labelSprite.texture.dispose();
        (box.labelSprite.sprite.material as THREE.SpriteMaterial).dispose();
      }
    });
    boxes.value = [];
    selectedBoxIndex.value = -1;
    selectedBoxPointCount.value = 0;
    // 三视图随选中框清空
    requestAnimationFrame(() => clearThreeViewCanvases());
    tvDragState = null;
    tvSnapshot = null;
    // 复位工具与视角控制（避免在绘制模式下切换文件后 OrbitControls 仍被禁用）
    activeTool.value = 'select';
    if (controls) controls.enabled = true;
    removeBoxPreview();
    boxAnchor = null;
  }

  /** ==================== 着色模式 ==================== */

  /** 应用着色模式：height（按 Z 高度）/ intensity（按强度）/ solid（纯色） */
  function applyColorMode() {
    if (!points) return;
    const material = points.material as THREE.PointsMaterial;
    const position = points.geometry.getAttribute('position') as THREE.BufferAttribute;
    if (colorMode.value === 'solid') {
      points.geometry.deleteAttribute('color');
      material.vertexColors = false;
      material.color.set(0xd7e8ff);
      material.needsUpdate = true;
      return;
    }
    // height / intensity 都需要构建逐顶点颜色
    const colors = new Float32Array(position.count * 3);
    const color = new THREE.Color();
    if (colorMode.value === 'height') {
      let minZ = Infinity, maxZ = -Infinity;
      for (let i = 0; i < position.count; i++) {
        const z = position.getZ(i);
        if (z < minZ) minZ = z;
        if (z > maxZ) maxZ = z;
      }
      const range = maxZ - minZ || 1;
      for (let i = 0; i < position.count; i++) {
        const t = (position.getZ(i) - minZ) / range;
        // 蓝 → 青 → 绿 → 黄 → 红 过渡色带
        color.setHSL(0.66 - t * 0.66, 1.0, 0.55);
        colors[i * 3] = color.r;
        colors[i * 3 + 1] = color.g;
        colors[i * 3 + 2] = color.b;
      }
    } else if (colorMode.value === 'intensity') {
      const intensity = points.geometry.getAttribute('intensity') as THREE.BufferAttribute | undefined;
      if (!intensity) {
        createMessage.warning('该 PCD 无强度数据，已回退高度着色');
        colorMode.value = 'height';
        applyColorMode();
        return;
      }
      let minV = Infinity, maxV = -Infinity;
      for (let i = 0; i < intensity.count; i++) {
        const v = intensity.getX(i);
        if (v < minV) minV = v;
        if (v > maxV) maxV = v;
      }
      const range = maxV - minV || 1;
      for (let i = 0; i < intensity.count; i++) {
        const t = (intensity.getX(i) - minV) / range;
        color.setHSL(0.66 - t * 0.66, 1.0, 0.55);
        colors[i * 3] = color.r;
        colors[i * 3 + 1] = color.g;
        colors[i * 3 + 2] = color.b;
      }
    }
    points.geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));
    material.vertexColors = true;
    material.needsUpdate = true;
    console.info('[点云标注] 着色模式应用', { mode: colorMode.value });
  }

  /** ==================== 帧工作流 ==================== */

  /** 跳转到最近一个未标注帧（标注状态判定 pcdMetadata.annotations 非空） */
  function jumpToUnannotated() {
    if (unannotatedCount.value === 0) {
      createMessage.success('全部帧均已标注');
      return;
    }
    // 在完整列表（files）中查找：与 currentFileIndex 的索引口径保持一致
    const list = files.value;
    const start = Math.max(0, currentFileIndex.value);
    for (let i = start + 1; i < list.length; i++) {
      if (!list[i].annotated) {
        stopPlay();
        selectFile(list[i]);
        return;
      }
    }
    for (let i = 0; i <= start; i++) {
      if (!list[i].annotated) {
        stopPlay();
        selectFile(list[i]);
        return;
      }
    }
  }

  /** ==================== 编辑/查看模式 ==================== */

  function toggleViewMode() {
    viewMode.value = viewMode.value === 'edit' ? 'view' : 'edit';
    // 查看模式下隐藏 3D 框；分割对象区域统一由 applySegVisibility 控制（编辑+选中只显示当前，否则全部显示）
    boxes.value.forEach((box) => (box.group.visible = viewMode.value === 'edit'));
    applySegVisibility();
    removeSelectionOverlay();
    removePolygonOverlay();
    removeRectOverlay();
    createMessage.info(viewMode.value === 'view' ? '查看模式：标注已隐藏' : '编辑模式');
    console.info('[点云标注] 视图模式切换', { viewMode: viewMode.value });
  }

  /** ==================== 撤销 / 重做 ==================== */

  /** 记录一条历史（命令模式：撤回到过去、重做到未来） */
  function recordHistory(description: string, undo: () => void, redo: () => void) {
    // 丢弃当前索引之后的分支（新操作使重做栈失效）
    historyStack.value = historyStack.value.slice(0, historyIndex.value + 1);
    historyStack.value.push({ description, undo, redo });
    historyIndex.value = historyStack.value.length - 1;
    console.info('[点云标注] 记录历史', { description, 深度: historyIndex.value + 1 });
  }

  function undo() {
    if (!canUndo.value) return;
    const entry = historyStack.value[historyIndex.value];
    historyIndex.value--;
    entry.undo();
    createMessage.info(`已撤销：${entry.description}`);
    console.info('[点云标注] 撤销', { description: entry.description, 索引: historyIndex.value + 1 });
  }

  function redo() {
    if (!canRedo.value) return;
    historyIndex.value++;
    const entry = historyStack.value[historyIndex.value];
    entry.redo();
    createMessage.info(`已重做：${entry.description}`);
    console.info('[点云标注] 重做', { description: entry.description, 索引: historyIndex.value + 1 });
  }

  /** ==================== 快捷键 ==================== */

  function handleKeydown(event: KeyboardEvent) {
    // 输入框内不响应全局快捷键（对齐图片标注页约定）
    const target = event.target as HTMLElement;
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) return;

    // 撤销/重做：Ctrl+Z / Ctrl+Y
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'z' && !event.shiftKey) {
      event.preventDefault();
      undo();
      return;
    }
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'y') {
      event.preventDefault();
      redo();
      return;
    }

    // 数字键 1-9：快速选择标签
    if (/^[1-9]$/.test(event.key)) {
      const index = parseInt(event.key, 10) - 1;
      if (labelOptions.value[index]) {
        draft.label = labelOptions.value[index].value;
        createMessage.info(`已选择标签：${draft.label}`);
      }
      return;
    }

    // 视图切换
    if (event.key.toLowerCase() === 'q') {
      toggleViewMode();
      return;
    }

    // 帧导航（←/→ 或 ↑/↓）
    if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      event.preventDefault();
      prevFrame();
      return;
    }
    if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      event.preventDefault();
      nextFrame();
      return;
    }

    // 空格：播放/暂停
    if (event.key === ' ') {
      event.preventDefault();
      togglePlay();
      return;
    }

    // 删除：优先删除选中 Box，其次当前分割对象
    if (event.key === 'Delete' || event.key === 'Backspace') {
      event.preventDefault();
      if (selectedBoxIndex.value >= 0) removeSelectedBox();
      else if (currentObjectId.value) removeSegObject(currentObjectId.value);
      return;
    }

    // Esc：取消选区 → 取消 Cuboid → 返回选择工具
    if (event.key === 'Escape') {
      if (selection.value.length > 0) {
        clearSelection();
      } else if (activeTool.value !== 'select') {
        // 统一走 setTool：同步恢复 OrbitControls 视角控制
        setTool('select');
      }
      return;
    }
  }

  /** ==================== 标注 UI 辅助 ==================== */

  /** Box 序号悬浮标签 */
  function renderBoxLabel(box: BoxAnnotation, index: number) {
    if (!scene) return;
    if (box.labelSprite) {
      scene.remove(box.labelSprite.sprite);
      box.labelSprite.texture.dispose();
      (box.labelSprite.sprite.material as THREE.SpriteMaterial).dispose();
      box.labelSprite = undefined;
    }
    const center = new THREE.Vector3(...box.center);
    const topOffset = Math.max(box.size[2], 0.5) + 1;
    const label = createLabelSprite(`#${index} · ${box.label}`, box.color, false);
    label.sprite.position.set(center.x, center.y + topOffset, center.z);
    scene.add(label.sprite);
    box.labelSprite = markRaw(label);
  }

  /** 创建 Sprite 标签（canvas 纹理）
   *  关键：canvas 宽高比必须严格等于 sprite.scale 宽高比，否则纹理被拉伸变形
   *  文字看不清的根本原因：之前 canvas=320×72（4.44:1），scale=(2.4, 0.6)（4:1）→ 文本纵向被拉伸 1.11 倍 */
  function createLabelSprite(text: string, color: string, isObject: boolean) {
    // Box 标签 / 分割对象标签共用一张纹理：宽度按文字长度自动撑开，高度固定 72
    const canvas = document.createElement('canvas');
    const FONT_PX = 30;
    const PAD_X = 14;
    canvas.width = 320; // 基础宽度（足够容纳 #99 · XXXXX 之类）
    canvas.height = 72;
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.fillStyle = 'rgba(11,17,32,0.85)';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.strokeStyle = color;
      ctx.lineWidth = 4;
      ctx.strokeRect(2, 2, canvas.width - 4, canvas.height - 4);
      ctx.fillStyle = isObject ? color : '#ffffff';
      ctx.font = `bold ${FONT_PX}px sans-serif`;
      ctx.textBaseline = 'middle';
      // 用 measureText 自动判断文字宽度，按需扩展 canvas（保持 4:1 比例以便与 sprite.scale 一致）
      const metrics = ctx.measureText(text);
      const neededW = Math.ceil(metrics.width) + PAD_X * 2 + 8; // +8 给描边
      const desiredH = Math.round(neededW / (320 / 72)); // 保持原始 320:72 比例
      if (neededW > canvas.width) {
        canvas.width = neededW;
        canvas.height = desiredH;
        // 重画背景与边框（清空后重画）
        ctx.fillStyle = 'rgba(11,17,32,0.85)';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.strokeStyle = color;
        ctx.lineWidth = 4;
        ctx.strokeRect(2, 2, canvas.width - 4, canvas.height - 4);
        ctx.fillStyle = isObject ? color : '#ffffff';
        ctx.font = `bold ${FONT_PX}px sans-serif`;
        ctx.textBaseline = 'middle';
        ctx.fillText(text, PAD_X, canvas.height / 2);
      } else {
        ctx.fillText(text, PAD_X, canvas.height / 2);
      }
    }
    const texture = new THREE.CanvasTexture(canvas);
    const material = new THREE.SpriteMaterial({ map: texture, depthTest: false });
    const sprite = new THREE.Sprite(material);
    // scale 严格按 canvas 宽高比设置（4:1 即 320:80，避免文字被拉伸）
    // 对象标签不宜过大：之前 7 世界单位宽对几米大小的目标遮挡严重，调小为 3.2
    const baseW = isObject ? 3.2 : 2.4;
    sprite.scale.set(baseW, baseW * (canvas.height / canvas.width), 1);
    sprite.renderOrder = 999;
    return { sprite, texture };
  }

  /** 统计单个框内点云数（导出时对每个 box 都计算；复用选中框统计的矩阵逆变换逻辑） */
  function countPointsInBox(box: BoxAnnotation): number {
    if (!points) return 0;
    const positions = points.geometry.getAttribute('position') as THREE.BufferAttribute;
    // 导出前确保 matrixWorld 已同步（否则可能读到旧矩阵导致点数错误）
    box.group.updateMatrixWorld(true);
    const inv = box.group.matrixWorld.clone().invert();
    const local = new THREE.Vector3();
    const half = new THREE.Vector3(box.size[0] / 2, box.size[1] / 2, box.size[2] / 2);
    let count = 0;
    for (let i = 0; i < positions.count; i++) {
      local.set(positions.getX(i), positions.getY(i), positions.getZ(i)).applyMatrix4(inv);
      if (Math.abs(local.x) <= half.x && Math.abs(local.y) <= half.y && Math.abs(local.z) <= half.z) count++;
    }
    return count;
  }

  /** 收集单个框内的点云索引（画 3D Box 后自动归入当前分割对象，替代手动"矩形选区→加入对象"） */
  function collectPointsInBoxIndices(box: BoxAnnotation): number[] {
    if (!points) return [];
    const positions = points.geometry.getAttribute('position') as THREE.BufferAttribute;
    box.group.updateMatrixWorld(true);
    const inv = box.group.matrixWorld.clone().invert();
    const local = new THREE.Vector3();
    const half = new THREE.Vector3(box.size[0] / 2, box.size[1] / 2, box.size[2] / 2);
    const indices: number[] = [];
    for (let i = 0; i < positions.count; i++) {
      local.set(positions.getX(i), positions.getY(i), positions.getZ(i)).applyMatrix4(inv);
      if (Math.abs(local.x) <= half.x && Math.abs(local.y) <= half.y && Math.abs(local.z) <= half.z) indices.push(i);
    }
    return indices;
  }

  /** 导出下拉菜单点击：format 为 Menu 的 key（platform / kitti / nuscenes） */
  function handleExportClick({ key }: { key: string }) {
    exportAnnotations(key as ExportFormat);
  }

  /**
   * 导出标注（支持 平台内部 JSON / KITTI / nuScenes 三种格式）。
   * 存储模型见 pcAnnotationFormats.ts：平台内部以自设计 JSON 存储，
   * 面向自动驾驶主流的 KITTI / nuScenes 在导出时做格式转换。
   */
  function exportAnnotations(format: ExportFormat = 'platform') {
    const file = currentFile.value;
    if (!file) {
      createMessage.warning('请先选择 PCD 文件');
      return;
    }
    // KITTI / nuScenes 为 3D 框格式，必须要有 3D Box 才能导出；
    // 平台内部 JSON 只要有任一标注（3D Box 或点级分割对象）即可导出
    if (format !== 'platform' && boxes.value.length === 0) {
      createMessage.warning('KITTI / nuScenes 导出需要至少一个 3D Box 标注');
      return;
    }
    if (format === 'platform' && boxes.value.length === 0 && segObjects.value.length === 0) {
      createMessage.warning('当前没有任何标注可导出');
      return;
    }
    // 收集统一存储模型所需字段（框内点云数逐框计算，供 num_points 使用）
    const source: ExportAnnotationSource = {
      frameId: file.id ?? file.name,
      meta: {
        datasetId,
        fileName: file.name,
        pointCount: pointCount.value,
        exportedAt: new Date().toISOString(),
      },
      objects: boxes.value.map((box) => ({
        id: box.id,
        label: box.label,
        center: [...box.center] as [number, number, number],
        size: [...box.size] as [number, number, number],
        yawDeg: box.yaw,
        numPoints: countPointsInBox(box),
      })),
      segments: segObjects.value.map((obj) => ({
        id: obj.id,
        label: obj.label,
        color: obj.color,
        numPoints: obj.indices.length,
        indices: obj.indices,
      })),
    };
    const { content, mime, fileName } = buildExportContent(format, source, file.name);
    const blob = new Blob([content], { type: mime });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    // 前端标记为已标注（刷新后以后端 pcdMetadata.annotations 为准）
    file.annotated = true;
    const formatLabel = format === 'kitti' ? 'KITTI' : format === 'nuscenes' ? 'nuScenes' : '平台 JSON';
    createMessage.success(`已导出 ${formatLabel} 标注（${boxes.value.length} 个 3D Box）`);
    console.info('[点云标注] 导出标注', {
      format,
      fileName: file.name,
      boxes: boxes.value.length,
      segments: segObjects.value.length,
    });
  }

  /** 保留后端保存渠道（供后续接入真实保存时使用，当前以导出 JSON 为主） */
  async function saveAnnotations(datasetId: number, fileId: number, payload: { datasetId: number; fileId: number; fileName: string; boxes: unknown[] }) {
    return savePcDatasetAnnotations(datasetId, fileId, payload);
  }

  /** ==================== 场景资源清理与辅助 ==================== */

  function clearSceneObjects() {
    clearAnnotations();
    clearAllSegObjects(false);
    if (scene && points) {
      scene.remove(points);
      points.geometry.dispose();
      (points.material as THREE.Material).dispose();
      points = undefined;
      pointCloudReady.value = false;
    }
    removeSelectionOverlay();
    removePolygonOverlay();
  }

  function resizeScene() {
    const container = renderRef.value;
    if (!container || !renderer || !camera) return;
    camera.aspect = container.clientWidth / container.clientHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(container.clientWidth, container.clientHeight);
  }

  function animate() {
    animationId = requestAnimationFrame(animate);
    controls?.update();
    if (scene && camera && renderer) renderer.render(scene, camera);
  }

  function formatCount(value?: number) {
    return Number(value || 0).toLocaleString();
  }

  function formatBoxSize(box: BoxAnnotation) {
    return box.size.map((value) => value.toFixed(1)).join('×');
  }

  function resetView() {
    if (points) fitCameraToPoints(points);
  }

  function goBack() {
    router.go(-1);
  }

  /** ==================== 监听 ==================== */

  // 点大小实时联动（修复：原 @change 只在拖拽结束时更新）
  watch(pointSize, (value) => {
    if (points) (points.material as THREE.PointsMaterial).size = value;
    if (selectionOverlay) (selectionOverlay.material as THREE.PointsMaterial).size = value * 1.2;
    segObjects.value.forEach((obj) => {
      if (obj.overlay) (obj.overlay.material as THREE.PointsMaterial).size = value * 0.9;
    });
  });

  // Box 参数变化（表单编辑）→ 更新选中框
  watch(
    () => [draft.label, draft.length, draft.width, draft.height, draft.yaw],
    () => updateSelectedBox(),
  );

  // 着色模式切换 → 立即应用
  watch(colorMode, () => applyColorMode());
</script>

<style scoped lang="less">
  /* 样式与图片标注页（annotate/*）保持一致的深色主题：
     - 内容区背景 #242e49 / 面板背景 #181d31（参考页实测值）
     - 工具栏 48px 高度 + 底部阴影
     - 左栏 160px、右栏 294px（参考页栏宽） */
  .pc-annotate-page {
    --panel-bg: #181d31;
    --panel-border: #2a3850;
    --page-bg: #242e49;
    --muted: #8fa3bf;
    height: calc(100vh - 48px);
    min-height: 680px;
    display: flex;
    flex-direction: column;
    color: #c9d1d9;
    background: var(--page-bg);
    outline: none;
  }

  .annotate-header {
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 18px;
    background: var(--panel-bg);
    border-bottom: 1px solid var(--panel-border);
    box-shadow: rgba(0, 21, 41, 0.4) 0px 1px 4px 0px;
  }

  .header-title,
  .header-actions,
  .toolbar-group,
  .number-row {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  /* 框尺寸（长/宽/高）竖排：一行一个，避免右栏被挤出屏幕 */
  .size-input-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }
  .size-input-row:last-child { margin-bottom: 0; }
  .size-input-label {
    flex: 0 0 24px;
    color: var(--muted);
    font-size: 12px;
  }
  .size-input {
    flex: 1 1 0;
    min-width: 0;
    width: 100% !important;
  }
  /* 确保 ant-design-vue InputNumber 根元素也撑满行宽（默认宽度由内容决定，会导致三行不一致） */
  .size-input :deep(.ant-input-number),
  .size-input.ant-input-number {
    width: 100% !important;
  }

  .back-button { color: #d9e6f7; }
  .title { font-size: 17px; font-weight: 600; color: #eef2f7; }
  .subtitle { margin-top: 2px; font-size: 12px; color: var(--muted); }

  .annotate-main {
    flex: 1;
    min-height: 0;
    display: grid;
    /* 中工作区自适应 / 右 294px 设置栏 */
    grid-template-columns: minmax(520px, 1fr) 294px;
    /* 单行固定高度（minmax(0,1fr) 允许子项收缩，防止内容把行撑高挤出屏幕） */
    grid-template-rows: minmax(0, 1fr);
    gap: 1px;
    background: var(--panel-border);
  }

  .panel { min-width: 0; min-height: 0; background: var(--panel-bg); }
  .setting-panel { padding: 10px; overflow: hidden; }
  /* 右栏内容较高（表单 + 分割对象 + 3D Box + 三视图），整栏纵向滚动，避免被挤出屏幕 */
  .setting-panel { overflow-y: auto; overflow-x: hidden; }
  .panel-title { margin-bottom: 10px; font-size: 13px; font-weight: 600; color: #e8eef8; }

  /* ============ 底部帧播放器（时序帧切换，视频播放器式）============ */
  .frame-player {
    height: 62px;
    min-height: 62px;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 0 14px;
    background: var(--panel-bg);
    border-top: 1px solid var(--panel-border);
    box-shadow: rgba(0, 21, 41, 0.4) 0px -1px 4px 0px;
    z-index: 2;
  }
  .fp-controls {
    display: flex;
    align-items: center;
    gap: 4px;
    flex: none;
  }
  .fp-progress {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 200px;
  }
  .fp-slider { flex: 1; margin: 0; }
  .fp-frame-label {
    flex: none;
    min-width: 72px;
    text-align: center;
    font-size: 12px;
    font-variant-numeric: tabular-nums;
    color: #e8eef8;
    background: #2a2d3e;
    border: 1px solid #3a3d4e;
    border-radius: 3px;
    padding: 2px 6px;
  }
  .fp-info {
    display: flex;
    align-items: center;
    gap: 10px;
    flex: none;
    min-width: 220px;
  }
  .fp-filename {
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 12px;
    color: #c9d1d9;
  }
  .fp-stat {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: var(--muted);
    white-space: nowrap;
  }
  .fp-dot-green {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #4caf50;
    box-shadow: 0 0 4px #4caf50;
    flex: none;
  }
  .fp-dot-gray {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #5a6a84;
    flex: none;
  }
  .fp-speed {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: none;
  }
  .fp-speed-label { font-size: 12px; color: var(--muted); white-space: nowrap; }
  .fp-speed-select { width: 92px; }

  /* 帧点阵：全部帧状态条（绿=已标 灰=未标 蓝=当前），点击跳转 */
  .frame-dots {
    height: 12px;
    display: flex;
    gap: 2px;
    padding: 0 14px 4px;
    background: var(--panel-bg);
    border-top: 1px solid rgba(42, 56, 80, 0.6);
    overflow-x: auto;
    overflow-y: hidden;
  }
  .frame-dot {
    flex: none;
    min-width: 6px;
    height: 6px;
    border-radius: 1px;
    background: #3d4a63;
    cursor: pointer;
    transition: background 0.15s, transform 0.15s;
    margin-top: 3px;
  }
  .frame-dot:hover { transform: scaleY(1.4); background: #6b7fa3; }
  .frame-dot.annotated { background: #2e7d32; }
  .frame-dot.active {
    background: #4fc3f7;
    box-shadow: 0 0 4px #4fc3f7;
    transform: scaleY(1.4);
  }

  .box-list { margin-top: 10px; max-height: 220px; overflow-y: auto; }
  .box-item {
    display: flex;
    align-items: center;
    gap: 6px;
    width: 100%;
    padding: 5px 8px;
    margin-bottom: 4px;
    background: #141a2e;
    border: 1px solid #2a3850;
    border-radius: 4px;
    color: #cdd9e8;
    cursor: pointer;
    transition: all 0.15s;
    text-align: left;
  }
  .box-item:hover { border-color: #4fc3f7; background: rgba(79, 195, 247, 0.06); }
  .box-item.active {
    border-color: #4fc3f7;
    background: rgba(79, 195, 247, 0.14);
    box-shadow: 0 0 6px rgba(79, 195, 247, 0.35) inset;
  }
  .box-item.active .box-label { color: #4fc3f7; font-weight: 600; }
  .box-color { width: 12px; height: 12px; border-radius: 3px; flex: none; }
  .box-label { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .box-size { color: var(--muted); font-size: 11px; flex: none; }

  /* 当前操作对象指示条 */
  .current-object-bar {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 5px 8px;
    margin-bottom: 8px;
    background: rgba(79, 195, 247, 0.10);
    border: 1px solid #2a3850;
    border-left: 3px solid #4fc3f7;
    border-radius: 4px;
    font-size: 12px;
  }
  .current-object-bar.co-empty { border-left-color: #ff7875; color: var(--muted); background: rgba(255, 120, 117, 0.06); }
  .co-dot { width: 12px; height: 12px; border-radius: 3px; flex: none; }
  .co-label { flex: 1; color: #e8eef8; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .co-count { color: var(--muted); font-size: 11px; flex: none; }

  .box-object-tag {
    font-size: 10px;
    padding: 0 4px;
    border-radius: 2px;
    color: #fff;
    margin-left: 4px;
    flex: none;
  }

  .workspace { display: flex; flex-direction: column; }
  .workspace-toolbar {
    height: 48px;
    min-height: 48px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding: 0 12px;
    background: var(--panel-bg);
    border-bottom: 1px solid var(--panel-border);
    box-shadow: rgba(0, 21, 41, 0.4) 0px 1px 4px 0px;
    z-index: 1;
    flex-wrap: wrap;
  }
  .toolbar-right {
    display: flex;
    align-items: center;
    justify-content: flex-end; /* 右侧设置区内容右对齐（工具按钮在左，设置区贴右） */
    gap: 8px;
    flex: 1;
    min-width: 300px;
  }
  .toolbar-label { font-size: 12px; color: var(--muted); white-space: nowrap; }
  .point-size-slider { flex: 1; max-width: 160px; margin: 0 4px; }
  .color-mode-select { width: 96px; }
  /* 导出格式下拉：主文案 + 灰色说明 */
  .export-menu-desc { color: #8a93a6; font-size: 11px; margin-left: 6px; }
  .workspace-toolbar :deep(.ant-dropdown-trigger) { display: inline-flex; align-items: center; }

  .render-container { position: relative; flex: 1; min-height: 0; overflow: hidden; }
  .render-container :deep(.ant-spin-nested-loading),
  .render-container :deep(.ant-spin-container),
  .render-fill { width: 100%; height: 100%; }

  .render-stats {
    position: absolute;
    left: 12px;
    bottom: 12px;
    display: flex;
    gap: 14px;
    padding: 5px 10px;
    font-size: 12px;
    color: #8fa3bf;
    background: rgba(24, 29, 49, 0.82);
    border: 1px solid var(--panel-border);
    border-radius: 4px;
    pointer-events: none;
    z-index: 10;
  }

  .load-error {
    position: absolute;
    top: 12px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 14px;
    color: #ff7875;
    background: rgba(60, 20, 20, 0.9);
    border: 1px solid #a61d24;
    border-radius: 4px;
    z-index: 20;
  }

  /* 分割对象图例（左上角悬浮，仿图片页悬浮面板半透明风格） */
  .legend-panel {
    position: absolute;
    left: 12px;
    top: 12px;
    min-width: 200px;
    max-width: 260px;
    padding: 8px 10px;
    background: rgba(24, 29, 49, 0.92);
    border: 1px solid var(--panel-border);
    border-radius: 6px;
    font-size: 12px;
    z-index: 50;
    backdrop-filter: blur(4px);
  }
  .legend-title { font-weight: 600; color: #4fc3f7; margin-bottom: 6px; font-size: 12px; }
  .legend-item { display: flex; align-items: center; gap: 6px; padding: 2px 0; cursor: pointer; }
  .legend-item:hover { background: rgba(79, 195, 247, 0.12); }
  .legend-dot { width: 10px; height: 10px; border-radius: 2px; flex: none; }
  .legend-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .legend-name.active { color: #4fc3f7; font-weight: 600; }
  .legend-count { color: var(--muted); font-size: 11px; }

  .label-select-row { display: flex; gap: 8px; align-items: center; }
  .label-select-row :deep(.ant-select) { flex: 1; }

  .seg-object-list .item-delete { color: #ff7875; font-size: 13px; flex: none; }
  .item-delete:hover { color: #ff4d4f; }

  .box-inline-info {
    margin: -8px 0 8px;
    font-size: 12px;
    color: #ffd666;
  }

  .help-text { font-size: 12px; line-height: 1.9; color: var(--muted); }
  .help-text p { margin-bottom: 2px; }

  /* 三视图：tab 切换 + 单 canvas 显示（右栏，选中 3D 框时显示） */
  .three-view-wrap { display: flex; flex-direction: column; gap: 6px; }
  .tv-tabs {
    display: flex;
    gap: 2px;
    background: #0b1120;
    border: 1px solid var(--panel-border);
    border-radius: 6px;
    padding: 2px;
  }
  .tv-tab {
    flex: 1;
    padding: 4px 6px;
    background: transparent;
    border: none;
    border-radius: 4px;
    color: var(--muted);
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.15s, color 0.15s;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 3px;
  }
  .tv-tab:hover { background: rgba(79, 195, 247, 0.08); color: #c9d1d9; }
  .tv-tab.active {
    background: rgba(79, 195, 247, 0.18);
    color: #4fc3f7;
  }
  .tv-tab-icon {
    display: inline-block;
    width: 14px;
    height: 14px;
    line-height: 14px;
    text-align: center;
    font-size: 11px;
    font-weight: 700;
    border: 1px solid currentColor;
    border-radius: 3px;
    opacity: 0.85;
  }
  .tv-canvas-area { position: relative; }
  .tv-item {
    background: #0f1527;
    border: 1px solid var(--panel-border);
    border-radius: 6px;
    padding: 6px;
  }
  .tv-label {
    font-size: 11px;
    color: var(--muted);
    margin-bottom: 4px;
    user-select: none;
  }
  .tv-canvas {
    display: block;
    width: 100%;
    height: auto;
    border-radius: 4px;
    cursor: crosshair;
    touch-action: none;
  }
  input, textarea { font-family: inherit; }
</style>