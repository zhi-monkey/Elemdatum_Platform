<template>
  <div style="height: 100%; width: 100%">
    <a-card class="p">
      <div class="flex flex-row justify-start w-full">
        <div class="p1 w-1/7">
          <h3 style="font-size: 20px">镜像文件所属任务</h3>
          <a-input
            v-model:value="inputValue"
            style="width: 80%; background-color: #303030; font-size: 18px"
            readOnly
          />
        </div>
        <div class="p1 w-4/7">
          <h3 style="font-size: 20px">镜像文件描述</h3>
          <a-input
            placeholder="请输入镜像文件的描述"
            style="width: 80%; background-color: #303030; font-size: 18px"
            v-model:value="description"
          />
        </div>
      </div>

      <a-divider />

      <div class="p2">
        <h3 style="font-size: 20px; margin-bottom: 1%">选择上传方式</h3>
        <a-radio-group button-style="solid">
          <a-button @click="functionSelect1">镜像仓库地址</a-button>
          <a-button @click="functionSelect2">本地文件上传</a-button>
          <a-button @click="functionSelect3" v-if="imageType === '训练'">复用已上传镜像</a-button>
        </a-radio-group>
        <div>
          <a-card v-show="visible1" style="height: 50%">
            <p style="font-size: 20px">输入镜像仓库地址</p>
            <a-row>
              <a-col :span="12">
                <a-input-group>
                  <a-input placeholder="输入镜像仓库地址" style="width: 50%" v-model:value="link" />
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
        <div>
          <a-card v-show="visible3">
            <a-row>
              <a-col :span="4"
                ><span style="margin-right: 10px">算法</span>
                <Select
                  :dropdownMatchSelectWidth="false"
                  placeholder="请选择要复用的算法"
                  style="max-width: 200px"
                  :options="modelList"
                  :onSelect="handleSelectModel"
              /></a-col>
              <a-col :span="4"
                ><span style="margin-right: 10px">镜像</span>
                <Select
                  :dropdownMatchSelectWidth="false"
                  v-model:value="reusedModelVersion"
                  placeholder="请选择要复用的镜像"
                  style="max-width: 200px"
                  :options="modelVersionList"
              /></a-col>
              <a-col :span="4"
                ><a-button type="primary" @click="onClick">上传</a-button></a-col
              ></a-row
            >
          </a-card>
        </div>
      </div>
    </a-card>
    <a-card style="margin-top: 1%; width: 100%">
      <div class="p3">
        <h3 style="font-size: 20px">使用说明</h3>
        <a-typography-paragraph style="margin-left: 20px">
          <ATypographyParagraph style="font-size: medium; margin-top: 10px"
            >算法来源
          </ATypographyParagraph>
          <a-typography-paragraph style="width: 100%">
            <blockquote>{{ functionContent }}</blockquote>
          </a-typography-paragraph>
        </a-typography-paragraph>
        <a-typography-paragraph style="margin-left: 20px">
          <ATypographyParagraph style="font-size: medium; margin-top: 10px"
            >注意
          </ATypographyParagraph>
          <a-typography-paragraph style="width: 100%">
            <blockquote>{{ functionContent1 }}</blockquote>
          </a-typography-paragraph>
        </a-typography-paragraph>
      </div>
    </a-card>
  </div>
