package emneg.zeerd.qrpebble;

import java.util.Hashtable;
import java.util.UUID;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleAckReceiver;
import com.getpebble.android.kit.PebbleKit.PebbleNackReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity {

	private static String TAG = "QRPebble";
	  private static final int WHITE = 0xFFFFFFFF;
	  private static final int BLACK = 0xFF000000;
	  
	  private static final UUID QRPEBBLE_UUID = UUID.fromString("022f046d-66b7-4c0c-9465-23cfccbe0caf");
	  
	  Button btn_launch_app;
	  ImageView image_view;
	  
	  private int current_seq = 0;
	  private boolean first_half = true;
	  private MyPixels pixels;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	
		Bitmap bm = null;
		try {
			bm = encodeAsBitmap("Hello world!", BarcodeFormat.QR_CODE, MyPixels.SIDE, MyPixels.SIDE);
		} catch (WriterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		image_view = (ImageView) this.findViewById(R.id.imageView1);
		if(bm != null) {
		    image_view.setImageBitmap(bm);
		}
		
		btn_launch_app = (Button) this.findViewById(R.id.btn_launch_app);
		btn_launch_app.setOnClickListener(new OnClickListener() {
			 
			  @Override
			  public void onClick(View v) {
				  
				// Launching my app
				PebbleKit.startAppOnPebble(getApplicationContext(), QRPEBBLE_UUID);
				
				if (PebbleKit.areAppMessagesSupported(getApplicationContext())) {
					Log.i(TAG, "App Message is supported!");
					
					PebbleDictionary data = new PebbleDictionary();
					data.addString(0, "Home Page");
					PebbleKit.sendDataToPebble(getApplicationContext(), QRPEBBLE_UUID, data);

					try {
						pixels = encodeAsPixels("Hello world!", BarcodeFormat.QR_CODE, MyPixels.SIDE, MyPixels.SIDE);

						sendPixelsToPebble(current_seq, first_half);
						
					} catch (WriterException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {
					Log.i(TAG, "App Message is not supported");
				}
				
		  }
		});
		

		PebbleKit.registerReceivedAckHandler(getApplicationContext(), new PebbleAckReceiver(QRPEBBLE_UUID) {
			  @Override
			  public void receiveAck(Context context, int transactionId) {
			    Log.i(TAG, "Received ack for transaction " + transactionId);
			    
				first_half = !first_half;
				
				if(first_half){
					current_seq++;
				}
			    if(current_seq < MyPixels.SIDE){
			    	sendPixelsToPebble(current_seq, first_half);
			    }
			    else{
			    	current_seq = 0;
			    	first_half = true;
			    	
			    	Log.i(TAG, "QR END");
					PebbleDictionary data = new PebbleDictionary();
					data.addString(2, "QR END");
					PebbleKit.sendDataToPebble(getApplicationContext(), QRPEBBLE_UUID, data);
			    }
			  }
			});

			PebbleKit.registerReceivedNackHandler(getApplicationContext(), new PebbleNackReceiver(QRPEBBLE_UUID) {
			  @Override
			  public void receiveNack(Context context, int transactionId) {
			    Log.i(TAG, "Received nack for transaction " + transactionId);
		    	sendPixelsToPebble(current_seq, first_half);
			  }
			});
		
		boolean connected = PebbleKit.isWatchConnected(getApplicationContext());
		Log.i(TAG, "Pebble is " + (connected ? "connected" : "not connected"));
	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void sendPixelsToPebble(int line, boolean first_half){
		PebbleDictionary data = new PebbleDictionary();
		data.addBytes(1, pixels.getLine(line, first_half));
		PebbleKit.sendDataToPebble(getApplicationContext(), QRPEBBLE_UUID, data);
		Log.i(TAG, "sendDataToPebble : " + line);
	}

	static MyPixels encodeAsPixels(String contents,
              BarcodeFormat format,
              int desiredWidth,
              int desiredHeight) throws WriterException {
		Hashtable<EncodeHintType,Object> hints = null;
		String encoding = guessAppropriateEncoding(contents);
		if (encoding != null) {
			hints = new Hashtable<EncodeHintType,Object>(2);
			hints.put(EncodeHintType.CHARACTER_SET, encoding);
		}
		
		MultiFormatWriter writer = new MultiFormatWriter();    
		BitMatrix result = writer.encode(contents, format, desiredWidth, desiredHeight, hints);
		int width = result.getWidth();
		int height = result.getHeight();
		MyPixels pixels = new MyPixels(width, height);
		// All are 0, or black, by default
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels.pixels[offset + x] = (result.get(x, y) ? BLACK : WHITE);
			}
		}
		
		return pixels;
	}
	
	static Bitmap encodeAsBitmap(String contents,
            BarcodeFormat format,
            int desiredWidth,
            int desiredHeight) throws WriterException {
		
		MyPixels pixels = encodeAsPixels(contents, format, desiredWidth, desiredHeight);
		
		Bitmap bitmap = Bitmap.createBitmap(pixels.width, pixels.height, Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels.pixels, 0, pixels.width, 0, 0, pixels.width, pixels.height);
		return bitmap;
		}	
	  private static String guessAppropriateEncoding(CharSequence contents) {
		    // Very crude at the moment
		    for (int i = 0; i < contents.length(); i++) {
		      if (contents.charAt(i) > 0xFF) {
		        return "UTF-8";
		      }
		    }
		    return null;
		  }
}
