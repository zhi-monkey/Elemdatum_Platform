/**
 * @class
 * @classdesc Extracts thumbnail sized frames from a video
 *
 * @author Abhishek Dutta <adutta@robots.ox.ac.uk>
 * @since 5 Feb. 2019
 * @param {_via_file} an instance of _via_file corresponding to a video file
 */

'use strict';

import {_VIA_FILE_TYPE} from './_via_file.js';
import {_via_event} from './_via_event.js';
import {
  _via_msg_clear_timer,
  _via_page_current,
  _via_page_action_map,
  _via_util_get_svg_button,
  _via_util_get_html_input_element_value,
  _via_util_get_filename_from_uri,
  _via_util_file_type_to_str,
  _via_util_file_type_str_to_id,
  _via_util_file_loc_to_str,
  _via_util_file_loc_str_to_id,
  _via_util_metadata_shape_str,
  _via_util_escape_quote_for_csv,
  _via_util_file_ext,
  _via_util_infer_file_loc_from_filename,
  _via_util_infer_file_type_from_filename,
  _via_util_download_as_file,
  _via_util_file_select_local,
  _via_util_rand_int,
  _via_util_page_show,
  _via_util_page_process_action,
  _via_util_page_gather_user_input,
  _via_util_page_hide,
  _via_util_msg_show,
  _via_util_msg_hide,
  _via_util_pad10,
  _via_util_date_to_filename_str,
  _via_util_remote_get,
  _via_util_remote_head,
  _via_util_float_arr_to_fixed,
  _via_util_float_to_fixed,
  _via_util_uuid,
  _via_util_gen_project_id,
  _via_util_uid6,
  _via_util_array_eq,
  _via_util_obj_to_csv,
  _via_util_attribute_to_html_element,
  _via_util_obj2csv,
  _via_util_merge_object,
  _via_util_collect_object_by_keys,
  _via_util_clamp,
  _via_util_merge_three_way_str,
  _via_seconds_to_hh_mm_ss_ms,
  _via_hh_mm_ss_ms_to_seconds,
}from './_via_util.js';
function _via_video_thumbnail(fid, data) {
  this._ID = '_via_video_thumbnail_';
  this.fid = fid;
  this.d = data;
  this.fwidth = 160;
  this.file_object_url = undefined; // file contents are in this the object url
  this.frames = {}; // indexed by second

  if ( this.d.store.file[this.fid].type !== _VIA_FILE_TYPE.VIDEO ) {
    console.log('_via_video_thumbnail() : file type must be ' +
                _VIA_FILE_TYPE.VIDEO + ' (got ' + this.d.store.file[this.fid].type + ')');
    return;
  }

  // state
  this.is_thumbnail_read_ongoing = false;
  this.thumbnail_time = 0;
  this.thumbnail_canvas = document.createElement('canvas');

  // registers on_event(), emit_event(), ... methods from
  // _via_event to let this module listen and emit events
  _via_event.call( this );

  this._init();
}

_via_video_thumbnail.prototype._init = function() {
}

_via_video_thumbnail.prototype.load = function() {
  return new Promise( function(ok_callback, err_callback) {
    this._load_video().then( function() {
      this.video.currentTime = 0.0;
      ok_callback();
    }.bind(this), function(load_err) {
      console.log(load_err);
      err_callback();
    }.bind(this));
  }.bind(this));
}

_via_video_thumbnail.prototype._load_video = function() {
  return new Promise( function(ok_callback, err_callback) {
    this.video = document.createElement('video');
    this.video.setAttribute('src', this.d.file_get_src(this.fid));
    //this.video.setAttribute('autoplay', false);
    //this.video.setAttribute('loop', false);
    //this.video.setAttribute('controls', '');
    this.video.setAttribute('preload', 'auto');
    //this.video.setAttribute('crossorigin', 'anonymous');

    this.video.addEventListener('loadeddata', function() {
      this.d.file_free_resources(this.fid);
      var aspect_ratio = this.video.videoHeight / this.video.videoWidth;
      this.fheight = Math.floor(this.fwidth * aspect_ratio);
      this.thumbnail_canvas.width = this.fwidth;
      this.thumbnail_canvas.height = this.fheight;
      this.thumbnail_context = this.thumbnail_canvas.getContext('2d', { alpha:false });
      ok_callback();
    }.bind(this));
    this.video.addEventListener('error', function() {
      this.d.file_free_resources(this.fid);
      console.log('_via_video_thumnnail._load_video() error')
      err_callback('error');
    }.bind(this));
    this.video.addEventListener('abort', function() {
      this.d.file_free_resources(this.fid);
      console.log('_via_video_thumbnail._load_video() abort')
      err_callback('abort');
    }.bind(this));

    this.video.addEventListener('seeked', this._on_seeked.bind(this));
  }.bind(this));
}

_via_video_thumbnail.prototype.get_thumbnail = function(time_float) {
  this.is_thumbnail_read_ongoing = true;
  this.thumbnail_time = parseInt(time_float);
  this.video.currentTime = this.thumbnail_time;
  return this.thumbnail_canvas;
}

_via_video_thumbnail.prototype._on_seeked = function() {
  if ( this.is_thumbnail_read_ongoing &&
       this.thumbnail_context
     ) {
    this.is_thumbnail_read_ongoing = false;
    this.thumbnail_context.drawImage(this.video,
                                     0, 0, this.video.videoWidth, this.video.videoHeight,
                                     0, 0, this.fwidth, this.fheight
                                    );
  }
}

export {
  _via_video_thumbnail
}