</template>
<script lang="ts">
  import {
    Button as AButton,
    InputGroup as AInputGroup,
    Input as AInput,
    Row as ARow,
    Col as ACol,
    Card as ACard,
    RadioGroup as ARadioGroup,
    Divider as ADivider,
    Select,
  } from 'ant-design-vue';
  import { computed, defineComponent, onMounted, reactive, ref } from 'vue';
  import axios from 'axios';
  import SparkMD5 from 'spark-md5/spark-md5';
  import 'vue-simple-uploader/dist/style.css';
  import { TypographyParagraph as ATypographyParagraph } from 'ant-design-vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { CloudUploadOutlined } from '@ant-design/icons-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  // import { onBeforeRouteLeave } from 'vue-router';

  export default defineComponent({
    components: {
      ARadioGroup,
      ATypographyParagraph,
      AButton,
      CloudUploadOutlined,
      AInputGroup,
      ARow,
      ACol,
      ACard,
      AInput,
      ADivider,
      Select,
    },
    props: { generationName: String, generationId: Number, imageType: String },
    emits: ['next'],
    setup(props, { emit }) {
      // const { createMessage, createWarningModal } = useMessage();
      // const uploadSucceed = ref(false);

      const { createMessage } = useMessage();

      const inputValue = computed(() => props.generationName);
      const filePath = ref('');
      if (props.generationName != null && props.generationId != null) {
        filePath.value = 'generation' + props.generationId;
      }

      const skip = ref<boolean>(false);

      const checked = ref<boolean>(false);

      const link = ref();
      const tag = ref('');
      const description = ref('');

      const visible1 = ref<boolean>(true);
      const visible2 = ref<boolean>(false);
      const visible3 = ref<boolean>(false);

      const reusedModelVersion = ref();
      interface item {
        value: number;
        label: string;
      }
      const modelList = ref([] as item[]);
      const modelVersionList = ref([] as item[]);

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

      //三选一功能
      const functionSelect1 = () => {
        visible1.value = true;
        visible2.value = false;
        visible3.value = false;
      };

      const functionSelect2 = () => {
        visible1.value = false;
        visible2.value = true;
        visible3.value = false;
      };

      const functionSelect3 = () => {
        visible1.value = false;
        visible2.value = false;
        visible3.value = true;
      };

      onMounted(() => {
        getModelData();
      });

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
      const fileComplete = (_rootFile) => {
        // 一个根文件（文件夹）成功上传完成。
      };
      const complete = () => {
        // 上传完毕。
      };
      const fileAdded = (file) => {
        //校验文件后缀是否为.tar
        const allowedExtensions = /(\.tar)$/i;
        if (!allowedExtensions.exec(file.name)) {
          alert('仅支持上传 tar 格式的文件');
          file.cancel();
        }
        if (tag.value === '') {
          createMessage.error('请填写镜像版本号（Tag）');
          return false;
        }
        // @ts-ignore
        uploadRef.fileList.push(file);
        console.log(uploadRef.fileList);
        computeMD5(file);
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
            console.log(
              `第${currentChunk}分片解析完成, 开始第${currentChunk + 1} / ${chunks}分片解析`,
            );
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
      const allStart = () => {
        console.log(uploadRef.fileList);
        uploadRef.fileList.map((e) => {
          // @ts-ignore
          if (e.paused) {
            // @ts-ignore
            e.resume();
          }
        });
      };
      const allStop = () => {
        console.log(uploadRef.fileList);
        uploadRef.fileList.map((e) => {
          // @ts-ignore
          if (!e.paused) {
            // @ts-ignore
            e.pause();
          }
        });
      };

      const allRemove = () => {
        uploadRef.fileList.map((e) => {
          // @ts-ignore
          e.cancel();
        });
        uploadRef.fileList = [];
      };

      function onChange(e) {
        filePath.value = e.toString();
      }

      //通过Url上传镜像
      function mirrorUpload() {
        maHttp
          .get(
            {
              url: 'modelVersion/addUrlModelVersion',
              params: {
                url: link.value,
                generationName: props.generationName,
                englishName: filePath.value,
                description: description.value,
                imageType: props.imageType,
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
            (modelVersion) => {
              createMessage.success('上传至服务器成功！');
              if (modelVersion) {
                if (props.imageType === '质检') {
                  console.log(props.imageType === '质检');
                  emit('next', {
                    modelVersion: modelVersion,
                    jobType: 2,
                  });
                } else {
                  console.log(props.imageType === '质检');
                  console.log(props.imageType == '质检');
                  emit('next', {
                    modelVersion: modelVersion,
                    jobType: 1,
                  });
                }
              }
            },
            () => {
              createMessage.error('上传至服务器失败！');
            },
          );
      }

      //本地镜像上传接口
      function localityUpload(fileName) {
        maHttp
          .get(
            {
              url: '/modelVersion/addLocalityModelVersion',
              params: {
                generationName: props.generationName,
                englishName: filePath.value,
                fileName: fileName,
                description: description.value,
                imageType: props.imageType,
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
            (modelVersion) => {
              createMessage.success('镜像上传成功');
              if (modelVersion) {
                if (props.imageType === '质检') {
                  emit('next', {
                    modelVersion: modelVersion,
                    jobType: 2,
                  });
                } else {
                  emit('next', {
                    modelVersion: modelVersion,
                    jobType: 1,
                  });
                }
              }
            },
            () => {
              createMessage.error('镜像上传失败！');
            },
          );
      }

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

      function handleSelectModel(e: number) {
        maHttp
          .get(
            {
              url: 'modelVersion/getModelVersionNameByModel',
              params: { modelId: e },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((v) => {
            modelVersionList.value = [];
            for (let key in v) {
              modelVersionList.value.push({ value: Number(key), label: v[key] });
            }
          });
      }

      function onClick() {
        // maHttp
        //   .get(
        //     {
        //       url: 'modelVersion/addReusedModelVersion',
        //       params: {
        //         modelVersionId: reusedModelVersion.value,
        //         modelEnglishName: props.modelEnglishName,
        //         showName: props.modelEnglishName + '_' + tag.value,
        //         description: description.value,
        //         imageType: props.imageType,
        //       },
        //       headers: {
        //         // @ts-ignore
        //         ignoreCancelToken: true,
        //       },
        //     },
        //     { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        //   )
        //   .then((modelVersion: any) => {
        //     createMessage.success('镜像上传成功');
        //     emit('next', {
        //       modelVersion: modelVersion,
        //       jobType: 1,
        //     });
        //   })
        //   .catch(() => {
        //     createMessage.error('镜像上传失败！');
        //   });
      }
      // //离开确认弹窗（存在bug）
      // onBeforeRouteLeave((_to, _from, next) => {
      //   if (uploadSucceed.value) {
      //     next(true);
      //   } else {
      //     next(false);
      //     createWarningModal({ title: '警告', content: '镜像文件尚未上传完毕！' });
      //   }
      // });

      return {
        inputValue,
        uploadRef,
        onChange,
        fileSuccess,
        fileComplete,
        complete,
        fileAdded,
        allStart,
        allStop,
        allRemove,
        functionContent: '镜像仓库地址上传 ; 本地文件上传 ; 现有模型库中选择复用',
        functionContent1:
          '训练时上述三个方法只能三选一 ; 部署时只可在镜像仓库地址和本地文件上传方法中二选一',
        checked,
        functionSelect1,
        functionSelect2,
        functionSelect3,
        visible1,
        visible2,
        visible3,
        reusedModelVersion,
        link,
        tag,
        description,
        modelList,
        modelVersionList,
        mirrorUpload,
        localityUpload,
        handleSelectModel,
        onClick,
        resetForm: (values: any) => {
          createMessage.success('表单值: ' + JSON.stringify(values));
        },
      };
    },
  });
</script>

<style scoped>
  .p {
    width: 100%;
  }

  .p1 {
    margin-top: 1%;
    margin-bottom: 2%;
  }

  .p2 {
    margin-bottom: 3%;
  }

  .p3 {
    margin-top: 3%;
    margin-bottom: 3%;
  }

  .uploader-example .uploader-list {
    max-height: 440px;
    margin-top: 4px;
    border-radius: 2px;
    overflow: auto;
    overflow-x: hidden;
  }

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
</style>
