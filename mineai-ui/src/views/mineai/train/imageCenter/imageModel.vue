<template>
  <BasicModal
    v-bind="$attrs"
    :title="getTitle"
    width="37%"
    @register="registerModal"
    @cancel="handleCancel"
    @visible-change="handleVisibleChange"
  >
    <card style="min-height: 700px">
      <div class="p1" style="display: flex; flex-direction: column; gap: 10px">
        <div style="display: flex; align-items: center; gap: 10px">
          <h3 style="font-size: 16px; margin-bottom: 0; width: 20%">镜像名称</h3>
          <div style="width: 70%">
            <a-input
              placeholder="请输入镜像名称"
              style="width: 100%; background-color: #303030; font-size: 16px"
              v-model:value="showName"
              @keydown="handleShowNameKeydown"
              @input="validateShowNameInput"
              :status="showNameError ? 'error' : ''"
            />
            <div v-if="showNameError" style="color: #ff4d4f; font-size: 16px; margin-top: 4px">
              {{ showNameError }}
            </div>
          </div>
        </div>
        <div style="display: flex; align-items: center; gap: 10px">
          <h3 style="font-size: 16px; margin-bottom: 0; width: 20%">镜像版本号</h3>
          <a-input
            placeholder="请输入镜像版本号"
            style="width: 70%; background-color: #303030; font-size: 16px"
            v-model:value="level"
            @keydown.space.prevent
            @change="level = level.replace(/\s/g, '')"
          />
        </div>
        <div style="display: flex; align-items: center; gap: 10px">
          <h3 style="font-size: 16px; margin-bottom: 0; width: 20%">镜像用途</h3>
          <!--          <radio-group v-model:value="use" style="width: 70%; font-size: 16px" button-style="solid">-->
          <!--            <radio-button v-if="!isUpdate || use === '训练'" value="训练" style="margin-right: 8px"-->
          <!--              >训练-->
          <!--            </radio-button>-->
          <!--            <radio-button v-if="!isUpdate || use === '转换'" value="转换" style="margin-right: 8px"-->
          <!--              >转换-->
          <!--            </radio-button>-->
          <!--            <radio-button v-if="!isUpdate || use === '自动标注'" value="自动标注">-->
          <!--              自动标注-->
          <!--            </radio-button>-->
          <!--          </radio-group>-->
          <Tag.CheckableTag
            :checked="selectedUses.includes('训练')"
            @change="handleTagChange('训练')"
            style="margin-right: 8px; font-size: 16px"
            v-if="!isUpdate || selectedUses.includes('训练')"
          >
            训练
          </Tag.CheckableTag>
          <Tag.CheckableTag
            :checked="selectedUses.includes('转换')"
            @change="handleTagChange('转换')"
            style="margin-right: 8px; font-size: 16px"
            v-if="!isUpdate || selectedUses.includes('转换')"
          >
            转换
          </Tag.CheckableTag>
          <Tag.CheckableTag
            :checked="selectedUses.includes('自动标注')"
            @change="handleTagChange('自动标注')"
            style="font-size: 16px"
            v-if="!isUpdate || selectedUses.includes('自动标注')"
          >
            自动标注
          </Tag.CheckableTag>
        </div>
        <div style="display: flex; align-items: center; gap: 10px">
          <h3 style="font-size: 16px; margin-bottom: 0; width: 20%">镜像描述</h3>
          <a-input
            placeholder="请输入镜像描述"
            style="width: 70%; background-color: #303030; font-size: 16px"
            v-model:value="description"
            @keydown.space.prevent
            @change="description = description.replace(/\s/g, '')"
          />
        </div>
        <div
          v-if="selectedUses.includes('转换')"
          style="display: flex; align-items: center; gap: 10px"
        >
          <h3 style="font-size: 16px; margin-bottom: 0; width: 20%">算力芯片</h3>
          <a-select
            placeholder="请选择算力芯片类型"
            style="width: 70%; background-color: #303030; font-size: 16px"
            v-model:value="chipType"
            :options="chipOptions"
          />
        </div>
      </div>
      <div class="p2" v-show="getTitle != '镜像编辑'">
        <h3 style="font-size: 16px; margin-bottom: 1%">选择上传方式</h3>
        <radio-group button-style="solid" v-model:value="type">
          <radio-button value="url上传" @click="functionSelect1">镜像仓库地址</radio-button>
          <radio-button value="本地上传" @click="functionSelect2" v-show="!isUpdate"
            >本地文件上传
          </radio-button>
        </radio-group>
        <div>
          <a-card v-if="visible1 && type === 'url上传'" style="height: 50%">
            <p style="font-size: 18px">输入镜像仓库地址</p>
            <a-row>
              <a-col :span="24">
                <a-input-group>
                  <a-input
                    placeholder="输入镜像仓库地址"
                    style="width: 100%"
                    v-model:value="link"
                  />
                  <!--                  <a-button type="primary" :onClick="mirrorUpload">-->
                  <!--                    <template #icon>-->
                  <!--                      <CloudUploadOutlined />-->
                  <!--                    </template>-->
                  <!--                  </a-button>-->
                </a-input-group>
              </a-col>
            </a-row>
          </a-card>
        </div>
        <div>
          <a-card v-show="isFileListShow" style="height: 50%">
            <div>
              <uploader
                :autoStart="false"
                :options="uploadRef.options"
                :file-status-text="uploadRef.statusText"
                class="uploader-example"
                @file-complete="fileComplete"
                @complete="complete"
                @file-success="getResponse"
                @file-added="fileAdded"
                ref="uploaderInstance"
              >
                <uploader-unsupport />
                <uploader-drop>
                  <uploader-btn class="uploader-btn ant-btn ant-btn-primary" type="button"
                    >选择文件
                  </uploader-btn>
                </uploader-drop>
                <uploader-list />
              </uploader>
              <br />
              <a-button
                type="primary"
                @click="allRemove()"
                style="margin-left: 2%; margin-bottom: 1%; margin-top: 1%"
                >清除已上传列表
              </a-button>
            </div>
          </a-card>
          <a-card v-show="isShowLocalUploadTip"> 请填写镜像信息之后再进行文件选择</a-card>
        </div>
      </div>
    </card>
    <template #footer>
      <a-button type="primary" @click="closeImageModal">关闭</a-button>
      <a-button
        type="primary"
        @click="mirrorUpload"
        v-show="type === 'url上传'"
        :loading="urlUploading"
        >确认上传
      </a-button>
      <a-button
        type="primary"
        @click="localUpload"
        v-show="type === '本地上传'"
        :loading="localUploadLoading || uploading"
        >确认上传
      </a-button>
    </template>
  </BasicModal>
