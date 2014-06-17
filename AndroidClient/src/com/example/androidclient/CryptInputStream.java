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
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

public class CryptInputStream {
	private Socket socket;
	private InputStream inputStream;
	private BigInteger dhmKey;
	private SecretKeySpec skeySpec;
	private Cipher cipher;
	private Scanner scanner;
	private Integer headerModifiedSize, ret;
	private MessageDigest md;
	private AlgorithmParameterSpec IVSpec;
	
	private byte[] headerModified, headerClean, md5Header, buf, IV;
	private int PAYLOAD_SIZE, ALIGN_SIZE, MD5_SIZE, FLAG_SIZE, MD5_OFFSET_M;
	private int newPayloadSize, remainingSize, flag, offset;
	private int bufSize, bufferedSize, transferredBytes, payloadSize, alignSize,
	 			headerCleanSize;
	
	public CryptInputStream(Socket socket) {
		this.socket = socket;
		PAYLOAD_SIZE = 8;
		ALIGN_SIZE = 8;
		MD5_SIZE = 16;
		FLAG_SIZE = 1;
		MD5_OFFSET_M = FLAG_SIZE + PAYLOAD_SIZE + ALIGN_SIZE + MD5_SIZE;
		headerModifiedSize = PAYLOAD_SIZE + ALIGN_SIZE + MD5_SIZE + FLAG_SIZE;
		headerModified = new byte[headerModifiedSize];
		headerCleanSize = FLAG_SIZE + MD5_SIZE;
		headerClean = new byte[headerCleanSize];
		md5Header = new byte[MD5_SIZE];
		flag = 1;
	}
	
	public void setDhmKey(BigInteger key) throws NoSuchAlgorithmException {
		this.dhmKey = key;
		md = MessageDigest.getInstance("MD5");
	}

	public void initAES() throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException,
			UnsupportedEncodingException, InvalidAlgorithmParameterException {
		byte[] key = dhmKey.toByteArray();
		byte[] cleanKey = new byte[256];
		byte[] aesKey = new byte[32];

		/* clean dhm key */
		if (key[0] == (byte) 0x00) {
			cleanKey = Arrays.copyOfRange(key, 1, 257);
		} else
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
		return inputStream.read(b, off, len);
	}

	public int read1(byte[] b, int off, int len) throws IOException {
		/* 0 bytes buffered => read from socket */
		if (bufferedSize == 0) {
			/* first receive of data*/
			if (bufSize == 0) {
				ret = readAll(headerModified, 0, headerModifiedSize, 0);
				if (ret < 0)
					return ret;
				System.arraycopy(headerModified, MD5_OFFSET_M - MD5_SIZE,
												md5Header, 0, MD5_SIZE);
				setPayloadAlignSize(headerModified);
				offset = 0;
				buf = new byte[payloadSize + headerCleanSize];
				bufSize = payloadSize + headerCleanSize;
			}
			
			remainingSize = bufSize - flag * headerCleanSize;
			ret = readAll(buf, 0, remainingSize, 1);
			if (ret == -1)
				return ret;
			bufferedSize = payloadSize - alignSize;
	
			if (flag == 0) {
				offset = MD5_SIZE + FLAG_SIZE;
				/* consider that we receive a clean packet */
				System.arraycopy(buf, 0, headerClean, 0, headerCleanSize);
				
				if (headerClean[0] == (byte)0x30) 
					System.arraycopy(buf, FLAG_SIZE, md5Header, 0, MD5_SIZE);
				else {
					setPayloadAlignSize(headerClean);
					remainingSize = payloadSize  + MD5_OFFSET_M - bufSize;
					bufferedSize = payloadSize - alignSize;
					
					if (remainingSize > 0) {
						byte[] tempBuf = new byte[payloadSize + headerModifiedSize];
						System.arraycopy(buf, 0, tempBuf, 0, bufSize);
						ret = readAll(tempBuf, bufSize, remainingSize, 0);
						if (ret < 0 ) {
							scanner.close();
							return ret;
						}
						bufSize = payloadSize + headerCleanSize;
						buf = new byte[bufSize];
						System.arraycopy(tempBuf, MD5_OFFSET_M - MD5_SIZE , 
													md5Header, 0, MD5_SIZE);
						System.arraycopy(tempBuf, MD5_OFFSET_M, buf, 
											FLAG_SIZE + MD5_SIZE, newPayloadSize);
					}
					else {
						System.arraycopy(buf, MD5_OFFSET_M - MD5_SIZE, 
													md5Header, 0, MD5_SIZE);
						offset = MD5_OFFSET_M;
					}
				}
			}
			flag = 0;
			try {
				cipher.doFinal(buf, offset, payloadSize, buf);
				md.update(buf, 0, payloadSize);
				if (Arrays.equals(md5Header, md.digest()) == false)
					throw new IOException();
				} catch (BadPaddingException e) {
					Log.e("decrypt", "Bad Padding");
					e.printStackTrace();
					throw new IOException();
				} catch (IllegalBlockSizeException e) {
					Log.e("decrypt", "Illegal Block");
					e.printStackTrace();
					throw new IOException();
				} catch (ShortBufferException e) {
					Log.e("decrypt", "Short Buffer");
					e.printStackTrace();
					throw new IOException();
				}
		}
		
		if (bufferedSize <= len) {
			System.arraycopy(buf, transferredBytes, b, 0, bufferedSize);
			ret = bufferedSize;
			bufferedSize = 0;
			transferredBytes = 0;
		}
		else {
			System.arraycopy(buf, transferredBytes, b, 0, len);
			bufferedSize -= len;
			transferredBytes += len;
			ret = len;
		} 
		return ret;
	}
	
	
	private void setPayloadAlignSize(byte[] headerModified) throws IOException { 
		String decoded = new String(Arrays.copyOfRange(headerModified, FLAG_SIZE, 
													headerModifiedSize), "UTF-8");
		scanner = new Scanner(decoded);
		/* get total size and align size */
		try {
			payloadSize = scanner.nextInt();
			alignSize = scanner.nextInt();
		} catch (InputMismatchException e) {
			Log.e("header", "corrupt");
			e.printStackTrace();
			throw new IOException();
		} finally {
			scanner.close();
		}
	}
	
	private int readAll(byte[] b, int off, int totalSize, int checkLast) 
			throws IOException {
		int received = 0, realSize = 0, smallerBuf = 0;
		ret = 0;
		
		while (ret != totalSize) {
			received = inputStream.read(b, off + ret.intValue(), totalSize-ret);
			if (received == -1) {
				if (ret == 0)
					return -1;
				else
					return ret;
			}
			ret += received;
			if ((checkLast == 1) && (ret > (FLAG_SIZE + PAYLOAD_SIZE))) {
				if (b[0] == 0x31) {
					String decoded = new String(Arrays.copyOfRange(b, FLAG_SIZE, 
											FLAG_SIZE + PAYLOAD_SIZE), "UTF-8");
					scanner = new Scanner(decoded);
					try {
						realSize = scanner.nextInt();
					} catch (InputMismatchException e) {
						Log.e("header", "corrupt");
						e.printStackTrace();
						throw new IOException();
					} finally {
						scanner.close();
					}
					
					realSize += headerModifiedSize;
					if (realSize < totalSize)
						smallerBuf = 1;
					checkLast = 0;
				}
			}	
			if ((smallerBuf == 1) && (realSize == ret)) {
				socket.getOutputStream().write(0x00);
				return ret;
			}
		}
		return ret;
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
