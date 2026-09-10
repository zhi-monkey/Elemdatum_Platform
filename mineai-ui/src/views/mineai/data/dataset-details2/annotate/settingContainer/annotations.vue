<template>
  <div class="annotation-table mb-10">
    <el-form-item label="全部标注" style="margin-bottom: 0" />
    <el-table
      :data="notions"
      fit
      style="width: 100%"
      max-height="600"
      :row-class-name="rowClass"
      @row-click="handleRowClick"
    >
      <el-table-column label="标注 Id" width="110">
        <template #default="{ row }">
          <span>{{ row.name }}</span>
          <Edit
            v-if="!props.actionDisabled"
            :row="row"
            title="修改标注 Id"
            valueBy="name"
            rules="required"
            label="标注 Id "
            @handle-ok="handleEdit"
          />
        </template>
      </el-table-column>
      <el-table-column label="标签类型" width="110">
        <template #default="{ row }">
          <span>{{ row.labelName }}</span>
          <EditLabel
            :row="row"
            :labels="labels"
            :handleEditLabel="handleEditLabel(row)"
            v-if="!props.actionDisabled"
          />
        </template>
      </el-table-column>
      <!--      不展示置信度-->
      <!--      <el-table-column v-if="showScore" label="置信分" :width="72">-->
      <!--      <el-table-column v-if="false" label="置信分" :width="72">-->
      <!--        <template #default="{ row }">-->
      <!--          <el-tooltip effect="dark" :content="String(row.data.rawScore)" placement="top">-->
      <!--            <span>{{ row.data._score || '&#45;&#45;' }}</span>-->
      <!--          </el-tooltip>-->
      <!--        </template>-->
      <!--      </el-table-column>-->
      <el-table-column label="操作">
        <template #default="{ row }" v-if="!props.actionDisabled">
          <el-popconfirm title="确定删除这个标注吗？" @confirm="() => onConfirm(row)">
            <template #reference>
              <DeleteOutlined style="color: #108ee9; cursor: pointer" />
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
  import { isNil } from 'lodash-es';
  import { reactive, computed, watch } from 'vue';
  import { leadingZero, replace, toFixed } from '/@/utils/dubhe';
  import Edit from '/@/views/mineai/data/dataset-details2/components/InlineTableEdit/index.vue';
  import EditLabel from '/@/views/mineai/data/dataset-details2/annotate/settingContainer/editLabel.vue';
  import { DeleteOutlined } from '@ant-design/icons-vue';

  export default {
    name: 'Annotations',
    components: {
      Edit,
      EditLabel,
      DeleteOutlined,
    },
    props: {
      annotations: {
        type: Array,
        default: () => [],
      },
      annotationType: String,
      updateState: Function,
      getColorLabel: Function,
      deleteAnnotation: Function,
      findRowIndex: Function,
      currentAnnotationId: String,
      labels: {
        type: Array,
        default: () => [],
      },
      actionDisabled: Boolean,
    },
    setup(props) {
      const { updateState, getColorLabel, findRowIndex, deleteAnnotation, annotationType } = props;
      let labelMap = {};

      const state = reactive({
        annotations: props.annotations,
        visible: false,
      });

      // 将 labels 转化为 map
      const rLabels = computed(() =>
        props.labels.reduce((acc, cur) => {
          return Object.assign(acc, {
            [cur.id]: cur.name,
          });
        }, {}),
      );

      const showScore = computed(() => annotationType !== 'shapes');

      const validTrackId = (trackId) => {
        if (isNil(trackId) || trackId === -1) return false;
        return trackId;
      };

      const withEdit = (item, isEdit = false) => {
        const { categoryId, track_id } = item.data || {};
        // 获取到分类标签名
        const labelName = rLabels.value[categoryId];
        const labelNameTxt = labelName ? `${labelName}_` : '';
        // 更新索引
        const labelIndex = !isNil(labelMap[categoryId])
          ? (labelMap[categoryId] += 1)
          : (labelMap[categoryId] = 0);

        const newIndex = leadingZero(labelIndex + 1);
        // 新创建的注释 item，执行顺序
        // 1. 获取已存在的 name
        // 2. 如果已存在 track_id
        // 3. 拼接 label + index
        const newName = (() => {
          if (item.name) return item.name;
          if (validTrackId(track_id) !== false) {
            return track_id;
          }
          return `${labelNameTxt}${newIndex}`;
        })();

        return {
          ...item,
          data: {
            ...item.data,
            rawScore: toFixed(item.data.score),
            _score: toFixed(item.data.score, 2, 0),
          },
          name: newName,
          index: newIndex,
          labelName,
          edit: isEdit,
        };
      };

      // 删除标注确认
      const onConfirm = (row) => {
        return deleteAnnotation(row.id);
      };

      // 点击行
      const handleRowClick = (row) => {
        updateState({
          currentAnnotationId: row.id,
        });
      };

      // 修改标注名称
      const handleEdit = (name, row) => {
        const updateIndex = findRowIndex(row.id);
        if (updateIndex > -1) {
          const curItem = props.annotations[updateIndex];
          // 修改 name
          const nextItem = { ...curItem, name };
          const updateList = replace(props.annotations, updateIndex, nextItem);
          updateState({
            [annotationType]: updateList,
          });
        }
      };

      // 类别变更
      const handleEditLabel = (row) => (value) => {
        const updateIndex = findRowIndex(row.id);
        if (updateIndex > -1) {
          const curItem = props.annotations[updateIndex];
          const nextItem = {
            ...curItem,
            data: {
              ...curItem.data,
              categoryId: value,
              color: getColorLabel(value),
            },
          };
          const updateList = replace(props.annotations, updateIndex, nextItem);
          updateState({
            [annotationType]: updateList,
          });
        }
      };

      const notions = computed(() => state.annotations.map((d) => withEdit(d, false)));

      const rowClass = computed(() => ({ row }) => {
        return row.id === props.currentAnnotationId ? 'activeRow' : '';
      });

      // 外部更新后同步为 state
      watch(
        () => props.annotations,
        (next) => {
          // 重置 labelMap
          labelMap = {};
          Object.assign(state, {
            annotations: next,
          });
        },
      );

      return {
        state,
        notions,
        rLabels,
        onConfirm,
        handleRowClick,
        handleEdit,
        handleEditLabel,
        rowClass,
        showScore,
        props,
      };
    },
  };
</script>
<style lang="scss">
  @import 'src/assets/scss/variables.scss';

  .annotation-table {
    .activeRow {
      td {
        background: $primaryHoverColor;
      }
    }

    .activeRow:hover {
      td {
        background-color: $primaryColor !important;
      }
    }
  }
</style>
