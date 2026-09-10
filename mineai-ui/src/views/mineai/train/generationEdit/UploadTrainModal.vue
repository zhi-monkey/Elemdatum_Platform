<template>
  <BasicModal v-bind="$attrs" :title="getTitle" @register="registerModal">
    <card>
      <div class="p1"
        ><h3 style="font-size: 20px; margin-bottom: 1%">镜像文件描述</h3>
        <a-input
          placeholder="请输入镜像文件的描述"
          style="width: 70%; background-color: #303030; font-size: 18px"
          v-model:value="description"
      /></div>
      <div class="p2">
        <h3 style="font-size: 18px; margin-bottom: 1%">选择上传方式</h3>
        <a-radio-group button-style="solid">
          <a-button :type="function1ButtonType" @click="functionSelect1">镜像仓库地址</a-button>
          <a-button :type="function2ButtonType" @click="functionSelect2">本地文件上传</a-button>
        </a-radio-group>
        <div>
          <!--          <a-card style="height: 50%">-->
          <a-card v-show="visible1" style="height: 50%">
            <p style="font-size: 18px">输入镜像仓库地址</p>
            <a-row>
              <a-col :span="24">
                <a-input-group>
                  <a-input placeholder="输入镜像仓库地址" style="width: 70%" v-model:value="link" />
                  <a-button type="primary" :onClick="mirrorUpload">
                    <template #icon>
                      <CloudUploadOutlined />
                    </template>
                  </a-button>
                </a-input-group>
              </a-col>
            </a-row>
          </a-card>
        </div>
        <div>
          <a-card v-show="visible2">
            <div>
              <uploader
                :autoStart="false"
                :options="uploadRef.options"
                :file-status-text="uploadRef.statusText"
                class="uploader-example"
                @file-complete="fileComplete"
                @complete="complete"
                @file-success="fileSuccess"
                @file-added="fileAdded"
              >
                <uploader-unsupport />
                <uploader-drop>
                  <uploader-btn class="ant-btn ant-btn-primary" type="button"
                    >选择文件
                  </uploader-btn>
                </uploader-drop>
                <uploader-list />
              </uploader>
              <br />

              <a-button
                type="primary"
                @click="allRemove()"
                style="margin-left: 10px; margin-bottom: 5px"
                >清除已上传列表
              </a-button>
            </div>
          </a-card>
        </div>
      </div>
    </card>
  </BasicModal>
