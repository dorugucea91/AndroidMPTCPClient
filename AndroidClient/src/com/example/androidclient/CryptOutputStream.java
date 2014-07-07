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
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

public class CryptOutputStream {
	private Socket socket;
	private OutputStream outputStream;
	private Cipher cipher;
	private MessageDigest md;
	private byte[] buf, sizesByte;
	private int newSize, alignSize, totalSize, bufSize;
	private int PAYLOAD_SIZE, ALIGN_SIZE, MD5_SIZE, FLAG_SIZE, MD5_OFFSET_M, 
				newBufferedSize , bufferedSize, modifiedLen, offset, sendSize;
	
	public CryptOutputStream(Socket socket) {
		this.socket = socket;
		PAYLOAD_SIZE = 8;
		ALIGN_SIZE = 8;
		MD5_SIZE = 16;
		FLAG_SIZE = 1;
		MD5_OFFSET_M = FLAG_SIZE + PAYLOAD_SIZE + ALIGN_SIZE + MD5_SIZE;
	}
	
	public void setDhmKey(BigInteger key) throws NoSuchAlgorithmException {
		md = MessageDigest.getInstance("MD5");
	}
	
	public void setOuputStream() throws IOException {
		this.outputStream = socket.getOutputStream();
	}
	
	public void initAES(SecretKeySpec skeySpec, AlgorithmParameterSpec IV) throws 
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, 
			UnsupportedEncodingException, InvalidAlgorithmParameterException {
		cipher = Cipher.getInstance("AES/CBC/NOPADDING");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, IV);
	}
	
	public int write(byte[] b, int off, int len) throws IOException {
		int smaller_buf = 0;
		
		/* make len divisible by 16 for AES */
		if ((len % 16) != 0) {
			newSize = roundUp(len, 16);
			alignSize = newSize - len;
		}
		else {
			newSize = len;
			alignSize = 0;
		}
		
		newBufferedSize = newSize - alignSize;
		modifiedLen = 0;
		if (bufSize == 0 || (bufferedSize != newBufferedSize)) {
			modifiedLen = 1;
			if ((bufSize != 0) && (newBufferedSize < bufferedSize))
				smaller_buf = 1;
			totalSize = MD5_OFFSET_M + newSize;
			buf = new byte[totalSize];
			bufSize = totalSize;
			bufferedSize = newBufferedSize;
		}
		
		totalSize = bufSize;

		Arrays.fill(buf, (byte)0x00);		
		if (modifiedLen == 0) {
			offset = FLAG_SIZE + MD5_SIZE;
			buf[0] = 0x30;
			sendSize = totalSize - PAYLOAD_SIZE - ALIGN_SIZE;
		}
		else {
			offset = MD5_OFFSET_M;
			String sizes = String.format("%d%d %d", 1, newSize, alignSize);
			sizesByte = sizes.getBytes();
			System.arraycopy(sizesByte, 0, buf, 0, sizesByte.length);
			sendSize = totalSize;
		}
		
		System.arraycopy(b, 0, buf, offset, len);
		md.update(buf, offset, newSize);
		System.arraycopy(md.digest(), 0, buf, offset - MD5_SIZE, MD5_SIZE);
		
		try {
			cipher.doFinal(buf, offset, newSize, buf, offset);
		} catch (ShortBufferException e) {
			e.printStackTrace();
			throw new IOException();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			throw new IOException();
		} catch (BadPaddingException e) {
			e.printStackTrace();
			throw new IOException();
		}
		
		outputStream.write(buf, 0, sendSize);
		if (smaller_buf == 1) 
			socket.getInputStream().read();
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
