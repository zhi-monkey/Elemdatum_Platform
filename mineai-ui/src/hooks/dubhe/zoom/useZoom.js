// zoom hook，用于图片、容器缩放管理
import { reactive } from 'vue';
import { getBounding } from '../../../utils/dubhe';

function useZoom(
  initialZoom,
  wrapperRef,
  options = {
    max: 4,
    min: 0.2,
  },
) {
  const state = reactive(initialZoom);
  const { max, min } = options;

  function bounding() {
    const bounding = wrapperRef.value ? getBounding(wrapperRef.value) : {};
    return bounding;
  }

  function updateZoom({ newZoom, zoom, zoomX, zoomY }) {
    const { width, height } = bounding(wrapperRef.value);
    const result = {
      zoomX: width / 2 - (newZoom / zoom) * (width / 2 - zoomX),
      zoomY: height / 2 - (newZoom / zoom) * (height / 2 - zoomY),
      zoom: newZoom,
    };
    Object.assign(state, result);
  }

  function zoomIn() {
    const { zoom, zoomX, zoomY } = state;
    // 浮点数异常处理
    const newZoom = zoom >= max ? max : (zoom * 10 + 1) / 10;
    updateZoom({ newZoom, zoom, zoomX, zoomY });
  }

  function zoomOut() {
    const { zoom, zoomX, zoomY } = state;
    // 浮点数异常处理
    const newZoom = zoom <= min ? min : (zoom * 10 - 1) / 10;
    updateZoom({ newZoom, zoom, zoomX, zoomY });
  }

  function setZoom({ zoom, zoomX, zoomY }) {
    Object.assign(state, {
      zoom,
      zoomX,
      zoomY,
    });
  }

  function reset() {
    updateZoom({ newZoom: 1, zoom: 1, zoomX: 0, zoomY: 0 });
  }

  function getZoom() {
    return state;
  }

  return {
    zoom: state,
    getZoom,
    setZoom,
    zoomIn,
    zoomOut,
    reset,
  };
}

export default useZoom;
