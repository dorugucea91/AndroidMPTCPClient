package com.example.androidclient;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import android.annotation.SuppressLint;
import android.util.Log;

public class CryptOutputStream {
	private Socket socket;
	private OutputStream outputStream;
	private BigInteger dhmKey;
	private Cipher cipher;
	
	MessageDigest md;
	
	int newSize, alignSize, totalSize, headerSize;
	byte[] header; 
	byte[] buf;
	byte[] payloadBuf;
	int payloadSize;
	int bufSize;
	int transferred;
	byte[] md5;
	
	int TOTAL_SIZE, ALIGN_SIZE, MD5_SIZE;
	
	public CryptOutputStream(Socket socket) {
		this.socket = socket;
		headerSize = 32;
		header = new byte[headerSize];
		md5 = new byte[16];
		TOTAL_SIZE = 8;
		ALIGN_SIZE = 8;
		MD5_SIZE = 16;
	}
	
	public void setDhmKey(BigInteger key) throws NoSuchAlgorithmException {
		this.dhmKey = key;
		md = MessageDigest.getInstance("MD5");
	}
	
	public void setOuputStream() throws IOException {
		this.outputStream = socket.getOutputStream();
	}
	
	public void initAES(SecretKeySpec skeySpec, AlgorithmParameterSpec IV) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, UnsupportedEncodingException, InvalidAlgorithmParameterException {
		cipher = Cipher.getInstance("AES/CBC/NOPADDING");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, IV);
	}
	
	public int write(byte[] b, int off, int len) throws IOException {
		if ((len % 16) != 0) {
			newSize = roundUp(len, 16);
			alignSize = newSize - len;
		}
		else {
			newSize = len;
			alignSize = 0;
		}
		
		totalSize = newSize + TOTAL_SIZE + ALIGN_SIZE + MD5_SIZE;
		if ((bufSize == 0) || (bufSize < totalSize)) {
			buf = new byte[totalSize];
			bufSize = totalSize;
		}
		if ((payloadSize == 0) || (payloadSize != newSize)) {
			payloadBuf = new byte[newSize];
			payloadSize = newSize;
		}
		
		System.arraycopy(b, 0, payloadBuf, 0, len);
		
		/* get crc */
		md5 = md.digest(payloadBuf);
		/* get sizes */
		String sizes = String.format("%d %d", newSize, alignSize);
		byte[] sizesByte = sizes.getBytes();
		try {
			payloadBuf = cipher.doFinal(payloadBuf);
		} catch (BadPaddingException e) {
	 			Log.e("decrypt", "Bad Padding");
				e.printStackTrace();
				throw new IOException();
	 	} catch (IllegalBlockSizeException e) {
	 			Log.e("decrypt", "Illegal Block");
				e.printStackTrace();
				throw new IOException();
	 	}
		

		/* transfer to final array */
		System.arraycopy(payloadBuf, 0,
				buf, TOTAL_SIZE + ALIGN_SIZE + MD5_SIZE, newSize);
		System.arraycopy(md5, 0, buf, TOTAL_SIZE + ALIGN_SIZE, 16);
		System.arraycopy(sizesByte, 0, buf, 0, sizesByte.length);
		
		/* send data */
		outputStream.write(buf, 0, totalSize);
		
		return len;
	}
	
	public void close() throws IOException {
		outputStream.close();
	}
	
	int roundUp(int numToRound, int multiple) { 
		if(multiple == 0) { 
	  		return numToRound; 
	 	} 

	 	int remainder = numToRound % multiple;
	 	if (remainder == 0)
	  		return numToRound;
	 	return numToRound + multiple - remainder;
	} 	
}
