package com.example.androidclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;
import android.util.Log;

@SuppressLint("NewApi")
public class MyInputStream {
	private InputStream is;
	private byte[] key;
	private SecretKeySpec skeySpec;
	private Cipher cipher;
	
	public MyInputStream(InputStream is, byte[] key) {
		this.is = is;
		this.key = key;
	}
	
	public void initAES() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, UnsupportedEncodingException {
		byte[] aesKey = new byte[32];
		if (key[0] == (byte) 0x00) 
			aesKey = Arrays.copyOfRange(key, 1, 33);
		else 
			aesKey = Arrays.copyOfRange(key, 0, 32);
		
		/*
		String [] arr = new String[32];
        for (int i = 0; i < 32; i++) {
           arr[i] = String.format("%02x", aesKey[i]);
        }
        Log.i("aesKey", java.util.Arrays.toString(arr));
        */
        
		skeySpec = new SecretKeySpec(aesKey, "AES");
		cipher = Cipher.getInstance("AES/ECB/NoPADDING");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
	}
	
	public int read(byte[] buffer, int off, int len) throws IOException, IllegalBlockSizeException, BadPaddingException {
		Integer readBytes = is.read(buffer, off, len);
	
		/* DEBUG */
		String [] arr = new String[len];
        for (int i = 0; i < len; i++) {
           arr[i] = String.format("%02x", buffer[i]);
        }
        Log.i("Encrypted 16", java.util.Arrays.toString(arr));
		 
		Integer size = buffer.length;
		
		buffer = cipher.doFinal(buffer);			
		
		String decoded = new String(buffer, 0, len, "UTF-8");
		Log.i("Decrypted 16", decoded);
		
		return readBytes;
	}
	
	public void close() throws IOException {
		Log.i("is", "close");
		is.close();
	}
	
	public InputStream getIs() {
		return is;
	}
	public void setIs(InputStream is) {
		this.is = is;
	}
	public byte[] getKey() {
		return key;
	}
	public void setKey(byte[] key) {
		this.key = key;
	}
}
