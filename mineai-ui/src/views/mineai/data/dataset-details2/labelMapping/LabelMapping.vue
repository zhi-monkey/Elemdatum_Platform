<template>
  <div class="label-mapping-container">
    <a-form ref="formRef" :model="formState">
      <a-alert
        v-if="unmappedLabels.length > 0"
        type="info"
        show-icon
        class="margin-bottom"
        message="以下标签需要映射"
        :description="`发现 ${unmappedLabels.length} 个非标准标签需要映射到标准标签库或选择不映射`"
      />

      <div class="action-buttons margin-bottom">
        <a-space>
          <!--          <a-button @click="autoMap">自动映射</a-button>-->
          <a-button @click="clearAllMappings">清除映射</a-button>
        </a-space>
      </div>
      <a-divider class="item-divider" />

      <div class="mapping-list">
        <a-form-item
          v-for="label in datasetLabels"
          :key="label.id"
          :name="['mappings', label.id.toString()]"
          :rules="[{ required: true, message: '请选择映射标签或选择不映射' }]"
        >
          <div class="mapping-item">
            <div class="dataset-label">
              <a-tag :color="label.color">{{ label.name }}</a-tag>
            </div>
            <div class="mapping-arrow">→</div>
            <div class="template-label-select">
              <a-select
                show-search
                v-model:value="formState.mappings[label.id]"
                placeholder="选择标准标签"
                style="width: 200px"
                :filter-option="filterOption"
              >
                <!-- 自动映射时只显示对应标签和不映射 -->
                <template v-if="isAutomaticallyMapped[label.id]">
                  <a-select-option value="none">不映射</a-select-option>
                  <a-select-option :value="autoMappedLabels[label.id]?.id">
                    {{ autoMappedLabels[label.id]?.name }}
                  </a-select-option>
                </template>

                <!-- 非自动映射时显示全部选项 -->
                <template v-else>
                  <a-select-option value="none">不映射</a-select-option>
                  <a-select-option
                    v-for="templateLabel in templateLabels"
                    :key="templateLabel.id"
                    :value="templateLabel.id"
                  >
                    {{ templateLabel.name }}
                  </a-select-option>
                </template>
              </a-select>
            </div>
          </div>
          <a-divider class="item-divider" />
        </a-form-item>
      </div>
    </a-form>
  </div>
</template>

<script setup lang="ts">
  import { ref, computed, reactive, onMounted, watch } from 'vue';
  import {
    Form as AForm,
    FormItem as AFormItem,
    Select as ASelect,
    Tag as ATag,
    SelectOption as ASelectOption,
    Button as AButton,
    Space as ASpace,
    Alert as AAlert,
    Divider as ADivider,
  } from 'ant-design-vue';

  const props = defineProps({
    datasetLabels: {
      type: Array,
      default: () => [],
    },
    templateLabels: {
      type: Array,
      default: () => [],
    },
  });

  const emit = defineEmits(['update:labelMappings']);

  const formRef = ref();
  const formState = reactive({
    mappings: {},
  });
  const autoMappedLabels = reactive({});

  const isAutomaticallyMapped = reactive({});

  // 计算哪些标签不是标准标签（需要映射）
  const unmappedLabels = computed(() => {
    return props.datasetLabels.filter((label) => !isStandardLabel(label));
  });

  // 判断是否为标准标签
  function isStandardLabel(datasetLabel) {
    return props.templateLabels.some((templateLabel) => templateLabel.name.toLowerCase() === datasetLabel.name.toLowerCase());
  }

  // 自动映射相同名称的标签
  function autoMap() {
    props.datasetLabels.forEach((datasetLabel) => {
      const matchingTemplateLabel = props.templateLabels.find(
        (templateLabel) => templateLabel.name.toLowerCase() === datasetLabel.name.toLowerCase(),
      );

      if (matchingTemplateLabel) {
        formState.mappings[datasetLabel.id] = matchingTemplateLabel.id;
        isAutomaticallyMapped[datasetLabel.id] = true;
        // 存储自动映射关系
        autoMappedLabels[datasetLabel.id] = matchingTemplateLabel;
      }
    });
  }

  // 清除所有映射，保留自动映射的
  function clearAllMappings() {
    props.datasetLabels.forEach((label) => {
      if (!isAutomaticallyMapped[label.id]) {
        formState.mappings[label.id] = null;
      }
    });
  }

  const filterOption = (input, option) => {
    return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0;
  };

  // 初始化默认映射
  onMounted(() => {
    props.datasetLabels.forEach((datasetLabel) => {
      const matchingTemplateLabel = props.templateLabels.find(
        (templateLabel) => templateLabel.name.toLowerCase() === datasetLabel.name.toLowerCase(),
      );

      if (matchingTemplateLabel) {
        formState.mappings[datasetLabel.id] = matchingTemplateLabel.id;
        isAutomaticallyMapped[datasetLabel.id] = true;
        // 初始化时存储自动映射关系
        autoMappedLabels[datasetLabel.id] = matchingTemplateLabel;
      }
    });
  });

  // 监听映射变化，向父组件发送最新映射
  watch(
    () => formState.mappings,
    (newMappings) => {
      emit('update:labelMappings', { ...newMappings });
    },
    { deep: true },
  );

  // 验证表单
  async function validate() {
    try {
      await formRef.value.validate();
      return formState.mappings;
    } catch (error) {
      return false;
    }
  }

  // 暴露验证方法给父组件
  defineExpose({
    validate,
    formState,
  });
</script>

<style scoped>
  .label-mapping-container {
    padding: 16px;
    max-width: 800px;
    margin: 0 auto;
  }

  .margin-bottom {
    margin-bottom: 16px;
  }

  .action-buttons {
    display: flex;
    justify-content: flex-start;
  }

  .mapping-list {
    max-height: 400px;
    overflow-y: auto;
    border-radius: 4px;
    padding: 12px;
  }

  .mapping-item {
    max-height: 50vh;
    display: flex;
    align-items: center;
    margin-bottom: 8px;
    width: 100%;
  }

  .dataset-label {
    margin-left: 10vh;
    min-width: 120px;
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex-shrink: 0;
  }

  .mapping-arrow {
    margin: 0 32px;
    color: #999;
    flex-shrink: 0;
  }

  .template-label-select {
    flex: 1;
    width: 100%;
  }

  /* 让表单项占满容器宽度 */
  :deep(.ant-form-item) {
    width: 100%;
    margin-bottom: 8px;
  }
  .item-divider {
    margin: 0;
    opacity: 0.7;
  }
</style>
