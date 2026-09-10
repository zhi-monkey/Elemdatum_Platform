<template>
  <div class="workspace-toolbar flex flex-between">
    <!-- 导航区域局部遮罩：在加载期间阻止点击工具栏按钮 -->
    <div v-if="isUIBlocked" class="nav-blocker"></div>
    <div class="ml-4 flex flex-row justify-start items-center gap-x-1">
      <el-tooltip
        :content="isSegmentation ? '自定义绘制' : '绘制选框'"
        placement="top"
        :open-delay="400"
        v-if="false"
      >
        <a-button
          type="primary"
          @click="onSelection"
          :class="api.active === 'selection' ? 'active' : ''"
          shape="circle"
          :icon="h(BorderOutlined)"
          :disabled="!hasFile"
        />
      </el-tooltip>
      <el-tooltip content="放大" placement="top" :open-delay="400">
        <a-button
          type="primary"
          @click="handleZoomIn"
          :class="api.active === 'zoomIn' ? 'active' : ''"
          shape="circle"
          :icon="h(ZoomInOutlined)"
          :disabled="!hasFile"
        />
      </el-tooltip>
      <el-tooltip content="缩小" placement="top" :open-delay="400">
        <a-button
          type="primary"
          @click="handleZoomOut"
          :class="api.active === 'zoomOut' ? 'active' : ''"
          shape="circle"
          :icon="h(ZoomOutOutlined)"
          :disabled="!hasFile"
        />
      </el-tooltip>
      <el-tooltip content="重置" placement="top" :open-delay="400">
        <a-button
          type="primary"
          @click="handleZoomReset"
          :class="api.active === 'reset' ? 'active' : ''"
          shape="circle"
          :icon="h(RedoOutlined)"
          :disabled="!hasFile"
        />
      </el-tooltip>
      <!-- 新增的上下箭头按钮 -->
      <el-tooltip content="查看上一张" placement="top" :open-delay="400">
        <a-button
          type="primary"
          @click="handleMoveUp"
          :class="api.active === 'moveUp' ? 'active' : ''"
          shape="circle"
          :icon="h(UpOutlined)"
          :disabled="!hasFile"
        />
      </el-tooltip>
      <el-tooltip content="查看下一张" placement="top" :open-delay="400">
        <a-button
          type="primary"
          @click="handleMoveDown"
          :class="api.active === 'moveDown' ? 'active' : ''"
          shape="circle"
          :icon="h(DownOutlined)"
          :disabled="!hasFile"
        />
      </el-tooltip>
      <el-tooltip
        content="跳转至下方最近未标注图片"
        placement="top"
        :open-delay="400"
        v-if="showJumpUnannotated"
      >
        <a-button
          type="primary"
          @click="handleJumpUnannotated"
          :class="api.active === 'jumpUnannotated' ? 'active' : ''"
          shape="circle"
          :icon="h(AimOutlined)"
          :disabled="!hasFile"
        />
      </el-tooltip>
    </div>
    <div class="tip-center" v-if="actionDisabled">
      <ExclamationCircleOutlined />
      当前处于查看模式, 操作按钮已隐藏
    </div>
    <div class="toolbar-right">
      <!--      <div class="action-item" @click="save">-->
      <!--        <SaveOutlined />-->
      <!--        <span>保存</span>-->
      <!--      </div>-->

      <!-- 返回按钮 -->
      <div class="action-item" @click="handleGoBack">
        <ArrowLeftOutlined />
        <span>返回</span>
      </div>

      <div
        class="action-item"
        @click="confirm"
        :class="{
          unclick: !hasFile || isConfirming,
          'loading-state': isConfirming,
        }"
        v-if="!actionDisabled"
      >
        <CheckOutlined v-if="!isConfirming" />
        <LoadingOutlined v-else />
        <span>{{ isConfirming ? '处理中...' : '完成' }}</span>
      </div>

      <!-- 一键确认按钮 - 仅在多人标注模式下显示 -->
      <!-- <div
        class="action-item"
        @click="handleBatchConfirm"
        :class="{
          unclick: !hasFile || isBatchConfirming,
          'loading-state': isBatchConfirming,
        }"
        v-if="!actionDisabled && isTeamLabel"
      >
        <CheckOutlined v-if="!isBatchConfirming" />
        <LoadingOutlined v-else />
        <span>{{ isBatchConfirming ? '处理中...' : '一键确认' }}</span>
      </div> -->

      <div class="action-item" v-if="!actionDisabled" @click="handleToggleViewMode">
        <EyeOutlined />
        <span>查看模式</span>
      </div>

      <div class="action-item" v-if="deleteable && !actionDisabled" :class="{ unclick: !hasFile }">
        <DeleteOutlined />
        <el-popconfirm
          title="确定删除该文件吗? (按V键确认)"
          @confirm="remove"
          v-model:visible="deletePopConfirmVisible"
          :disabled="!hasFile"
        >
          <template #reference>
            <span ref="deleteButtonRef">删除文件</span>
          </template>
        </el-popconfirm>
      </div>
      <div class="action-tip">
        <el-popover
          effect="dark"
          placement="bottom-start"
          trigger="click"
          :visible-arrow="false"
          v-model:visible="helpVisible"
        >
          <template #reference>
            <div>
              <QuestionCircleOutlined />
              <span>帮助</span>
            </div>
          </template>
          <div class="flex flex-col justify-start">
            <div class="f14 mb-1 font-bold flex justify-between items-center">
              <span>快捷键说明：</span>
              <CloseOutlined class="close-icon" @click="helpVisible = false" />
            </div>
            <div class="tips-wrapper f12">
              <div>上一张：A</div>
              <div>下一张：D</div>
              <div>删除标签：Delete</div>
              <div>切换移动模式和绘图模式：W</div>
              <div>切换无限绘制模式：E</div>
              <div>切换标注/查看模式：Q</div>
              <div>完成当前图片标注：S</div>
              <div>清空标注：R</div>
              <div>删除文件：C键显示确认，V键确认</div>
              <div>修改标注框标签：鼠标右键</div>
              <div v-if="isSegmentation">完成绘制：F</div>
              <div v-if="isSegmentation">放弃绘制：Esc</div>
              <div v-if="isSegmentation">连续绘制：Shift</div>
            </div>
          </div>
        </el-popover>
      </div>
    </div>
  </div>
