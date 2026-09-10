<template>
  <div style="height: 100%; width: 100%">
    <a-card title="基础信息编辑">
      <div>
        <BasicForm
          style="width: 50%"
          :schemas="generationSchemas"
          @register="registerInfoData"
          @submit="handleBindInfoSubmit"
        />
      </div>
    </a-card>
    <a-card style="margin-top: 10px" title="数据集编辑">
      <div>
        <BasicForm
          style="width: 50%"
          :schemas="dataSchemas"
          @register="registerData"
          @submit="handleBindDatasetSubmit"
        />
      </div>
    </a-card>
    <a-card style="margin-top: 10px" title="参数配置">
      <div class="flex flex-row justify-around">
        <a-card title="训练参数" style="width: 33%">
          <BasicForm
            style="width: 100%"
            :schemas="trainSchemas"
            @register="registerTrainData"
            @submit="handleBindTrainParams"
          />
        </a-card>
        <a-card title="设置训练信息" style="width: 33%">
          <BasicForm
            style="width: 100%"
            :schemas="trainTimeSchemas"
            @register="registerTime"
            @submit="handleTrainTime"
          />
        </a-card>
        <a-card title="资源配置" style="width: 33%">
          <a-row>
            <a-col :span="4">
              <a-button type="primary" @click="openConfigurationModal()">添加配置</a-button>
            </a-col>
            <a-col :span="20">
              <BasicForm
                :schemas="ResourceSchemas"
                @register="registerResourceData"
                @submit="handleBindResource"
              />
            </a-col>
          </a-row>
        </a-card>
      </div>
    </a-card>
    <UploadTrainModal @register="registerModal" @update-train="handleTrainUpdate" />
    <UploadInspectModal @register="registerModal2" @update-inspect="handleInspectUpdate" />
    <UploadDeployModal @register="registerModal3" @update-deploy="handleDeployUpdate" />
    <ConfigurationModal @register="registerModal4" />
  </div>
