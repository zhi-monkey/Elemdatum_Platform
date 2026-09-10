<template>
  <PageWrapper
    :title="`更换训练任务镜像`"
    @back="goBack"
    contentFullHeight
    style="margin: 0 16px 0 16px"
  >
    <a-card class="p">
      <div class="p1">
        <h3 style="font-size: 20px">算法镜像所属任务</h3>
        <a-row>
          <a-col>
            <a-input
              v-model:value="selectedValue"
              style="width: 25%; background-color: #303030; margin-right: 20px; font-size: 24px"
              readOnly
            />
          </a-col>
        </a-row>
      </div>
      <div class="p1"
        ><h3 style="font-size: 20px; margin-bottom: 1%">镜像文件描述</h3>
        <a-input
          placeholder="请输入镜像文件的描述"
          style="width: 25%; background-color: #303030; font-size: 18px"
          v-model:value="description"
        />
      </div>
      <div class="p1"
        ><h3 style="font-size: 20px; margin-bottom: 1%">镜像任务选择(必填)</h3>
        <a-radio-group
          class="checkbox"
          v-model:value="checkbox"
          style="width: 25%"
          :options="plainOptions"
          @change="Change"
        />
      </div>

      <a-divider />

      <div class="p2">
        <h3 style="font-size: 20px; margin-bottom: 1%">选择上传方式</h3>

        <a-radio-group button-style="solid">
          <a-button @click="functionSelect1">镜像仓库地址</a-button>
          <a-button @click="functionSelect2">本地文件上传</a-button>
          <a-button v-show="checkbox === '训练'" @click="functionSelect3">复用已上传镜像</a-button>
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
              <a-col :span="4">
                <a-button type="primary" @click="onClick">上传</a-button>
              </a-col>
            </a-row>
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
  </PageWrapper>
</template>
<script lang="ts">
  import {
    Button as AButton,
    Card as ACard,
    Col as ACol,
    Divider as ADivider,
    Input as AInput,
    InputGroup as AInputGroup,
    RadioGroup as ARadioGroup,
    Row as ARow,
    Select,
    TypographyParagraph as ATypographyParagraph,
  } from 'ant-design-vue';
  import { defineComponent, onMounted, reactive, ref } from 'vue';
  import axios from 'axios';
  import SparkMD5 from 'spark-md5/spark-md5';
  import 'vue-simple-uploader/dist/style.css';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { CloudUploadOutlined } from '@ant-design/icons-vue';
  import { PageWrapper } from '/@/components/Page';
  import { useRoute } from 'vue-router';
  import { useGo } from '/@/hooks/web/usePage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useTabs } from '/@/hooks/web/useTabs';

  export default defineComponent({
    components: {
      Select,
      ARadioGroup,
      ATypographyParagraph,
      AButton,
      CloudUploadOutlined,
      AInputGroup,
      ARow,
      ACol,
      ACard,
      AInput,
      PageWrapper,
      ADivider,
    },
    setup() {
      const { createMessage } = useMessage();
      const { closeCurrent } = useTabs();
      const go = useGo();
      //在这获取模型管理页面传过来的参数
      const route = useRoute();
      // selectedValue 是生产任务的名称 filePath对应的是在服务器中存储的路径（非中文）规则是generation+生产任务id
      const modelGenerationName = route.query.modelGenerationName;
      const modelGenerationId = route.query.modelGenerationId;
      let filePath = ref('');
      let selectedValue = ref('');
      if (modelGenerationName != null && modelGenerationId != null) {
        filePath.value = 'generation' + modelGenerationId.toString();
        selectedValue.value = modelGenerationName.toString();
      }

      const skip = ref<boolean>(false);

      const checked = ref<boolean>(false);

      let link = ref();
      let tag = ref('');
      let checkbox = ref('');
      let description = ref('');

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

      //在这获取模型管理页面传过来的参数
      // const route = useRoute();
      // const modelId = route.params?.modelId;
      //二选一功能
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
          // axios
          //   .post(
          //     '/mm/upload/merge',
          //     {
          //       identifier: file.uniqueIdentifier,
          //       filename: file.name,
          //       totalChunks: chunk.offset,
          //       filePath: filePath.value,
          //       zip: checked.value,
          //     },
          //     { timeout: 1024000000 },
          //   )
          //   .then((res) => {
          //     if (res.data.success) {
          //       console.log('上传成功');
          //       localityUpload(file.name);
          //     } else {
          //       console.log(res);
          //     }
          //   })
          //   .catch(function (error) {
          //     console.log(error);
          //   });
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
        if (checkbox.value.length === 0) {
          createMessage.error('请选择镜像可执行的任务');
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
      // const plainOptions = ['训练', '质检', '推理'];
      const plainOptions = ['训练', '质检', '推理'];
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

      const goBack = () => {
        go('/maTrainingCenter/modelList');
        closeCurrent();
      };

      //通过Url上传镜像
      //selectedValue 是生产任务名称
      function mirrorUpload() {
        if (checkbox.value.length === 0) {
          createMessage.error('请选择镜像可执行的任务');
          return false;
        }
        maHttp
          .get(
            {
              url: 'modelVersion/updateUrlModelVersion',
              params: {
                url: link.value,
                generationName: selectedValue.value,
                englishName: filePath.value,
                description: description.value,
                imageType: checkbox.value,
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
              goBack();
            },
            () => {
              createMessage.error('上传至服务器失败！');
            },
          );
      }

      //本地镜像上传接口
      //这里的filename是本地镜像文件的名称 而modelName则是生产任务的英文名称
      function localityUpload(fileName) {
        maHttp
          .get(
            {
              url: '/modelVersion/addLocalityModelVersion',
              params: {
                generationName: selectedValue.value,
                englishName: filePath.value,
                fileName: fileName,
                description: description.value,
                imageType: checkbox.value,
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
              createMessage.success('镜像上传成功');
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

      //切换任务类型的时候
      function Change(e) {
        if (e.target.value === '推理') {
          functionSelect1();
        }
      }

      /**
       * 复用方式上传的方法
       * */
      function onClick() {
        //   if (checkbox.value.length === 0) {
        //     createMessage.error('请选择镜像可执行的任务');
        //     return false;
        //   }
        //   maHttp
        //     .get(
        //       {
        //         url: 'modelVersion/addReusedModelVersion',
        //         params: {
        //           modelVersionId: reusedModelVersion.value,
        //           modelEnglishName: modelEnglishName,
        //           showName: modelEnglishName + '_' + tag.value,
        //           description: description.value,
        //           imageType: checkbox.value,
        //         },
        //         headers: {
        //           // @ts-ignore
        //           ignoreCancelToken: true,
        //         },
        //       },
        //       { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        //     )
        //     .then(() => {
        //       createMessage.success('镜像复用成功');
        //     });
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
        closeCurrent,
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
        selectedValue,
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
        plainOptions,
        checkbox,
        Change,
        description,
        modelList,
        modelVersionList,
        mirrorUpload,
        localityUpload,
        goBack,
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
    width: 100%;
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

  .checkbox >>> .ant-checkbox {
    background-color: white;
    font-size: 18px;
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
