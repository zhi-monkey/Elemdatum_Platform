<template>
  <div class="info-card" @click="handleClick">
    <div class="info-card-inner">
      <slot name="image">
        <!--        <i class="el-icon-plus"></i>-->
      </slot>
      <div class="content">
        <slot name="desc"></slot>
        <slot name="action"></slot>
      </div>
    </div>
  </div>
</template>

<script>
  import { defineComponent } from 'vue';
  import { noop } from '/@/utils/dubhe';

  export default defineComponent({
    name: 'InfoCard',
    props: {
      handleClick: {
        type: Function,
        default: noop,
      },
    },
  });
</script>
<style lang="scss">
  @import 'src/assets/scss/variables.scss';

  .info-card {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100px;
    height: 100px;
    overflow: hidden;
    font-size: 12px;
    color: #606266;
    cursor: pointer;
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    outline: none;

    .info-card-inner {
      text-align: center;
      .exception {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 100%;
        color: $infoColor;
        text-align: center;
        font-size: larger;

        .imgBlock {
          font-size: 144px;
        }
      }
    }

    i {
      margin-bottom: 8px;
      font-size: 28px;
      color: #8c939d;
    }

    &:hover,
    &:focus {
      i {
        opacity: 0.8;
      }
    }
  }
</style>
