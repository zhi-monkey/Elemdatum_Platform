<template>
  <PageWrapper :contentStyle="{ 'background-color': '#181d31', 'border-radius': '4px' }">
    <div class="step-form-form">
      <a-steps :current="currentStep" style="height: 50px">
        <a-step title="基础信息填写" />
        <a-step title="设置训练参数" />
        <a-step title="设置训练信息" />
        <a-step title="设置资源配置" />
      </a-steps>
    </div>
    <div class="mt-5 justify-center">
      <!-- Step 1: 基础信息填写 -->
      <div v-if="currentStep === 0">
        <a-card>
          <!--          <a-card title="基础信息填写">-->
          <div class="flex flex-row justify-around">
            <BasicForm
              style="width: 50%"
              :schemas="generationSchemas"
              @register="registerInfoData"
            />
            <!--                @submit="handleBindInfoSubmit"-->
          </div>
        </a-card>
        <div style="margin-top: 20px; margin-bottom: 20px; display: flex; justify-content: center">
          <a-button type="primary" @click="handleBindInfoSubmit">下一步</a-button>
        </div>
      </div>

      <!-- Step 2: 训练参数 -->
      <div v-if="currentStep === 1">
        <a-card>
          <div class="flex flex-row justify-around">
            <!--          <BasicForm
                          style="width: 50%"
                          :schemas="trainSchemas"
                          @register="registerTrainData"/>
            &lt;!&ndash;              @submit="handleBindTrainParams"&ndash;&gt;-->

            <BasicForm
              style="width: 60%"
              :schemas="schemas"
              :actionColOptions="{ span: 0 }"
              @submit="handleBindTrainParams"
            />
          </div>
        </a-card>
        <div style="margin-top: 20px; margin-bottom: 20px; display: flex; justify-content: center">
          <a-button type="primary" @click="handleBindTrainParams">下一步</a-button>
        </div>
      </div>

      <!-- Step 3: 设置训练信息 -->
      <div v-if="currentStep === 2">
        <a-card>
          <div class="flex flex-row justify-around">
            <BasicForm style="width: 50%" :schemas="trainTimeSchemas" @register="registerTime" />
            <!--              @submit="handleTrainTime"-->
          </div>
        </a-card>
        <div style="margin-top: 20px; margin-bottom: 20px; display: flex; justify-content: center">
          <a-button type="primary" @click="handleTrainTime">下一步</a-button>
        </div>
      </div>

      <!-- Step 4: 资源配置 -->
      <div v-if="currentStep === 3">
        <a-card style="width: 100%">
          <div class="flex flex-row justify-around">
            <a-row>
              <a-col :span="4">
                <a-button type="primary" @click="openConfigurationModal()">添 加 配 置</a-button>
              </a-col>
              <a-col :span="16">
                <BasicForm
                  style="width: 50%"
                  :schemas="ResourceSchemas"
                  @register="registerResourceData"
                />
                <!--                  @submit="handleBindResource"-->
              </a-col>
            </a-row>
          </div>
        </a-card>
        <div style="margin-top: 20px; margin-bottom: 20px; display: flex; justify-content: center">
          <!--          <a-button type="primary" @click="handleBindResource">提交</a-button>-->
          <a-button type="primary" @click="handleSubmit">提交</a-button>
        </div>
      </div>

      <!-- Modals for additional configurations -->
      <UploadTrainModal @register="registerModal" @update-train="handleTrainUpdate" />
      <UploadInspectModal @register="registerModal2" @update-inspect="handleInspectUpdate" />
      <UploadDeployModal @register="registerModal3" @update-deploy="handleDeployUpdate" />
      <ConfigurationModal @register="registerModal4" />
    </div>
  </PageWrapper>
</template>

