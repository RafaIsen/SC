/***************************************************************************
*   Seguranca e Confiabilidade 2016/17
*
*
***************************************************************************/

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

//Servidor myServer

public class myServer{

	public static void main(String[] args) {
		System.out.println("servidor: main");
		myServer server = new myServer();
		server.startServer();
	}

	public void startServer (){
		ServerSocket sSoc = null;
        
		try {
			sSoc = new ServerSocket(23456);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
         
		while(true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		}
		//sSoc.close();
	}


	//Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}
 
		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				String user = null;
				String passwd = null;
				boolean autenticado = false;
				
				while(!autenticado){
					try {
						user = (String)inStream.readObject();
						passwd = (String)inStream.readObject();
						System.out.println("thread: depois de receber a password e o user");
					}catch (ClassNotFoundException e1) {
						e1.printStackTrace();
					}
				
					if(user.equals("Rafa") && passwd.equals("1234"))
						autenticado = true;
				
					if (autenticado){
						outStream.writeObject(new Boolean(true));
					}
					else {
						outStream.writeObject(new Boolean(false));
					}	
				}
			
				File pdf = new File("C:\\Users\\rafae\\Desktop\\SC\\b.pdf");
				
				FileOutputStream pdfOut = new FileOutputStream(pdf);
				
				int lengthFile = inStream.readInt();
				
				int n = 0;
				
				byte[] buf = new byte[1024];
				
				while(lengthFile > 0){
					n = inStream.read(buf, 0, buf.length);
					lengthFile -= n;
					pdfOut.write(buf, 0, n);
					pdfOut.flush();
				}
				
				pdfOut.close();
				
				outStream.close();
				inStream.close();
 			
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}