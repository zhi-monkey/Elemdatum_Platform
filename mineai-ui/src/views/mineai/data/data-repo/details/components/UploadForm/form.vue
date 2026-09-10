<template>
  <div id="upload-form-style" class="upload-form">
    <el-upload
      :action="props.action"
      :accept="props.accept"
      class="upload-field"
      :limit="props.limit"
      multiple
      list-type="text"
      :auto-upload="false"
      :disabled="uploading"
      :on-change="fileChange"
      :on-remove="onRemove"
      :on-exceed="onExceed"
      v-model:file-list="fileList"
      ref="uploader"
    >
      <el-button :disabled="uploading || attrs.disabled" size="default" type="primary">
        选择文件
      </el-button>
      <template #tip>
        <div class="tip-container">
          <div class="tip-top">
            <span v-if="props.accept === 'unspecified'">文件格式不限</span>
            <span v-else>文件格式：{{ acceptFormatStr }}</span>
            <span v-if="props.acceptSize > 0"
              >, 单个文件不大于 {{ acceptSizeFormat(props.acceptSize) }}
            </span>
          </div>
          <div class="tip-bottom">
            <span v-if="showFileCount" class="upload-chosen-tip">
              已选择 <span class="highlight-text">{{ lenOfFileList }}</span> 个
            </span>
          </div>
        </div>
      </template>
    </el-upload>
  </div>