</template>

<script>
  import vClickOutside from 'v-click-outside';
  import { Button } from 'ant-design-vue';
  import {
    ArrowLeftOutlined,
    BorderOutlined,
    CheckOutlined,
    CloseOutlined,
    DeleteOutlined,
    DownOutlined,
    ExclamationCircleOutlined,
    EyeOutlined,
    LoadingOutlined,
    QuestionCircleOutlined,
    RedoOutlined,
    UpOutlined,
    ZoomInOutlined,
    ZoomOutOutlined,
    AimOutlined,
  } from '@ant-design/icons-vue';
  import { defineExpose, h, inject, onMounted, ref } from 'vue';
  import { useRouter } from 'vue-router';

  export default {
    name: 'ToolBar',
    components: {
      AButton: Button,
      CheckOutlined,
      QuestionCircleOutlined,
      DeleteOutlined,
      ExclamationCircleOutlined,
      ArrowLeftOutlined,
      CloseOutlined,
      LoadingOutlined,
      EyeOutlined,
    },
    directives: {
      clickOutside: vClickOutside.directive,
    },
    props: {
      api: {
        type: Object,
        default: () => ({}),
      },
      showJumpUnannotated: {
        type: Boolean,
        default: false,
      },
      zoomIn: Function,
      zoomOut: Function,
      zoomReset: Function,
      clearSelection: Function,
      confirm: Function,
      setApi: Function,
      isSegmentation: Boolean,
      deleteable: Boolean,
      hasFile: Boolean,
      actionDisabled: Boolean,
      handlePrev: Function,
      handleNext: Function,
      isConfirming: Boolean,
      toggleViewMode: Function,
      isTeamLabel: Boolean,
      isBatchConfirming: Boolean,
      batchConfirm: Function,
    },
    emits: [
      'save',
      'remove',
      'selection',
      'prev',
      'next',
      'toggle-view-mode',
      'jump-unannotated',
      'batch-confirm',
    ],
    setup(props, ctx) {
      const router = useRouter();
      const helpVisible = ref(false);

      // 注入 UI 屏蔽状态
      const isUIBlocked = inject('isUIBlocked', ref(false));
      const deletePopConfirmVisible = ref(false);
      const deleteButtonRef = ref(null);

      onMounted(() => {
        // 等待页面布局稳定后再显示
        setTimeout(() => {
          helpVisible.value = true;
        }, 1000);
      });
      const { clearSelection, zoomIn, zoomOut, zoomReset, setApi } = props;

      // 返回上一页
      const handleGoBack = () => {
        router.go(-1);
      };

      const onSelection = () => {
        setApi({ active: 'selection' });
        ctx.emit('selection', true);
      };
      const onClickOutside = (event) => {
        if (!event.target.closest('.toolbar-left')) {
          setApi({ active: '' });
        }
        clearSelection();
      };
      const save = () => {
        ctx.emit('save');
      };

      const remove = () => {
        ctx.emit('remove');
      };

      const handleZoomIn = () => {
        setApi({ active: 'zoomIn' });
        zoomIn();
      };

      const handleZoomOut = () => {
        setApi({ active: 'zoomOut' });
        zoomOut();
      };

      const handleZoomReset = () => {
        setApi({ active: 'reset' });
        zoomReset();
      };

      // 新增：处理上移按钮点击
      const handleMoveUp = () => {
        setApi({ active: 'moveUp' });
        ctx.emit('prev');
      };

      // 新增：处理下移按钮点击
      const handleMoveDown = () => {
        setApi({ active: 'moveDown' });
        ctx.emit('next');
      };

      const handleJumpUnannotated = () => {
        setApi({ active: 'jumpUnannotated' });
        ctx.emit('jump-unannotated');
      };

      // 处理视图模式切换
      const handleToggleViewMode = () => {
        if (props.toggleViewMode) {
          props.toggleViewMode();
        } else {
          ctx.emit('toggle-view-mode');
        }
      };

      // 处理一键确认
      const handleBatchConfirm = () => {
        if (props.batchConfirm) {
          props.batchConfirm();
        } else {
          ctx.emit('batch-confirm');
        }
      };

      // 显示删除popconfirm（供外部调用）
      const showDeletePopConfirm = () => {
        if (props.hasFile && !props.actionDisabled && props.deleteable) {
          deletePopConfirmVisible.value = true;
        }
      };

      // 隐藏删除popconfirm（供外部调用）
      const hideDeletePopConfirm = () => {
        deletePopConfirmVisible.value = false;
      };

      // 暴露方法给父组件
      defineExpose({
        showDeletePopConfirm,
        hideDeletePopConfirm,
      });

      return {
        isUIBlocked,
        helpVisible,
        deletePopConfirmVisible,
        showDeletePopConfirm,
        hideDeletePopConfirm,
        deleteButtonRef,
        onClickOutside,
        onSelection,
        handleZoomIn,
        handleZoomOut,
        handleZoomReset,
        handleMoveUp,
        handleMoveDown,
        handleJumpUnannotated,
        handleToggleViewMode,
        handleBatchConfirm,
        handleGoBack,
        save,
        LoadingOutlined,
        remove,
      };
    },
    methods: {
      ArrowLeftOutlined,
      DownOutlined,
      UpOutlined,
      BorderOutlined,
      ZoomInOutlined,
      ZoomOutOutlined,
      RedoOutlined,
      AimOutlined,
      h,
    },
  };
