package com.example.androidclient;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
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
	private CryptSocket clientSocket;
	private CryptInputStream is;
	private CryptOutputStream os;
	
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
		private byte[] decrypted = new byte[10000];	
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
					while ((bytesRead = is.read(decrypted, 0, 10000)) != -1) {
						//start1 = System.currentTimeMillis();
						//String decoded = new String(decrypted, 0, bytesRead);
						//writeToFile(decoded);
						//start2 = System.currentTimeMillis();
						//writtenTime += (start2 - start1);
					}
					stopTime = System.currentTimeMillis();
				    elapsedTime = stopTime - startTime - writtenTime;
				    Log.i("elapsed time", Long.valueOf(elapsedTime).toString());
				    is.close();
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
		
		private String sendFile(CryptOutputStream os) throws IOException {
			long start1, start2, elapsed = 0;
			byte[] buf = new byte[5000];
		    String ret = "";
		    int bytesRead;
		    try {
		        InputStream inputStream = openFileInput("test");
		        if ( inputStream != null ) {
		            while ( (bytesRead = inputStream.read(buf, 0, 5000)) != -1) {
		            	start1 = System.currentTimeMillis();
		            	os.write(buf, 0, bytesRead);
		            	start2 = System.currentTimeMillis();
		            	elapsed += (start2 - start1);
		            }
		            Log.i("elapsed", Long.valueOf(elapsed).toString());
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
			clientSocket  = new CryptSocket();	
			try {
				response = "Connecting to server...";
				publishProgress(2);
				clientSocket.connect(serverAddress);
				is = clientSocket.getCryptInputStream();
				os = clientSocket.getCryptOutputStream();
				response = "Connected to server.";
				publishProgress(1);
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
