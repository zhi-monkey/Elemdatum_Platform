<template>
  <li v-show="mode === 'text'" @click="handleClick(item)" :class="textItemClass">
    <span class="item-index">{{ index }}.</span>
    {{
      basename.length > 20
        ? basename.substring(0, 10) + '...' + basename.substring(basename.length - 7)
        : basename
    }}
  </li>
  <li v-show="mode === 'image'" :class="classnames" :style="style" @click="handleClick(item)">
    <span class="image-item-index">{{ index }}</span>
  </li>
</template>
<script lang="ts" setup>
  import cx from 'classnames';
  import { computed } from 'vue';
  import { getBasename } from '/@/views/mineai/data/dataset-details2/components/UploadForm/util';

  const props = defineProps<{
    item: object;
    index: number;
    handleClick: Function;
    currentImgId: number | null;
    mode: 'text' | 'image';
  }>();
  const style = {
    backgroundImage: `url(${props.item.url})`,
  };

  const classnames = computed(() => {
    return cx('thumb-list-item', {
      active: props.currentImgId === props.item.id,
      annotated: props.item.status === 104, // 已标注
    });
  });

  const textItemClass = computed(() => {
    return cx('text-link', {
      focus: props.currentImgId === props.item.id,
      annotated: props.item.status === 104, // 已标注
    });
  });
  // 文件后缀
  const basename = computed(() => getBasename(props.item.url));
</script>

<style scoped lang="scss">
  .thumb-wrapper {
    position: relative;
    z-index: 2;
    display: flex;
    flex-direction: column;
    width: 160px;
    padding-top: 8px;
    text-align: center;
    background: #fff;
    box-shadow: 2px 0 6px 0 rgba(0, 0, 0, 0.15);

    .file-infobar {
      padding: 0 8px;
      font-size: 16px;
      line-height: 32px;
      border-top: 1px solid #d8d8d8;
      border-bottom: 1px solid #d8d8d8;

      .el-dropdown {
        display: inline-block;
        width: 66%;
        white-space: nowrap;
      }
    }

    .thumb-list-item {
      background-repeat: no-repeat;
      background-position: center;
      background-origin: content-box;
      background-size: contain;
      width: 90px;
      height: 74px;
      padding: 4px;
      margin: 12px auto 0;
      cursor: pointer;

      &.active {
        border-radius: 2px;
        box-shadow: 0 0 4px 2px rgba(117, 117, 117, 0.5);
      }
    }

    .text-link {
      margin-bottom: 2px;
      cursor: pointer;

      &.focus {
        color: dodgerblue;
      }
    }

    .infinite-list-wrapper {
      flex: 1;
    }

    .annotate-pagination {
      span {
        display: inline-block;
        margin-left: 0;
      }

      .el-icon-question {
        line-height: 28px;
      }
    }
  }

  .thumb-list-item {
    background-repeat: no-repeat;
    background-position: center;
    background-origin: content-box;
    background-size: contain;
    width: 90px;
    height: 74px;
    padding: 4px;
    margin: 12px auto 0;
    cursor: pointer;

    &.active {
      border-radius: 2px;
      box-shadow: 0 0 4px 2px rgba(117, 117, 117, 0.5);
    }

    // 已标注状态显示绿色背景
    &.annotated {
      background-color: rgba(76, 175, 80, 0.1); // 淡绿色背景
    }

    // 选中状态优先级更高，保持原有样式
    &.active {
      background-color: transparent;
    }
  }

  .text-link {
    margin-bottom: 2px;
    cursor: pointer;
    white-space: nowrap; // 防止折行
    overflow: hidden; // 超出隐藏
    text-overflow: ellipsis; // 显示省略号
    display: block; // 确保能正确应用文本溢出样式
    padding: 2px 4px; // 增加一点内边距

    &.focus {
      color: dodgerblue;
    }

    // 已标注状态显示绿色文字
    &.annotated {
      color: #4caf50;
    }

    // 选中状态优先级更高，保持蓝色
    &.focus {
      color: dodgerblue;
    }

    .item-index {
      color: #909399;
      margin-right: 4px;
      font-size: 12px;
      display: inline-block; // 确保序号和文件名在一行
    }
  }

  .image-item-index {
    position: absolute;
    top: 2px;
    left: 2px;
    background: rgba(0, 0, 0, 0.6);
    color: white;
    padding: 2px 6px;
    border-radius: 3px;
    font-size: 12px;
    font-weight: bold;
    z-index: 1;
  }

  .thumb-list-item {
    position: relative;
  }
</style>
