/**
 *
 * @class
 * @classdesc VIA
 * @author Abhishek Dutta <adutta@robots.ox.ac.uk>
 * @date 12 May 2019
 *
 */

'use strict';

import { _VIA_REMOTE_STORE, _VIA_VERSION } from './_via_config';
import { _via_data } from './_via_data';
import { _via_share } from './_via_share';
import { _via_control_panel } from './_via_control_panel';
import { _via_util_msg_hide } from './_via_util';
import { _via_import_export } from './_via_import_export';
import { _via_view_annotator, _VIA_VIEW_MODE } from './_via_view_annotator';
import { _via_editor } from './_via_editor';
import { _via_view_manager } from './_via_view_manager';

function _via(via_container) {
  this._ID = '_via';

  // console.log('Initializing VGG Image Annotator (VIA) version ' + _VIA_VERSION);
  this.via_container = via_container;

  this.d = new _via_data();
  var conf = { ENDPOINT: _VIA_REMOTE_STORE };
  this.s = new _via_share(this.d, conf);

  if (typeof _VIA_DEBUG === 'undefined' || _VIA_DEBUG === true) {
    // ADD DEBUG CODE HERE (IF NEEDED)
  }
  //// define the html containers
  if (!document.getElementById('via_control_panel_container')) {
    // 重新生成了via_control_panel_container
    this.control_panel_container = document.createElement('div');
    this.control_panel_container.setAttribute('id', 'via_control_panel_container');
    this.via_container.appendChild(this.control_panel_container);

    this.view_container = document.createElement('div');
    this.view_container.setAttribute('id', 'view_container');
    this.via_container.appendChild(this.view_container);

    this.editor_container = document.createElement('div');
    this.editor_container.setAttribute('id', 'editor_container');
    this.editor_container.classList.add('hide');
    this.via_container.appendChild(this.editor_container);

    this.message_container = document.createElement('div');
    this.message_container.setAttribute('id', '_via_message_container');
    this.message_container.setAttribute('class', 'message_container');
    this.message_container.addEventListener('click', _via_util_msg_hide);
    this.message_panel = document.createElement('div');
    this.message_panel.setAttribute('id', '_via_message');
    this.message_container.appendChild(this.message_panel);
    this.via_container.appendChild(this.message_container);
  } else {
    // 没有重新生成，直接append
    this.control_panel_container = document.getElementById('via_control_panel_container');
    this.via_container.appendChild(this.control_panel_container);

    this.view_container = document.getElementById('view_container');
    this.via_container.appendChild(this.view_container);

    this.editor_container = document.getElementById('editor_container');
    this.via_container.appendChild(this.editor_container);

    this.message_container = document.getElementById('_via_message_container');
    this.message_panel = document.getElementById('_via_message');
    this.message_container.appendChild(this.message_panel);
    this.via_container.appendChild(this.message_container);
  }

  //// initialise content creators and managers
  this.ie = new _via_import_export(this.d);

  this.va = new _via_view_annotator(this.d, this.view_container);
  this.editor = new _via_editor(this.d, this.va, this.editor_container);

  this.view_manager_container = document.createElement('div');
  this.view_manager_container.setAttribute('id', 'view_manager_container');
  this.vm = new _via_view_manager(this.d, this.va, this.view_manager_container);
  this.vm._init();

  // control panel shows the view_manager_container
  this.cp = new _via_control_panel(this.control_panel_container, this);
  this.cp._set_region_shape('RECTANGLE');

  // event handlers for buttons in the control panel
  this.cp.on_event(
    'region_shape',
    this._ID,
    function (data, event_payload) {
      this.va.set_region_draw_shape(event_payload.shape);
    }.bind(this),
  );
  this.cp.on_event(
    'editor_toggle',
    this._ID,
    function () {
      this.editor.toggle();
    }.bind(this),
  );
  this.cp.on_event(
    'magnifier_toggle',
    this._ID,
    function () {
      if (this.va.view_mode === _VIA_VIEW_MODE.IMAGE1) {
        this.va.file_annotator[0][0]._magnifier_toggle();
      }
    }.bind(this),
  );
  this.cp.on_event(
    'zoom_in',
    this._ID,
    function () {
      if (this.va.view_mode === _VIA_VIEW_MODE.IMAGE1) {
        this.va.file_annotator[0][0]._zoom_in();
      }
    }.bind(this),
  );
  this.cp.on_event(
    'zoom_out',
    this._ID,
    function () {
      if (this.va.view_mode === _VIA_VIEW_MODE.IMAGE1) {
        this.va.file_annotator[0][0]._zoom_out();
      }
    }.bind(this),
  );
  this.cp.on_event(
    'fit_screen',
    this._ID,
    function () {
      if (this.va.view_mode === _VIA_VIEW_MODE.IMAGE1) {
        this.va.file_annotator[0][0]._zoom_fit_screen();
      }
    }.bind(this),
  );

  // keyboard event handlers
  //this.via_container.focus()
  //this.via_container.addEventListener('keydown', this._keydown_handler.bind(this));
  window.addEventListener('keydown', this._keydown_handler.bind(this)); // @todo: should be attached only to VIA application container

  // update VIA version number
  var el = document.getElementById('via_page_container');
  var pages = el.getElementsByClassName('via_page');
  var n = pages.length;
  for (var i = 0; i < n; ++i) {
    if (pages[i].dataset.pageid === 'page_about') {
      var content0 = pages[i].innerHTML;
      pages[i].innerHTML = content0.replace('__VIA_VERSION__', _VIA_VERSION);
    }
  }

  // load any external modules (e.g. demo) which should be defined as follows
  // function _via_load_submodules()
  if (typeof _via_load_submodules === 'function') {
    console.log('VIA submodule detected, invoking _via_load_submodules()');
    this._load_submodule = new Promise(
      function (ok_callback, err_callback) {
        try {
          _via_load_submodules.call(this);
        } catch (err) {
          console.warn('VIA submodule load failed: ' + err);
          err_callback(err);
        }
      }.bind(this),
    );
  } else {
    // debug code (disabled for release)
    if (typeof _VIA_DEBUG === 'undefined' || _VIA_DEBUG === true) {
      //this.s.pull(''); // load shared project
      //this.d.project_load_json(_via_dp[2]['store']); // video
      //this.d.project_load_json(_via_dp[1]['store']); // audio
      //this.d.project_load_json(_via_dp[4]['store']); // image
      //this.d.project_load_json(_via_dp[3]['store']); // pair
      /*
      setTimeout( function() {
      //this.va.view_show('1');
      //this.editor.show();
      //this.cp._page_show_import_export();
      //this.cp._share_show_info();
      }.bind(this), 200);
      */
    }
  }

  // ready
  // _via_util_msg_show(_VIA_NAME + ' (' + _VIA_NAME_SHORT + ') ' + _VIA_VERSION + '.');
}

_via.prototype._hook_on_browser_resize = function () {
  if (typeof this.va.vid !== 'undefined') {
    this.va.view_show(this.va.vid);
  }
};

_via.prototype._keydown_handler = function (e) {
  // avoid handling events when text input field is in focus
  if (e.target.type !== 'text' && e.target.type !== 'textarea') {
    this.va._on_event_keydown(e);
  }
};

_via.prototype._update_region_shape_number = function (annoShape, shape, n) {
  this.cp._add_region_shape_selector(annoShape);
  this.cp._set_region_shape(shape);
  this.va._update_via_rinput_limit(n);
  this.va.file_annotator[0][0]._update_via_rinput_limit(n);
};

export { _via };