</template>
<script lang="ts" setup>
  import {
    Button as AButton,
    Card as ACard,
    Col as ACol,
    Input as AInput,
    InputGroup as AInputGroup,
    message,
    Radio,
    Row as ARow,
    Select as ASelect,
    Tag,
  } from 'ant-design-vue';
  import { computed, defineEmits, onMounted, onUnmounted, reactive, ref, unref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import 'vue-simple-uploader/dist/style.css';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import axios from 'axios';
  import SparkMD5 from 'spark-md5/spark-md5';
  import { useUserStore } from '/@/store/modules/user';
  import { findAllChips } from './api';

  const chipType = ref(''); // 绑定下拉框的值
  const chipOptions = ref([]); // 用于存储下拉框选项
  const RadioGroup = Radio.Group;
  const RadioButton = Radio.Button;
  const userStore = useUserStore();
  const userData = userStore.getUserInfo;
  const visible1 = ref<boolean>(true);
  const visible2 = ref<boolean>(false);
  const skip = ref<boolean>(false);
  const link = ref('');
  const uploadLinkURL = ref('modelVersion/createUrlImage');
  const uploadLocal = ref('modelVersion/createLocalImage');
  const inputValue = ref('');
  const showName = ref('');
  const level = ref('');
  const description = ref('');
  const selectedUses = ref([]);
  const checked = ref<boolean>(false);
  const isUpdate = ref(false);
  const emits = defineEmits(['success']);
  const mdId = ref(0);
  const type = ref('url上传');
  const localUploadLoading = ref(false);
  const urlUploading = ref(false);
  const uploading = ref(false);
  const uploaderInstance = ref(null); // 关键：获取 uploader 组件实例
  import { notification as ANotification } from 'ant-design-vue';

  // 新增：镜像名称错误提示
  const showNameError = ref('');

  const { createMessage } = useMessage();
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false, showCancelBtn: false, showOkBtn: false });
    visible1.value = true;
    visible2.value = false;
    type.value = 'url上传';
    inputValue.value = data.inputValue;
    isUpdate.value = data.isUpdate;
    description.value = '';
    showName.value = '';
    chipType.value = '';
    level.value = '';
    selectedUses.value = [];
    link.value = '';
    mdId.value = 0;
    showNameError.value = ''; // 重置错误提示
    uploadLinkURL.value = 'modelVersion/createUrlImage';
    uploadLocal.value = 'modelVersion/createLocalImage';
    if (isUpdate.value) {
      uploadLinkURL.value = 'modelVersion/updateUrlImage';
      link.value = '';
      mdId.value = data.record.id;
      description.value = data.record.description;
      showName.value = data.record.showName;
      chipType.value = data.record.chipType;
      level.value = data.record.level;
      // 一会再改传参的事
      if (data.record.trainable) {
        selectedUses.value.push('训练');
      }
      if (data.record.inspectable) {
        selectedUses.value.push('转换');
      }
      if (data.record.autoLabel) {
        selectedUses.value.push('自动标注');
      }
    }
  });

  // 镜像名称输入验证函数，结尾连字符留给验证
  const isValidShowName = (value: string): boolean => {
    const pattern = /^(?!-)[a-z0-9-]+$/;
    return pattern.test(value);
  };

  // 镜像名称键盘输入拦截
  const handleShowNameKeydown = (event: KeyboardEvent) => {
    const key = event.key;

    // 允许功能键
    const allowedKeys = [
      'Backspace',
      'Delete',
      'Tab',
      'Escape',
      'Enter',
      'Home',
      'End',
      'ArrowLeft',
      'ArrowRight',
      'ArrowUp',
      'ArrowDown',
    ];

    if (allowedKeys.includes(key)) {
      showNameError.value = '';
      return;
    }

    // 允许控制键组合
    if (event.ctrlKey || event.metaKey) return;

    // 阻止空格
    if (key === ' ') {
      event.preventDefault();
      showNameError.value = '镜像名称不允许包含空格';
      return;
    }

    // 验证
    const input = event.target as HTMLInputElement;
    const cursorPosition = input.selectionStart || 0;
    const currentValue = showName.value;

    const newValue =
      currentValue.substring(0, cursorPosition) +
      key +
      currentValue.substring(input.selectionEnd || 0);

    if (!isValidShowName(newValue)) {
      event.preventDefault();

      // 根据具体错误给出提示
      if (key === '_') {
        showNameError.value = '不支持下划线，请使用连字符（-）';
      } else if (key === '-' && cursorPosition === 0) {
        showNameError.value = '不能以连字符（-）开头';
      } else {
        showNameError.value = `不允许输入此字符，仅支持小写字母、数字、连字符（-）`;
      }
      return;
    }

    showNameError.value = '';
  };

  // 新增：镜像名称输入实时验证
  const validateShowNameInput = () => {
    if (!showName.value) {
      showNameError.value = '';
      return;
    }

    if (!isValidShowName(showName.value)) {
      showNameError.value = '不允许输入此字符，仅支持小写字母、数字、连字符（-）';
    } else {
      showNameError.value = '';
    }
  };

  // 是否展示未完全填写镜像信息的提示
  const isShowLocalUploadTip = computed(() => {
    if (selectedUses.value.includes('转换')) {
      return (
        visible2.value &&
        (!showName.value ||
          !level.value ||
          !selectedUses.value.length ||
          !description.value ||
          !chipType.value ||
          chipType.value.length === 0) &&
        !isUpdate.value &&
        !currentFile.value
      );
    } else if (type.value === '本地上传')
      return (
        visible2.value &&
        type.value === '本地上传' &&
        (!showName.value || !level.value || !selectedUses.value.length || !description.value) &&
        !isUpdate.value &&
        !currentFile.value
      );
    else return false;
  });
  // 是否展示文件列表
  const isFileListShow = computed(() => {
    if (selectedUses.value.includes('转换')) {
      return visible2.value && type.value === '本地上传' && currentFile.value
        ? true
        : visible2.value &&
            type.value === '本地上传' &&
            !!showName.value &&
            !!level.value &&
            !!selectedUses.value.length &&
            !!description.value &&
            !!chipType.value;
    } else
      return visible2.value && type.value === '本地上传' && currentFile.value
        ? true
        : visible2.value &&
            type.value === '本地上传' &&
            !!showName.value &&
            !!level.value &&
            !!selectedUses.value.length &&
            !!description.value;
  });

  const handleTagChange = (tag) => {
    if (isUpdate.value) {
      // 用户编辑状态不让修改
      return;
    }
    if (tag === '转换') {
      // 如果“转换”已被选中，再次点击将其取消选择
      if (selectedUses.value.includes('转换')) {
        selectedUses.value = [];
      } else {
        // 选择“转换”将取消其他所有选项
        selectedUses.value = ['转换'];
      }
    } else {
      const index = selectedUses.value.indexOf(tag);

      if (selectedUses.value.includes('转换')) {
        // 如果当前选择了"转换"，选择其他选项时取消"转换"
        selectedUses.value = [tag];
      } else {
        if (index > -1) {
          // 移除已选中的标签
          selectedUses.value.splice(index, 1);
        } else {
          // 添加新的标记
          selectedUses.value.push(tag);
        }
      }
    }
  };

  const closeImageModal = () => {
    closeModal();
    allRemove();
  };
  const getTitle = computed(() => (!unref(isUpdate) ? '镜像上传' : '镜像编辑'));

  //三选一功能
  const functionSelect1 = () => {
    visible1.value = true;
    visible2.value = false;
  };

  const functionSelect2 = () => {
    visible1.value = false;
    visible2.value = true;
  };

  const fileComplete = (_rootFile) => {
    // 一个根文件（文件夹）成功上传完成。
    uploading.value = false;
  };

  const currentFile = ref(null);
  const fileAdded = (file) => {
    try {
      uploaderInstance.value = file;
      // 拦截逻辑：如果已有文件，直接移除新文件并提示
      if (uploadRef.fileList.length > 0) {
        uploaderInstance.value.removeFile(file); // 核心：立即移除多余文件
        message.error('只能上传一个文件！');
        return false; // 阻止后续流程
      }

      currentFile.value = file;
      // @ts-ignore
      uploadRef.fileList.push(file);
      // 先不上传,点击之后上传
      uploading.value = true;
      computeMD5(file);
      //message.success('文件添加成功');
      return true;
    } catch (e) {
      console.log(e);
      return false;
    }
  };

  let fileSuccessResponse = ref(null);
  let fileSuccessChunk = ref(null);
  const getResponse = (_rootFile, file, response, chunk) => {
    fileSuccessResponse.value = response;
    fileSuccessChunk.value = chunk;
  };

  const localUpload = () => {
    //校验文件后缀是否为.tar
    const allowedExtensions = /(\.tar)$/i;
    // 不允许_- 开头和结尾，但允许中间有_-
    const allowedImageName = /^(?!-)[a-z0-9-]+(?<!-)$/;
    if (!allowedImageName.exec(showName.value)) {
      return createMessage.error('镜像名称格式不符合规范, 请重新填写！');
    }
    if (!selectedUses.value.length) {
      return createMessage.error('请选择要镜像用途');
    }
    if (!currentFile.value) {
      return createMessage.error('请选择要上传的文件');
    }
    if (!allowedExtensions.exec(currentFile.value.name)) {
      return createMessage.error('仅支持上传 tar 格式的文件');
    }
    if (level.value === '') {
      return createMessage.error('请填写镜像版本号（Tag）');
    }
    // 只考虑单文件上传
    // localityUpload(uploadRef.fileList[0]);
    try {
      fileSuccess(null, uploadRef.fileList[0], fileSuccessResponse.value, fileSuccessChunk.value);
    } catch (e) {
      createMessage.error(e.message);
      return;
    }
    createMessage.success('上传成功，后面步骤会自动执行，请不要刷新页面');
    localUploadLoading.value = true;
  };

  const complete = () => {
    // 上传完毕。
  };

  const fileSuccess = (_rootFile, file, response, chunk) => {
    const result = JSON.parse(response);
    console.log(result.success, skip.value, checked.value);
    console.log('file upload result', result);
    if (result.success && !skip.value) {
      const HTTP_AXIOS = axios.create();
      HTTP_AXIOS({
        method: 'post',
        url: '/mm/upload/merge',
        data: {
          identifier: file.uniqueIdentifier,
          filename: showName.value + '_' + level.value,
          totalChunks: chunk.offset,
          filePath: showName.value + '_' + level.value,
          zip: checked.value,
        },
        timeout: 1024000000,
      })
        .then((res) => {
          if (res.data.success) {
            createMessage.success('分块上传成功');
            localityUpload(file);
          } else {
            createMessage.error('分块上传失败');
            console.log(res);
          }
        })
        .catch((error) => {
          if (error.config.timeout == 1024000000) {
            console.error('请求超时，请检查网络');
            localUploadLoading.value = false;
          } else {
            console.error(error);
          }
        });
    } else {
      createMessage.success('上传成功，不需要合并');
      localUploadLoading.value = false;
    }
    if (skip.value) {
      skip.value = false;
    }
  };
  let currentChunk = ref(0);
  let chunks = ref(0);
  const computeMD5 = (file) => {
    let fileReader = new FileReader();
    let time = new Date().getTime();
    let blobSlice =
      // @ts-ignore
      File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice;
    const chunkSize = 10485760;
    chunks = Math.ceil(file.size / chunkSize);
    let spark = new SparkMD5.ArrayBuffer();
    // 文件状态设为"计算MD5"
    file.cmd5 = true; //文件状态为"计算md5..."
    file.pause();
    loadNext();
    fileReader.onload = (e) => {
      // @ts-ignore
      spark.append(e.target.result);
      if (currentChunk.value < chunks.value) {
        currentChunk.value++;
        loadNext();
        // 实时展示MD5的计算进度
        console.log(
          `第${currentChunk.value}分片解析完成, 开始第${currentChunk.value + 1} / ${
            chunks.value
          }分片解析`,
        );
      } else {
        let md5 = spark.end();
        console.log(
          `MD5计算完毕：${file.name} \nMD5：${md5} \n分片：${chunks.value} 大小:${
            file.size
          } 用时：${new Date().getTime() - time} ms`,
        );
        spark.destroy(); //释放缓存
        file.uniqueIdentifier = md5; //将文件md5赋值给文件唯一标识
        file.cmd5 = false; //取消计算md5状态
        file.resume(); //开始上传
      }
    };
    fileReader.onerror = function () {
      console.log(`文件${file.name}读取出错，请检查该文件`);
      localUploadLoading.value = false;
      file.cancel();
    };

    function loadNext() {
      let start = currentChunk.value * chunkSize;
      let end = start + chunkSize >= file.size ? file.size : start + chunkSize;
      fileReader.readAsArrayBuffer(blobSlice.call(file.file, start, end));
    }
  };

  const allRemove = () => {
    uploadRef.fileList.map((e) => {
      // @ts-ignore
      e.cancel();
    });
    uploadRef.fileList = [];
    currentFile.value = null;
  };

  const uploadRef = reactive({
    options: {
      target: '/mm/upload/chunk',
      // 开启服务端username分片校验功能
      chunkSize: '10485760',
      testChunks: true,
      //maxFiles: 1, // 核心配置：限制最大文件数为1
      parseTimeRemaining: function (_timeRemaining, parsedTimeRemaining) {
        return parsedTimeRemaining
          .replace(/\syears?/, '年')
          .replace(/\days?/, '天')
          .replace(/\shours?/, '小时')
          .replace(/\sminutes?/, '分钟')
          .replace(/\sseconds?/, '秒');
      },
      // 服务器分片校验函数
      checkChunkUploadedByResponse: (chunk, message) => {
        const result = JSON.parse(message);
        if (result.data.skipUpload) {
          skip.value = true;
          return true;
        }
        return (result.data.uploaded || []).indexOf(chunk.offset + 1) >= 0;
      },
    },
    attrs: {
      accept: 'image/*',
    },
    statusText: {
      success: '上传成功',
      error: '上传出错了',
      uploading: '上传中...',
      paused: '暂停中...',
      waiting: '等待中...',
      cmd5: '计算文件MD5中...',
    },
    fileList: [],
    disabled: true,
  });

  const handleCancel = () => {
    closeImageModal();
  };

  const handleVisibleChange = (visible) => {
    if (!visible) {
      closeImageModal();
    }
  };

  //通过Url上传镜像
  function mirrorUpload() {
    if (showName.value.length === 0) {
      createMessage.error('请填写镜像名称');
      return false;
    }
    if (!isShowNameValid(showName.value)) {
      // 检查是否以连字符结尾
      if (showName.value.endsWith('-')) {
        createMessage.error('镜像名称不能以连字符（-）结尾');
      } else {
        createMessage.error('镜像名称格式不符合要求，仅支持小写字母、数字、连字符');
      }
      return false;
    }
    if (level.value.length === 0) {
      createMessage.error('请填写镜像版本号');
      return false;
    }
    if (!isLevelValid(level.value)) {
      createMessage.error('镜像版本号的格式不正确，仅支持字母、数字、英文横杠、英文句号和下划线');
      return false;
    }
    if (description.value.length === 0) {
      createMessage.error('请填写镜像的描述信息');
      return false;
    }
    if (description.value.length > 100) {
      createMessage.error('镜像的描述信息长度不能超过100');
      return false;
    }
    if (!selectedUses.value.length) {
      createMessage.error('请选择镜像用途');
      return false;
    }
    if (!isUpdate.value) {
      if (link.value.length === 0) {
        createMessage.error('请填写镜像的URL地址');
        return false;
      }
    }
    // 判断是否选择了“转换”用途，并确保选择了算力芯片
    if (selectedUses.value.includes('转换') && (!chipType.value || chipType.value.length === 0)) {
      createMessage.error('请选择相应的算力芯片');
      return false;
    }
    urlUploading.value = true;
    maHttp
      .get(
        {
          url: uploadLinkURL.value,
          params: {
            userId: userData.id,
            mdId: mdId.value,
            url: link.value,
            description: description.value,
            showName: showName.value,
            chipType: chipType.value,
            level: level.value,
            use: selectedUses.value.join(','),
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
          timeout: 1024000000,
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((res) => {
        if (!isUpdate.value && (!res.isSucceed || !!res.errMsg)) {
          ANotification.error({
            message: '镜像上传失败',
            description: res.errMsg ? res.errMsg : '未知原因',
            duration: null,
          });
          urlUploading.value = false;
          return;
        }
        urlUploading.value = false;
        closeModal();
        emits('success');
      })
      .catch((err) => {
        urlUploading.value = false;
        console.error(err);
      });
  }

  //本地镜像上传接口
  function localityUpload(file) {
    if (showName.value.length === 0) {
      createMessage.error('请填写镜像名称');
      localUploadLoading.value = false;
      return false;
    }
    if (!isShowNameValid(showName.value)) {
      createMessage.error('镜像名称不符合要求，请重新填写');
      localUploadLoading.value = false;
      return false;
    }
    if (level.value.length === 0) {
      createMessage.error('请填写镜像版本号');
      localUploadLoading.value = false;
      return false;
    }
    if (!isLevelValid(level.value)) {
      createMessage.error('镜像版本号的格式不正确，仅支持字母、数字、英文横杠、英文句号和下划线');
      localUploadLoading.value = false;
      return false;
    }
    if (description.value.length === 0) {
      createMessage.error('请填写镜像的描述信息');
      localUploadLoading.value = false;
      return false;
    }
    if (description.value.length > 100) {
      createMessage.error('镜像的描述信息长度不能超过100');
      localUploadLoading.value = false;
      return false;
    }
    if (!selectedUses.value.length) {
      createMessage.error('请选择镜像用途');
      localUploadLoading.value = false;
      return false;
    }
    // 判断是否选择了"转换"用途，并确保选择了算力芯片
    if (selectedUses.value.includes('转换') && (!chipType.value || chipType.value.length === 0)) {
      createMessage.error('请选择相应的算力芯片');
      localUploadLoading.value = false;
      return false;
    }
    maHttp
      .get(
        {
          url: uploadLocal.value,
          params: {
            userId: userData.id,
            fileName: showName.value + '_' + level.value,
            description: description.value,
            showName: showName.value,
            chipType: chipType.value,
            level: level.value,
            // 将数组转为字符串 ["训练", "自动标注"] -> "训练,自动标注"
            use: selectedUses.value.join(','),
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: false,
          },
          timeout: 1024000000,
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(
        () => {
          localUploadLoading.value = false;
          closeModal();
          emits('success');
          createMessage.success('镜像上传成功');
          file.cancel();
        },
        () => {
          localUploadLoading.value = false;
          closeModal();
          createMessage.error('镜像上传失败！');
          file.cancel();
        },
      );
  }

  function isLevelValid(level) {
    const pattern = /^[a-zA-Z0-9-._]+$/;
    return pattern.test(level);
  }

  function isShowNameValid(showName) {
    const pattern = /^(?!-)[a-z0-9-]+(?<!-)$/;
    return pattern.test(showName);
  }

  onMounted(async () => {
    try {
      const response = await findAllChips(); // 调用 API 获取所有算力芯片
      if (response && Array.isArray(response)) {
        chipOptions.value = response.map((chip: any) => ({
          label: chip.chipType, // 用 chipType 作为下拉框显示的文本
          value: chip.chipType, //作为选中时的值
        }));
      } else {
        console.error('Invalid response format', response);
      }
    } catch (error) {
      console.error('Error fetching chips:', error);
    }
  });

  onUnmounted(async () => {
    allRemove();
  });
</script>
<style scoped>
  /*上传文件的样式*/
  .uploader-list :deep(.uploader-file) {
    background-color: #434960;
    border-bottom: 1px solid #181d31;
  }

  .uploader-list :deep(.uploader-file-progress) {
    background-color: #0960bd;
  }

  .uploader-list :deep(.uploader-file-actions) {
    display: none;
  }

  .uploader-list :deep(.uploader-file-info) {
    display: flex; /* 使用弹性布局 */
    justify-content: space-between; /* 文件名和其他信息分布两边 */
    align-items: center; /* 垂直居中 */
  }

  .uploader-list :deep(.uploader-file-size) {
    //margin-left: auto;
    margin-right: auto;
  }

  .uploader-list :deep(.uploader-file-status) {
    //margin-left: auto;
    text-align: left;
    margin-right: auto;
  }

  .uploader-list :deep(.uploader-file-meta) {
    display: none;
  }

  .uploader-list :deep(.uploader-file-status span) {
    max-width: 100%; /* 避免内容超出容器 */
    white-space: nowrap; /* 确保内容不换行 */
  }

  .uploader-list :deep(.uploader-file-status em) {
    max-width: 100%;
  }

  .uploader-list :deep(.uploader-file-status i) {
    //display: inline-block;
    width: auto;
    max-width: 100%;
  }

  .uploader-drop {
    position: relative;
    padding: 10px;
    overflow: hidden;
    border-radius: 4px;
    border: 2px solid #0960bd;
    background-color: #181d31;
  }

  .uploader-btn-prg {
    margin-top: 10px;
    background-color: #181d31;
    height: 50px;
  }

  .p1 {
    width: 100%;
    margin-top: 1%;
    margin-bottom: 2%;
  }
</style>
