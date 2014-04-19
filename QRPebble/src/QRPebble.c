#include <pebble.h>

#define SIDE 144

static Window *window;
static Layer *layer;
static TextLayer *text_layer;

static int width = SIDE;
static int height = SIDE;
static int line = 0;
static char pixels[SIDE][SIDE];

char *translate_error(AppMessageResult result) {
  switch (result) {
    case APP_MSG_OK: return "APP_MSG_OK";
    case APP_MSG_SEND_TIMEOUT: return "APP_MSG_SEND_TIMEOUT";
    case APP_MSG_SEND_REJECTED: return "APP_MSG_SEND_REJECTED";
    case APP_MSG_NOT_CONNECTED: return "APP_MSG_NOT_CONNECTED";
    case APP_MSG_APP_NOT_RUNNING: return "APP_MSG_APP_NOT_RUNNING";
    case APP_MSG_INVALID_ARGS: return "APP_MSG_INVALID_ARGS";
    case APP_MSG_BUSY: return "APP_MSG_BUSY";
    case APP_MSG_BUFFER_OVERFLOW: return "APP_MSG_BUFFER_OVERFLOW";
    case APP_MSG_ALREADY_RELEASED: return "APP_MSG_ALREADY_RELEASED";
    case APP_MSG_CALLBACK_ALREADY_REGISTERED: return "APP_MSG_CALLBACK_ALREADY_REGISTERED";
    case APP_MSG_CALLBACK_NOT_REGISTERED: return "APP_MSG_CALLBACK_NOT_REGISTERED";
    case APP_MSG_OUT_OF_MEMORY: return "APP_MSG_OUT_OF_MEMORY";
    case APP_MSG_CLOSED: return "APP_MSG_CLOSED";
    case APP_MSG_INTERNAL_ERROR: return "APP_MSG_INTERNAL_ERROR";
    default: return "UNKNOWN ERROR";
  }
}

static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
  text_layer_set_text(text_layer, "Select");
}

static void up_click_handler(ClickRecognizerRef recognizer, void *context) {
  text_layer_set_text(text_layer, "Up");
}

static void down_click_handler(ClickRecognizerRef recognizer, void *context) {
  text_layer_set_text(text_layer, "Down");
}

static void click_config_provider(void *context) {
  window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
  window_single_click_subscribe(BUTTON_ID_UP, up_click_handler);
  window_single_click_subscribe(BUTTON_ID_DOWN, down_click_handler);
}

static void window_load(Window *window) {
  Layer *window_layer = window_get_root_layer(window);
  GRect bounds = layer_get_bounds(window_layer);

  text_layer = text_layer_create((GRect) { .origin = { 0, 72 }, .size = { bounds.size.w, 20 } });
  text_layer_set_text(text_layer, "Press a button");
  text_layer_set_text_alignment(text_layer, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(text_layer));
}

static void window_unload(Window *window) {
  text_layer_destroy(text_layer);
}

static void init(void) {
  window = window_create();
  window_set_click_config_provider(window, click_config_provider);
  window_set_window_handlers(window, (WindowHandlers) {
    .load = window_load,
    .unload = window_unload,
  });
  const bool animated = true;
  window_stack_push(window, animated);
}

static void deinit(void) {
  window_destroy(window);
}

static void layer_update_callback(Layer *me, GContext* ctx) {
  int i,j;
  GPoint point;
  
  for(i=0;i<SIDE;i++){
    for(j=0;j<SIDE;j++){
		if(pixels[j][i] == (char)1){
		  point.x = i;
		  point.y = j+2;
		  graphics_draw_pixel(ctx, point);
	    }
	}
  }
  APP_LOG(APP_LOG_LEVEL_DEBUG,  "layer_update_callback");
	
}

static void in_received_handler(DictionaryIterator *iter, void *context) {
  Tuple *tuple = dict_find(iter, 0);
  Tuple *qr_tuple = dict_find(iter, 1);
  Tuple *end_tuple = dict_find(iter, 2);

  if (tuple) {
    text_layer_set_text(text_layer, tuple->value->cstring);
    APP_LOG(APP_LOG_LEVEL_DEBUG, 
      "line of received bitmap: %s\n", tuple->value->cstring);
  }
  if (qr_tuple) {
    line = qr_tuple->value->data[3] 
          + ((qr_tuple->value->data[2] << 8) && 0x0000ff00)
          + ((qr_tuple->value->data[1] << 16) && 0x00ff0000)
          + ((qr_tuple->value->data[0] << 24) && 0xff000000);
          
    bool first_half = (qr_tuple->value->data[4] == 1);
    //APP_LOG(APP_LOG_LEVEL_DEBUG, 
    //  "line of received bitmap: %d\n", line);

    int i, s;
    if(first_half){
		s = 0;
	}
	else{
		s = SIDE/2;
	}
    
    for(i=0;i<SIDE/2;i++){
      pixels[line][i+s] = qr_tuple->value->data[i+5]; 
	}
	
  }
  if (end_tuple) {
     layer_mark_dirty(layer);
  }
}

void in_dropped_handler(AppMessageResult reason, void *context) {
   // incoming message dropped
     APP_LOG(APP_LOG_LEVEL_DEBUG, 
      "incoming message dropped: %s\n", translate_error(reason));
}

static void app_message_init(void) {
  // Reduce the sniff interval for more responsive messaging at the expense of
  // increased energy consumption by the Bluetooth module
  // The sniff interval will be restored by the system after the app has been
  // unloaded
  app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);
  
  // Init buffers
  const uint32_t inbound_size = 124;
  const uint32_t outbound_size = 16;
  app_message_open(inbound_size, outbound_size);
  
  // Register message handlers
  app_message_register_inbox_received(in_received_handler);
  app_message_register_inbox_dropped(in_dropped_handler);
}

int main(void) {
	
  width = SIDE;
  height = SIDE;
  
  init();

  Layer *window_layer = window_get_root_layer(window);
  GRect bounds = layer_get_frame(window_layer);
  layer = layer_create(bounds);
  layer_set_update_proc(layer, layer_update_callback);
  layer_add_child(window_layer, layer);
  
  app_message_init();

  APP_LOG(APP_LOG_LEVEL_DEBUG, "Done initializing, pushed window: %p", window);

  app_event_loop();
  deinit();

}
