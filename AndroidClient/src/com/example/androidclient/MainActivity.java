package com.example.androidclient;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import com.example.androidclient.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	private TextView textResponse;
	private EditText editTextAddress, editTextPort; 
	private Button buttonConnect, buttonDownload, buttonUpload;
	private Socket clientSocket;
	private InputStream is;
	private OutputStream os;
	private int cnt = 1;
	static private long elapsed;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		editTextAddress = (EditText)findViewById(R.id.address);
		editTextPort = (EditText)findViewById(R.id.port);
		buttonConnect = (Button)findViewById(R.id.connect);
		buttonDownload = (Button)findViewById(R.id.download);
		buttonUpload = (Button)findViewById(R.id.upload);
		textResponse = (TextView)findViewById(R.id.response);
		
		buttonConnect.setOnClickListener(buttonConnectOnClickListener);
		buttonDownload.setOnClickListener(buttonDownloadOnClickListener);
		buttonUpload.setOnClickListener(buttonUploadOnClickListener);
		
		buttonDownload.setEnabled(false);
		buttonUpload.setEnabled(false);
		
		editTextAddress.setText("141.85.37.123");
		editTextPort.setText("80");
//		int i;
		elapsed = 0;
			
//		for (i = 0; i < 1; i++ ) {
//			buttonConnect.callOnClick();
//			buttonDownload.callOnClick();
//			if ((i % 30) == 0 )
//				try {
//					Thread.sleep(500);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//		}	
	}
	
	OnClickListener buttonConnectOnClickListener = 
			new OnClickListener() {
				Integer port;
				String dstAddress;
				
				@Override
				public void onClick(View arg0) {
					dstAddress = editTextAddress.getText().toString();
					if (dstAddress.length() == 0) {
						textResponse.setText("Invalid IP Address");
						return;
					}
					
					try {
						port = Integer.parseInt(editTextPort.getText().toString());
					} catch (NumberFormatException e) {
						textResponse.setText("Invalid port number");
						return;
					}
					MyClientTask myClientTask = new MyClientTask(dstAddress, port);
					myClientTask.execute();
				}
	};
	
	OnClickListener buttonDownloadOnClickListener = 
			new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					textResponse.setText("");
					DownloadFile downloadFileTask = new DownloadFile();
					downloadFileTask.execute();
				}
	};
	
	OnClickListener buttonUploadOnClickListener = 
			new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					textResponse.setText("");
					UploadFile uploadFile = new UploadFile();
					uploadFile.execute();
				}
	};
	

	public void freeResources() throws IOException {
		if (clientSocket != null) {
			clientSocket.close();
			clientSocket = null;
			Log.i("clientSocket", "closed");
		}
		if (is != null) {
			is.close();
			is = null;
			Log.i("is", "closed");
		}
		if (os != null) {
			os.close();
			Log.i("os", "closed");
		}
	}
	
	public class DownloadFile extends AsyncTask<Void, Integer, Void> {
		private String response;
		private byte[] deed = new byte[8192];	
		private int bytesRead;
		private byte start[] = new byte[1];
		long startTime, stopTime, elapsedTime, start1, start2, writtenTime;
		
		@Override
		protected Void doInBackground(Void... arg0) {
			if (clientSocket != null && is != null && os != null) {
				try {
					os.write(start, 0, 1);
					clearContent();
					response = "Downloading...";
					publishProgress(1);
					startTime = System.currentTimeMillis();
					while ((bytesRead = is.read(deed, 0, 8192)) != -1) {
						start1 = System.currentTimeMillis();
						String decoded = new String(deed, 0, bytesRead);
						writeToFile(decoded);
						start2 = System.currentTimeMillis();
						writtenTime += (start2 - start1);
					}
					stopTime = System.currentTimeMillis();
				    elapsedTime = stopTime - startTime - writtenTime;
				    elapsed += elapsedTime;
				    Log.i("download time", Long.valueOf(elapsed / cnt).toString());
				    cnt++;
				    clientSocket.shutdownInput();
				    clientSocket.shutdownOutput();
				    clientSocket.close();
				    //is.disp();
					response = "Download finished.";
					publishProgress(0);
				} catch (UnknownHostException e) {
					e.printStackTrace();
					response = "UnknownHostException: " + e.toString();
					publishProgress(0);
				} catch (IOException e) {
					e.printStackTrace();
					response = "IOException: " + e.toString();
					publishProgress(0);
				} catch (Exception e) {
					e.printStackTrace();
					response = "Exception: " + e.toString();
					publishProgress(0);
				}
			}
			else {
				response = "Socket is closed";
				publishProgress(0);
			}
			return null;
		}
		
		@Override
		 protected void onProgressUpdate(Integer... progress) { 
			if (progress[0].intValue() == 1) {
				textResponse.setText(response);
				buttonConnect.setEnabled(false);
				buttonDownload.setEnabled(false);
				buttonUpload.setEnabled(false);
			}
			else if (progress[0].intValue() == 0){
				textResponse.setText(response);
				buttonConnect.setEnabled(true);
				buttonDownload.setEnabled(false);
				buttonUpload.setEnabled(false);
			}
			super.onProgressUpdate(progress);
		 }
		
		private void clearContent() {
			try {
		        OutputStreamWriter outputStreamWriter = 
		        		new OutputStreamWriter(
		        				openFileOutput("config.txt", Context.MODE_PRIVATE));
		        outputStreamWriter.close();
		    }
		    catch (IOException e) {
		        Log.e("Exception", "Clear content: " + e.toString());
		    } 
		}
		
		private void writeToFile(String data) {
		    try {
		        OutputStreamWriter outputStreamWriter = 
		        		new OutputStreamWriter(
		        				openFileOutput("config.txt", Context.MODE_APPEND));
		        outputStreamWriter.write(data);
		        outputStreamWriter.close();
		    }
		    catch (IOException e) {
		        Log.e("Exception", "File write failed: " + e.toString());
		    } 
		}
	}
	
	public class UploadFile extends AsyncTask<Void, Integer, Void> {
		private String response;
		
		@Override
		protected Void doInBackground(Void... arg0) {
			if (clientSocket != null && is != null && os != null) {
				try {
					response = "Uploading...";
					publishProgress(1);
					sendFile(os);
					os.close();
					is.close();
					clientSocket.close();
					response = "Uploaded.";
					publishProgress(0);
				} catch (IOException e) {
					e.printStackTrace();
					response = "IOException: " + e.toString();
					publishProgress(0);
				} catch (Exception e) {
					e.printStackTrace();
					response = "Exception: " + e.toString();
					publishProgress(0);
				}
			}
			else {
				response = "Socket is closed";
				publishProgress(0);
			}
			return null;
		}
		
		@Override
		 protected void onProgressUpdate(Integer... progress) { 
			if (progress[0].intValue() == 1) {
				textResponse.setText(response);
				buttonConnect.setEnabled(false);
				buttonDownload.setEnabled(false);
				buttonUpload.setEnabled(false);
			}
			else if (progress[0].intValue() == 0) {
				buttonConnect.setEnabled(true);
				textResponse.setText(response);
				buttonDownload.setEnabled(false);
				buttonUpload.setEnabled(false);	
			}
			super.onProgressUpdate(progress);
		 }
		
		private String sendFile(OutputStream os) throws IOException {
			long start1, start2, elapsedTime = 0, stopTime, startTime, writtenTime = 0;
			byte[] buf = new byte[8192];
		    String ret = "";
		    int bytesRead;
		    try {
		        InputStream inputStream = openFileInput("config.txt");
		        
		        if ( inputStream != null ) {
		            while ( (bytesRead = inputStream.read(buf, 0, 8192)) != -1) {
		                start1 = System.currentTimeMillis();
		            	os.write(buf, 0, bytesRead);
		            	 start2 = System.currentTimeMillis();
		            	 elapsedTime += (start2 - start1);
		            }
		             
				    elapsed += elapsedTime;
				    Log.i("totalTime", Long.valueOf(elapsed / cnt).toString());
				    cnt++;
		            inputStream.close();
		        }
		    }
		    catch (FileNotFoundException e) {
		        Log.e("login activity", "File not found: " + e.toString());
		    } 
		    return ret;
		}
	}
	
	public class MyClientTask extends AsyncTask<Void, Integer, Void> {
		private String dstAddress;
		private int dstPort;
		private String response = "";
		
		public MyClientTask(String addr, int port){
			dstAddress = addr;
			dstPort = port;
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			SocketAddress serverAddress = new InetSocketAddress(dstAddress, dstPort);		
			clientSocket  = new Socket();
			long start, finish;
			try {
				response = "Connecting to server...";
				publishProgress(2);
				start = System.currentTimeMillis();
				clientSocket.connect(serverAddress);
				finish = System.currentTimeMillis();
				Log.i("connect time", Long.valueOf(finish-start).toString());
				is = clientSocket.getInputStream();
				os = clientSocket.getOutputStream();
				response = "Connected to server.";
				publishProgress(1);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				response = "UnknownHostException: " + e.toString();
				publishProgress(0);
				System.exit(-1);
			} catch (IOException e) {
				e.printStackTrace();
				response = "IOException: " + e.toString();
				publishProgress(0);
				System.exit(-1);
			} catch (Exception e) {
				e.printStackTrace();
				response = "Exception: " + e.toString();
				publishProgress(0);
				System.exit(-1);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			textResponse.setText(response);
			super.onPostExecute(result);
		}
		
		@Override
		 protected void onProgressUpdate(Integer... progress) { 
			if (progress[0].intValue() == 1) {
				textResponse.setText(response);
				buttonDownload.setEnabled(true);
				buttonConnect.setEnabled(false);
				buttonUpload.setEnabled(true);
			}
			else if (progress[0].intValue() == 2) {
				textResponse.setText(response);
				buttonDownload.setEnabled(false);
				buttonConnect.setEnabled(false);
				buttonUpload.setEnabled(false);
			}
			else {
				textResponse.setText(response);
				buttonConnect.setEnabled(true);
			}	
			super.onProgressUpdate(progress);
		 }
	}
}
