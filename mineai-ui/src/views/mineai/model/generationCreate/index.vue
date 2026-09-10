<template>
  <PageWrapper style="margin: 0 16px 0 16px">
    <a-row>
      <!-- 左侧基础信息卡片 -->
      <a-col :span="12">
        <a-card title="基础信息填写" style="min-height: 800px">
          <div class="justify-around">
            <BasicForm
              style="width: 100%"
              :schemas="generationSchemas"
              @register="registerInfoData"
              @submit="handleSubmit"
            />
          </div>
        </a-card>
      </a-col>

      <!-- 右侧训练信息卡片 -->
      <a-col :span="12">
        <a-card title="训练信息" style="min-height: 800px">
          <a-collapse :active-key="activeKeys" @change="handleCollapseChange">
            <!-- 训练参数 -->
            <a-collapse-panel header="训练参数" key="1">
              <div class="flex flex-row justify-around">
                <BasicForm
                  style="width: 100%"
                  :schemas="trainSchemas"
                  @register="registerTrainData"
                />
              </div>
            </a-collapse-panel>

            <!--            &lt;!&ndash; 设置训练信息 &ndash;&gt;-->
            <!--            <a-collapse-panel header="设置训练信息" key="2">-->
            <!--              <div class="flex flex-row justify-around">-->
            <!--                <BasicForm-->
            <!--                  style="width: 100%"-->
            <!--                  :schemas="trainTimeSchemas"-->
            <!--                  @register="registerTime"-->
            <!--                />-->
            <!--              </div>-->
            <!--            </a-collapse-panel>-->

            <!-- 资源配置 -->
            <a-collapse-panel header="资源配置" key="3">
              <div class="flex flex-row justify-around">
                <a-row>
                  <a-col :span="4">
                    <a-button type="primary" @click="openConfigurationModal()">添加配置</a-button>
                  </a-col>
                  <a-col :span="12">
                    <BasicForm :schemas="ResourceSchemas" @register="registerResourceData" />
                  </a-col>
                </a-row>
              </div>
              <ConfigurationModal @register="registerConfigurationModal" />
            </a-collapse-panel>
          </a-collapse>
        </a-card>
      </a-col>
    </a-row>
  </PageWrapper>
</template>

<script setup lang="ts">
  import { useForm } from '/@/components/Form';
  import { generationSchemas, trainSchemas } from './data';
  import { onMounted, ref } from 'vue';
  import { on } from '/@/views/mineai/model/generationCreate/modelCreateEventBus';
  import { ResourceSchemas } from '/@/views/mineai/model/generationCreate/data';
  import { useModal } from '/@/components/Modal';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { maHttp } from '/@/utils/http/axios';
  import { useUserStore } from '/@/store/modules/user';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useGo } from '/@/hooks/web/usePage';

  onMounted(() => {
    //监听算法选择结果，更新算法选择对应的form
    on('modelChanged', handleModelChange);
    // 展开所有面板
    expandAll();
  });
  const { createMessage } = useMessage();
  const go = useGo();

  const trainModelVersion = ref();
  let hyperParamTrainList: string[] = [];

  // 定义一个 ref 来存储当前展开的面板 key
  const activeKeys = ref<string[]>([]);

  // 展开所有面板
  async function expandAll() {
    activeKeys.value = ['1', '2', '3'];
  }

  const userStore = useUserStore();
  const userData = userStore.getUserInfo;

  // 当用户手动改变 Collapse 时更新 activeKeys
  function handleCollapseChange(keys: string[]) {
    activeKeys.value = keys;
  }

  const [registerConfigurationModal, { openModal: openConfigurationModal }] = useModal();

  //绑定基础信息
  const [registerInfoData, { validate: validateInfo }] = useForm({
    labelWidth: 200,
    schemas: generationSchemas,
    showActionButtonGroup: true,
    showResetButton: false,
    showSubmitButton: true,
    submitButtonOptions: {
      text: '提交',
    },
  });

  //绑定训练参数
  const [
    registerTrainData,
    { appendSchemaByField, validate: validateTrain, setFieldsValue: setTrainParams, resetSchema },
  ] = useForm({
    labelWidth: 150,
    schemas: trainSchemas,
    showActionButtonGroup: true,
    showResetButton: false,
    showSubmitButton: false,
  });

  //训练信息form
  // const [registerTime, { validate: validateTime }] = useForm({
  //   labelWidth: 200,
  //   showActionButtonGroup: true,
  //   showResetButton: false,
  //   showSubmitButton: false,
  // });

  //绑定资源参数Form
  const [registerResourceData, { validate: validateResource }] = useForm({
    labelWidth: 150,
    schemas: ResourceSchemas,
    actionColOptions: { span: 24 },
    showActionButtonGroup: true,
    showResetButton: false,
    showSubmitButton: false,
  });

  //选择算法时，调整右侧超参
  const handleModelChange = async (model) => {
    // 展开所有面板
    await expandAll();
    //重置form样式
    resetSchema(trainSchemas);
    //追加训练超参
    if (model.trainModelVersion != null) {
      trainModelVersion.value = model.trainModelVersion;
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
    }
  };

  //提交基础信息之后返回的generationId
  const newGenerationId = ref(0);

  // 提交
  async function handleSubmit() {
    try {
      await submitBasicInfo();
      await submitTrainHyperParam();
      // await submitTrainTime();
      await submitResourceParam();
      createMessage.success('所有信息提交成功！');
      go('/maTrainingCenter/modelGeneration');
    } catch (error) {
      createMessage.error(`提交失败：${error.message}`);
    }
  }

  // 绑定基础信息方法
  async function submitBasicInfo() {
    const infoParams = await validateInfo();
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

    try {
      const v = await maHttp.post(
        {
          url: 'modelGeneration/addModelGeneration',
          params: infoValues,
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
      newGenerationId.value = v;
    } catch {
      throw new Error('基础信息提交失败！');
    }
  }

  // 提交超参
  async function submitTrainHyperParam() {
    const trainParams = await validateTrain();
    let trainValues = {};
    let params = {};

    Object.keys(trainParams).forEach((key) => {
      params[key] = trainParams[key];
    });

    params['MODE_WORKING_MODE'] = '1';
    trainValues['modelGenerationId'] = Number(newGenerationId.value);
    trainValues['params'] = params;

    try {
      await maHttp.post(
        {
          url: 'modelGeneration/setTrainParams',
          params: trainValues,
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      );
    } catch {
      throw new Error('训练参数提交失败！');
    }
  }

  // 保存资源配置参数
  async function submitResourceParam() {
    const resourceParams = await validateResource();

    try {
      await maHttp.get(
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
      );
    } catch {
      throw new Error('资源参数提交失败！');
    }
  }
</script>

<style scoped lang="less">
  .drawer-content {
    height: calc(100% - 55px);
    overflow-y: auto;
  }

  .drawer-footer {
    position: absolute;
    bottom: 0;
    width: 100%;
    border-top: 1px solid #e8e8e8;
    padding: 10px 16px;
    text-align: right;
    left: 0;
    background: #fff;
    border-radius: 0 0 4px 4px;
  }
</style>
