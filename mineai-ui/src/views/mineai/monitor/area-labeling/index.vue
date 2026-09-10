<template onresize="via._hook_on_browser_resize()">
  <div style="padding: 16px">
    <div
      style="
        background-color: #181d31;
        padding: 12px 10px;
        margin-bottom: 16px;
        border-radius: 2px;
        height: 50px;
      "
    >
      <Space size="large">
        <span style="margin-left: 10px; margin-right: 5px">算法配置</span>
        <div>
          <a-select
            v-if="!showMonitorSelect"
            v-model:value="monitorValue"
            :dropdownMatchSelectWidth="false"
            :onSelect="handleSelectMonitor"
            placeholder="请选择数据流"
            style="max-width: 200px"
            @popupScroll="handlePopupScroll"
          >
            <a-select-option
              v-for="({ monitorName, monitorId }, index) in curPlainOptions"
              :key="index"
              :value="monitorId"
            >
              {{ monitorName }}
            </a-select-option>
          </a-select>
          <Spin :spinning="showMonitorSelect" />
        </div>
        <Spin :spinning="monitorSpin" />
        <div v-if="showModelSelect">
          <a-select
            v-model:value="modelValue"
            :dropdownMatchSelectWidth="false"
            :onSelect="handleAlgorithmChoice"
            :options="modelOptions"
            placeholder="请选择算法"
            style="max-width: 200px"
          />
        </div>
        <!--        <a-popconfirm-->
        <!--          title="是否进行配置下发"-->
        <!--          ok-text="是"-->
        <!--          cancel-text="否"-->
        <!--          @confirm="confirm"-->
        <!--          @cancel="cancel"-->
        <!--          placement="bottom"-->
        <!--        >-->
        <!--          <a-button type="primary" @click="saveInfo">保存</a-button>-->
        <!--        </a-popconfirm>-->
        <a-button type="primary" @click="save" :loading="buttonLoading">配置下发</a-button>
        <a-button type="primary" @click="myReload">重新标注</a-button>
      </Space>
    </div>
    <svg
      style="display: none"
      xmlns="http://www.w3.org/2000/svg"
      xmlns:xlink="http://www.w3.org/1999/xlink"
    >
      <defs>
        <symbol id="shape_rectangle">
          <rect fill="none" height="14" stroke="black" width="18" x="3" y="5" />
        </symbol>
        <symbol id="shape_extreme_rectangle">
          <rect fill="none" height="14" stroke="black" width="18" x="3" y="5" />
          <circle cx="3" cy="10" fill="grey" r="2" stroke="black" />
          <circle cx="10" cy="19" fill="grey" r="2" stroke="black" />
          <circle cx="15" cy="5" fill="grey" r="2" stroke="black" />
          <circle cx="21" cy="14" fill="grey" r="2" stroke="black" />
        </symbol>
        <symbol id="shape_circle">
          <circle cx="12" cy="12" fill="none" r="9" stroke="black" />
        </symbol>
        <symbol id="shape_extreme_circle">
          <circle cx="12" cy="12" fill="none" r="9" stroke="black" />
          <circle cx="3" cy="10" fill="grey" r="2" stroke="black" />
          <circle cx="19" cy="6" fill="grey" r="2" stroke="black" />
          <circle cx="16" cy="20" fill="grey" r="2" stroke="black" />
        </symbol>
        <symbol id="shape_ellipse">
          <ellipse cx="12" cy="12" fill="none" rx="10" ry="8" stroke="black" />
        </symbol>
        <symbol id="shape_point">
          <circle cx="12" cy="12" fill="grey" r="3" stroke="black" />
        </symbol>
        <symbol id="shape_polygon">
          <path d="M 4 12 L 10 2 L 20 6 L 18 16 L 8 20 z" fill="none" stroke="black" />
        </symbol>
        <symbol id="shape_polyline">
          <line fill="none" stroke="black" x1="3" x2="8" y1="4" y2="18" />
          <line stroke="black" x1="8" x2="14" y1="18" y2="6" />
          <line stroke="black" x1="14" x2="20" y1="6" y2="14" />
          <circle cx="3" cy="4" r="2" stroke="black" />
          <circle cx="8" cy="18" r="2" stroke="black" />
          <circle cx="14" cy="6" r="2" stroke="black" />
          <circle cx="20" cy="14" r="2" stroke="black" />
        </symbol>
        <symbol id="shape_line">
          <line stroke="black" x1="6" x2="19" y1="6" y2="19" />
          <circle cx="6" cy="6" r="2" stroke="black" />
          <circle cx="19" cy="19" r="2" stroke="black" />
        </symbol>

        <symbol id="micon_settings">
          <path
            d="M19.43 12.98c.04-.32.07-.64.07-.98s-.03-.66-.07-.98l2.11-1.65c.19-.15.24-.42.12-.64l-2-3.46c-.12-.22-.39-.3-.61-.22l-2.49 1c-.52-.4-1.08-.73-1.69-.98l-.38-2.65C14.46 2.18 14.25 2 14 2h-4c-.25 0-.46.18-.49.42l-.38 2.65c-.61.25-1.17.59-1.69.98l-2.49-1c-.23-.09-.49 0-.61.22l-2 3.46c-.13.22-.07.49.12.64l2.11 1.65c-.04.32-.07.65-.07.98s.03.66.07.98l-2.11 1.65c-.19.15-.24.42-.12.64l2 3.46c.12.22.39.3.61.22l2.49-1c.52.4 1.08.73 1.69.98l.38 2.65c.03.24.24.42.49.42h4c.25 0 .46-.18.49-.42l.38-2.65c.61-.25 1.17-.59 1.69-.98l2.49 1c.23.09.49 0 .61-.22l2-3.46c.12-.22.07-.49-.12-.64l-2.11-1.65zM12 15.5c-1.93 0-3.5-1.57-3.5-3.5s1.57-3.5 3.5-3.5 3.5 1.57 3.5 3.5-1.57 3.5-3.5 3.5z"
          />
        </symbol>
        <symbol id="micon_save">
          <path
            d="M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z"
          />
        </symbol>
        <symbol id="micon_open">
          <path
            d="M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z"
          />
        </symbol>
        <symbol id="micon_upload">
          <path
            d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"
          />
        </symbol>
        <symbol id="micon_download">
          <path
            d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM17 13l-5 5-5-5h3V9h4v4h3z"
          />
        </symbol>
        <symbol id="micon_zoomin">
          <path
            d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"
          />
          <path d="M12 10h-2v2H9v-2H7V9h2V7h1v2h2v1z" />
        </symbol>
        <symbol id="micon_zoomout">
          <path
            d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14zM7 9h5v1H7z"
          />
        </symbol>
        <symbol id="micon_search">
          <path
            d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"
          />
        </symbol>
        <symbol id="micon_fit_screen">
          <path
            d="M17 4h3c1.1 0 2 .9 2 2v2h-2V6h-3V4zM4 8V6h3V4H4c-1.1 0-2 .9-2 2v2h2zm16 8v2h-3v2h3c1.1 0 2-.9 2-2v-2h-2zM7 18H4v-2H2v2c0 1.1.9 2 2 2h3v-2zM18 8H6v8h12V8z"
          />
        </symbol>
        <symbol id="micon_delete">
          <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z" />
        </symbol>
        <symbol id="micon_copy">
          <path
            d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"
          />
        </symbol>
        <symbol id="micon_paste">
          <path
            d="M19 2h-4.18C14.4.84 13.3 0 12 0c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm7 18H5V4h2v3h10V4h2v16z"
          />
        </symbol>
        <symbol id="micon_insertcomment">
          <path
            d="M20 2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"
          />
        </symbol>
        <symbol id="micon_edit">
          <path
            d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"
          />
        </symbol>

        <!-- File Manager -->
        <symbol id="micon_lib_add">
          <path
            d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9h-4v4h-2v-4H9V9h4V5h2v4h4v2z"
          />
        </symbol>
        <symbol id="micon_add_circle">
          <path
            d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 11h-4v4h-2v-4H7v-2h4V7h2v4h4v2z"
          />
        </symbol>
        <symbol id="micon_remove_circle">
          <path
            d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 11H7v-2h10v2z"
          />
        </symbol>
        <symbol id="micon_navigate_next">
          <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z" />
        </symbol>
        <symbol id="micon_navigate_prev">
          <path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z" />
        </symbol>
        <symbol id="micon_search">
          <path
            d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"
          />
        </symbol>
        <!-- Import/Export -->
        <symbol id="micon_import">
          <path d="M9 16h6v-6h4l-7-7-7 7h4zm-4 2h14v2H5z" />
        </symbol>
        <symbol id="micon_export">
          <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z" />
        </symbol>
        <symbol id="micon_import_export">
          <path d="M9 3L5 6.99h3V14h2V6.99h3L9 3zm7 14.01V10h-2v7.01h-3L15 21l4-3.99h-3z" />
        </symbol>

        <!-- composed by Abhishek Dutta from existing materian icons, 31 Jan. 2019 -->
        <symbol id="micon_add_image">
          <path d="M19 7v2.99s-1.99.01-2 0V7h-3s.01-1.99 0-2h3V2h2v3h3v2h-3z" />
          <path d="M5 19l3-4 2 3 3-4 4 5H5z" transform="translate(-9,-12) scale(1.8 1.8)" />
        </symbol>
        <!-- composed by Abhishek Dutta from existing materian icons, 31 Jan. 2019 -->
        <symbol id="micon_add_media">
          <path d="M19 7v2.99s-1.99.01-2 0V7h-3s.01-1.99 0-2h3V2h2v3h3v2h-3z" />
          <path d="M10 16.5l6-4.5-6-4.5v9z" transform="translate(-10,-7) scale(1.6 1.6)" />
        </symbol>
        <!-- composed by Abhishek Dutta from existing materian icons, 31 Jan. 2019 -->
        <symbol id="micon_add_audio">
          <path d="M19 7v2.99s-1.99.01-2 0V7h-3s.01-1.99 0-2h3V2h2v3h3v2h-3z" />
          <path
            d="M8 15c0-1.66 1.34-3 3-3 .35 0 .69.07 1 .18V6h5v2h-3v7.03c-.02 1.64-1.35 2.97-3 2.97-1.66 0-3-1.34-3-3z"
            transform="translate(-11,-4) scale(1.4 1.4)"
          />
        </symbol>
        <!-- composed by Abhishek Dutta from existing materian icons, 13 May. 2019 -->
        <symbol id="micon_add_remote">
          <path
            d="M4.5 11h-2V9H1v6h1.5v-2.5h2V15H6V9H4.5v2zm2.5-.5h1.5V15H10v-4.5h1.5V9H7v1.5zm5.5 0H14V15h1.5v-4.5H17V9h-4.5v1.5zm9-1.5H18v6h1.5v-2h2c.8 0 1.5-.7 1.5-1.5v-1c0-.8-.7-1.5-1.5-1.5zm0 2.5h-2v-1h2v1z"
          />
        </symbol>

        <symbol id="micon_share">
          <path
            d="M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z"
          />
        </symbol>
        <!-- Video player controls -->
        <symbol id="micon_play">
          <path d="M8 5v14l11-7z" />
        </symbol>
        <symbol id="micon_pause">
          <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
        </symbol>
        <symbol id="micon_mark_start">
          <path d="M18.41 16.59L13.82 12l4.59-4.59L17 6l-6 6 6 6zM6 6h2v12H6z" />
        </symbol>
        <symbol id="micon_mark_end">
          <path d="M5.59 7.41L10.18 12l-4.59 4.59L7 18l6-6-6-6zM16 6h2v12h-2z" />
        </symbol>

        <!-- Temporal Segments -->
        <symbol id="micon_add">
          <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
        </symbol>

        <!-- Help -->
        <symbol id="micon_help">
          <path
            d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z"
          />
        </symbol>

        <symbol id="micon_import_export">
          <path d="M9 3L5 6.99h3V14h2V6.99h3L9 3zm7 14.01V10h-2v7.01h-3L15 21l4-3.99h-3z" />
        </symbol>

        <!-- Restore -->
        <symbol id="micon_restore_load">
          <path
            d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm-2 16c-2.05 0-3.81-1.24-4.58-3h1.71c.63.9 1.68 1.5 2.87 1.5 1.93 0 3.5-1.57 3.5-3.5S13.93 9.5 12 9.5c-1.35 0-2.52.78-3.1 1.9l1.6 1.6h-4V9l1.3 1.3C8.69 8.92 10.23 8 12 8c2.76 0 5 2.24 5 5s-2.24 5-5 5z"
          />
        </symbol>
        <symbol id="micon_restore_save">
          <path
            d="M19 12v7H5v-7H3v7c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2v-7h-2zm-6 .67l2.59-2.58L17 11.5l-5 5-5-5 1.41-1.41L11 12.67V3h2z"
          />
        </symbol>
        <symbol id="micon_keyboard">
          <path
            d="M20 5H4c-1.1 0-1.99.9-1.99 2L2 17c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm-9 3h2v2h-2V8zm0 3h2v2h-2v-2zM8 8h2v2H8V8zm0 3h2v2H8v-2zm-1 2H5v-2h2v2zm0-3H5V8h2v2zm9 7H8v-2h8v2zm0-4h-2v-2h2v2zm0-3h-2V8h2v2zm3 3h-2v-2h2v2zm0-3h-2V8h2v2z"
          />
        </symbol>
      </defs>
    </svg>

    <!-- VIA Information Pages -->
    <div id="via_page_container" class="left">
      <div class="via_page" data-pageid="page_import_export">
        <div class="toolbar"
          ><span class="text_button" onclick="_via_util_page_hide()">&times;</span></div
        >
        <h2>导出</h2>
        <ul>
          <li
            >导出格式
            <select id="via_page_export_format">
              <option value="via3_csv">VIA3 (CSV)</option>
              <option value="temporal_segments_csv">仅 CSV 格式的时间段</option>
            </select>
          </li>
        </ul>
        <h2>导入</h2>
        <ul>
          <li
            >VIA Shared Project:
            <input
              id="via_page_import_pid"
              placeholder="e.g. 71578187-3cd3-45d0-8198-7c441fbc06af"
              style="width: 25em"
              type="text"
            />
          </li>
          <li
            >VIA2 project created (json file):
            <input id="via_page_import_via2_project_json" type="file" />
          </li>
        </ul>

        <div class="controls">
          <button id="via_page_button_import" onclick="_via_util_page_process_action(event)">
            Import
          </button>
          <button id="via_page_button_export" onclick="_via_util_page_process_action(event)">
            Export
          </button>
          <button onclick="_via_util_page_hide()">取消</button>
        </div>
      </div>

      <div id="page_uri_add" class="via_page" data-pageid="page_fileuri_bulk_add">
        <div class="toolbar">
          <span class="text_button" onclick="_via_util_page_hide()">&times;</span>
        </div>

        <div id="getDataset">
          <h2>获取数据集</h2>
          <div>
            <button onclick="onLoadRead();" value="获取列表">点击此处获取数据集列表</button>
            <br />
            <select id="folder" class="view_selector" name="folder"> </select
            ><br />
          </div>
          <button id="getPic" onclick="alert('warning!')">获取图片列表</button>
        </div>
        <textarea
          id="via_page_fileuri_urilist"
          cols="80"
          placeholder="首先获取可选择数据集，随后获取数据集内容。"
          readonly
          rows="10"
        ></textarea>
        <!--      <h2>Import URI from a File</h2>-->
        <!--      <input id="via_page_fileuri_importfile" type="file">-->

        <div class="controls">
          <!--        <button id="label_button" onclick="if_label(event)">开始标注</button>-->
          <button
            id="via_page_fileuri_button_bulk_add"
            onclick="_via_util_page_process_action(event)"
          >
            开始标注
          </button>
          <button onclick="_via_util_page_hide()">取消</button>
        </div>
      </div>

      <div class="via_page" data-pageid="page_share_not_shared_yet"></div>

      <div class="via_page" data-pageid="page_share_already_shared"></div>

      <div class="via_page" data-pageid="page_share_open_shared"></div>

      <div class="via_page" data-pageid="page_demo_instructions"></div>

      <div class="via_page" data-pageid="page_keyboard_shortcut">
        <div class="toolbar">
          <span class="text_button" onclick="_via_util_page_hide()">&times;</span>
        </div>
        <h1>键盘快捷键</h1>
        <h3>通用</h3>
        <table>
          <thead>
            <tr>
              <th>命令</th>
              <th>快捷键</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>转到前一个或后一个图片</td>
              <td><span class="key">n</span> / <span class="key">p</span></td>
            </tr>
            <tr>
              <td>以1像素为单位移动所选区域 (10像素按Shift键)</td>
              <td
                ><span class="key">Shift</span> + <span class="key">&larr;</span> /
                <span class="key">&rarr;</span> / <span class="key">&uarr;</span> /
                <span class="key">&darr;</span></td
              >
            </tr>
            <tr>
              <td>删除选中标注框</td>
              <td><span class="key">Backspace</span> or <span class="key">Delete</span></td>
            </tr>
            <tr>
              <td>选中所有标注框</td>
              <td><span class="key">Ctrl</span> + a</td>
            </tr>
            <tr>
              <td>切换标注框的可见性</td>
              <td><span class="key">b</span></td>
            </tr>
            <tr>
              <td>切换区域标签的可见性</td>
              <td><span class="key">l</span></td>
            </tr>
            <tr>
              <td>切换放大镜</td>
              <td><span class="key">m</span></td>
            </tr>
            <tr>
              <td>放大镜：不同的放大级别进行循环</td>
              <td><span class="key">M</span></td>
            </tr>
            <tr>
              <td>放大、缩小或还原图像</td>
              <td
                ><span class="key">+</span>, <span class="key">-</span> or
                <span class="key">=</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div>点击上方下拉框选择数据流，进行算法配置</div>
      </div>
    </div>
    <!-- end of page container -->

    <!-- used by _via_view_annotator._show_start_info() -->
    <div id="via_start_info_content" class="hide"></div>

    <!-- VIA dynamically populates this container with control panel, media (image, video, etc), etc. -->
    <div id="via_container" class="via_container" style="width: 100%; height: 700px"></div>
    <div></div>
    <div v-show="customAttrList.length" class="right" style="width: 100%; height: 200px">
      <a-collapse v-model:activeKey="activeKey">
        <a-collapse-panel key="1" header="算法配置自定义参数">
          <BasicForm @register="registerForm" />
        </a-collapse-panel>
      </a-collapse>
    </div>
  </div>