</template>
<script lang="ts">
  import { Button as AButton, Card as ACard, Col as ACol, Row as ARow } from 'ant-design-vue';
  import { defineComponent, onMounted, reactive, ref } from 'vue';
  import {
    dataSchemas,
    generationSchemas,
    getDatasetList,
    hyperParamInspectPrefix,
    hyperParamTrainPrefix,
    inspectSchemas,
    ResourceSchemas,
    trainSchemas,
    trainTimeSchemas,
  } from './data';
  import 'vue-simple-uploader/dist/style.css';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useRoute } from 'vue-router';
  import { useModal } from '/@/components/Modal';
  import UploadTrainModal from './UploadTrainModal.vue';
  import UploadInspectModal from './UploadInspectModal.vue';
  import UploadDeployModal from './UploadDeployModal.vue';
  import ConfigurationModal from './ConfigurationModal.vue';

  export default defineComponent({
    components: {
      ARow,
      ACol,
      BasicForm,
      AButton,
      ACard,
      UploadTrainModal,
      UploadInspectModal,
      UploadDeployModal,
      ConfigurationModal,
    },
    emits: ['next'],
    setup() {
      // const { createMessage, createWarningModal } = useMessage();
      // const uploadSucceed = ref(false);
      const route = useRoute();
      const { createMessage } = useMessage();
      const customStyle = 'background:#181d31';
      const activeKey = ref(['1', '2']);

      const inputValue = ref('');
      const filePath = ref('');
      const delayTrainTime = ref(0);
      const maxTrainTime = ref(0);
      if (route.query.modelGenerationId != null)
        filePath.value = 'generation' + route.query.modelGenerationId;

      const [registerModal, { openModal: openTrainModal }] = useModal();
      const [registerModal2, { openModal: openInspectModal }] = useModal();
      const [registerModal3, { openModal: openDeployModal }] = useModal();
      const [registerModal4, { openModal: openConfigurationModal }] = useModal();
      //绑定基础信息
      const [registerInfoData, { setFieldsValue: setInfo, validate: validateInfo }] = useForm({
        labelWidth: 200,
        schemas: generationSchemas,
        showActionButtonGroup: true,
        showResetButton: false,
        submitButtonOptions: {
          text: '提交',
        },
      });
      const [registerTime, { setFieldsValue: setTrainTimeInfo, validate: validateTime }] = useForm({
        schemas: trainTimeSchemas,
        labelWidth: 200,
        showActionButtonGroup: true,
        showResetButton: false,
        submitButtonOptions: {
          text: '提交',
        },
      });

      //绑定数据集
      const [registerData, { setFieldsValue, validate }] = useForm({
        labelWidth: 200,
        schemas: dataSchemas,
        showActionButtonGroup: true,
        showResetButton: false,
        submitButtonOptions: {
          text: '提交',
        },
      });
      //绑定训练参数
      const [
        registerTrainData,
        {
          appendSchemaByField,
          removeSchemaByFiled,
          validate: validateTrain,
          setFieldsValue: setTrainParams,
        },
      ] = useForm({
        labelWidth: 150,
        schemas: trainSchemas,
        showActionButtonGroup: true,
        showResetButton: false,
        submitButtonOptions: {
          text: '提交 ',
        },
      });
      //绑定质检参数
      const [
        registerInspectData,
        {
          appendSchemaByField: appendInspect,
          removeSchemaByFiled: removeInspect,
          validate: validateInspect,
        },
      ] = useForm({
        labelWidth: 150,
        schemas: inspectSchemas,
        showActionButtonGroup: true,
        showResetButton: false,
        submitButtonOptions: {
          text: '提交 ',
        },
      });
      //绑定资源参数
      const [registerResourceData, { setFieldsValue: setResource, validate: validateResource }] =
        useForm({
          labelWidth: 150,
          schemas: ResourceSchemas,
          actionColOptions: { span: 24 },
          showActionButtonGroup: true,
          showResetButton: false,
          submitButtonOptions: {
            text: '提交 ',
          },
        });

      async function handleTrainTime() {
        const trainTimeInfo = await validateTime();
        let trainTimeValue = {};
        trainTimeValue['delayTrainTime'] = trainTimeInfo.delayTrainTime;
        trainTimeValue['maxTrainTime'] = trainTimeInfo.maxTrainTime;
        trainTimeValue['modelGenerationId'] = route.query.modelGenerationId;
        await maHttp
          .post(
            {
              url: 'modelGeneration/saveModelGenerationTrainTime',
              params: {
                trainTimeValue,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then(
            () => {
              createMessage.success('提交训练时间要求参数成功！');
              getPageData(route.query.modelGenerationId);
            },
            () => {
              createMessage.error('提交训练时间要求参数失败！');
            },
          );
      }

      //绑定数据集对应方法
      async function handleBindDatasetSubmit() {
        try {
          const values = await validate();
          await maHttp
            .post(
              {
                url: 'modelGeneration/bindDataset',
                params: {
                  id: values.id,
                  trainDataset: values.trainDataset,
                  testDataset: values.testDataset,
                  valDataset: values.valDataset,
                  datasetSource: values.datasetSource,
                  splitSize: values.splitSize,
                },
                headers: {
                  // @ts-ignore
                  ignoreCancelToken: true,
                },
              },
              { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
            )
            .then(
              () => {
                createMessage.success('更新数据集成功！');
                getPageData(route.query.modelGenerationId);
              },
              () => {
                createMessage.error('更新数据集失败！');
              },
            );
        } catch (error) {
          console.log(error);
        }
      }

      // 绑定基础信息方法
      async function handleBindInfoSubmit() {
        const infoParams = await validateInfo();
        await maHttp
          .post(
            {
              url: 'modelGeneration/updateModelGeneration',
              params: infoParams,
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then(
            () => {
              createMessage.success('更新基础信息成功！');
              getPageData(route.query.modelGenerationId);
            },
            () => {
              createMessage.error('更新基础信息失败！');
            },
          );
      }

      //绑定训练参数对应方法
      async function handleBindTrainParams() {
        const trainParams = await validateTrain();
        let trainValues = {};
        let params = {};
        Object.keys(trainParams).forEach((key) => {
          params[key] = trainParams[key];
        });
        params['MODE_WORKING_MODE'] = '1';
        trainValues['modelGenerationId'] = Number(route.query.modelGenerationId);
        trainValues['params'] = params;
        await maHttp
          .post(
            {
              url: 'modelGeneration/setTrainParams',
              params: trainValues,
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then(
            () => {
              createMessage.success('提交训练参数成功！');
              getPageData(route.query.modelGenerationId);
            },
            () => {
              createMessage.error('提交训练参数失败！');
            },
          );
      }

      // 质检参数保存按钮
      // async function handleBindInspectParams() {
      //   const testParams = await validateInspect();
      //   let testValues = {};
      //   let testJob = { params: {} };
      //   Object.keys(testParams).forEach((key) => {
      //     if (key.length <= hyperParamInspectPrefix.length) {
      //       testJob[key] = testParams[key];
      //     } else {
      //       const prefix = key.slice(0, hyperParamInspectPrefix.length);
      //       const hyperParam = key.slice(hyperParamInspectPrefix.length);
      //       if (prefix === hyperParamInspectPrefix) {
      //         testJob['params'][hyperParam] = testParams[key];
      //       } else {
      //         testJob[key] = testParams[key];
      //       }
      //     }
      //   });
      //   testValues['modelGenerationId'] = Number(route.query.modelGenerationId);
      //   testValues['testJob'] = testJob;
      //   await maHttp
      //     .post(
      //       {
      //         url: 'modelGeneration/editTestJob',
      //         params: testValues,
      //         headers: {
      //           // @ts-ignore
      //           ignoreCancelToken: true,
      //         },
      //       },
      //       { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      //     )
      //     .then(
      //       () => {
      //         createMessage.success('提交质检参数成功！');
      //         getPageData(route.query.modelGenerationId);
      //       },
      //       () => {
      //         createMessage.error('提交质检参数失败！');
      //       },
      //     );
      // }
      //保存资源配置参数
      async function handleBindResource() {
        const resourceParams = await validateResource();
        await maHttp
          .get(
            {
              url: 'modelGeneration/bindHardwareParams',
              params: {
                modelGenerationId: route.query.modelGenerationId,
                hardwareParamsId: resourceParams.resource,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then(
            () => {
              createMessage.success('提交资源参数成功！');
              getPageData(route.query.modelGenerationId);
            },
            () => {
              createMessage.error('提交资源参数失败！');
            },
          );
      }

      const handleTrainUpdate = () => {
        getPageData(route.query.modelGenerationId);
      };

      const handleInspectUpdate = () => {
        getPageData(route.query.modelGenerationId);
      };

      const handleDeployUpdate = () => {
        getPageData(route.query.modelGenerationId);
      };

      const skip = ref<boolean>(false);

      const checked = ref<boolean>(false);

      const link = ref('');
      const tag = ref('');
      const description = ref('');
      const trainDescription = ref('');
      const testDescription = ref('');
      const deployDescription = ref('');

      //checkbox  用来表示镜像是训练质检还是推理
      //functionValue 用来表示镜像是上传还是更新
      //plainOptions是表示镜像功能选项 有三种值
      //functionOptions表示镜像操作选项 有两种值
      //hyperParamList 表示超参数组
      const checkbox = ref('训练');
      const functionValue = ref('上传');
      const plainOptions = ['训练', '质检', '推理'];
      const functionOptions = ['上传', '更新'];
      const trainModelVersion = ref();
      const testModelVersion = ref();
      let hyperParamTrainList: string[] = [];
      let hyperParamInspectList: string[] = [];
      const isReuseModel = ref(false);

      //这三个变量用来表示镜像上传三种方式的显示
      const visible1 = ref<boolean>(true);
      const visible2 = ref<boolean>(false);
      const visible3 = ref<boolean>(false);

      //这三个颜色来表示当前生产任务三种镜像的情况
      const trainImageColor = ref('');
      const inspectImageColor = ref('');
      const deployImageColor = ref('');

      const reusedModelVersion = ref();

      interface item {
        value: number;
        label: string;
      }

      function uploadTrainImage() {
        openTrainModal(true, {
          trainDescription: trainDescription,
          trainAdd: trainAdd,
          inputValue: inputValue,
          filePath: filePath,
          isUpdate: false,
        });
      }

      function uploadTestImage() {
        openInspectModal(true, {
          testDescription: testDescription,
          InspectAdd: InspectAdd,
          inputValue: inputValue,
          filePath: filePath,
          isUpdate: false,
        });
      }

      function uploadDeployImage() {
        openDeployModal(true, {
          deployDescription: deployDescription,
          DeployAdd: DeployAdd,
          inputValue: inputValue,
          filePath: filePath,
          isUpdate: false,
        });
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

      onMounted(async () => {
        await getModelData();
        await getPageData(route.query.modelGenerationId);
        await getDatasetList();
      });

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

      //复用镜像上传时，getModelData获取其他算法的函数
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

      //镜像操作响应函数
      function functionChange(e) {
        if (e.target.value === '更新') {
          uploadLinkURL.value = 'modelVersion/updateUrlModelVersion';
        } else {
          uploadLinkURL.value = 'modelVersion/addUrlModelVersion';
        }
      }

      //获取当前页面所有数据的接口
      function getPageData(mgId) {
        maHttp
          .get(
            {
              url: 'modelGeneration/findModelGenerationById',
              params: {
                id: mgId,
              },
              headers: {
                // @ts-ignore
                ignoreCancelToken: true,
              },
              timeout: 102400000,
            },
            { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
          )
          .then((mg) => {
            inputValue.value = mg.name;
            TrainImage.value = mg.model.trainModelVersion != null ? '已上传' : '未上传';
            InspectImage.value = mg.testModelVersion != null ? '已上传' : '未上传';
            DeployImage.value = mg.deployModelVersion != null ? '已上传' : '未上传';
            trainImageColor.value = mg.model.trainModelVersion != null ? 'green' : '';
            inspectImageColor.value = mg.testModelVersion != null ? 'green' : '';
            deployImageColor.value = mg.deployModelVersion != null ? 'green' : '';
            isReuseModel.value = mg.isReuse;
            delayTrainTime.value = mg.delayTrainTime;
            maxTrainTime.value = mg.maxTrainTime;
            //追加训练超参
            if (mg.model.trainModelVersion != null) {
              trainModelVersion.value = mg.model.trainModelVersion;
              hyperParamTrainList = [];
              if (trainModelVersion.value.modelConfigList != null) {
                trainModelVersion.value.modelConfigList.forEach((e) => {
                  if (e.field != null && e.field != '') hyperParamTrainList.push(e.field);
                });
              }
              hyperParamTrainList.forEach((value) => {
                appendSchemaByField(
                  {
                    field: value,
                    label: value,
                    component: 'Input',
                    required: true,
                    colProps: { span: 18 },
                  },
                  undefined,
                  undefined,
                );
              });
              //把已有超参内容输入到超参列表中
              if (mg.hyperParams != null) {
                setTrainParams(mg.hyperParams);
              }
            }

            //设置数据集信息
            if (mg.trainDataset != null) {
              let datasetfullName;
              if (
                mg.trainDataset.versionName.startsWith('V') ||
                mg.trainDataset.versionName.startsWith('v')
              ) {
                datasetfullName = mg.trainDataset.fullName;
              } else {
                datasetfullName = mg.trainDataset?.name + ' ' + mg.trainDataset?.versionSource;
              }
              setFieldsValue({ trainDataset: datasetfullName });
            }
            if (mg.testDataset != null) {
              let datasetfullName2;
              if (
                mg.testDataset.versionName.startsWith('V') ||
                mg.testDataset.versionName.startsWith('v')
              ) {
                datasetfullName2 = mg.testDataset.fullName;
              } else {
                datasetfullName2 = mg.testDataset?.name + ' ' + mg.testDataset?.versionSource;
              }
              setFieldsValue({ testDataset: datasetfullName2 });
            }
            if (mg.valDataset != null) {
              let datasetfullName3;
              if (
                mg.valDataset.versionName.startsWith('V') ||
                mg.valDataset.versionName.startsWith('v')
              ) {
                datasetfullName3 = mg.valDataset.fullName;
              } else {
                datasetfullName3 = mg.valDataset?.name + ' ' + mg.valDataset?.versionSource;
              }
              setFieldsValue({ valDataset: datasetfullName3 });
            }
            setFieldsValue({
              id: mg.id,
              splitSize: mg.splitSize,
              datasetSource: mg.datasetSource,
            });
            hyperParamTrainList.forEach(async (value) => {
              await removeSchemaByFiled(value);
            });
            hyperParamInspectList.forEach(async (value) => {
              await removeInspect(hyperParamInspectPrefix + value);
            });
            setInfo({ id: mg.id });
            setInfo({ name: mg.name });
            setInfo({ description: mg.description });
            setInfo({ createTime: mg.createTime });
            setTrainTimeInfo({ maxTrainTime: mg.maxTrainTime });
            setTrainTimeInfo({ delayTrainTime: mg.delayTrainTime });
            if (mg.hardwareParams != null) {
              setResource({ resource: mg.hardwareParams.id });
            }
            if (TrainImage.value === '已上传') {
              trainAdd.value = false;
              trainDescription.value = mg.model.trainModelVersion.description;
            }
            if (InspectImage.value === '已上传') {
              InspectAdd.value = false;
              testDescription.value = mg.model.estModelVersion.description;
            }
            if (DeployImage.value === '已上传') {
              DeployAdd.value = false;
              deployDescription.value = mg.model.deployModelVersion.description;
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

      //下面的参数和页面内容显示有关
      //前三个代表当前镜像是否上传
      //uploadLinkURL 代表通过URL方式上传镜像的URL路径
      const TrainImage = ref('');
      const InspectImage = ref('');
      const DeployImage = ref('');
      const uploadLinkURL = ref('modelVersion/updateUrlModelVersion');
      //传参用，表明是更新还是新增
      const trainAdd = ref<boolean>(true);
      const InspectAdd = ref<boolean>(true);
      const DeployAdd = ref<boolean>(true);

      return {
        handleTrainUpdate,
        handleInspectUpdate,
        handleDeployUpdate,
        trainDescription,
        testDescription,
        deployDescription,
        activeKey,
        customStyle,
        trainAdd,
        InspectAdd,
        DeployAdd,
        UploadTrainModal,
        UploadInspectModal,
        UploadDeployModal,
        uploadTrainImage,
        uploadTestImage,
        uploadDeployImage,
        handleBindDatasetSubmit,
        handleBindTrainParams,
        handleBindResource,
        trainModelVersion,
        testModelVersion,
        registerModal,
        registerModal2,
        registerModal3,
        registerModal4,
        openTrainModal,
        openInspectModal,
        openDeployModal,
        openConfigurationModal,
        uploadLinkURL,
        functionValue,
        functionChange,
        isReuseModel,
        checkbox,
        plainOptions,
        functionOptions,
        validate,
        validateTrain,
        validateInspect,
        appendInspect,
        removeInspect,
        registerData,
        registerTrainData,
        registerInspectData,
        inputValue,
        trainImageColor,
        inspectImageColor,
        deployImageColor,
        TrainImage,
        InspectImage,
        DeployImage,
        uploadRef,
        onChange,
        allRemove,
        functionContent: '镜像仓库地址上传 ; 本地文件上传 ; 现有模型库中选择复用',
        functionContent1:
          '训练时上述三个方法只能三选一 ; 部署时只可在镜像仓库地址和本地文件上传方法中二选一',
        checked,
        visible1,
        visible2,
        visible3,
        reusedModelVersion,
        link,
        tag,
        description,
        modelList,
        modelVersionList,
        handleSelectModel,
        dataSchemas,
        trainSchemas,
        generationSchemas,
        validateResource,
        registerResourceData,
        registerInfoData,
        handleBindInfoSubmit,
        setTrainParams,
        setResource,
        inspectSchemas,
        ResourceSchemas,
        hyperParamTrainPrefix,
        getDatasetList,
        onClick,
        setInfo,
        handleTrainTime,
        validateInfo,
        registerTime,
        trainTimeSchemas,
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

  .vben-collapse-container :deep(.app-iconify svg) {
    font-size: 35px;
    display: inline-flex;
  }
</style>
