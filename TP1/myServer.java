/***************************************************************************
*   Seguranca e Confiabilidade 2016/17
*
*
***************************************************************************/

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
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
		
		class Client {
			
			private String name;
			private String pass;
			
			public Client(String name, String pass){
				this.name = name;
				this.pass = pass;
			}
		}
 
		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				int continua = 1;
				
				while (continua == 1) {
					String in = (String) inStream.readObject();
					String[] split = in.split(" ");
					
					switch (split[0]) {
					
						case "pushFile":
							pushFile(outStream, inStream);
							break;
							
						case "pushRep":
							pushRep();
							break;
							
						case "pullFile":
							pullFile();
							break;
							
						case "pullRep":
							pullRep();
							break;
						
						case "share":
							shareRep();
							break;
							
						case "remove":
							removeRep();
							break;
						}
					
					
				}
				
<<<<<<< HEAD
				Client c = new Client(user, passwd);
				File nomes = new File("C:\\Users\\rafae\\git\\SC\\TP1\\nomes.txt");
				boolean i = checkClient(c, nomes);
				System.out.println(i);
				if(!i)
					createClient(c, nomes);
			
				receiveFile(outStream, inStream);
=======
				
				
				//Client c= null;		
				
				//File nomes = new File("${user.home}/Rep/nomes.txt");
				//createClient(c, nomes);
				//boolean i = checkClient(c, nomes);
				//System.out.println(i);
						
				//receiveFile(outStream, inStream);
>>>>>>> refs/heads/Andrade
				
				outStream.close();
				inStream.close();
 			
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
			
		private void shareRep() {
			// TODO Auto-generated method stub
			
		}


		private void removeRep() {
			// TODO Auto-generated method stub
			
		}


		private void pullRep() {
			// TODO Auto-generated method stub
			
		}


		private void pullFile() {
			// TODO Auto-generated method stub
			
		}


		private void pushFile(ObjectOutputStream outStream, ObjectInputStream inStream) {
			// TODO 
			
			
		}


		private void pushRep() {
			// TODO Auto-generated method stub
			
		}


		public int receiveFile(ObjectOutputStream  outStream, ObjectInputStream inStream) throws IOException{
			int result = -1;
			File pdf = new File("C:\\Users\\rafae\\git\\SC\\TP1\\a_copy.pdf");
				
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
				return result;
<<<<<<< HEAD
=======
		}
		
		
		public int autenticate(ObjectOutputStream  outStream, ObjectInputStream inStream, Client c) throws IOException{
			int result = -1;
			String user = null;
			String passwd = null;
			boolean autenticado = false;
				
			while(!autenticado){
				try {
					user = (String)inStream.readObject();
					passwd = (String)inStream.readObject();
					System.out.println("thread:depois de receber a password e o user");
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
			c = new Client(user, passwd);
			}
			return result;
>>>>>>> refs/heads/Andrade
		}
		
		
		public int createClient(Client c, File f){
			int result = -1;
			StringBuilder ya = new StringBuilder();
			ya.append(c.name);
			ya.append(':');
			ya.append(c.pass);
			
			try {
				if(checkClient(c, f))
					result = 0;
				
				else{
					byte[] buf = ya.toString().getBytes(StandardCharsets.UTF_8);
					Files.write(f.toPath(), buf);
					result = 0;				
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return result;
			
		}
		
		
		public boolean checkClient(Client c, File f) throws FileNotFoundException {
			boolean result = false;
			Scanner scan = new Scanner(f);
			
			while(scan.hasNextLine()){
				String[] split = scan.nextLine().split(":");
				if(split[0].equals(c.name))
					result = true; 
			}
			scan.close();
			return result;
			
		}
	}
}