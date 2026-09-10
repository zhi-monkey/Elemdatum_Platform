<template>
  <a-modal
    :visible="visible"
    title="自定义分配比例"
    width="70%"
    @ok="handleOk"
    @cancel="close"
    :maskClosable="false"
    :okButtonProps="{ disabled: isDisabled }"
    wrapClassName="dark-theme-modal"
    :bodyStyle="{ padding: '0', background: '#1f1f1f', color: '#e0e0e0' }"
  >
    <div class="visual-proportion-container" v-if="!isDisabled">
      <!-- 1. 核心操作区：任务分配进度条 -->
      <div class="allocation-section">
        <h3 class="section-title">拖拽分配区域</h3>
        <div class="progress-wrapper">
          <div class="progress-container">
            <!-- 顶部刻度标签 -->
            <div class="offset-labels-row">
              <div class="offset-label-item start">0</div>

              <div
                v-for="(item, index) in memberList"
                :key="item.id + '-offset'"
                class="offset-label-item dynamic"
                v-show="index < memberList.length - 1 && getImageCount(item) > 0"
                :style="{ left: getImagePosition(item.endOffset + 1) + '%' }"
              >
                <div class="offset-line"></div>
                <div class="offset-value">{{ item.endOffset + 1 }}</div>
              </div>

              <div class="offset-label-item end">{{ totalImages }}</div>
            </div>

            <!-- 主进度条 -->
            <div class="progress-bar-wrapper">
              <div class="progress-bar" ref="progressBarRef">
                <!-- 未分配区域 -->
                <div
                  v-if="memberList.length > 0 && getActualStartOffset() > 0"
                  class="progress-segment unassigned-segment"
                  :style="{ width: (getActualStartOffset() / props.totalImages) * 100 + '%' }"
                >
                  <div class="segment-info">
                    <span class="segment-icon">⚠️</span>
                    <span class="segment-count">未分配 {{ getActualStartOffset() }}</span>
                  </div>
                </div>

                <!-- 成员分配区域 -->
                <div
                  v-for="(item, index) in memberList"
                  :key="item.id"
                  class="progress-segment member-segment"
                  :class="{ 'zero-width': getImageCount(item) === 0 }"
                  :style="{
                    width: getPercentage(item) + '%',
                    backgroundColor: item.color,
                  }"
                >
                  <div v-if="getImageCount(item) > 0" class="segment-content">
                    <div class="segment-name">{{ item.username }}</div>
                    <div class="segment-sub"
                      >{{ getImageCount(item) }}张 ({{ getPercentage(item).toFixed(1) }}%)</div
                    >
                  </div>
                </div>

                <!-- 拖拽手柄 (左边界) -->
                <div
                  v-if="memberList.length > 0"
                  class="resize-handle left-boundary-handle"
                  :class="{ 'is-resizing': resizing && resizeIndex === -1 }"
                  :style="{ left: getLeftBoundaryPosition() }"
                  @mousedown="startResizeLeftBoundary($event)"
                >
                  <div class="handle-line"></div>
                  <div class="handle-tag" @mousedown="startResizeLeftBoundary($event)">起点</div>
                </div>

                <!-- 拖拽手柄 (成员间) -->
                <div
                  v-for="(item, index) in memberList.slice(0, -1)"
                  :key="'handle-' + item.id"
                  class="resize-handle"
                  :class="{ 'is-resizing': resizing && resizeIndex === index }"
                  :style="{ left: getHandlePosition(index) }"
                  @mousedown="startResize($event, index)"
                >
                  <div class="handle-line"></div>
                  <div
                    class="handle-label"
                    :class="{
                      overlapped: getHandleOverlapTotal(index) > 1,
                      active: resizing && resizeIndex === index,
                    }"
                    :style="{
                      top: getHandleLabelTop(index),
                      borderColor: item.color,
                    }"
                    :title="getBoundaryFullLabel(index)"
                    @mousedown="startResize($event, index)"
                  >
                    {{ getBoundaryShortLabel(index) }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 2. 辅助参考：已标注/未标注状态条 (下移) -->
      <div class="status-bar-wrapper">
        <div class="status-info-header">
          <span>数据集总览</span>
          <span class="sub-text">用于参考已标注数据的位置分布</span>
        </div>

        <!-- 下面的条现在会占满 100% 宽度，没有 padding 挤压 -->
        <div class="mini-status-bar">
          <div
            class="status-segment annotated"
            :style="{ width: getAnnotatedPercentage() + '%' }"
            :title="`已标注: ${props.annotatedCount}张`"
          ></div>
          <div
            class="status-segment unannotated"
            :style="{ width: 100 - getAnnotatedPercentage() + '%' }"
            :title="`未标注: ${totalImages - props.annotatedCount}张`"
          ></div>
        </div>

        <div class="status-legend">
          <div class="legend-item"
            ><span class="dot annotated"></span> 已标注 ({{ props.annotatedCount }})</div
          >
          <div class="legend-item"
            ><span class="dot unannotated"></span> 未标注 ({{
              totalImages - props.annotatedCount
            }})</div
          >
        </div>
      </div>

      <!-- 3. 详情列表 -->
      <div class="member-summary">
        <div class="summary-header">
          <div class="summary-title">分配详情列表</div>
          <a-button type="link" size="small" @click="resetToEqual" class="reset-button">
            重置为均分
          </a-button>
        </div>
        <div class="summary-grid">
          <div v-for="item in memberList" :key="item.id" class="summary-card">
            <div class="card-left">
              <div class="color-dot" :style="{ backgroundColor: item.color }"></div>
              <span class="member-name">{{ item.username }}</span>
            </div>
            <div class="card-right">
              <template v-if="getImageCount(item) > 0">
                <div class="range-tag">{{ item.startOffset + 1 }} ~ {{ item.endOffset + 1 }}</div>
                <div class="count-text">{{ getImageCount(item) }} 张</div>
              </template>
              <template v-else>
                <span class="empty-text">未分配</span>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 禁用提示 -->
    <div v-else class="disabled-notice">
      <a-alert :message="disabledReason" type="warning" show-icon banner />
    </div>
  </a-modal>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue';
  import { Alert as AAlert, Button as AButton, Modal as AModal } from 'ant-design-vue';

  const props = defineProps({
    visible: Boolean,
    members: {
      type: Array,
      default: () => [],
    },
    totalImages: {
      type: Number,
      default: 0,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    disabledReason: {
      type: String,
      default: '',
    },
    annotatedCount: {
      type: Number,
      default: 0,
    },
  });

  const emit = defineEmits(['update:visible', 'submit']);

  const memberList = ref<any[]>([]);
  const progressBarRef = ref<HTMLElement>();

  // 拖拽相关状态
  const resizing = ref(false);
  const resizeIndex = ref(-1);
  const startMouseX = ref(0);
  const initialEndOffset = ref(0);
  const originalStartOffset = ref(0); // 记录初始化时的起始位置，作为右侧拖动的上限

  const PRESET_COLORS = [
    '#177ddc', // 科技蓝
    '#d89614', // 琥珀金
    '#49aa19', // 极光绿
    '#d32029', // 警戒红
    '#13a8a8', // 青松色
    '#642ab5', // 酱紫色
    '#e84749', // 珊瑚红
    '#d84a1b', // 烈焰橙
    '#8a58bc', // 浅藤紫
    '#2b4acb', // 深海蓝
  ];

  // 是否禁用
  const isDisabled = computed(() => {
    return (
      props.disabled ||
      (props.totalImages > 0 &&
        props.members.length > 0 &&
        props.totalImages < props.members.length)
    );
  });

  // 计算已标注百分比
  function getAnnotatedPercentage() {
    if (props.totalImages === 0) return 0;
    return (props.annotatedCount / props.totalImages) * 100;
  }

  // 生成随机颜色
  function generateRandomColor() {
    const hue = Math.floor(Math.random() * 360);
    const saturation = 65 + Math.floor(Math.random() * 20);
    const lightness = 50 + Math.floor(Math.random() * 15);
    return `hsl(${hue}, ${saturation}%, ${lightness}%, 0.4)`;
  }

  // 为每个成员生成唯一的颜色
  function assignColors(list: any[]) {
    return list.map((item, index) => {
      // 使用取模运算循环分配颜色，保证同一位置的成员总是同一个颜色
      const color = PRESET_COLORS[index % PRESET_COLORS.length];
      return {
        ...item,
        color,
      };
    });
  }

  // 初始化成员列表（均分）
  function initializeMemberList() {
    if (props.members.length === 0 || props.totalImages === 0 || isDisabled.value) {
      memberList.value = [];
      return;
    }

    const memberCount = props.members.length;
    // 从已标注位置开始分配（默认不分配已标注的图片）
    const startFrom = props.annotatedCount || 0;
    const availableImages = props.totalImages - startFrom;

    if (availableImages <= 0) {
      // 如果没有未标注的图片，从0开始分配
      const base = Math.floor(props.totalImages / memberCount);
      const remainder = props.totalImages % memberCount;
      let currentOffset = 0;
      const list = props.members.map((member, index) => {
        const count = base + (index < remainder ? 1 : 0);
        const startOffset = currentOffset;
        const endOffset = currentOffset + count - 1;
        currentOffset += count;

        return {
          id: member.id,
          userId: member.id,
          nickName: member.nickName,
          username: member.username,
          startOffset,
          endOffset,
        };
      });
      memberList.value = assignColors(list);
      // 全是已标注时，允许左边界向右拖动到最大位置
      originalStartOffset.value = props.totalImages;
      return;
    }

    const base = Math.floor(availableImages / memberCount);
    const remainder = availableImages % memberCount;

    let currentOffset = startFrom;
    const list = props.members.map((member, index) => {
      const count = base + (index < remainder ? 1 : 0);
      const startOffset = currentOffset;
      const endOffset = currentOffset + count - 1;
      currentOffset += count;

      return {
        id: member.id,
        userId: member.id,
        nickName: member.nickName,
        username: member.username,
        startOffset,
        endOffset,
      };
    });

    memberList.value = assignColors(list);

    // 记录原始的起始位置（第一个成员的startOffset）
    originalStartOffset.value = startFrom;
  }

  // 获取图片数量
  function getImageCount(item: any) {
    const diff = item.endOffset - item.startOffset;
    return diff >= 0 ? diff + 1 : 0;
  }

  // 获取实际的起始偏移量
  function getActualStartOffset() {
    if (memberList.value.length === 0) return 0;
    return memberList.value[0]?.startOffset || 0;
  }

  // 计算百分比（基于整个数据集）
  function getPercentage(item: any) {
    if (props.totalImages === 0) return 0;
    const count = getImageCount(item);
    return (count / props.totalImages) * 100;
  }

  // 获取张数标签的位置（基于整个数据集）
  function getImagePosition(imageNumber: number) {
    if (props.totalImages === 0) return 0;
    return (imageNumber / props.totalImages) * 100;
  }

  // 计算拖拽手柄的位置（基于整个数据集）
  function getHandlePosition(index: number) {
    if (!progressBarRef.value || props.totalImages === 0) return '0%';

    const item = memberList.value[index];
    if (!item) return '0%';

    const percentage = ((item.endOffset + 1) / props.totalImages) * 100;
    return `${percentage}%`;
  }

  function getDisplayName(item: any) {
    return item?.username || item?.userName || item?.nickName || '-';
  }

  function truncateName(name: string, maxLength = 4) {
    if (!name) return '-';
    return name.length > maxLength ? `${name.slice(0, maxLength)}…` : name;
  }

  function getBoundaryShortLabel(index: number) {
    const current = memberList.value[index];
    return truncateName(getDisplayName(current), 6);
  }

  function getBoundaryFullLabel(index: number) {
    const current = memberList.value[index];
    return getDisplayName(current);
  }

  function getHandleOverlapIndex(index: number) {
    const current = memberList.value[index];
    if (!current) return 0;
    const currentBoundary = current.endOffset;
    let overlapIndex = 0;
    for (let i = 0; i < index; i++) {
      if (memberList.value[i]?.endOffset === currentBoundary) {
        overlapIndex += 1;
      }
    }
    return overlapIndex;
  }

  function getHandleOverlapTotal(index: number) {
    const current = memberList.value[index];
    if (!current) return 1;
    const currentBoundary = current.endOffset;
    return memberList.value.slice(0, -1).filter((m) => m.endOffset === currentBoundary).length;
  }

  function getHandleLabelTop(index: number) {
    const overlapIndex = getHandleOverlapIndex(index);
    return `${-26 - overlapIndex * 20}px`;
  }

  // 计算左侧边界的位置
  function getLeftBoundaryPosition() {
    if (memberList.value.length === 0 || props.totalImages === 0) return '0%';
    const firstMember = memberList.value[0];
    const percentage = (firstMember.startOffset / props.totalImages) * 100;
    return `${percentage}%`;
  }

  // 开始拖拽左侧边界
  function startResizeLeftBoundary(event: MouseEvent) {
    event.preventDefault();
    event.stopPropagation();
    resizing.value = true;
    resizeIndex.value = -1; // 使用-1表示左侧边界
    startMouseX.value = event.clientX;
    initialEndOffset.value = memberList.value[0].startOffset;

    document.addEventListener('mousemove', handleResizeLeftBoundary);
    document.addEventListener('mouseup', stopResize);
    document.addEventListener('mouseleave', stopResize);

    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
  }

  // 处理左侧边界拖拽（允许在0到originalStartOffset之间拖动）
  function handleResizeLeftBoundary(event: MouseEvent) {
    if (!resizing.value || !progressBarRef.value) return;

    const deltaX = event.clientX - startMouseX.value;
    const barWidth = progressBarRef.value.offsetWidth;
    const deltaImages = Math.round((deltaX / barWidth) * props.totalImages);

    let newStart = initialEndOffset.value + deltaImages;
    const newList = [...memberList.value];

    // 边界检查：不能小于0
    if (newStart < 0) {
      newStart = 0;
    }

    // 不能超过原始起始位置
    if (newStart > originalStartOffset.value) {
      newStart = originalStartOffset.value;
    }

    // 允许第一个人分配为0，但不能超过最后一个人的endOffset+1（即不能超出总范围）
    const lastMemberEndOffset = newList[newList.length - 1]?.endOffset ?? props.totalImages - 1;
    if (newStart > lastMemberEndOffset + 1) {
      newStart = lastMemberEndOffset + 1;
    }

    // 更新所有受影响成员的offset（允许中间成员分配为0）
    let currentStart = newStart;
    for (let idx = 0; idx < newList.length; idx++) {
      const member = newList[idx];
      if (currentStart > member.endOffset) {
        // 这个成员被"挤掉"了，分配为0
        member.startOffset = currentStart;
        member.endOffset = currentStart - 1; // endOffset < startOffset 表示0张
      } else {
        member.startOffset = currentStart;
        break; // 后面的成员不受影响
      }
    }
    memberList.value = newList;
  }

  // 开始拖拽调整
  function startResize(event: MouseEvent, index: number) {
    event.preventDefault();
    event.stopPropagation();
    resizing.value = true;
    resizeIndex.value = index;
    startMouseX.value = event.clientX;
    initialEndOffset.value = memberList.value[index].endOffset;

    document.addEventListener('mousemove', handleResize);
    document.addEventListener('mouseup', stopResize);
    document.addEventListener('mouseleave', stopResize);

    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
  }

  // 处理拖拽
  function handleResize(event: MouseEvent) {
    if (!resizing.value || !progressBarRef.value) return;

    const deltaX = event.clientX - startMouseX.value;
    const barWidth = progressBarRef.value.offsetWidth;
    const deltaImages = Math.round((deltaX / barWidth) * props.totalImages);

    // 如果没有移动足够的像素对应一张图片，则不处理
    if (deltaImages === 0) return;

    const i = resizeIndex.value;
    if (i < 0 || i >= memberList.value.length - 1) return;

    let newEnd = initialEndOffset.value + deltaImages;
    const newList = [...memberList.value];
    // 锁定当前被按下的边界：始终只调整 i 与 i+1
    const minLimit = newList[i].startOffset - 1;
    const maxLimit = newList[i + 1].endOffset;
    if (newEnd < minLimit) newEnd = minLimit;
    if (newEnd > maxLimit) newEnd = maxLimit;

    newList[i].endOffset = newEnd;
    newList[i + 1].startOffset = newEnd + 1;
    memberList.value = newList;
  }

  // 停止拖拽
  function stopResize() {
    resizing.value = false;
    resizeIndex.value = -1;
    initialEndOffset.value = 0;

    // 移除事件监听
    document.removeEventListener('mousemove', handleResize);
    document.removeEventListener('mousemove', handleResizeLeftBoundary);
    document.removeEventListener('mouseup', stopResize);
    document.removeEventListener('mouseleave', stopResize);

    document.body.style.cursor = '';
    document.body.style.userSelect = '';
  }

  // 重置为均分
  function resetToEqual() {
    initializeMemberList();
  }

  // 关闭模态框
  function close() {
    emit('update:visible', false);
  }

  // 提交比例数据
  function handleOk() {
    if (isDisabled.value) return;

    if (memberList.value.length === 0) {
      emit('submit', { proportions: [], startOffset: 0 });
      close();
      return;
    }

    // 获取实际的起始偏移量（第一个成员的startOffset）
    const actualStartOffset = memberList.value[0]?.startOffset || 0;

    // 直接计算精确的小数比例，让后端处理分配
    const proportions = memberList.value.map((item) => ({
      userId: item.userId,
      imageCount: getImageCount(item),
    }));

    const totalCount = proportions.reduce((sum, p) => sum + p.imageCount, 0);

    if (totalCount === 0) {
      emit('submit', { proportions: [], startOffset: actualStartOffset });
      close();
      return;
    }

    // 计算精确比例（保留4位小数）
    const exactProportions = proportions.map((p) => {
      const exact = (p.imageCount / totalCount) * 100;
      return {
        userId: p.userId,
        proportion: parseFloat(exact.toFixed(4)),
      };
    });

    // 调整最后一个成员的比例，确保总和精确为100
    const sum = exactProportions.reduce((acc, p) => acc + p.proportion, 0);
    if (exactProportions.length > 0 && Math.abs(sum - 100) > 0.0001) {
      const diff = parseFloat((100 - sum).toFixed(4));
      exactProportions[exactProportions.length - 1].proportion = parseFloat(
        (exactProportions[exactProportions.length - 1].proportion + diff).toFixed(4),
      );
    }

    emit('submit', { proportions: exactProportions, startOffset: actualStartOffset });
    close();
  }

  // 监听成员和图片数变化
  watch(
    () => [props.members, props.totalImages, props.visible],
    () => {
      if (props.visible && !isDisabled.value) {
        initializeMemberList();
      }
    },
    { immediate: true, deep: true },
  );
</script>

<style scoped lang="less">
  /* 全局容器深色背景适配 */
  .visual-proportion-container {
    padding: 24px 32px;
    min-height: 450px;
    background-color: #1f1f1f;
    color: #e0e0e0;
  }

  /* 1. 分配区域样式 */
  .allocation-section {
    margin-bottom: 24px;

    .section-title {
      color: #a0a0a0;
      font-size: 14px;
      margin-bottom: 30px;
      font-weight: 500;
    }
  }

  .progress-container {
    position: relative;
    padding-top: 10px;
    width: 100%; /* 确保占满 */

    /* 顶部刻度 */
    .offset-labels-row {
      position: relative;
      height: 24px;
      margin-bottom: 8px;
      font-size: 12px;
      color: #8c8c8c;
      width: 100%;

      .offset-label-item {
        position: absolute;
        transform: translateX(-50%);
        white-space: nowrap;
        pointer-events: none; /* 防止遮挡点击 */

        &.start {
          left: 0;
          transform: none;
        }
        &.end {
          left: 100%;
          transform: translateX(-100%);
        }

        &.dynamic {
          display: flex;
          flex-direction: column;
          align-items: center;
          z-index: 5;

          .offset-line {
            width: 1px;
            height: 6px;
            background: #434343;
            margin-bottom: 2px;
          }
          .offset-value {
            font-family: monospace;
          }
        }
      }
    }

    /* 核心进度条 */
    .progress-bar-wrapper {
      width: 100%;
    }

    .progress-bar {
      display: flex;
      height: 64px;
      background: #141414;
      border-radius: 4px;
      overflow: visible;
      position: relative;
      box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.4);
      border: 1px solid #303030;
      box-sizing: border-box; /* 关键：确保边框计算在宽度内 */

      .progress-segment {
        height: 100%;
        position: relative;
        /* !!! 关键修改：移除 transition 解决拖拽延迟 !!! */
        /* transition: width 0.1s ease;  <-- 删除这行 */
        overflow: hidden;

        &.unassigned-segment {
          background: repeating-linear-gradient(
            -45deg,
            #262626,
            #262626 10px,
            #1f1f1f 10px,
            #1f1f1f 20px
          );
          display: flex;
          align-items: center;
          justify-content: center;
          border-right: 1px solid #303030;
          box-sizing: border-box;

          .segment-info {
            color: #595959;
            font-size: 12px;
            display: flex;
            flex-direction: column;
            align-items: center;
            user-select: none;
          }
        }

        .segment-content {
          height: 100%;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          color: rgba(255, 255, 255, 0.95);
          font-size: 13px;
          text-shadow: 0 1px 2px rgba(0, 0, 0, 0.6);
          padding: 0 4px;
          user-select: none;

          .segment-name {
            font-weight: 600;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            max-width: 100%;
          }
          .segment-sub {
            font-size: 11px;
            opacity: 0.85;
            margin-top: 2px;
            white-space: nowrap;
          }
        }
      }

      .resize-handle {
        position: absolute;
        top: -6px;
        bottom: -6px;
        width: 14px;
        z-index: 10;
        cursor: col-resize;
        transform: translateX(-50%);
        display: flex;
        justify-content: center;
        /* !!! 关键修改：移除过渡，确保手柄紧跟鼠标 !!! */
        transition: none;

        .handle-line {
          width: 2px;
          height: 100%;
          background: #fff;
          box-shadow: 0 0 4px rgba(0, 0, 0, 0.5);
          opacity: 0.6;
        }

        .handle-label {
          position: absolute;
          left: 50%;
          transform: translateX(-50%);
          max-width: 88px;
          padding: 1px 6px;
          font-size: 10px;
          line-height: 14px;
          color: #f0f0f0;
          background: rgba(0, 0, 0, 0.72);
          border: 1px solid rgba(255, 255, 255, 0.35);
          border-radius: 10px;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          pointer-events: auto;
          cursor: col-resize;
          box-shadow: 0 1px 4px rgba(0, 0, 0, 0.35);
          opacity: 0.9;
        }

        .handle-label.overlapped {
          background: rgba(17, 17, 17, 0.9);
        }

        .handle-label.active {
          color: #fff;
          background: rgba(24, 144, 255, 0.92);
          border-color: rgba(24, 144, 255, 0.95) !important;
          box-shadow: 0 0 10px rgba(24, 144, 255, 0.45);
          opacity: 1;
        }

        &:hover .handle-line,
        &.is-resizing .handle-line {
          background: #1890ff;
          width: 3px;
          opacity: 1;
          box-shadow: 0 0 8px rgba(24, 144, 255, 0.6);
        }

        &.left-boundary-handle {
          z-index: 20;
          .handle-line {
            background: #faad14;
          }
          .handle-tag {
            position: absolute;
            top: -22px;
            background: #faad14;
            color: #000;
            font-size: 10px;
            padding: 1px 4px;
            border-radius: 2px;
            font-weight: bold;
            white-space: nowrap;
            pointer-events: auto;
            cursor: col-resize;
          }
        }
      }
    }
  }

  /* 2. 状态条区域 */
  .status-bar-wrapper {
    margin-top: 20px;
    /* !!! 关键修改：移除左右 padding，改为 0 !!! */
    padding: 12px 0;
    background: #262626;
    border-radius: 6px;
    border: 1px solid #303030;
    width: 100%;
    box-sizing: border-box;

    /* 文字部分单独加 padding，保持美观 */
    .status-info-header {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      margin-bottom: 8px;
      color: #a0a0a0;
      padding: 0 16px;

      .sub-text {
        opacity: 0.6;
      }
    }

    /* 进度条现在可以撑满宽度，与上面的分配条严格对齐 */
    .mini-status-bar {
      height: 8px;
      display: flex;
      /* 移除 border-radius 以保持硬边对齐，或者只保留微小的圆角 */
      background: #141414;
      width: 100%;
      position: relative;

      .status-segment {
        height: 100%;
        /* 加上过渡效果让下面的条看起来平滑一点（可选） */
        transition: width 0.3s ease;
        &.annotated {
          background: #52c41a;
        }
        &.unannotated {
          background: #434343;
        }
      }
    }

    .status-legend {
      display: flex;
      gap: 16px;
      margin-top: 8px;
      font-size: 12px;
      color: #8c8c8c;
      padding: 0 16px; /* 文字部分单独加 padding */

      .legend-item {
        display: flex;
        align-items: center;
        gap: 6px;

        .dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          &.annotated {
            background: #52c41a;
          }
          &.unannotated {
            background: #434343;
          }
        }
      }
    }
  }

  /* 3. 详情列表区域 */
  .member-summary {
    margin-top: 24px;

    .summary-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .summary-title {
        font-size: 14px;
        font-weight: 500;
        color: #e0e0e0;
        border-left: 3px solid #1890ff;
        padding-left: 8px;
      }

      .reset-button {
        color: #177ddc;
      }
    }

    .summary-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 12px;
    }

    .summary-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 14px;
      background: #262626;
      border: 1px solid #303030;
      border-radius: 4px;
      transition: all 0.2s;

      &:hover {
        border-color: #434343;
        background: #2a2a2a;
      }

      .card-left {
        display: flex;
        align-items: center;
        gap: 10px;

        .color-dot {
          width: 10px;
          height: 10px;
          border-radius: 50%;
          box-shadow: 0 0 4px rgba(0, 0, 0, 0.5);
        }

        .member-name {
          color: #e0e0e0;
          font-size: 13px;
          font-weight: 500;
        }
      }

      .card-right {
        text-align: right;

        .range-tag {
          font-size: 12px;
          color: #8c8c8c;
          font-family: monospace;
        }

        .count-text {
          color: #d9d9d9;
          font-size: 12px;
          font-weight: 600;
        }

        .empty-text {
          color: #595959;
          font-size: 12px;
          font-style: italic;
        }
      }
    }
  }

  .disabled-notice {
    padding: 20px;
  }
</style>
