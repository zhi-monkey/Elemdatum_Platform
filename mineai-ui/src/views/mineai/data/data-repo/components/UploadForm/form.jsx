import axios from 'axios';
import './style.css';
import { useMessage } from '/@/hooks/web/useMessage';
import { getFileExtension } from './util';

const { createMessage } = useMessage();

// const path = require('path');

let msgInstance;

const defaultAccept = (d) => `${d} MB`;

export default {
  name: 'UploadForm',
  props: {
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
      default: defaultAccept,
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
  },
  emits: ['fileChange', 'onUploadPercent'],
  data() {
    return {
      uploading: false,
      lenOfFileList: 0,
      source: axios.CancelToken.source(),
      fileList: [],
    };
  },
  computed: {
    acceptFormatStr() {
      const formats = this.accept.split(',');
      return formats.join('/');
    },
  },
  methods: {
    onMessageClose() {
      // 清理 message 实例
      msgInstance = null;
    },
    reset() {
      this.$refs.uploader.clearFiles();
      this.lenOfFileList = 0;
    },
    /**
     * 标准文件过滤入口，返回布尔值
     * @param {*} file 被过滤文件
     * @param {*} fileList 文件列表
     * @param {Boolean} bool 如果布尔值为 true 则过滤改文件
     * @param {*} message Message 信息
     * @return {Boolean} 返回传入的布尔值
     */
    addFileFilter(file, fileList, bool, message) {
      if (bool) {
        fileList.splice(fileList.indexOf(file), 1);
        if (!msgInstance) {
          msgInstance = createMessage.info({
            content: message,
            onClose: this.onMessageClose,
          });
        }
      }
      return bool;
    },
    fileChange(file, fileList) {
      // 根据后缀名进行格式匹配
      const acceptTypes = this.accept.split(',');
      const extname = getFileExtension(file.raw.name);
      const mimeType = acceptTypes.includes(extname.toLowerCase());

      const addFilter = this.addFileFilter.bind(this, file, fileList);

      // 不限定文件格式时跳过验证
      if (this.accept !== 'unspecified' && addFilter(!mimeType, '文件格式不支持')) {
        return;
      }

      // acceptSize 支持传入 0 代表不限制大小
      const isOverSize = this.acceptSize !== 0 && file.size / (1024 * 1024) > this.acceptSize;
      if (addFilter(isOverSize, `不能添加大于${this.acceptSize}MB的文件`)) {
        return;
      }

      for (const item of fileList.slice(0, fileList.length - 1)) {
        if (addFilter(item.name === file.name, '不能添加文件名相同的文件')) {
          return;
        }
      }

      for (const filter of this.filters) {
        if (addFilter(filter.judge(file, fileList), filter.message)) {
          return;
        }
      }

      this.lenOfFileList = fileList.length;
      // 触发文件变动事件
      this.$emit('fileChange', file, fileList);
    },
    onProgress(res) {
      const { loaded } = res;
      const { total } = res;
      const uploadPercent =
        Math.floor((loaded / total) * 100) > 1 ? Math.floor((loaded / total) * 100) : 1;
      this.$emit('onUploadPercent', uploadPercent);
    },
    onRemove(file, fileList) {
      this.lenOfFileList = fileList.length;
      this.$attrs['on-remove'] && this.$attrs['on-remove'](file, fileList);
    },
    cancelUpload() {
      if (this.source) {
        this.source.cancel('取消上传');
      }
    },
    onExceed(files, fileList) {
      if (files.length > this.limit || fileList.length > this.limit) {
        createMessage.info(`单次上传文件数量不能超过${this.limit}`);
      }
    },
  },
  render() {
    // vue jsx 属性传递需要把 on- 放到 props 内
    // 详细参考：https://zhuanlan.zhihu.com/p/37920151
    // const uploadProps = {
    //   props: {
    //     onChange: this.fileChange,
    //     onRemove: this.onRemove,
    //     onExceed: this.onExceed,
    //     ...this.$attrs,
    //   },
    //   ref: 'uploader',
    // };
    // console.log('onremoe', this.$attrs);
    return (
      <div id="upload-form-style" class="upload-form">
        <el-upload
          action={this.action}
          accept={this.accept}
          class="upload-field"
          limit={this.limit}
          multiple
          list-type="text"
          // list-type={this.lenOfFileList > 100 || this.dataType === 'text' ? 'text' : 'picture'}
          auto-upload={false}
          disabled={this.uploading}
          on-change={this.fileChange}
          on-remove={this.onRemove}
          on-exceed={this.onExceed}
          v-model:file-list={this.fileList}
          ref="uploader"
        >
          <el-button
            disabled={this.uploading || this.$attrs.disabled}
            size="default"
            type="primary"
          >
            选择文件
          </el-button>
          <div slot="tip" class="flex f1 flex-between" style="margin-left: 20px;">
            <div class="upload-tip">
              {this.accept === 'unspecified' ? (
                <span>文件格式不限</span>
              ) : (
                <span>文件格式：{this.acceptFormatStr}</span>
              )}
              {this.acceptSize > 0 && (
                <span>, 单个文件不大于 {this.acceptSizeFormat(this.acceptSize)}, </span>
              )}
              {this.accept === '.jpg,.png,.bmp,.jpeg' && <br />}
              {this.acceptSize > 0 && <span>文件名不支持中文</span>}
            </div>
            {this.showFileCount && (
              <span class="upload-chosen-tip">
                已选择 <span class="highlight-text">{this.lenOfFileList}</span> 个
              </span>
            )}
          </div>
        </el-upload>
      </div>
    );
  },
};
