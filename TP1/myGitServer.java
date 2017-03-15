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
				Path path = java.nio.file.Paths.get(myGitPath);
				
				//creates the directory users if it does not exist
				File usersDir = new File(path + "/users");
				if(!usersDir.exists())
					usersDir.mkdir();
				//creates the text file users if it does not exist
				File users = new File(path + "/users/users.txt");
				if(!users.exists()){
					users.createNewFile();
				}

				boolean foundU = checkUser(username, users);
				
				outStream.writeObject(foundU);
								
				if (!foundU) { //create user
				
					String pass = (String) inStream.readObject();
					User newUser = new User(username, pass);
					outStream.writeObject(createUser(newUser, users, path));
					
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
								if (pushFile(outStream, inStream, messIn, path) > 0)
								break;
								
							case "pushRep":
								if (pushRep(outStream, inStream, messIn, path) > 0)
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


		private int pushFile(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) throws IOException {
			int result = -1;

			StringBuilder sb = new StringBuilder();
			sb.append(messIn.fileName);
			int firstI = sb.indexOf("/");
			int secondI = sb.indexOf("/", firstI);
			
			File file = null;
			
			if(secondI == -1)
				file = new File(path + "/users/" + messIn.user + "/" + messIn.fileName[0]);
			else
				file = new File(path + "/users/" + messIn.fileName[0]);
			File newFile = null;

			Date date = null;
			boolean exists = file.exists();
			
			Message messOut = null;
			boolean[] ya = new boolean[1];
			
			int versao = 0;

			if (exists) {
				//actualiza o ficheiro para uma versao mais recente
				date = new Date(file.lastModified());
				
				if (date.compareTo(messIn.fileDate[0]) < 0) {
					
					if(secondI == -1)
						versao = countNumVersions(path, messIn.fileName[0], messIn.user);
					else
						versao = countNumVersions(path, messIn.fileName[0], null);
					newFile = new File(path + "/users/" + messIn.fileName[0] + "temp");
					
					ya[0] = true;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user);
					outStream.writeObject(messOut);
					
					if (receiveFile(outStream, inStream, newFile) >= 0) {
						result = 0;	
						file.renameTo(new File(path + "/users/" + messIn.fileName[0] + "." + Integer.toString(versao)));
						newFile.renameTo(new File(path + "/users/" + messIn.fileName[0]));
					}
					
				} else {
					//nao actualiza o ficheiro porque nao he mais recente
					ya[0] = false;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user);
					outStream.writeObject(messOut);
					result = 0;
				}	
				
			//cria o ficheiro porque ainda existe
			} else {
				
				ya[0] = true;
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user);
				outStream.writeObject(messOut);
				if (receiveFile(outStream, inStream, file) >= 0) {
					result = 0;
					file.createNewFile();
				}
				
			}
			return result;			
		}


		private int pushRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) {
			int result = -1;
			
			File file = new File(path + "/users/" + messIn.fileName[0]);
			File newFile = null;

			Date date = null;
			boolean exists = file.exists();
			
			Message messOut = null;
			boolean[] ya = new boolean[1];
			
			int versao = 0;
			return result;
			
			
		}
		
		
		public int countNumVersions(Path path, String filename, String username) throws IOException{
			
			String[] pathFile = filename.split("/");
			
			File folder = null;
			
			int numVersions = 0;
			
			String name = null;
			
			if (pathFile.length == 3) {
				
				folder = new File(path + "/" + pathFile[0] + "/" + pathFile[1] + "/");
				name = pathFile[2];
			
			}
			else {
				
				folder = new File(path + "/" + username + "/" + pathFile[0] + "/");
				name = pathFile[1];
			
			}
				
			File[] listOfFiles = folder.listFiles();
			
			for (int i = 0; i < listOfFiles.length; i++) {
				
				String[] nameFile = listOfFiles[i].getName().split(".");
				
				if(nameFile[0].equals(name))
					numVersions ++;
				
			}
			
			return numVersions;
			
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
				result = 0;
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
		
		public boolean createUser(User u, File f, Path path){
			boolean result = false;
			StringBuilder userPass = new StringBuilder();
			userPass.append(u.name);
			userPass.append(':');
			userPass.append(u.pass);
			
			try {
				if(checkUser(u.name, f))
					result = true;
				else{
					//writes the name and pass in the file
					byte[] buf = userPass.toString().getBytes(StandardCharsets.UTF_8);
					Files.write(f.toPath(), buf);
					//creates a directory to the user
					new File(path + "/users/" + u.name).mkdir();
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