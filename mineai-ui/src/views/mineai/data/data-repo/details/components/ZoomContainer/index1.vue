<script lang="jsx">
  import { onMounted, ref, toRefs, reactive, watch } from 'vue';
  import { select as d3Select, event as d3Event } from 'd3-selection';
  import { zoom as d3Zoom, zoomIdentity, zoomTransform as d3ZoomTransform } from 'd3-zoom';
  import * as d3 from 'd3';
  import { isFunction, isEqual } from 'lodash-es';

  function zoomTransformFromProps(props) {
    const { zoom, zoomX, zoomY } = props;
    return zoomIdentity.translate(zoomX || 0, zoomY || 0).scale(zoom || 1);
  }

  export default {
    name: 'ZoomContainer',
    props: {
      controlled: {
        type: Boolean,
        default: false,
      },
      disableMouseWheelZoom: {
        type: Boolean,
        default: false,
      },
      zoomX: {
        type: Number,
        default: 0,
      },
      zoomY: {
        type: Number,
        default: 0,
      },
      zoom: {
        type: Number,
        default: 1,
      },
      minZoom: {
        type: Number,
        default: 0.1,
      },
      maxZoom: {
        type: Number,
        default: 8,
      },
      filter: Function,
      onZoom: Function,
    },
    setup(props) {
      // eslint-disable-next-line vue/no-setup-props-destructure
      const { controlled, minZoom, maxZoom, disableMouseWheelZoom, onZoom, filter } = props;
      const wrapperRef = ref(null);

      let zoomInstance = null;

      // let selection = null;

      // 缩放状态
      const state = reactive({
        lastZoomTransform: null,
        selection: null,
        zoomKey: Math.random(), // only trigger by user Action
      });

      // 缩放控制器
      const handleZoom = (...args) => {
        const nextZoomTransform = d3Event.transform;

        if (controlled) {
          const { selection, lastZoomTransform } = state;

          selection.on('zoom', null);
          // zoomInstance.transform(selection, lastZoomTransform);
          selection.call(d3Zoom.transform, lastZoomTransform);
          selection.on('zoom', handleZoom);
        } else {
          Object.assign(state, { zoomKey: Math.random() });
        }

        typeof onZoom === 'function' && onZoom(nextZoomTransform, ...args);
      };

      const _updateZoomProps = () => {
        if (isFunction(filter)) zoomInstance.filter(filter);
        // if (isFunction(filter)) selection.call(d3Zoom.filter, filter);
      };

      onMounted(() => {
        // 获取初始缩放比例
        const initialZoomTransform = zoomTransformFromProps(props);
        // 获取容器 Dom 实例
        const wrapper = wrapperRef.value;
        // d3 选择器
        const selection = d3.select(wrapper);

        // 获取 zoom 实例
        zoomInstance = d3Zoom().scaleExtent([minZoom, maxZoom]);
        // selection.call(zoomInstance);
        _updateZoomProps();
        zoomInstance.on('zoom', handleZoom);

        if (disableMouseWheelZoom) {
          selection.call(zoomInstance).on('wheel.zoom', null);
        } else {
          selection.call(zoomInstance);
        }

        // todo: 下面报错了
        // zoomInstance.transform(selection, initialZoomTransform);
        // _updateZoomProps();
        // zoomInstance.on('zoom', handleZoom);

        selection.call(d3Zoom.transform, initialZoomTransform);

        Object.assign(state, {
          selection,
          lastZoomTransform: initialZoomTransform,
        });
      });

      watch(
        () => [props.zoom, props.zoomX, props.zoomY],
        (nextProps, prevProps) => {
          const hasChangedZoom = !isEqual(nextProps, prevProps);

          const nextZoomProps = {
            zoom: nextProps[0],
            zoomX: nextProps[1],
            zoomY: nextProps[2],
          };

          if (hasChangedZoom) {
            const { selection } = state;
            selection.on('zoom', null);
            const nextZoomTransform = zoomTransformFromProps(nextZoomProps);
            // zoomInstance.transform(state.selection, nextZoomTransform);
            selection.call(d3Zoom.transform, nextZoomTransform);
            selection.on('zoom', handleZoom);
            state.lastZoomTransform = nextZoomTransform;
            // 需要强制刷新，vue sucks
            state.zoomKey = Math.random();
          }

          _updateZoomProps();
        },
      );

      return {
        wrapperRef,
        ...toRefs(state),
      };
    },
    render() {
      const zoomTransform = this.wrapperRef ? d3ZoomTransform(this.wrapperRef) : {};

      const { x, y, k } = zoomTransform;

      const innerStyle = {
        transform: `translate(${x || 0}px, ${y || 0}px) scale(${k || 1})`,
      };

      console.log('this.$slots', this.$slots);

      return (
        <div ref="wrapperRef" class="zoom-wrapper">
          <span class="dn">{this.zoomKey}</span>
          <div class="zoom-inner" style={innerStyle}>
            {this.$slots.default}
          </div>
        </div>
      );
    },
  };
</script>

<style lang="css" scoped>
  .zoom-inner {
    transform-origin: 0 0;
  }
</style>