</template>
<script lang="ts" setup>
  import {
    Button as AButton,
    Input as AInput,
    Card as ACard,
    RadioGroup as ARadioGroup,
    Row as ARow,
    Col as ACol,
    InputGroup as AInputGroup,
  } from 'ant-design-vue';
  import { CloudUploadOutlined } from '@ant-design/icons-vue';
  import { ref, reactive, onMounted, computed, unref, defineEmits } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import axios from 'axios';
  import SparkMD5 from 'spark-md5/spark-md5';

  const visible1 = ref<boolean>(true);
  const visible2 = ref<boolean>(false);
  const skip = ref<boolean>(false);
  const link = ref('');
  const uploadLinkURL = ref('modelVersion/addUrlModelVersion');
  const uploadLocal = ref('modelVersion/addLocalityModelVersion');
  const inputValue = ref('');
  const filePath = ref('');
  const description = ref('');
  const checked = ref<boolean>(false);
  const trainAdd = ref<boolean>(true);
  const isUpdate = ref(false);
  const emits = defineEmits(['updateTrain']);

  const { createMessage } = useMessage();
  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    setModalProps({ confirmLoading: false, showCancelBtn: false, showOkBtn: false });
    filePath.value = data.filePath;
    inputValue.value = data.inputValue;
    trainAdd.value = data.trainAdd;
    isUpdate.value = data.isUpdate;
    if (trainAdd.value === false) {
      description.value = data.trainDescription;
      isUpdate.value = true;
      uploadLinkURL.value = 'modelVersion/updateUrlModelVersion';
      uploadLocal.value = 'modelVersion/updateLocalityModelVersion';
    }
  });

  const function1ButtonType = computed(() => (visible1.value ? 'primary' : 'default'));
  const function2ButtonType = computed(() => (visible2.value ? 'primary' : 'default'));
  const getTitle = computed(() => (!unref(isUpdate) ? '训练镜像上传' : '训练镜像编辑'));

  //二选一功能
  const functionSelect1 = () => {
    visible1.value = true;
    visible2.value = false;
  };

  const functionSelect2 = () => {
    visible1.value = false;
    visible2.value = true;
  };

  onMounted(() => {
    getModelData();
  });

  interface item {
    value: number;
    label: string;
  }

  const modelList = ref([] as item[]);

  const fileComplete = (_rootFile) => {
    // 一个根文件（文件夹）成功上传完成。
  };

  async function getModelData() {
    maHttp
      .get(
        {
          url: 'model/getModelNameList',
          params: {},
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then((v) => {
        modelList.value = [];
        for (let key in v) {
          modelList.value.push({ value: Number(key), label: v[key] });
        }
      });
  }

  const fileAdded = (file) => {
    //校验文件后缀是否为.tar
    const allowedExtensions = /(\.tar)$/i;
    if (!allowedExtensions.exec(file.name)) {
      alert('仅支持上传 tar 格式的文件');
      file.cancel();
    }
    // @ts-ignore
    uploadRef.fileList.push(file);
    console.log(uploadRef.fileList);
    computeMD5(file);
  };

  const complete = () => {
    // 上传完毕。
  };

  const fileSuccess = (_rootFile, file, response, chunk) => {
    const result = JSON.parse(response);
    console.log(result.success, skip.value, checked.value);
    if (result.success && !skip.value) {
      const HTTP_AXIOS = axios.create();
      HTTP_AXIOS({
        method: 'post',
        url: '/mm/upload/merge',
        data: {
          identifier: file.uniqueIdentifier,
          filename: file.name,
          totalChunks: chunk.offset,
          filePath: filePath.value,
          zip: checked.value,
        },
        timeout: 1024000000,
      })
        .then((res) => {
          if (res.data.success) {
            console.log('上传至服务器成功');
            localityUpload(file.name);
          } else {
            console.log(res);
          }
        })
        .catch((error) => {
          if (error.config.timeout == 1024000000) {
            console.log('请求超时，请检查网络');
          } else {
            console.log(error);
          }
        });
    } else {
      console.log('上传成功，不需要合并');
    }
    if (skip.value) {
      skip.value = false;
    }
  };

  const computeMD5 = (file) => {
    let fileReader = new FileReader();
    let time = new Date().getTime();
    let blobSlice =
      // @ts-ignore
      File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice;
    let currentChunk = 0;
    const chunkSize = 1048576;
    let chunks = Math.ceil(file.size / chunkSize);
    let spark = new SparkMD5.ArrayBuffer();
    // 文件状态设为"计算MD5"
    file.cmd5 = true; //文件状态为“计算md5...”
    file.pause();
    loadNext();
    fileReader.onload = (e) => {
      // @ts-ignore
      spark.append(e.target.result);
      if (currentChunk < chunks) {
        currentChunk++;
        loadNext();
        // 实时展示MD5的计算进度
        console.log(`第${currentChunk}分片解析完成, 开始第${currentChunk + 1} / ${chunks}分片解析`);
      } else {
        let md5 = spark.end();
        console.log(
          `MD5计算完毕：${file.name} \nMD5：${md5} \n分片：${chunks} 大小:${file.size} 用时：${
            new Date().getTime() - time
          } ms`,
        );
        spark.destroy(); //释放缓存
        file.uniqueIdentifier = md5; //将文件md5赋值给文件唯一标识
        file.cmd5 = false; //取消计算md5状态
        file.resume(); //开始上传
      }
    };
    fileReader.onerror = function () {
      console.log(`文件${file.name}读取出错，请检查该文件`);
      file.cancel();
    };

    function loadNext() {
      let start = currentChunk * chunkSize;
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
  };

  const uploadRef = reactive({
    options: {
      target: '/mm/upload/chunk',
      // 开启服务端username分片校验功能
      testChunks: true,
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

  //通过Url上传镜像
  function mirrorUpload() {
    if (description.value.length === 0) {
      createMessage.error('请填写镜像的描述信息');
      return false;
    }
    if (description.value.length > 10) {
      createMessage.error('镜像的描述信息长度不能超过10');
      return false;
    }
    if (link.value.length === 0) {
      createMessage.error('请填写镜像的URL地址');
      return false;
    }
    maHttp
      .get(
        {
          url: uploadLinkURL.value,
          params: {
            url: link.value,
            generationName: inputValue.value,
            englishName: filePath.value,
            description: description.value,
            imageType: '训练',
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
          timeout: 1024000000,
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(
        () => {
          createMessage.success('上传至服务器成功！');
          emits('updateTrain');
          closeModal();
        },
        () => {
          createMessage.error('上传至服务器失败！');
        },
      );
  }

  //本地镜像上传接口
  function localityUpload(fileName) {
    if (description.value.length === 0) {
      createMessage.error('请填写镜像的描述信息');
      return false;
    }
    if (description.value.length > 10) {
      createMessage.error('镜像的描述信息长度不能超过10');
      return false;
    }
    maHttp
      .get(
        {
          url: uploadLocal.value,
          params: {
            generationName: inputValue.value,
            englishName: filePath.value,
            fileName: fileName,
            description: description.value,
            imageType: '训练',
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
          closeModal();
          emits('updateTrain');
          createMessage.success('镜像上传成功');
        },
        () => {
          createMessage.error('镜像上传失败！');
        },
      );
  }
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