<!--<template>
  <div style="height: 100%; width: 100%">
    <a-card title="基础信息填写">
      <div class="flex flex-row justify-around">
        <BasicForm
          style="width: 50%"
          :schemas="generationSchemas"
          @register="registerInfoData"
          @submit="handleBindInfoSubmit"
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
            <a-col :span="16">
              <BasicForm
                style="width: 130%"
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
</template>-->
<script lang="ts">
  import {
    Button as AButton,
    Card as ACard,
    Col as ACol,
    Row as ARow,
    Steps,
  } from 'ant-design-vue';
  import { defineComponent, onMounted, reactive, ref, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import {
    dataSchemas,
    trainSchemas,
    inspectSchemas,
    hyperParamTrainPrefix,
    ResourceSchemas,
    generationSchemas,
    trainTimeSchemas,
  } from './data';
  import 'vue-simple-uploader/dist/style.css';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useModal } from '/@/components/Modal';
  import UploadTrainModal from './UploadTrainModal.vue';
  import UploadInspectModal from './UploadInspectModal.vue';
  import UploadDeployModal from './UploadDeployModal.vue';
  import ConfigurationModal from './ConfigurationModal.vue';
  //import { useUserStore } from '/@/store/modules/user';
  import Convert2 from '/@/views/mineai/model/generationGuide/convertPage/Convert2.vue';
  import TrainInfo from '/@/views/mineai/model/modelGeneration/trainJobDetail/TrainInfo.vue';
  import Model from '/@/views/mineai/refPage/generationGuide/Model.vue';
  import { PageWrapper } from '/@/components/Page';
  import Release from '/@/views/mineai/model/generationGuide/Release.vue';
  //wwj
  import { FormSchema } from '/@/components/Form';
  import { CollapseContainer } from '/@/components/Container';

  const schemas: FormSchema[] = [
    {
      field: 'HP_BATCH_SIZE',
      component: 'Input',
      label: '* HP_BATCH_SIZE',
      colProps: {
        span: 24, // 使表单项占据整行
      },
      defaultValue: '',
      componentProps: {
        placeholder: '请输入',
      },
    },
    {
      field: 'HP_CONFIDENCE',
      component: 'Input',
      label: '* HP_CONFIDENCE',
      colProps: {
        span: 24,
      },
      defaultValue: '',
      componentProps: {
        placeholder: '请输入',
      },
    },
    {
      field: 'HP_WEIGHT_DECAY',
      component: 'Input',
      label: '* HP_WEIGHT_DECAY',
      colProps: {
        span: 24,
      },
      defaultValue: '',
      componentProps: {
        placeholder: '请输入',
      },
    },
    {
      field: 'HP_EPOCHES',
      component: 'Input',
      label: '* HP_EPOCHES',
      colProps: {
        span: 24,
      },
      defaultValue: '',
      componentProps: {
        placeholder: '请输入',
      },
    },
    {
      field: 'HP_MOMENTUN',
      component: 'Input',
      label: '* HP_MOMENTUN',
      colProps: {
        span: 24,
      },
      defaultValue: '',
      componentProps: {
        placeholder: '请输入',
      },
    },
    {
      field: 'HP_LEARNING_RATE',
      component: 'Input',
      label: '* HP_LEARNING_RATE',
      colProps: {
        span: 24,
      },
      defaultValue: '',
      componentProps: {
        placeholder: '请输入',
      },
    },
  ];

  //const router = useRouter();
  //const userStore = useUserStore();
  //const userData = userStore.getUserInfo;
  export default defineComponent({
    components: {
      Release,
      PageWrapper,
      Model,
      TrainInfo,
      Convert2,
      BasicForm,
      AButton,
      ARow,
      ACol,
      ACard,
      UploadTrainModal,
      UploadInspectModal,
      UploadDeployModal,
      //BasicForm,
      CollapseContainer,
      ConfigurationModal,
      [Steps.name]: Steps,
      [Steps.Step.name]: Steps.Step,
    },
    emits: ['next'],
    setup() {
      // const { createMessage, createWarningModal } = useMessage();
      // const uploadSucceed = ref(false);
      const { createMessage } = useMessage();
      const router = useRouter(); // 获取 router 实例
      const customStyle = 'background:#181d31';
      const activeKey = ref(['1', '2']);
      //提交基础信息之后返回的generationId
      const newGenerationId = ref(0);

      const inputValue = ref('');
      const filePath = ref('');
      const [registerModal, { openModal: openTrainModal }] = useModal();
      const [registerModal2, { openModal: openInspectModal }] = useModal();
      const [registerModal3, { openModal: openDeployModal }] = useModal();
      const [registerModal4, { openModal: openConfigurationModal }] = useModal();
      //绑定基础信息
      /*const [registerInfoData, { setFieldsValue: setInfo, validate: validateInfo }] = useForm({
      labelWidth: 200,
      schemas: generationSchemas,
      showActionButtonGroup: true,
      showResetButton: false,
      submitButtonOptions: {
        text: '下一步',
      },
    });*/

      const [registerInfoData, { setFieldsValue: setInfo, validate: validateInfo }] = useForm({
        labelWidth: 200,
        schemas: generationSchemas,
        showActionButtonGroup: false,
        showResetButton: false,
      });

      const currentStep = ref(0);

      /*const [registerTime, { validate: validateTime }] = useForm({
      labelWidth: 200,
      showActionButtonGroup: true,
      showResetButton: false,
      submitButtonOptions: {
        text: '下一步',
      },
    });*/
      const [registerTime] = useForm({
        labelWidth: 200,
        showActionButtonGroup: false,
        showResetButton: false,
      });

      //绑定训练参数
      /*const [
      registerTrainData,
      {
        appendSchemaByField,
        removeSchemaByFiled,
        validate: validateTrain,
        setFieldsValue: setTrainParams,
      },
    ] = useForm({
      labelWidth: 200,
      schemas: trainSchemas,
      showActionButtonGroup: true,
      showResetButton: false,
      submitButtonOptions: {
        text: '下一步 ',
      },
    });*/
      const [
        registerTrainData,
        {
          appendSchemaByField,
          removeSchemaByFiled,
          validate: validateTrain,
          setFieldsValue: setTrainParams,
        },
      ] = useForm({
        labelWidth: 200,
        schemas: trainSchemas,
        showActionButtonGroup: false,
        showResetButton: false,
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
          text: '提 交 ',
        },
      });

      async function handleTrainTime() {
        /*const trainTimeInfo = await validateTime();
      let trainTimeValues = {};
      trainTimeValues['delayTrainTime'] = trainTimeInfo.delayTrainTime;
      trainTimeValues['maxTrainTime'] = trainTimeInfo.maxTrainTime;
      await maHttp
        .post(
          {
            url: 'modelGeneration/saveModelGenerationTrainTime',
            params: {
              modelGenerationId: newGenerationId.value,
              delayTrainTime: trainTimeInfo.delayTrainTime,
              maxTrainTime: trainTimeInfo.maxTrainTime,
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
            getPageData(newGenerationId.value);
            //wwj
            //currentStep.value += 1; // 提交训练时间后进入下一步
          },
          () => {
            createMessage.error('提交训练时间要求参数失败！');
          },
        );*/
        //wwj
        currentStep.value += 1; // 提交训练时间后进入下一步
      }

      //绑定资源参数
      /*const [registerResourceData, { validate: validateResource }] = useForm({
      labelWidth: 200,
      schemas: ResourceSchemas,
      actionColOptions: { span: 24 },
      showActionButtonGroup: true,
      showResetButton: false,
      submitButtonOptions: {
        text: '提交 ',
      },
    });*/
      const [registerResourceData] = useForm({
        labelWidth: 200,
        schemas: ResourceSchemas,
        actionColOptions: { span: 30 },
        showActionButtonGroup: false,
        showResetButton: false,
      });

      //保存资源配置参数
      /*async function handleBindResource() {
      const resourceParams = await validateResource();
      await maHttp
        .get(
          {
            url: 'modelGeneration/bindHardwareParams',
            params: {
              modelGenerationId: newGenerationId.value,
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
            getPageData(newGenerationId.value);
            //完成后跳转页面到标准化训练管理页面
            router.push({
            path: '/maTrainingCenter/modelGeneration',
            });

          },
          () => {
            createMessage.error('提交资源参数失败！');
          },
        );
    }*/

      function handleSubmit() {
        router.push({
          path: '/maTrainingCenter/modelGeneration', // 页面路径
        });
      }

      // 绑定基础信息方法
      async function handleBindInfoSubmit() {
        /*const infoParams = await validateInfo();
      let infoValues = {};
      infoValues['name'] = infoParams.name;
      infoValues['description'] = infoParams.description;
      infoValues['splitSize'] = infoParams.splitSize;
      infoValues['datasetSource'] = infoParams.datasetSource;
      infoValues['trainDataset'] = infoParams.trainDataset;
      if (infoParams.testDataset != null) {
        infoValues['testDataset'] = infoParams.testDataset;
      } else {
        infoValues['testDataset'] = infoParams.trainDataset;
      }
      if (infoParams.valDataset != null) {
        infoValues['valDataset'] = infoParams.valDataset;
      } else {
        infoValues['valDataset'] = infoParams.trainDataset;
      }
      infoValues['isReuse'] = true;
      infoValues['modelExplore'] = { id: infoParams.model };
      if (infoParams.resource != null) {
        infoValues['hardwareParams'] = { id: infoParams.resource };
      }
      infoValues['isGuided'] = false;
      infoValues['userId'] = userData?.id;
      await maHttp
        .post(
          {
            url: 'modelGeneration/addModelGeneration',
            params: infoValues,
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
          },
          { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        )
        .then(
          (v) => {
            createMessage.success('提交基础信息成功！');
            newGenerationId.value = v;
            getPageData(newGenerationId.value);
            //wwj
            currentStep.value += 1; // 提交基础信息后进入下一步
          },
          () => {
            createMessage.error('提交基础信息失败！');
          },
        );*/

        currentStep.value += 1;
      }

      watch(newGenerationId, (newValue) => {
        if (newValue !== 0) {
          filePath.value = 'generation' + newValue;
          getPageData(newValue);
        }
      });

      //绑定训练参数对应方法
      async function handleBindTrainParams() {
        // const trainParams = await validateTrain();
        // let trainValues = {};
        // let params = {};
        // Object.keys(trainParams).forEach((key) => {
        //   params[key] = trainParams[key];
        // });
        // params['MODEL_WORKING_MODE'] = '1';
        // trainValues['modelGenerationId'] = Number(newGenerationId.value);
        // trainValues['params'] = params;
        // await maHttp
        //   .post(
        //     {
        //       url: 'modelGeneration/setTrainParams',
        //       params: trainValues,
        //       headers: {
        //         // @ts-ignore
        //         ignoreCancelToken: true,
        //       },
        //     },
        //     { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
        //   )
        //   .then(
        //     () => {
        //       createMessage.success('提交训练参数成功！');
        //       getPageData(newGenerationId);
        //       //wwj
        //       currentStep.value += 1; // 提交训练参数后进入下一步
        //     },
        //     () => {
        //       createMessage.error('提交训练参数失败！');
        //     },
        //   );

        currentStep.value += 1;
      }

      const handleTrainUpdate = () => {
        getPageData(newGenerationId.value);
      };

      const handleInspectUpdate = () => {
        getPageData(newGenerationId.value);
      };

      const handleDeployUpdate = () => {
        getPageData(newGenerationId.value);
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
      const canEditImage = ref(true);
      const canEditParams = ref(false);

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

      onMounted(async () => {});

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
            console.log(mg);
            inputValue.value = mg.name;
            TrainImage.value = mg.trainModelVersion != null ? '已上传' : '未上传';
            InspectImage.value = mg.testModelVersion != null ? '已上传' : '未上传';
            DeployImage.value = mg.deployModelVersion != null ? '已上传' : '未上传';
            trainImageColor.value = mg.trainModelVersion != null ? 'green' : '';
            inspectImageColor.value = mg.testModelVersion != null ? 'green' : '';
            deployImageColor.value = mg.deployModelVersion != null ? 'green' : '';
            canEditImage.value = mg.isReuse;
            hyperParamTrainList.forEach(async (value) => {
              await removeSchemaByFiled(value);
            });
            console.log(mg.model.trainModelVersion);
            //追加训练超参
            if (mg.model.trainModelVersion != null) {
              canEditParams.value = true;
              console.log('追加超参');
              trainModelVersion.value = mg.model.trainModelVersion;
              hyperParamTrainList = [];
              if (trainModelVersion.value.modelConfigList != null) {
                trainModelVersion.value.modelConfigList.forEach((e) => {
                  if (e.field != null && e.field != '') hyperParamTrainList.push(e.field);
                });
              }
              hyperParamTrainList.forEach((value) => {
                console.log('追加1个超参');
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
              if (mg.trainJob != null) {
                setTrainParams(mg.trainJob.params);
              }
            }

            //设置基础信息
            setInfo({
              id: mg.id,
              splitSize: mg.splitSize,
              datasetSource: mg.datasetSource,
              trainDataset: mg.trainDataset?.id,
              testDataset: mg.testDataset?.id,
              valDataset: mg.valDataset?.id,
              name: mg.name,
              description: mg.description,
              createTime: mg.createTime,
              resource: mg.hardwareParams?.id,
            });
            if (TrainImage.value === '已上传') {
              trainAdd.value = false;
              trainDescription.value = mg.trainModelVersion.description;
            }
            if (InspectImage.value === '已上传') {
              InspectAdd.value = false;
              testDescription.value = mg.testModelVersion.description;
            }
            if (DeployImage.value === '已上传') {
              DeployAdd.value = false;
              deployDescription.value = mg.deployModelVersion.description;
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
        trainTimeSchemas,
        registerTime,
        handleTrainTime,
        handleTrainUpdate,
        handleInspectUpdate,
        handleDeployUpdate,
        trainDescription,
        testDescription,
        deployDescription,
        activeKey,
        registerResourceData,
        customStyle,
        trainAdd,
        InspectAdd,
        DeployAdd,
        UploadTrainModal,
        UploadInspectModal,
        UploadDeployModal,
        uploadTrainImage,
        //handleBindResource,
        uploadTestImage,
        uploadDeployImage,
        handleBindTrainParams,
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
        canEditImage,
        canEditParams,
        checkbox,
        plainOptions,
        functionOptions,
        validateTrain,
        validateInspect,
        appendInspect,
        removeInspect,
        registerTrainData,
        setTrainParams,
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
        schemas,
        generationSchemas,
        registerInfoData,
        handleBindInfoSubmit,
        inspectSchemas,
        ResourceSchemas,
        hyperParamTrainPrefix,
        onClick,
        handleSubmit,
        setInfo,
        validateInfo,

        currentStep,

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

  .step-form-form {
    width: 80%;
    margin: 40px auto 0;
  }
</style>