</template>
<script setup>
  import axios from 'axios';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getFileExtension } from './util';
  import { computed, ref, useAttrs, defineExpose } from 'vue';

  const { createMessage } = useMessage();
  const props = defineProps({
    action: String,
    accept: {
      type: String,
      default: '.jpg,.png,.bmp,.jpeg',
    },
    acceptSize: {
      type: Number, // 如果传入 0 代表不限制
      default: 5, // MB
    },
    acceptSizeFormat: {
      // 格式化文本
      type: Function,
      default: (d) => `${d} MB`,
    },
    limit: {
      type: Number,
      default: 5000,
    },
    showFileCount: {
      type: Boolean,
      default: true,
    },
    dataType: {
      type: String,
      default: 'visual',
    },
    /**
     * filters 数组要求：
     * 1. 成员需要有一个 judge 方法返回布尔值，来判断是否需要过滤文件
     * 2. 成员需要有一个 message 属性，用来展示提示信息
     */
    filters: {
      type: Array,
      default: () => [],
      validator: (value) => {
        for (const filter of value) {
          if (!filter.message || typeof filter.judge !== 'function') {
            return false;
          }
        }
        return true;
      },
    },
  });
  const emits = defineEmits(['fileChange', 'onUploadPercent']);
  const uploading = ref(false);
  const lenOfFileList = ref(0);
  const fileList = ref([]);
  const uploader = ref();
  const source = axios.CancelToken.source();
  const acceptFormatStr = computed(() => {
    const formats = props.accept.split(',');
    console.log(formats);
    return formats.join('/');
  });
  const attrs = useAttrs();
  defineExpose({
    fileList,
    lenOfFileList,
    reset,
  });

  let msgInstance;

  function onMessageClose() {
    // 清理 message 实例
    msgInstance = null;
  }

  function reset() {
    uploader.value.clearFiles();
    lenOfFileList.value = 0;
  }

  import { ElMessage as Message } from 'element-plus';

  // 定义图片和视频的后缀名数组
  const imageExtensions = ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'tiff'];
  const videoExtensions = ['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv'];

  /**
   * 标准文件过滤入口，返回布尔值
   * @param {*} file 被过滤文件
   * @param {*} fileList 文件列表
   * @param {Boolean} bool 如果布尔值为 true 则过滤该文件
   * @param {*} message Message 信息
   * @return {Boolean} 返回传入的布尔值
   */
  function addFileFilter(file, fileList, bool, message) {
    // 获取文件后缀名
    const fileExtension = file.name.split('.').pop().toLowerCase();

    // 检查文件是否为图片或视频
    // if (imageExtensions.includes(fileExtension) || videoExtensions.includes(fileExtension)) {
    //   bool = true;
    //   message = '其他列表不能上传图片或视频文件';
    // }
    // bool = false;
    //
    // if (bool) {
    //   fileList.splice(fileList.indexOf(file), 1);
    //   Message.info(message);
    // }
    if (bool) {
      fileList.splice(fileList.indexOf(file), 1);
      if (!msgInstance) {
        msgInstance = createMessage.info({
          content: message,
          onClose: onMessageClose,
        });
      }
    }
    return bool;
  }

  function addFileFilterOther(file, fileList, bool, message) {
    // 获取文件后缀名
    const fileExtension = file.name.split('.').pop().toLowerCase();

    //检查文件是否为图片或视频
    if (imageExtensions.includes(fileExtension) || videoExtensions.includes(fileExtension)) {
      bool = true;
      message = '其他列表不能上传图片或视频文件';
    }

    if (bool) {
      fileList.splice(fileList.indexOf(file), 1);
      Message.info(message);
    }

    return bool;
  }

  function fileChange(file, fileList) {
    // 根据后缀名进行格式匹配
    const acceptTypes = props.accept.split(',');
    const extname = getFileExtension(file.raw.name);
    const mimeType = acceptTypes.includes(extname.toLowerCase());
    console.log(props.accept);
    // Other以外的文件校验文件格式
    if (
      props.accept !== 'unspecified' &&
      addFileFilter(file, fileList, !mimeType, '文件格式不支持')
    ) {
      return;
    }

    //Other文件校验是否为视频或图片，其他分类里不允许出现图片视频
    if (
      props.accept === 'unspecified' &&
      addFileFilterOther(file, fileList, false, '其他列表不能上传图片或视频文件')
    ) {
      return;
    }

    // acceptSize 支持传入 0 代表不限制大小
    const isOverSize = props.acceptSize !== 0 && file.size / (1024 * 1024) > props.acceptSize;
    if (addFileFilter(file, fileList, isOverSize, `不能添加大于${props.acceptSize}MB的文件`)) {
      return;
    }

    for (const item of fileList.slice(0, fileList.length - 1)) {
      if (addFileFilter(file, fileList, item.name === file.name, '不能添加文件名相同的文件')) {
        return;
      }
    }

    for (const filter of props.filters) {
      if (addFileFilter(file, fileList, filter.judge(file, fileList), filter.message)) {
        return;
      }
    }

    lenOfFileList.value = fileList.length;
    // 触发文件变动事件
    emits('fileChange', file, fileList);
  }

  function onProgress(res) {
    const { loaded } = res;
    const { total } = res;
    const uploadPercent =
      Math.floor((loaded / total) * 100) > 1 ? Math.floor((loaded / total) * 100) : 1;
    emits('onUploadPercent', uploadPercent);
  }

  function onRemove(file, fileList) {
    lenOfFileList.value = fileList.length;
    attrs['on-remove'] && attrs['on-remove'](file, fileList);
  }

  function cancelUpload() {
    if (source) {
      source.cancel('取消上传');
    }
  }

  function onExceed(files, fileList) {
    if (files.length > props.limit || fileList.length > props.limit) {
      createMessage.info(`单次上传文件数量不能超过${props.limit}`);
    }
  }
</script>
<style scoped>
  .upload-field {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
  }

  #upload-form-style .upload-chosen-tip {
    min-width: 100px;
    text-align: center;
  }

  #upload-form-style ul.el-upload-list {
    width: 100%;
    height: auto;
    max-height: 408px;
    margin-top: 10px;
    overflow-y: auto;
  }

  #upload-form-style ul.el-upload-list.el-upload-list--picture li {
    float: left;
    width: 92px;
    height: 92px;
    padding: 0 0 0 90px;
    margin: 8px;
  }

  #upload-form-style ul.el-upload-list.el-upload-list--picture li img {
    width: 90px;
    height: 90px;
    margin-left: -90px;
  }

  #upload-form-style .el-icon-close {
    top: 2px;
    right: 2px;
    z-index: 999;
    width: 20px;
    height: 20px;
    line-height: 20px;
    color: #fff;
    text-align: center;
    background-color: rgb(182 196 255);
    border-radius: 50%;
  }

  #upload-form-style .highlight-text {
    color: #ff0000;
  }

  .tip-container {
    display: flex;
    height: 100%;
    flex-direction: column;
    padding-left: 80px;
  }

  .tip-top {
    font-size: 12px;
    color: #606266;
  }

  .tip-bottom {
    font-size: 24px;
  }
</style>
