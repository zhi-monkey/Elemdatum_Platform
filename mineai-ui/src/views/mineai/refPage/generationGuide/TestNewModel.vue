<template>
  <div class="container">
    <div class="left-column">
      <a-form @submit.prevent="handleSubmit">
        <a-form-item label="训练任务名称" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
          <a-input v-model:value="taskName" placeholder="请输入训练任务名称" />
        </a-form-item>

        <a-form-item label="选择应用" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
          <a-select v-model:value="application" @change="handleApplicationChange">
            <a-select-option v-for="app in applications" :key="app.value" :value="app.value">
              {{ app.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="选择设备-固件" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
          <a-select v-model:value="deviceFirmware" @change="handleDeviceChange">
            <a-select-option
              v-for="device in deviceFirmwares"
              :key="device.value"
              :value="device.value"
            >
              {{ device.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          label="是否采用数据集划分"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-radio-group v-model:value="datasetSplit" @change="handleDatasetSplitChange">
            <a-radio value="yes">是</a-radio>
            <a-radio value="no">否</a-radio>
          </a-radio-group>
        </a-form-item>

        <div v-if="datasetSplit === 'yes'">
          <a-form-item label="数据集划分占比" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
            <a-slider v-model:value="splitRatio" range :min="0" :max="100" :step="10" />
            <div class="slider-labels">
              <span>测试集: {{ splitRatio[0] }}%</span>
              <span>训练集: {{ splitRatio[1] - splitRatio[0] }}%</span>
              <span>验证集: {{ 100 - splitRatio[1] }}%</span>
            </div>
          </a-form-item>

          <a-form-item label="训练数据集" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
            <a-select v-model:value="trainingDataset">
              <a-select-option
                v-for="dataset in datasets"
                :key="dataset.value"
                :value="dataset.value"
              >
                {{ dataset.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>

        <div v-else>
          <a-form-item label="训练数据集" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
            <a-select v-model:value="trainingDataset">
              <a-select-option
                v-for="dataset in datasets"
                :key="dataset.value"
                :value="dataset.value"
              >
                {{ dataset.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="测试数据集" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
            <a-select v-model:value="testingDataset">
              <a-select-option
                v-for="dataset in datasets"
                :key="dataset.value"
                :value="dataset.value"
              >
                {{ dataset.label }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item label="验证数据集" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
            <a-select v-model:value="validationDataset">
              <a-select-option
                v-for="dataset in datasets"
                :key="dataset.value"
                :value="dataset.value"
              >
                {{ dataset.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
      </a-form>
    </div>

    <a-divider type="vertical" class="divider-class" />

    <div class="right-column">
      <a-form @submit.prevent="handleSubmit">
        <a-form-item label="选择算法" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
          <a-select v-model:value="algorithm" @change="handleAlgorithmChange">
            <a-select-option v-for="algo in algorithms" :key="algo.value" :value="algo.value">
              {{ algo.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <div class="parameter-settings" v-if="selectedAlgorithm">
          <div class="parameter-title-wrapper">
            <h4 class="parameter-title">超参数设置</h4>
          </div>
          <div v-for="(param, index) in selectedAlgorithm.params" :key="index">
            <a-form-item :label="param.label" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
              <a-input v-model:value="hyperparameters[param.key]" :placeholder="param.label" />
            </a-form-item>
          </div>
        </div>

        <a-form-item :wrapper-col="{ offset: 6, span: 18 }">
          <a-button type="primary" html-type="submit">提交</a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import {
    Form as AForm,
    FormItem as AFormItem,
    Input as AInput,
    Select as ASelect,
    SelectOption as ASelectOption,
    RadioGroup as ARadioGroup,
    Radio as ARadio,
    Slider as ASlider,
    Button as AButton,
    Divider as ADivider,
  } from 'ant-design-vue';

  // 独立的变量
  const taskName = ref('');
  const application = ref(null);
  const deviceFirmware = ref(null);
  const algorithm = ref(null);
  const datasetSplit = ref('no');
  const splitRatio = ref([70, 30]); // 初始值
  const trainingDataset = ref(null);
  const testingDataset = ref(null);
  const validationDataset = ref(null);
  const hyperparameters = ref({
    HP_BATCH_SIZE: 32,
    HP_CONFIDENCE: 0.5,
    HP_WEIGHT_DECAY: 0.0001,
    HP_EPOCHES: 100,
    HP_MOMENTUM: 0.9,
    HP_LEARNING_RATE: 0.001,
  });

  const emits = defineEmits(['next']);

  const applications = ref([
    { value: 'safetyHelmetDetection', label: '安全帽检测' },
    { value: 'faceRecognition', label: '人脸识别' },
  ]);

  const deviceFirmwares = ref([
    { value: 'KBA12C-v1.0', label: 'KBA12C-v1.0' },
    { value: 'KBA12D-v1.1', label: 'KBA12D-v1.1' },
  ]);

  const algorithms = ref([
    {
      value: 'yolo-1',
      label: 'YOLO-1',
      params: [
        { key: 'HP_BATCH_SIZE', label: '批大小' },
        { key: 'HP_CONFIDENCE', label: '置信度' },
      ],
    },
    {
      value: 'yolo-2',
      label: 'YOLO-2',
      params: [
        { key: 'HP_EPOCHES', label: '训练轮数' },
        { key: 'HP_LEARNING_RATE', label: '学习率' },
        { key: 'HP_WEIGHT_DECAY', label: '权重衰减' },
      ],
    },
  ]);

  const datasets = ref([
    { value: 'testDatasetV0001', label: 'testDataset V0001' },
    { value: 'testDatasetV0002', label: 'testDataset V0002' },
  ]);

  const selectedAlgorithm = ref(null);

  const handleApplicationChange = (value: string) => {
    // 根据选择的应用更新算法
    updateAlgorithmSelection();
  };

  const handleDeviceChange = (value: string) => {
    // 根据选择的设备更新算法
    updateAlgorithmSelection();
  };

  const updateAlgorithmSelection = () => {
    // 当应用和设备都被选择后,更新算法选择
    if (application.value && deviceFirmware.value) {
      // 选择默认的算法
      const defaultAlgorithm = algorithms.value[0]; // 这里可以根据应用和设备的选择返回不同的算法
      algorithm.value = defaultAlgorithm.value; // 更新算法选择
      handleAlgorithmChange(defaultAlgorithm.value); // 更新超参数
    }
  };

  const handleAlgorithmChange = (value: string) => {
    selectedAlgorithm.value = algorithms.value.find((algo) => algo.value === value);
  };

  const handleDatasetSplitChange = (e: { target: { value: string } }) => {
    datasetSplit.value = e.target.value;
  };

  const handleSubmit = () => {
    const formData = {
      taskName: taskName.value,
      application: application.value,
      deviceFirmware: deviceFirmware.value,
      algorithm: algorithm.value,
      datasetSplit: datasetSplit.value,
      splitRatio: splitRatio.value,
      trainingDataset: trainingDataset.value,
      testingDataset: testingDataset.value,
      validationDataset: validationDataset.value,
      hyperparameters: hyperparameters.value,
    };
    console.log('表单数据:', formData);
    emits('next', {
      generationId: 1,
      generationName: 1111,
    });
  };
</script>

<style scoped>
  .container {
    display: flex;
    justify-content: center;
    align-items: flex-start;
    width: 100%;
  }

  .left-column {
    width: 60%;
    padding: 20px;
  }

  .right-column {
    width: 40%;
    padding: 20px;
  }

  .slider-labels {
    display: flex;
    justify-content: space-between;
    margin-top: 5px;
  }

  .parameter-title-wrapper {
    width: 100%;
    text-align: center;
    margin-bottom: 16px;
  }

  .parameter-title {
    display: inline-block;
  }

  .divider-class {
    height: 420px;
    margin-left: 50px;
  }
</style>
