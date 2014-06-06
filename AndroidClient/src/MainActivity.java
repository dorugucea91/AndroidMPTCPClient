

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import com.example.androidclient.R;
import com.example.androidclient.R.id;
import com.example.androidclient.R.layout;

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
	private Button buttonConnect, buttonDisconnect, buttonDownload, buttonUpload;
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
		buttonDisconnect = (Button)findViewById(R.id.disconnect);
		buttonDownload = (Button)findViewById(R.id.download);
		buttonUpload = (Button)findViewById(R.id.upload);
		textResponse = (TextView)findViewById(R.id.response);
		
		buttonConnect.setOnClickListener(buttonConnectOnClickListener);
		buttonDisconnect.setOnClickListener(buttonDisconnectOnClickListener);
		buttonDownload.setOnClickListener(buttonDownloadOnClickListener);
		buttonUpload.setOnClickListener(buttonUploadOnClickListener);
		
		buttonDisconnect.setEnabled(false);
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
	
	OnClickListener buttonDisconnectOnClickListener = 
			new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					textResponse.setText("");
					try {
						freeResources();
						buttonDownload.setEnabled(false);
						buttonDisconnect.setEnabled(false);
						buttonConnect.setEnabled(true);
						buttonUpload.setEnabled(false);
						
					} catch (IOException e) {
						e.printStackTrace();
						textResponse.setText("IOException: " + e.toString());
						buttonDownload.setEnabled(true);
						buttonDisconnect.setEnabled(true);
						buttonConnect.setEnabled(true);
						buttonUpload.setEnabled(true);
					}
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
		private byte[] decrypted = new byte[4096];	
		private int bytesRead;
		private byte start[] = new byte[1];
		
		@Override
		protected Void doInBackground(Void... arg0) {
			if (clientSocket != null && is != null && os != null) {
				try {
					os.write(start, 0, 1);
					clearContent();
					response = "Downloading...";
					publishProgress(1);
					while ((bytesRead = is.read(decrypted, 0, 4096)) != -1) {
						String decoded = new String(decrypted, 0, bytesRead);
						writeToFile(decoded);
					}
					response = "Download finished.";
					publishProgress(3);
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
				buttonDownload.setEnabled(false);
				buttonDisconnect.setEnabled(false);
				buttonConnect.setEnabled(false);
			}
			else if (progress[0].intValue() == 0){
				textResponse.setText(response);
				buttonDownload.setEnabled(true);
				buttonDisconnect.setEnabled(true);
				buttonConnect.setEnabled(true);
			}
			else {
				textResponse.setText(response);
				buttonDownload.setEnabled(true);
				buttonDisconnect.setEnabled(true);
				buttonConnect.setEnabled(false);
			}
			super.onProgressUpdate(progress);
		 }
		
		private void clearContent() {
			try {
		        OutputStreamWriter outputStreamWriter = 
		        		new OutputStreamWriter(openFileOutput("config.txt", Context.MODE_PRIVATE));
		        outputStreamWriter.close();
		    }
		    catch (IOException e) {
		        Log.e("Exception", "Clear content: " + e.toString());
		    } 
		}
		
		private void writeToFile(String data) {
		    try {
		        OutputStreamWriter outputStreamWriter = 
		        		new OutputStreamWriter(openFileOutput("config.txt", Context.MODE_APPEND));
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
					response = "Uploaded.";
					publishProgress(2);
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
				buttonDownload.setEnabled(false);
				buttonDisconnect.setEnabled(false);
				buttonConnect.setEnabled(false);
				buttonUpload.setEnabled(false);
			}
			else if (progress[0].intValue() == 0){
				textResponse.setText(response);
				buttonDownload.setEnabled(true);
				buttonDisconnect.setEnabled(true);
				buttonConnect.setEnabled(true);
				buttonUpload.setEnabled(true);
			}
			else {
				textResponse.setText(response);
				buttonDownload.setEnabled(true);
				buttonDisconnect.setEnabled(true);
				buttonConnect.setEnabled(false);
				buttonUpload.setEnabled(true);
			}
			super.onProgressUpdate(progress);
		 }
		
		private String sendFile(CryptOutputStream os) throws IOException {
			byte[] buf = new byte[20];
		    String ret = "";
		    int bytesRead;
		    try {
		        InputStream inputStream = openFileInput("config.txt");
		        if ( inputStream != null ) {
		            while ( (bytesRead = inputStream.read(buf, 0, 20)) != -1) {
		            	os.write(buf, 0, bytesRead);
		            }
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
				buttonDisconnect.setEnabled(true);
				buttonConnect.setEnabled(false);
				buttonUpload.setEnabled(true);
			}
			else if (progress[0].intValue() == 2) {
				textResponse.setText(response);
				buttonDownload.setEnabled(false);
				buttonDisconnect.setEnabled(false);
				buttonConnect.setEnabled(false);
			}
			else {
				textResponse.setText(response);
				buttonDownload.setEnabled(true);
				buttonDisconnect.setEnabled(true);
				buttonConnect.setEnabled(true);
			}	
			super.onProgressUpdate(progress);
		 }
	}
}
