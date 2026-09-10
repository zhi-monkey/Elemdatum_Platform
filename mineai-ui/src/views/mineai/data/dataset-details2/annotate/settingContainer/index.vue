<template>
  <div class="workspace-settings">
    <el-form label-position="top" @submit.enter.prevent>
      <el-form-item label="数据集名称">
        <div style="margin-top: -10px">
          <Tag>{{ state.datasetInfo.name }}</Tag>
        </div>
      </el-form-item>
      <el-form-item label="标注类型">
        <div style="margin-top: -10px">
          <Tag type="warning">{{ annotationTypeName }}</Tag>
        </div>
      </el-form-item>
      <!--      <el-form-item v-if="state.datasetInfo.labelGroupId" label="标签组">-->
      <!--        <div style="margin-top: -10px">-->
      <!--          <Tag type="success">{{ state.datasetInfo.labelGroupName }}</Tag>-->
      <!--                    <el-link-->
      <!--                      target="_blank"-->
      <!--                      type="primary"-->
      <!--                      :underline="false"-->
      <!--                      class="vm"-->
      <!--                      :href="`/data/labelgroup/detail?id=${state.datasetInfo.labelGroupId}`"-->
      <!--                    >-->
      <!--                      查看详情-->
      <!--                    </el-link>-->
      <!--        </div>-->
      <!--      </el-form-item>-->
      <LabelList
        :labels="labels"
        :editLabel="edit"
        :action-disabled="props.state.fromHistory"
        :annotations="annotations"
        :annotationType="annotationType"
        :currentAnnotationId="api.currentAnnotationId"
        :updateState="updateState"
        :getColorLabel="getColorLabel"
        :findRowIndex="findRowIndex"
        :state="props.state"
      >
        <template #addLabel>
          <AddLabelUnified
            v-if="showAddLabel"
            @handle-create="handleLabelCreate"
          />
        </template>
      </LabelList>
      <Annotations
        :annotations="annotations"
        :annotationType="annotationType"
        :currentAnnotationId="state.currentAnnotationId"
        :labels="labels"
        :action-disabled="props.state.fromHistory"
        :updateState="updateState"
        :getColorLabel="getColorLabel"
        :findRowIndex="findRowIndex"
        :deleteAnnotation="deleteAnnotation"
      />
      <!--      <Enhance-->
      <!--        v-if="!isTrack && state.hasEnhanceRecord"-->
      <!--        :fileInfo="state.fileInfo"-->
      <!--        :fileId="state.fileId"-->
      <!--        :datasetId="state.datasetId"-->
      <!--        :annotateType="state.datasetInfo.annotateType"-->
      <!--      />-->
      <!--      <Footer-->
      <!--        :isTrack="isTrack"-->
      <!--        :updateState="updateState"-->
      <!--        :showScore="state.showScore"-->
      <!--        :toggleShowScore="toggleShowScore"-->
      <!--        :showTag="state.showTag"-->
      <!--        :toggleShowTag="toggleShowTag"-->
      <!--        :showId="state.showId"-->
      <!--        :toggleShowId="toggleShowId"-->
      <!--        :isSegmentation="isSegmentation"-->
      <!--      />-->
    </el-form>
  </div>
</template>

