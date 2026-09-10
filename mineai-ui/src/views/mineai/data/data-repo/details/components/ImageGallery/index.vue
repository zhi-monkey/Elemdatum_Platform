<template>
  <div :class="rootClass" class="img-gallery">
    <ul v-if="dataImages.length" :class="rootClass + '__wrapper'">
      <ImageItem
        v-for="item in dataImages"
        :key="item.id"
        :item="item"
        :isHover="item.id === hoverId"
        :imageTagVisible="imageTagVisible"
        :selectedIds="selectedIds"
        v-bind="$attrs"
        @hover-id="handleHoverId"
        @click-img="handleClickImg"
        @change="toggleCheck"
        @go-annotate-img="$emit('goAnnotateImg', item)"
      />
    </ul>
  </div>
</template>

<script>
  import { ref, onMounted } from 'vue';
  import ImageItem from '/@/views/mineai/data/dataset-details2/components/ImageGallery/ImageItem.vue';

  export default {
    name: 'ImageGallery',
    components: {
      ImageItem,
    },
    inheritAttrs: false,
    props: {
      dataImages: {
        type: Array,
        default: () => [],
      },
      selectImgsId: {
        type: Array,
        default: () => [],
      },
      rootClass: {
        type: String,
        default: 'vue-select-image',
      },
    },
    emits: ['onSelectMultipleImage', 'clickImg', 'goAnnotateImg'],
    setup(props, ctx) {
      const selectedIds = ref([]);
      const hoverId = ref(null);
      const imageTagVisible = ref(true);

      const handleHoverId = (id) => {
        hoverId.value = id;
      };

      const hasSelected = (id) => selectedIds.value.includes(id);

      const toggleCheck = (item, checked) => {
        if (checked) {
          selectedIds.value.push(item.id);
        } else {
          selectedIds.value = selectedIds.value.filter((d) => d !== item.id);
        }

        ctx.emit('onSelectMultipleImage', selectedIds.value);
      };

      const handleClickImg = (item) => {
        ctx.emit('clickImg', item, selectedIds.value);
        if (selectedIds.value.length > 0) {
          const checked = hasSelected(item.id);
          toggleCheck(item, !checked);
        }
      };

      const resetMultipleSelection = () => {
        selectedIds.value = [];
        ctx.emit('onSelectMultipleImage', selectedIds.value);
      };

      const selectAll = () => {
        selectedIds.value = props.dataImages.map((d) => d.id);
        ctx.emit('onSelectMultipleImage', selectedIds.value);
      };

      const setImageTagVisible = (visible) => {
        imageTagVisible.value = visible;
      };

      onMounted(() => {
        selectedIds.value = [].concat(props.selectImgsId);
      });

      return {
        hoverId,
        imageTagVisible,
        resetMultipleSelection,
        selectAll,
        toggleCheck,
        selectedIds,
        handleHoverId,
        setImageTagVisible,
        handleClickImg,
      };
    },
  };
</script>

<style lang="scss" scoped>
  @import 'src/assets/scss/variables.scss';
  @import 'src/assets/scss/mixin.scss';

  .img-gallery {
    min-height: 200px;
    margin-left: 15px;
    margin-right: 5px;

    .vue-select-image__wrapper {
      padding: 0 0 2px;
      margin: 0;
      overflow: auto;
      list-style: none outside none;
      .vue-select-image__item {
        float: left;
        width: 48%;
        height: 410px;
        margin-left: 2px;
        margin-right: 2px;

        //.vue-select-image__thumbnail {
        //  position: relative;
        //  padding: 10px;
        //  line-height: 20px;
        //  border-color: transparent;
        //  border-style: solid;
        //  border-width: 1px;
        //  border-radius: 4px;
        //  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.055);
        //  transition: all 0.2s ease-in-out;
        //
        //  .vue-select-image__thumbnail:hover {
        //    border-color: $primaryColor;
        //  }
        //
        //  .vue-select-image__thumbnail--selected {
        //    border-color: $primaryColor;
        //  }
        //}

        //.vue-select-image__lbl {
        //  line-height: 3;
        //}
        //
        //.img-name-row {
        //  position: absolute;
        //  right: 3px;
        //  bottom: 3px;
        //  left: 3px;
        //  padding-left: 4px;
        //  color: #fff;
        //  background-color: $black;
        //  border-radius: 0 0 4px 4px;
        //}
        //
        //.img-name {
        //  @include text-overflow;
        //
        //  max-width: 90%;
        //  font-size: 14px;
        //  line-height: 2;
        //}
      }
    }
  }
</style>
