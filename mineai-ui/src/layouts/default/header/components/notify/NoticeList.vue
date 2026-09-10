<template>
  <List :class="prefixCls" :pagination="false">
    <template v-for="item in list" :key="item.id">
      <a-list-item
        class="list-item"
        :class="{ 'unread-item': item.readStatus === 0 }"
        style="position: relative; overflow: visible !important"
        @click="handleTitleClick(item)"
      >
        <div
          class="unread-indicator"
          v-if="item.readStatus === 0"
          style="display: block !important; visibility: visible !important; opacity: 1 !important"
        ></div>

        <a-list-item-meta style="overflow: visible !important">
          <template #title>
            <div class="title" style="position: relative">
              <a-typography-paragraph
                style="width: 100%; margin-bottom: 0 !important; padding-right: 20px"
                :style="{ cursor: isTitleClickable ? 'pointer' : '' }"
                :delete="!!item.titleDelete"
                :ellipsis="
                  $props.titleRows && $props.titleRows > 0
                    ? { rows: $props.titleRows, tooltip: !!item.title }
                    : false
                "
                :content="item.title"
              />
              <div class="extra" v-if="item.extra">
                <Tag class="tag" :color="item.color">
                  {{ item.extra }}
                </Tag>
              </div>
            </div>
          </template>

          <template #avatar v-if="item.avatar">
            <Avatar class="avatar" :src="item.avatar" />
          </template>

          <template #description>
            <div style="position: relative">
              <div class="description" v-if="item.description">
                <a-typography-paragraph
                  style="width: 100%; margin-bottom: 0 !important"
                  :ellipsis="
                    $props.descRows && $props.descRows > 0
                      ? { rows: $props.descRows, tooltip: !!item.description }
                      : false
                  "
                  :content="item.description"
                />
              </div>
              <div class="datetime">
                {{ item.datetime }}
              </div>
            </div>
          </template>
        </a-list-item-meta>
      </a-list-item>
    </template>
  </List>
</template>
<script lang="ts">
  import { computed, defineComponent, PropType } from 'vue';
  import { ListItem } from './data';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { List, ListItem as AListItem, Avatar, Tag, Typography } from 'ant-design-vue';
  export default defineComponent({
    components: {
      AListItem,
      [Avatar.name]: Avatar,
      [List.name]: List,
      [List.Item.name]: List.Item,
      AListItemMeta: List.Item.Meta,
      ATypographyParagraph: Typography.Paragraph,
      [Tag.name]: Tag,
      Tag,
      Avatar,
    },
    props: {
      list: {
        type: Array as PropType<ListItem[]>,
        default: () => [],
      },
      pageSize: {
        type: [Boolean, Number] as PropType<Boolean | Number>,
        default: 5,
      },
      currentPage: {
        type: Number,
        default: 1,
      },
      titleRows: {
        type: Number,
        default: 1,
      },
      descRows: {
        type: Number,
        default: 2,
      },
      onTitleClick: {
        type: Function as PropType<(Recordable) => void>,
      },
    },
    emits: ['title-click'],
    setup(props, { emit }) {
      const { prefixCls } = useDesign('header-notify-list');
      const isTitleClickable = computed(() => !!props.onTitleClick);

      function handleTitleClick(item: ListItem) {
        props.onTitleClick && props.onTitleClick(item);
        emit('title-click', item);
      }

      return { prefixCls, handleTitleClick, isTitleClickable };
    },
  });
</script>
<style lang="less" scoped>
  @prefix-cls: ~'@{namespace}-header-notify-list';

  .@{prefix-cls} {
    overflow: visible !important;

    &::-webkit-scrollbar {
      display: none;
    }

    ::v-deep(.ant-pagination-disabled) {
      display: inline-block !important;
    }

    ::v-deep(.ant-list) {
      overflow: visible !important;
    }

    ::v-deep(.ant-list-items) {
      overflow: visible !important;
    }

    ::v-deep(.ant-list-item) {
      position: relative !important;
      overflow: visible !important;
      padding: 16px 24px !important;
      margin-right: 8px !important;
      border-bottom: 1px solid rgba(59, 130, 246, 0.1) !important;
      background: transparent !important;
      transition: all 0.3s ease !important;
      cursor: pointer;

      &:last-child {
        border-bottom: none !important;
      }

      .unread-indicator {
        position: absolute !important;
        top: 20px !important;
        right: 20px !important;
        width: 10px !important;
        height: 10px !important;
        border-radius: 50% !important;
        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%) !important;
        box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.2), 0 2px 8px rgba(239, 68, 68, 0.4) !important;
        animation: dotPulse 2s ease-in-out infinite !important;
        z-index: 9999 !important;
        pointer-events: none !important;
        display: block !important;
        visibility: visible !important;
        opacity: 1 !important;
      }

      .ant-list-item-meta {
        overflow: visible !important;
        width: 100%;

        .ant-list-item-meta-content {
          overflow: visible !important;
          width: 100%;
          position: relative;
        }
      }

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        width: 3px;
        height: 100%;
        background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
        transform: scaleY(0);
        transition: transform 0.3s ease;
      }

      &:hover {
        background: rgba(59, 130, 246, 0.08) !important;
        transform: translateX(4px);

        &::before {
          transform: scaleY(1);
        }
      }

      &:active {
        transform: translateX(2px);
      }
    }

    .title {
      margin-bottom: 8px;
      font-weight: normal;
      position: relative;
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      width: 100%;

      .ant-typography {
        flex: 1;
        margin-bottom: 0 !important;
        padding-right: 16px; // 为小圆点留出空间
      }

      .extra {
        flex-shrink: 0;
        margin-top: -1.5px;
        font-weight: normal;

        .tag {
          margin-right: 0;
        }
      }
    }

    .avatar {
      margin-top: 4px;
    }

    .description {
      font-size: 12px;
      line-height: 18px;
      color: #94a3b8;
    }

    .datetime {
      margin-top: 4px;
      font-size: 12px;
      line-height: 18px;
      color: #64748b;
    }
  }

  @keyframes dotPulse {
    0%,
    100% {
      transform: scale(1);
      opacity: 1;
    }
    50% {
      transform: scale(1.2);
      opacity: 0.8;
    }
  }
</style>
