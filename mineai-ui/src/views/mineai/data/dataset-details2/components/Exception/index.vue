<template>
  <div class="exception">
    <div class="exception-inner">
      <div class="imgBlock">
        <slot name="image">
          <!--          <i class="el-icon-receiving"></i>-->
        </slot>
      </div>
      <div class="content">
        <slot name="desc"> 暂无内容 </slot>
        <slot name="action"></slot>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'Exception',
  };
</script>
<style lang="scss">
  @import 'src/assets/scss/variables.scss';

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
</style>