<script>
  import { ElMessage as Message, ElTag as Tag } from 'element-plus';
  import { computed, inject, onMounted, reactive, watch } from 'vue';
  import {
    editLabel,
    generateRandomHexColor,
    getAllLabelTemplate,
    getAutoLabels,
  } from '../../api/index.ts';
  import { annotationBy, isPresetDataset, labelGroupTypeMap, labelsSymbol } from '../../util';
  import AddLabelUnified from '/@/views/mineai/data/dataset-details2/annotate/settingContainer/addLabelUnified.vue';
  import LabelList from '/@/views/mineai/data/dataset-details2/annotate/settingContainer/labelList/index.vue';
  import Annotations from '/@/views/mineai/data/dataset-details2/annotate/settingContainer/annotations.vue';
  import Enhance from '/@/views/mineai/data/dataset-details2/annotate/settingContainer/enhance.vue';
  // import Footer from '/@/views/mineai/data/dataset-details2/annotate/settingContainer/footer.vue';
  import { generateColors } from '../../../../../../../build/config/themeConfig';

  const annotationByCode = annotationBy('code');

  export default {
    name: 'SettingContainer',
    components: {
      AddLabelUnified,
      Annotations,
      // Footer,
      LabelList,
      Enhance,
      Tag,
    },
    props: {
      createLabel: Function,
      getColorLabel: Function,
      updateState: Function,
      queryLabels: Function,
      findRowIndex: Function,
      deleteAnnotation: Function,
      state: Object,
      isTrack: Boolean,
      isSegmentation: Boolean,
      annotationType: String,
    },
    emits: ['label-created'],
    setup(props, { emit }) {
      // eslint-disable-next-line vue/no-setup-props-destructure
      const { createLabel, updateState, queryLabels, annotationType } = props;
      const api = reactive({
        selectedLabel: undefined,
        newLabel: undefined,
        newLabelColor: undefined,
        currentAnnotationId: props.state.currentAnnotationId || undefined,
      });
      // 当前所有标签信息
      const labels = inject(labelsSymbol);

      const annotations = computed(() => props.state[annotationType]);

      const annotationTypeName = computed(() =>
        annotationByCode(props.state.datasetInfo.annotateType, 'name'),
      );
      // 检查标签是否已存在（大小写不敏感）
      const isLabelExistsCaseInsensitive = (labelName) => {
        const lowerName = labelName.toLowerCase();
        return labels.value.some((d) => d.name.toLowerCase() === lowerName);
      };

      // 更新标签
      const refreshLabel = async () => {
        const nextLabels = await queryLabels();
        // 更新全局 provide
        updateState({
          labels: nextLabels,
        });
      };

      // 编辑标签
      const edit = (labelId, data) => {
        return editLabel(labelId, data, props.state.datasetId).then(refreshLabel);
      };

      // 新建自定义标签
      const handleLabelCreate = async (id, form) => {
        const newLabelName = form.name;
        const newLabelColor = form.color;

        if (!newLabelName) {
          Message.warning('请输入标签名称');
          return;
        }

        // 检查标签是否已经存在（大小写不敏感）
        if (isLabelExistsCaseInsensitive(newLabelName)) {
          const existingLabel = labels.value.find(
            (d) => d.name.toLowerCase() === newLabelName.toLowerCase(),
          );
          Message.warning(
            `当前数据集已存在相同标签[${existingLabel?.name || newLabelName}]（不区分大小写）`,
          );
          return;
        }

        try {
          await createLabel({ name: newLabelName, color: newLabelColor });
          Message.success(`标签[${newLabelName}]创建成功`);
          const nextLabels = await queryLabels();

          const newlyCreatedLabel = nextLabels.find((label) => label.name === newLabelName);

          if (newlyCreatedLabel) {
            updateState({
              labels: nextLabels,
              lastSelectedLabel: newlyCreatedLabel.id,
            });
          } else {
            updateState({ labels: nextLabels });
            console.error(
              'Could not find the newly created label by name. The label list is updated, but the default selection may not work.',
            );
          }
          api.newLabel = undefined;
          api.newLabelColor = undefined;
          emit('label-created');
        } catch (error) {
          console.error(error);
          Message.error(`创建标签失败: ${error.message || '未知错误'}`);
        }
      };

      const getSystemLabel = () => {
        getAutoLabels(labelGroupTypeMap.VISUAL.value).then((res) => {
          const labelsObj = res.map((item) => ({
            value: item.id,
            label: item.name,
            color: item.color,
            chosen: false,
          }));
          Object.assign(api, {
            systemLabels: labelsObj,
          });
        });
      };

      const toggleShowScore = (val) => {
        updateState({
          showScore: val,
        });
      };

      const toggleShowTag = (val) => {
        const newState = {
          showTag: val,
        };
        // 视频跟踪模式下标签和标注 Id 互斥
        if (!!val && !!props.isTrack) {
          newState.showId = false;
        }
        updateState(newState);
      };

      const toggleShowId = (val) => {
        const newState = {
          showId: val,
        };
        // 视频跟踪模式下标签和标注 Id 互斥
        if (!!val && !!props.isTrack) {
          newState.showTag = false;
        }
        updateState(newState);
      };

      // 预置数据集不支持新建标签
      // const showAddLabel = computed(() => !isPresetDataset(props.state.datasetInfo.type));
      const showAddLabel = computed(() => {
        return (
          !isPresetDataset(props.state.datasetInfo.type) &&
          props.state.datasetInfo.isGuided === false
        );
      });

      watch(
        () => props.state.currentAnnotationId,
        (next) => {
          api.currentAnnotationId = next || undefined;
        },
      );

      onMounted(() => {
        getSystemLabel();
      });

      return {
        api,
        toggleShowScore,
        toggleShowTag,
        toggleShowId,
        labels,
        edit,
        handleLabelCreate,
        showAddLabel,
        annotations,
        annotationTypeName,
        props,
      };
    },
  };
</script>
<style lang="scss">
  @import 'src/assets/scss/variables.scss';

  .workspace-settings {
    margin-top: 8px;
    padding: 16px 28px 0;
    overflow-y: auto;
    background-color: #181d31;
    box-shadow: 0 6px 20px 0 rgba(0, 0, 0, 0.25);

    .el-form-item {
      display: flex;
      flex-direction: column;
      margin-bottom: 10px;

      &::before,
      &::after {
        content: none;
      }

      .el-form-item__label {
        font-size: medium;
        font-weight: bold;
      }
    }

    .setting-container-footer {
      margin-top: 5px;
    }

    .el-icon-edit {
      margin-left: 4px;
    }
  }

  @media (max-width: 1440px) {
    .workspace-settings {
      padding: 10px 15px 0;
    }
  }
</style>