</script>

<style lang="scss" scoped>
  @import 'src/assets/scss/variables.scss';

  .workspace-toolbar {
    position: relative;
    z-index: 1;
    height: 48px;
    min-height: 48px;
    flex-shrink: 0;
    padding: 0 14px;
    line-height: 48px;
    background-color: #181d31;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.4);

    // 局部屏蔽层：拦截工具栏区域的点击
    .nav-blocker {
      position: absolute;
      inset: 0;
      z-index: 1000;
      background: transparent;
      pointer-events: auto;
    }

    .svg-icon {
      padding-right: 2px;
      padding-left: 2px;
      font-size: 18px;
      cursor: pointer;
      user-select: none;

      &.active {
        color: $primaryColor;
      }
    }

    .toolbar-right {
      display: flex;
      flex-direction: row;
      align-items: center;
      margin-right: 40px;

      .action-item {
        margin-left: 5px;
        font-size: 14px;
        line-height: 32px;
        color: #e1e1e1;
        cursor: pointer;
        border-radius: 4px;
        padding-left: 5px;
        padding-right: 5px;

        .svg-icon {
          line-height: 1;
          vertical-align: middle;
        }

        &:hover {
          color: dodgerblue;
        }

        &:active {
          color: #e1e1e1;
          background-color: dodgerblue;

          .svg-icon {
            opacity: 0.8;
          }
        }
      }

      .action-tip {
        margin-left: 15px;
        font-size: 14px;
        line-height: 32px;
        color: #e1e1e1;
        cursor: pointer;
        border-radius: 4px;

        .svg-icon {
          line-height: 1;
          vertical-align: middle;
        }

        &:hover {
          color: dodgerblue;
        }

        .close-icon {
          cursor: pointer;

          &:hover {
            color: $primaryColor;
          }

          .tips-wrapper {
            display: flex;
            flex-direction: column;

            div {
              width: 50%;
            }
          }
        }
      }
    }
  }

  .unclick {
    pointer-events: none;
  }

  .loading-state {
    opacity: 0.7;
    cursor: wait !important;
  }
</style>
