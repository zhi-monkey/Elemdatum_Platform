<template>
  <li :key="item.id" :class="rootClass + '__item'">
    <div v-if="!isMultiple" :class="thumbnailClass">
      <img :src="imgUrl" :alt="item.alt" :class="rootClass + '__img'" />
      <label :class="rootClass + '__lbl'">
        {{ item.alt }}
      </label>
    </div>
    <div
      v-if="isMultiple"
      style="position: relative"
      :class="thumbnailClass"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
    >
      <div v-show="!!isHover" style="position: absolute; left: 2%; margin-top: 1%">
        <Button ghost shape="circle" @click.stop size="small" @click="showModal">
          <template #icon>
            <ZoomInOutlined />
          </template>
        </Button>
      </div>
      <img
        :src="imgUrl"
        :alt="item.alt"
        style="display: block; height: 350px; margin-bottom: 3px; object-fit: cover"
        @click="$emit('clickImg', item)"
      />
      <div v-if="isShowModal" id="modal" class="modal" @click="hideModal">
        <div class="modal-button">
          <Button type="primary" @click.stop @click="$emit('goAnnotateImg')">
            <template #icon>
              <ArrowRightOutlined />
            </template>
            标注
          </Button>
        </div>
        <img :src="imgUrl" :alt="item.alt" id="modal-image" class="modal-image" />
        <div>
          <span style="font-size: medium">{{ item.name }}</span>
        </div>
      </div>
      <div v-if="imageTagVisible && !isStatus(item, 'UNANNOTATED')" class="image-tag">
        <el-tag :hit="false" :color="statusInfo.color">
          {{ statusInfo.text }}
        </el-tag>
        <el-tag
          v-for="label in labels"
          :key="label.name"
          :hit="false"
          :color="label.color"
          :title="label.name"
          class="mr-4"
          :style="getStyle(label)"
        >
          {{ label.name }}
        </el-tag>
      </div>
      <el-checkbox
        v-show="showOption"
        v-model="checked"
        @change="(checked) => $emit('change', item, checked)"
      />
      <!--      <el-checkbox-->
      <!--        v-show="showOption"-->
      <!--        v-model="checked"-->
      <!--        :label="`${item.name}${basename}`"-->
      <!--        @change="(checked) => $emit('change', item, checked)"-->
      <!--      />-->
    </div>
  </li>
</template>
<script>
  import { computed, inject, ref } from 'vue';
  import { uniqBy } from 'lodash-es';
  import { isStatus, findKey, fileCodeMap, templateTypeSymbol } from '../../util';
  import { bucketHost } from '/@/utils/dubhe/minIO';
  import { colorByLuminance } from '/@/utils/dubhe';
  import { imgStatusMap } from './consts';
  import { getBasename } from '/@/views/mineai/data/dataset-details2/components/UploadForm/util';
  import { Button } from 'ant-design-vue';
  import { ZoomInOutlined, ArrowRightOutlined } from '@ant-design/icons-vue';

  export default {
    name: 'ImageItem',
    components: { Button, ZoomInOutlined, ArrowRightOutlined },
    props: {
      item: {
        type: Object,
        default: () => ({}),
      },
      selectImgId: {
        type: Number,
      },
      selectedIds: {
        type: Array,
        default: () => [],
      },
      isHover: {
        type: Boolean,
        default: false,
      },
      rootClass: {
        type: String,
        default: 'vue-select-image',
      },
      isMultiple: {
        type: Boolean,
      },
      imageTagVisible: {
        type: Boolean,
      },
      categoryId2Name: {
        type: Object,
        default: () => ({}),
      },
    },
    emits: ['hoverId', 'change', 'clickImg', 'goAnnotateImg'],
    setup(props, ctx) {
      const templateType = inject(templateTypeSymbol, ''); // 公共组件，templateTypeSymbol可能上游未provide,需要默认值
      const getColor = (item) => {
        return props.categoryId2Name[item.category_id]?.color || null;
      };
      const getName = (item) => {
        return props.categoryId2Name[item.category_id]?.name || '-';
      };

      const thumbnailClass = computed(() => {
        const baseClass = `${props.rootClass}__thumbnail`;
        const baseMultipleClass = `${baseClass} is--multiple`;

        if (props.isMultiple) {
          return props.selectedIds.includes(props.item.id)
            ? `${baseMultipleClass} ${baseClass}--selected`
            : baseMultipleClass;
        }

        return props.selectImgId === props.item.id
          ? `${baseClass} ${baseClass}--selected`
          : baseClass;
      });

      const imgUrl = computed(() => `${bucketHost}/${props.item.url}`);

      // 文件标注状态
      const statusInfo = computed(() => imgStatusMap[findKey(props.item.status, fileCodeMap)]);

      const showOption = computed(
        () => !!props.isHover || props.selectedIds.includes(props.item.id),
      );

      // 是否选中
      const checked = computed(() => {
        return props.selectedIds.includes(props.item.id);
      });

      // 文件后缀
      const basename = computed(() => getBasename(props.item.url));

      const labels = computed(() => {
        try {
          const annotation = JSON.parse(props.item.annotation) || [];
          const uniqAnnotation = uniqBy(
            annotation.sort((a, b) => b.score - a.score),
            'category_id',
          ); // 标注按照score降序排序并去重
          const result =
            uniqAnnotation.map((d) => ({
              color: getColor(d),
              name: getName(d),
            })) || [];
          // 区分多标签和单标签
          return templateType.value === 'multiple-label' ? result : result.slice(0, 1);
        } catch (err) {
          console.error(err);
          return [];
        }
      });

      const getStyle = (label) => ({
        color: colorByLuminance(label.color),
      });

      const handleMouseEnter = () => {
        ctx.emit('hoverId', props.item.id);
      };

      const handleMouseLeave = () => {
        ctx.emit('hoverId', null);
      };

      const isShowModal = ref(false);

      function showModal() {
        isShowModal.value = true;
      }

      function hideModal() {
        isShowModal.value = false;
      }

      return {
        templateType,
        thumbnailClass,
        imgUrl,
        labels,
        getStyle,
        isStatus,
        statusInfo,
        showOption,
        checked,
        basename,
        handleMouseEnter,
        handleMouseLeave,
        isShowModal,
        showModal,
        hideModal,
      };
    },
  };
</script>
<style lang="scss" scoped>
  .modal {
    display: flex;
    flex-direction: column;
    align-items: center;
    position: fixed;
    z-index: 9999;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 3, 7, 0.6);
  }

  .modal-image {
    display: block;
    max-width: 90%;
    max-height: 90%;
  }

  .modal-button {
    margin-top: 20px;
    margin-bottom: 10px;
  }

  //.vue-select-image__thumbnail >>> .el-checkbox {
  //  position: relative;
  //  display: block;
  //  .el-checkbox__label {
  //    position: absolute;
  //    left: 23px;
  //    top: 50%;
  //    transform: translate(0, -50%);
  //    width: calc(100% - 25px);
  //    text-overflow: ellipsis;
  //    overflow: hidden;
  //    white-space: nowrap;
  //  }
  //}
</style>