</template>

<script lang="ts" setup name="MonitorElectronicFence">
  import {
    Button as AButton,
    Collapse as ACollapse,
    CollapsePanel as ACollapsePanel,
    Select as ASelect,
    SelectOption as ASelectOption,
    Space,
    Spin,
  } from 'ant-design-vue';
  import { onMounted, onUnmounted, Ref, ref, watch } from 'vue';
  import { _via } from '/@/assets/js/_via.js';
  import { has_meta, no_meta } from '/@/assets/js/_via_demo_image_annotator.js';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import '/@/assets/css/via_image_annotator.css';
  import {
    _via_util_load_url,
    _via_util_page_hide,
    _via_util_page_process_action,
    _via_util_page_show,
  } from '/@/assets/js/_via_util.js';
  import { _via_data } from '/@/assets/js/_via_data.js';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import debounce from 'lodash/debounce';

  const monitorSpin = ref(false);
  const { createMessage } = useMessage();
  let picUrl;
  let via;

  const areaLabelingTip = ref('');
  //标注框数量，默认无限制
  const annoNum = ref(-1);
  //标注框形状，默认全部启用
  const annoShape = ref('all');

  const showModelSelect = ref(false);
  const showMonitorSelect = ref(true);
  const plainOptions: Ref<any[]> = ref([]);
  const curPlainOptions: Ref<any[]> = ref([]);
  const modelOptions: Ref<any[]> = ref([]);
  const modelValue = ref();
  const monitorValue = ref();
  const activeKey = ref(['1']);

  const customAttrList = ref([]);
  let buttonLoading = ref<Boolean>(false);
  watch(activeKey, (val) => {
    console.log(val);
  });

  onMounted(() => {
    const via_container = document.getElementById('via_container');
    via = new _via(via_container);
    // _via_load_submodules.call(via);
  });

  const [registerForm, { resetFields, setFieldsValue, validateFields, resetSchema }] = useForm({
    labelWidth: 200,
    showActionButtonGroup: false,
    schemas: [],
  });

  createMessage.config({
    top: `60px`,
  });

  onUnmounted(() => {
    createMessage.destroy();
  });

  maHttp
    .get(
      {
        url: 'monitor/getMonitorForSelect',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    )
    .then((v) => {
      plainOptions.value = [];
      let counter = 0;
      for (let item of v) {
        const op = {
          name: item.name,
          monitorName: item.monitorName,
          monitorId: item.id,
        };
        plainOptions.value.push(op);
        if (counter <= 20) {
          curPlainOptions.value.push(op);
        }
        counter++;
      }

      showMonitorSelect.value = !showMonitorSelect.value;
    });

  //处理滚动事件
  const handlePopupScroll = debounce(function () {
    if (showMonitorSelect.value === false) {
      const curLen = curPlainOptions.value.length;
      const allLen = plainOptions.value.length;
      if (curLen + 10 < allLen) {
        const newArr = plainOptions.value.slice(curLen, curLen + 10);
        curPlainOptions.value = curPlainOptions.value.concat(newArr);
      } else {
        const newArr = plainOptions.value.slice(curLen, allLen);
        curPlainOptions.value = curPlainOptions.value.concat(newArr);
      }
    }
  }, 500);

  async function initModelConfigFormSchema(modelId) {
    //载入model本身的固有customAttr相关数据
    const v = await maHttp.get(
      {
        url: 'model/findModelConfigsByModelId',
        params: { modelId },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    customAttrList.value = v;
    await resetSchema(
      v.map((attr) => {
        return {
          field: attr.field,
          label: attr.label,
          component: 'InputNumber',
          rules: [
            {
              required: attr.required,
              // @ts-ignore
              validator: async (rule, value) => {
                if (value !== null) {
                  //不留空则不判断，留给required做判断
                  if (attr.min !== null && value < attr.min) {
                    /* eslint-disable-next-line */
                    return Promise.reject(attr.msg);
                  }
                  if (attr.max !== null && value > attr.max) {
                    /* eslint-disable-next-line */
                    return Promise.reject(attr.msg);
                  }
                } else {
                  if (attr.required) {
                    return Promise.reject('请填写！');
                  }
                }
                return Promise.resolve();
              },
              trigger: 'change',
            },
          ],
        };
      }),
    );
    // 设置默认值
    let result = {};
    v.forEach((attr) => {
      result[attr.field] = attr.defaultNum || null;
    });
    await setFieldsValue(result);
  }

  async function handleAlgorithmChoice(modelId) {
    createMessage.destroy();
    // 多个不相关的promise，需要用await Promise.all
    const results: any[] = await Promise.all([
      initModelConfigFormSchema(modelId),
      await maHttp.get(
        {
          url: 'model/findMonitorModelConfigByMonitorIdAndModelId',
          params: { modelId, monitorId: monitorValue.value },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      ),
      await maHttp.get(
        {
          url: 'model/findAreaLabelingTipByModelId',
          params: { modelId },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      ),
    ]);

    //读取已保存值
    const mmc = results[1];
    if (results[2]) {
      areaLabelingTip.value = results[2];
    } else {
      areaLabelingTip.value = '';
    }

    const metadataStr = mmc.viaMetadata === null || mmc.viaMetadata === '' ? '{}' : mmc.viaMetadata;
    has_meta.call(via, picUrl, JSON.parse(metadataStr));

    if (Object.getOwnPropertyNames(mmc.customConfig).length > 1) {
      //areaLabel占1个，如果还有其他自定义保存，则>1
      await resetFields();
      await setFieldsValue(mmc.customConfig);
    }
  }

  //动态显示标注形状按钮
  watch(areaLabelingTip, (newV) => {
    createMessage.success('选择算法成功! ');
    const shapeMap = {
      line: '直线',
      polygon: '多边形',
    };
    const shapeMMap = {
      all: 'RECTANGLE',
      line: 'LINE',
      polygon: 'RECTANGLE',
    };
    let message = ref('');
    if (newV === 'none') {
      annoShape.value = 'none';
      annoNum.value = -1;
      message.value = '不需要标注';
    } else if (newV === '') {
      annoShape.value = 'all';
      annoNum.value = -1;
      message.value = '标注形状：无限制';
    } else {
      annoShape.value = newV.split(':')[0];
      annoNum.value = Number(newV.split(':')[1]);
      message.value = `标注形状：${shapeMap[annoShape.value]}，数目：${annoNum.value}`;
    }
    _via.prototype._update_region_shape_number.call(
      via,
      annoShape.value,
      shapeMMap[annoShape.value],
      annoNum.value,
    );
    createMessage.info(message.value, 0);
  });
  //选择数据流
  async function handleSelectMonitor(monitorId) {
    createMessage.destroy();
    showModelSelect.value = false;
    monitorSpin.value = true;
    modelValue.value = undefined;
    await resetSchema([]);
    customAttrList.value = [];
    // let myMetadata;
    //获取图像信息
    picUrl = await maHttp.get(
      {
        url: 'zlm/getSnapUrl',
        params: { monitorId: monitorId },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    );

    no_meta.call(via, picUrl);

    const modelList = await maHttp.get(
      {
        url: 'model/findModelsByMonitorId',
        params: { monitorId: monitorId },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    modelOptions.value = [];
    for (let m of modelList) {
      modelOptions.value.push({
        value: m.id,
        label: m.modelName,
      });
    }
    showModelSelect.value = true;
    monitorSpin.value = false;
  }

  //保存按钮
  async function saveInfo() {
    //保存标注信息到数据库
    if (!modelValue.value) {
      createMessage.error('未选择算法模型');
      return;
    }
    if (!monitorValue.value) {
      createMessage.error('未选择监控设备');
      return;
    }
    buttonLoading.value = true;
    const cc = {};

    try {
      const customConfig = await validateFields();
      for (let k in customConfig) {
        if (customConfig[k] !== null) {
          cc[k] = customConfig[k];
        }
      }
    } catch (e) {
      console.log(e);
      createMessage.error('数据校验未通过');
    }

    return _via_data.prototype.json_save.call(via, modelValue.value, monitorValue.value, cc);
  }

  function save() {
    Promise.all([saveInfo()]).then(function () {
      handleConfig();
    });
  }

  function handleConfig() {
    maHttp
      .get(
        {
          url: 'controller/createAllControllerJsonConfig',
          //flag=1不重启
          params: {
            flag: 1,
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
      )
      .then(() => {
        buttonLoading.value = false;
        createMessage.success('配置下发成功！');
      })
      .catch(() => {
        buttonLoading.value = false;
      });
  }

  //重新标注按钮
  async function myReload() {
    await initModelConfigFormSchema(modelValue.value);
    no_meta.call(via, picUrl);
  }

  window['_via_util_page_hide'] = () => {
    _via_util_page_hide();
  };
  window['_via_util_page_show'] = (str) => {
    _via_util_page_show(str);
  };
  window['_via_util_page_process_action'] = (e) => {
    _via_util_page_process_action(e);
  };
  window['_via_util_load_url'] = () => {
    _via_util_load_url();
  };
  window['saveInfo'] = () => {
    saveInfo();
  };
</script>

<style scoped>
  @import url('../../../../assets/css/via_image_annotator.css');
</style>
<style>
  .ant-select-dropdown {
    max-width: 200px;
  }
</style>
