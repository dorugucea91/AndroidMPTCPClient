package com.example.androidclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Random;

import android.util.Log;

public class MySocket extends Socket {
	private InputStream inputStream;
	private OutputStream outputStream;
	private BigInteger P, G, X, b, y, k_b;
	private String decoded, pStr, gStr, xStr, yStr, k_bStr;
	
	@Override
	public void connect(SocketAddress remoteAddr) throws IOException {
		super.connect(remoteAddr);
		byte[] buffer = new byte[1024];
		Integer bytesRead;
		inputStream = this.getInputStream();
		outputStream = this.getOutputStream();
		
		/* get P parameter */	
		bytesRead = inputStream.read(buffer, 0, 513);
		if (bytesRead == -1) {
			Log.e("error reading ", bytesRead.toString());
			throw new IOException();
		}
		if (bytesRead != 513) {
			Log.e("P parameter, length", bytesRead.toString());
			throw new IOException();
		}
        decoded = new String(buffer, 0, bytesRead -1, "UTF-8");
        P = new BigInteger(decoded, 16);
        pStr = P.toString(16);
        Log.i("received P", pStr);
		
		/* get G parameter */	
		bytesRead = inputStream.read(buffer, 0, 3);
		if (bytesRead == -1) {
			Log.e("error reading ", bytesRead.toString());
			throw new IOException();
		}
		if (bytesRead != 3) {
			Log.e("G paramter, length", bytesRead.toString());
			throw new IOException();
		}
        String decoded = new String(buffer, 0, bytesRead -1, "UTF-8");
        G = new BigInteger(decoded, 16);
        gStr = G.toString(16);
        Log.i("received G", gStr);
        
        /* get X parameter */	
		bytesRead = inputStream.read(buffer, 0, 513);
		if (bytesRead == -1) {
			Log.e("error reading ", bytesRead.toString());
			throw new IOException();
		}
		if (bytesRead != 513) {
			Log.e("X paramter, length", bytesRead.toString());
			throw new IOException();
		}
        decoded = new String(buffer, 0, bytesRead -1, "UTF-8");
        X = new BigInteger(decoded, 16);
        xStr = X.toString(16);
        Log.i("received X", xStr);
        
        /* generate big random value < p -1 */
        b = nextRandomBigInteger(P);
        Log.i("b random value", b.toString(16));
        
        /* y = g^b mod p */
        y = G.modPow(b, P);
        yStr = y.toString(16);
        Log.i("y value", yStr);
        outputStream.write(yStr.getBytes());
        
        /* k_b = x^b mod p */
        k_b = X.modPow(b, P);
        k_bStr = k_b.toString(16);
        Log.i("DH Key", k_bStr);
        
        
        //bytesRead = inputStream.read(buffer, 0, 16);
		//if (bytesRead == -1) {
		//	Log.e("error reading ", bytesRead.toString());
		//	throw new IOException();
		//}
		
        //decoded = new String(buffer, 0, bytesRead, "UTF-8");
        //Log.i("cact", decoded);
        //String [] arr = new String[16];
        //for (int i = 0; i < 16; i++) {
        //   arr[i] = String.format("%02x", buffer[i]);
        //}
        //Log.i("cacat", java.util.Arrays.toString(arr));
      
	}
	
	public MyInputStream getMyInputStream() throws Exception {
		if (k_b == null)
			return null;
		MyInputStream is = new MyInputStream(inputStream, k_b.toByteArray());
		is.initAES();
		return is;
	}
	
	public MyOutputStream getMyOutputStream() throws Exception {
		if (k_b == null)
			return null;
		MyOutputStream os = new MyOutputStream(outputStream, k_b);
		return os;
	}
	
	public BigInteger nextRandomBigInteger(BigInteger n) {
	    Random rand = new Random();
	    BigInteger result = new BigInteger(n.bitLength(), rand);
	    while( result.compareTo(n) >= 0 ) {
	        result = new BigInteger(n.bitLength(), rand);
	    }
	    return result;
	}
}
