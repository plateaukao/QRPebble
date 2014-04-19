package emneg.zeerd.qrpebble;

public class MyPixels {
	
	static int SIDE = 144;
	
	public int[] pixels;
	public int width;
	public int height;
	
	MyPixels(int w, int h){
		width = w;
		height = h;
		pixels = new int[w*h];
	}

	byte[] getLine(int l, boolean first_half){
		byte[] r = new byte[SIDE/2+5];
		
		r[0] = (byte) (l >> 24);
		r[1] = (byte) (l >> 16);
		r[2] = (byte) (l >> 8);
		r[3] = (byte) (l /*>> 0*/);

		r[4] = (byte) (first_half? 1 : 0);
		
		int s;
		if(first_half){
			s = 0;
		}
		else{
			s = SIDE/2;
		}
		for(int i=0;i<SIDE/2;i++){
			r[i+5] = (byte)((pixels[(l*SIDE)+i+s]==0xffffffff) ? 0 : 1);
		}
		
		return r;
	}

	byte[] toBytes(int i){
		byte[] r = new byte[4];
		r[0] = (byte) (i >> 24);
		r[1] = (byte) (i >> 16);
		r[2] = (byte) (i >> 8);
		r[3] = (byte) (i /*>> 0*/);
		return r;
	}
}
