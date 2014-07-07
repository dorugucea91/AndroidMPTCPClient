package com.example.androidclient;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.NoSuchPaddingException;

import android.util.Log;

public class CryptSocket extends Socket {
	private BigInteger P, G, X, b, y, k_b;
	private String decoded, yStr;
	private CryptInputStream is;
	private CryptOutputStream os;
	private OutputStream osOriginal;
	static long ec, ep, eg, ex, ey, ek, eyw, tot; 
	static long cnt = 1;
	
	@Override
	public void connect(SocketAddress remoteAddr) throws IOException {
		long start, finish, s, f;
		s = System.currentTimeMillis();
		
		start = System.currentTimeMillis();
		super.connect(remoteAddr);
		finish = System.currentTimeMillis();
		ec += (finish -start);
		Log.i("original connect: ", Long.valueOf(ec / cnt).toString());
		
		byte[] buffer = new byte[1024];
		Integer bytesRead;
		is = this.getCryptInputStream();
		os = this.getCryptOutputStream();
		osOriginal = this.getOutputStream();
	
		
		/* get P parameter */
		start = System.currentTimeMillis();
		bytesRead = is.readClear(buffer, 0, 513);
		if (bytesRead == -1) {
			Log.e("error reading ", bytesRead.toString());
			throw new IOException();
		}
		if (bytesRead != 513) {
			Log.e("P parameter, length", bytesRead.toString());
			throw new IOException();
		}
		finish = System.currentTimeMillis();
		ep += (finish - start);
		Log.i("receive P time: ", Long.valueOf(ep / cnt).toString());
		
        decoded = new String(buffer, 0, bytesRead -1, "UTF-8");
        P = new BigInteger(decoded, 16);
		
		/* get G parameter */
        start = System.currentTimeMillis();
		bytesRead = is.readClear(buffer, 0, 3);
		if (bytesRead == -1) {
			Log.e("error reading ", bytesRead.toString());
			throw new IOException();
		}
		if (bytesRead != 3) {
			Log.e("G paramter, length", bytesRead.toString());
			throw new IOException();
		}
		finish = System.currentTimeMillis();
		eg +=  (finish - start);
		Log.i("receive G time: ", Long.valueOf(eg / cnt).toString());
		
        String decoded = new String(buffer, 0, bytesRead -1, "UTF-8");
        G = new BigInteger(decoded, 16);
     
        /* get X parameter */
        start = System.currentTimeMillis();
		bytesRead = is.readClear(buffer, 0, 513);
		if (bytesRead == -1) {
			Log.e("error reading ", bytesRead.toString());
			throw new IOException();
		}
		if (bytesRead != 513) {
			Log.e("X paramter, length", bytesRead.toString());
			throw new IOException();
		}
		finish = System.currentTimeMillis();
		ex +=  (finish - start);
		Log.i("receive X time: ", Long.valueOf(ex / cnt).toString());
		
        decoded = new String(buffer, 0, bytesRead -1, "UTF-8");
        X = new BigInteger(decoded, 16);
        
        /* generate big random value < p -1 */
      
        b = nextRandomBigInteger(P);
      
        
        
        /* y = g^b mod p */
        start = System.currentTimeMillis();
        y = G.modPow(b, P);
        yStr = y.toString(16);
        finish = System.currentTimeMillis();
        ey += (finish -start);
        Log.i("setting y time:", Long.valueOf(ey /cnt).toString());
        
        start = System.currentTimeMillis();
        osOriginal.write(yStr.getBytes());
        finish = System.currentTimeMillis();
        eyw += (finish - start);
        Log.i("sending y", Long.valueOf(eyw /cnt).toString());
        
        
        /* k_b = x^b mod p */
        start = System.currentTimeMillis();
        k_b = X.modPow(b, P);
        finish = System.currentTimeMillis();
        ek += (finish - start);
        Log.i("setting key", Long.valueOf(ek /cnt).toString());
        
       
        
        try {
        	is.setDhmKey(k_b);
        	is.initAES();
        	os.setDhmKey(k_b);
        	os.initAES(is.getSkeySpec(), is.getIVSpec());
        	finish = System.currentTimeMillis();
        } catch (NoSuchAlgorithmException e) {
			Log.e("initAES", "AES not found");
			e.printStackTrace();
			is.close();
			throw new IOException();
		} catch (NoSuchPaddingException e) {
			Log.e("initAES", "Bad padding");
			e.printStackTrace();
			is.close();
			throw new IOException();
		} catch (InvalidKeyException e) {
			Log.e("initAES", "Invalid DHM Key");
			e.printStackTrace();
			is.close();
			throw new IOException();
		} catch (UnsupportedEncodingException e) {
			Log.e("initAES", "Encoding Format not supported");
			e.printStackTrace();
			is.close();
			throw new IOException();
		} catch (InvalidAlgorithmParameterException e) {
			Log.e("initAES", "Invalid parameter for AES");
			e.printStackTrace();
			is.close();
			throw new IOException();
		}
        f = System.currentTimeMillis();
        tot += (f - s);
        Log.i("TOTAL", Long.valueOf(tot /cnt).toString() + " " + cnt);
        cnt++;
	}
	
	public BigInteger nextRandomBigInteger(BigInteger n) {
	    Random rand = new Random();
	    BigInteger result = new BigInteger(n.bitLength(), rand);
	    while( result.compareTo(n) >= 0 ) {
	        result = new BigInteger(n.bitLength(), rand);
	    }
	    return result;
	}
	
	public CryptInputStream getCryptInputStream() {
		if (is == null) {
			is = new CryptInputStream(this);
			try {
				is.setInputStream();
			} catch (IOException e) {
				Log.e("getInputStream()", "Error getting inputStream");
				e.printStackTrace();
				is = null;
			}
		}
		return is;
	}
	
	public CryptOutputStream getCryptOutputStream() {
		if (os == null) {
			os = new CryptOutputStream(this);
			try {
				os.setOuputStream();
			} catch (IOException e) {
				Log.e("getOutputStream()", "Error getting OutputStream");
				e.printStackTrace();
				os = null;
			}
		}
		return os;
	}
	
	public void close() throws IOException {
		super.close();
	}
}
