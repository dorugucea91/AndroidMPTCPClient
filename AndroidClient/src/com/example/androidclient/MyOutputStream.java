package com.example.androidclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

import android.util.Log;

public class MyOutputStream {
	private OutputStream os;
	private BigInteger key;
	
	public MyOutputStream(OutputStream os, BigInteger key) {
		this.os = os;
		this.key = key;
	}
	
	public void close() throws IOException {
		Log.i("os", "close");
		os.close();
	}
}
