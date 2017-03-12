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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Scanner;

//Servidor myGitServer

public class myGitServer{

	public static void main(String[] args) {
		System.out.println("servidor: main");
		myGitServer server = new myGitServer();
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
		
		class User {
			
			private String name;
			private String pass;
			
			public User(String name, String pass){
				this.name = name;
				this.pass = pass;
			}
		}
 
		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				int continua = 1;
				
				int num_args = (int) inStream.readObject();
				
				boolean param_p = (boolean) inStream.readObject();
				
				String username = (String) inStream.readObject();
				
				/*Trying to get the path of the server*/
				URI myGitPath = myGit.class.getProtectionDomain().getCodeSource().getLocation().toURI();
				java.nio.file.Path path = java.nio.file.Paths.get(myGitPath);
				File users = new File(path + "/" + "users.txt");
				if(!users.exists())
					users.createNewFile();

				boolean foundU = checkUser(username, users);
				
				outStream.writeObject(foundU);
								
				if (!foundU) { //create user
				
					String pass = (String) inStream.readObject();
					User newUser = new User(username, pass);
					outStream.writeObject(createUser(newUser, users));
					
				} else if(param_p){ //confirm password
					
					boolean autentic = false;
					while(!autentic){
						String pass = (String) inStream.readObject();
						User user = new User(username, pass);
						autentic = autenticate(user, users);
						outStream.writeObject(autentic);
					}
					
				} else { //Receive pass
					
					boolean autentic = false;
					while(!autentic){
						String pass = (String) inStream.readObject();
						User user = new User(username, pass);
						autentic = autenticate(user, users);
						outStream.writeObject(autentic);
					}
					
				}
				

				//verifies if it has any methods in args
				if ((param_p && num_args > 4) || (!param_p && num_args > 2)) {
				
					while (continua == 1) {

						
						Message messIn = (Message) inStream.readObject();
											
						switch (messIn.method) {
						
							case "pushFile":
								pushFile(outStream, inStream, messIn, path);
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
					
				}
						
								
				outStream.close();
				inStream.close();
 			
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
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


		private void pushFile(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) {
			int result = -1;
			File file = new File(path + "/" + messIn.fileName[0]);
			
			boolean exists = file.exists();
			Date date = new Date(file.lastModified());
			
			Message messOut = null;
			boolean[] ya = new boolean[1];
		
			
			if (exists) {
				if (date.compareTo(messIn.fileDate[0]) < 0) {
					File newFile = new File();
					ya[0] = true;
					messOut = new Message(messIn.method, messIn.fileName, messIn.fileVersion, messIn.fileDate, ya);
					if (receiveFile(outStream, inStream, newFile) )
				}
			}
				
				
			
		}


		private void pushRep() {
			// TODO Auto-generated method stub
			
		}


		public int receiveFile(ObjectOutputStream  outStream, ObjectInputStream inStream, File file) throws IOException{
			int result = -1;
				
			FileOutputStream pdfOut = new FileOutputStream(file);
				
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
		}
		
		
		public boolean autenticate(User u, File f) throws IOException{
			
			boolean autenticado = false;
	
			Scanner scan = new Scanner(f);
			
			while (scan.hasNextLine()) {
				
				String[] split = scan.nextLine().split(":");
				if(split[0].equals(u.name) && split[1].equals(u.pass))
					autenticado = true; 
			
			}
			
			scan.close();
			
			return autenticado;
		}
		
		
		public boolean createUser(User u, File f){
			boolean result = false;
			StringBuilder userPass = new StringBuilder();
			userPass.append(u.name);
			userPass.append(':');
			userPass.append(u.pass);
			
			try {
				if(checkUser(u.name, f))
					result = true;
				else{
					byte[] buf = userPass.toString().getBytes(StandardCharsets.UTF_8);
					Files.write(f.toPath(), buf);
					result = true;				
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return result;
		}
		
		
		public boolean checkUser(String username, File f) throws FileNotFoundException {
			
			boolean result = false;
			Scanner scan = new Scanner(f);
			
			while (scan.hasNextLine()) {
				
				String[] split = scan.nextLine().split(":");
				if(split[0].equals(username))
					result = true; 
			
			}
			
			scan.close();
			return result;
			
		}
	}
}