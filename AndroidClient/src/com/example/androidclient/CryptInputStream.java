package com.example.androidclient;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

public class CryptInputStream {
	private Socket socket;
	private InputStream inputStream;
	private BigInteger dhmKey;
	private SecretKeySpec skeySpec;
	private Cipher cipher;
	private Integer headerSize;
	private byte[] IV;
	MessageDigest md;
	private AlgorithmParameterSpec IVSpec;
	private byte[] buf;
	private int bufSize, bufferedSize, transferredBytes, totalSize, alignSize;
	private byte[] header; 
	
	public CryptInputStream(Socket socket) {
		this.socket = socket;
		headerSize = 32;
		header = new byte[headerSize];
	}
	
	public void setDhmKey(BigInteger key) throws NoSuchAlgorithmException {
		this.dhmKey = key;
		md = MessageDigest.getInstance("MD5");
	}
	
	public void initAES() throws 
		NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, 
		UnsupportedEncodingException, InvalidAlgorithmParameterException {
		byte[] key = dhmKey.toByteArray();
		byte[] cleanKey = new byte[256];
		byte[] aesKey = new byte[32];
		
		/* clean dhm key */
		if (key[0] == (byte) 0x00) {
			cleanKey = Arrays.copyOfRange(key, 1, 257);
		}
		else
			cleanKey = key;
	     
		/* initialization vector */
		IV = md.digest(cleanKey);
		
		/* aes key */
		aesKey = Arrays.copyOfRange(cleanKey, 0, 32);
		
		setSkeySpec(new SecretKeySpec(aesKey, "AES"));
		cipher = Cipher.getInstance("AES/CBC/NOPADDING");
		setIVSpec(new IvParameterSpec(IV));
        cipher.init(Cipher.DECRYPT_MODE, getSkeySpec(), getIVSpec());
	}
	
	public void setInputStream() throws IOException {
		this.inputStream = socket.getInputStream();
	}
	
 	public int read(byte[] b, int off, int len) throws IOException {
 		Integer ret;
 		String decoded;
 		Scanner scanner = null;
 		if (bufferedSize == 0) {
 			/* receive header */
 	 		ret = inputStream.read(header, 0, headerSize);
 	 		if (ret == -1)
 	 			return ret;
 	 		if (ret != headerSize) {
 	 			Log.e("error reading header", ret.toString());
 	 			throw new IOException();
 	 		}
 	 		decoded = new String(header, "UTF-8");
	 	    scanner = new Scanner(decoded);
 	 		/* get total size and align size*/
 	 		 try {	
 	 	 		totalSize = scanner.nextInt();
 	 	 		alignSize = scanner.nextInt();
 	 	 	} catch (InputMismatchException e) {
 	 	 		Log.e("header", "corrupt");
 	 	 		e.printStackTrace();
 	 	 	}
 	 		scanner.close();
 	 		/* adjust receive buffer */
 	 		if ((bufSize == 0) || (bufSize < totalSize)) {
 	 			buf = new byte[totalSize];
 	 			bufSize = totalSize;
 	 		}
 	 		
 	 		/* receive data */
 	 		ret = inputStream.read(buf, 0, totalSize);
 	 		if (ret.compareTo(totalSize) == 1) {
 	 			Log.e("error reading data", ret.toString());
 	 			throw new IOException();
 	 		}
 	 		bufferedSize = totalSize - alignSize;
 	 		try {
 	 			buf = cipher.doFinal(buf, 0, totalSize);
 	 			if (checkCrc(header, buf) == 0) {
 	 				Log.e("crc", "corrupt crc");
 	 				throw new IOException();
 	 			}
 	 			decoded = new String(buf, 0, bufferedSize, "UTF-8");
 	 		} catch (BadPaddingException e) {
 	 			Log.e("decrypt", "Bad Padding");
 				e.printStackTrace();
 				throw new IOException();
 	 		} catch (IllegalBlockSizeException e) {
 	 			Log.e("decrypt", "Illegal Block");
 				e.printStackTrace();
 				throw new IOException();
 	 		}
 		}
 		if (bufferedSize > len) {
 			System.arraycopy(buf, transferredBytes, b, 0, len);
 			bufferedSize -= len;
 			transferredBytes += len;
 			ret = len;
 		}
 		else {
 			System.arraycopy(buf, transferredBytes, b, 0, bufferedSize);
 			ret = bufferedSize;
 			bufferedSize = 0;
 			transferredBytes = 0;
 		}
 		return ret;
 	}
 	
 	private Integer checkCrc(byte[] header, byte[] data) {
 		byte[] headerCrc = new byte[16];
 		byte[] calcCrc = new byte[16];
 		
 		/* get crc */
 		System.arraycopy(header, 16, headerCrc, 0, 16);
 		calcCrc = md.digest(data);
 	 		
 		if (Arrays.equals(headerCrc, calcCrc) == true)
 			return 1;
 	
 		return 0;
 	}
 	
 	public int readClear(byte[] b, int off, int len) throws IOException {
 	 	return inputStream.read(b, off, len);
 	}
 	
 	public void close() throws IOException {
		inputStream.close();
	}
 
	public SecretKeySpec getSkeySpec() {
		return skeySpec;
	}

	public void setSkeySpec(SecretKeySpec skeySpec) {
		this.skeySpec = skeySpec;
	}

	public AlgorithmParameterSpec getIVSpec() {
		return IVSpec;
	}

	public void setIVSpec(AlgorithmParameterSpec iVSpec) {
		IVSpec = iVSpec;
	}
}
