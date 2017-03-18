/***************************************************************************
*   Seguranca e Confiabilidade 2016/17
*dd
*
***************************************************************************/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
						
					Message messIn = (Message) inStream.readObject();
										
					switch (messIn.method) {
					
						case "pushFile":
							pushFile(outStream, inStream, messIn, path);
							break;
							
						case "pushRep":
							pushRep(outStream, inStream, messIn, path);
							break;
							
						case "pullFile":
							pullFile(outStream, inStream, messIn, path);
							break;
							
						case "pullRep":
							pullRep(outStream, inStream, messIn, path);
							break;
						
						case "share":
							shareRep(outStream, inStream, messIn, path);
							break;
							
						case "remove":
							removeRep();
							break;
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
		
			
		private void shareRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) {
			try{
				File users = new File(path + "/users/users.txt");
				
				boolean foundUser = checkUser(messIn.user[1], users);
				
				if(foundUser){
					
					File shareLog = new File(path + "/users/shareLog.txt");
					if(!shareLog.exists())
						shareLog.createNewFile();
					
					FileWriter fw = new FileWriter(shareLog);
					
					if(checkUser(messIn.user[0], shareLog)){
						fw.write("," + messIn.user[1]);
					}
					else{
						fw.write(System.lineSeparator() + messIn.user[0] + ":" + messIn.user[1]); 
					    fw.flush();
					    fw.close();			
					}	
					
					Message messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, messIn.toBeUpdated, messIn.user, messIn.delete, "-- O repositório myrep foi partilhado com o utilizador " + messIn.user[1]);
					outStream.writeObject(messOut);
					
				}else{	
					
					Message messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, messIn.toBeUpdated, messIn.user, messIn.delete, "Erro: O utilizador " + messIn.user[1] + " não existe");
					outStream.writeObject(messOut);
					
				}
				
			}catch(IOException e){
				e.printStackTrace();
			}
		}


		private void removeRep() {
			// TODO Auto-generated method stub
			
		}


		private int pullRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) throws IOException {
			int result = -1;
			File rep  = null;
			Path currPath = null;
			File newFile = null;
			File currFile = null;
			File[] fileRep = null;
			Date date = null;
			boolean[] exists = null;
			
			Message messOut = null;
			boolean[] ya = null;
			int[] versions = null;
			
			//criar path para o rep
			if (messIn.repName.contains("/")) {
				rep = new File(path + "/users/" + messIn.repName);
				currPath = rep.toPath();
			} else { 
				rep = new File(path + "/users/" + messIn.user[0] + messIn.repName);
				currPath = rep.toPath();
			}
			//criar rep caso nao exista
			if (!rep.exists())
				return 0;
			//criar lista com todos os ficheiros
			else 
				fileRep = rep.listFiles();
			
			//caso o rep venha sem ficheiros (so para iniciar rep)
			if (messIn.fileName.length > 0) {
				ya = new boolean[messIn.fileName.length];
				versions = new int[messIn.fileName.length];
				exists = new boolean[fileRep.length];
				//
				for (int i = 0; i < messIn.fileName.length; i++) {
					currFile = new File(currPath + "/" + messIn.fileName[i]);
					if (currFile.exists()) {
						date = new Date(currFile.lastModified());
						
						//verificar quais os ficheiros que precisam de ser actualizados
						if (date.compareTo(messIn.fileDate[i]) < 0) {
							versions[i] = countNumVersions(path, messIn.fileName[i], messIn.user[0]);
							ya[i] = true;
							
						}
						
						//verificar quais os ficheiros q foram "apagados"
						//if (fileRep.)
						
					} else {
						ya[i] = true;
						versions[i] = 0;
					}
					
				}
			}
			
			return result;
		}


		private int pullFile(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) throws IOException {
			int result = -1;
			File file = null;
			
			boolean myrep = false;
			
			String repName = null;
			
			String otherUser = null;
			
			String filename = null;
			
			//criar path para o rep
			if (messIn.repName.contains("/")){
				file = new File(path + "/users/" + messIn.fileName[0]);
				otherUser = messIn.fileName[0].split("/")[0];
				filename = messIn.fileName[0].split("/")[2];
				repName = messIn.fileName[0].split("/")[1];
			}
			 else {
				file = new File(path + "/users/" + messIn.user[0] + messIn.fileName[0]);
				myrep = true;
				filename = messIn.fileName[0].split("/")[1];
				repName = messIn.fileName[0].split("/")[0];
			 }
						
			Date date = null;
					
			Message messOut = null;
			boolean[] ya = new boolean[1];
								
			if (file.exists()) {
				//actualiza o ficheiro para uma versao mais recente
				date = new Date(file.lastModified());
				
				if (date.compareTo(messIn.fileDate[0]) > 0) {
					ya[0] = true;
					if(myrep)
						messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- O repositório " + repName + " foi copiado do servidor");
					else
						messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- O repositório " + repName + " do utilizador " + otherUser + " foi copiado do servidor");
					outStream.writeObject(messOut);
					
					if (sendFile(outStream, inStream, file) >= 0) 
						result = 0;
										
				} else {
					//nao actualiza o ficheiro porque nao he mais recente
					ya[0] = false;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- O ficheiro do seu repositório local é o mais recente");
					outStream.writeObject(messOut);
					result = 0;
				}	
				
			//cria o ficheiro porque ainda existe
			} else {
				ya[0] = false;
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- O ficheiro " + filename + " foi copiado do servidor");
				outStream.writeObject(messOut);
			}
			return result;
		}


		private int pushFile(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) throws IOException {
			int result = -1;

			StringBuilder sb = new StringBuilder();
			sb.append(messIn.fileName[0]);
			int firstI = sb.indexOf("/");
			int secondI = sb.indexOf("/", firstI+1);
			String[] split = messIn.fileName[0].split("/");
			
			String filename = null;
			
			File file = null;
			Path pathFolder = null;
			
			if(secondI == -1) {
				pathFolder = new File(path + "/users/" + messIn.user + "/" + split[0]).toPath();
				file = new File(path + "/users/" + messIn.user + "/" + messIn.fileName[0]);
				filename = split[1];
			
			} else {
				pathFolder = new File(path + "/users/" + messIn.user + "/" + split[0] + "/" + split[1]).toPath();
				file = new File(path + "/users/" + messIn.fileName[0]);
				filename = split[2];
			}
			
			File newFile = null;

			Date date = null;
			boolean exists = file.exists();
			
			Message messOut = null;
			boolean[] ya = new boolean[1];
			String tempPath = null;
			
			int versao = 0;

			if (exists) {
				//actualiza o ficheiro para uma versao mais recente
				date = new Date(file.lastModified());
				
				if (date.compareTo(messIn.fileDate[0]) < 0) {
					
					versao = countNumVersions1(pathFolder, split[split.length-1]);
					newFile = new File(file + ".temp");
					
					ya[0] = true;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- O ficheiro " + filename + " foi atualizado no servidor");
					outStream.writeObject(messOut);
					newFile.createNewFile();
					if (receiveFile(outStream, inStream, newFile) >= 0) {
						
						file.renameTo(new File(pathFolder.toString() + split[split.length-1] + "." + String.valueOf(versao)));
						newFile.renameTo(new File(pathFolder.toString() + split[split.length-1]));

						result = 0;
						
					}
					
				} else {
					//nao actualiza o ficheiro porque nao he mais recente
					ya[0] = false;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- Nada a atualizar no servidor");
					outStream.writeObject(messOut);
					result = 0;
				}	
				
			//cria o ficheiro porque ainda existe
			} else {
				ya[0] = true;
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null);
				outStream.writeObject(messOut);
				file.createNewFile();
				if (receiveFile(outStream, inStream, file) >= 0) {
					result = 0;
					
				}
				
			}
			return result;			
		}


		private int pushRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) throws IOException {
			int result = -1;
			File rep  = null;
			Path currPath = null;
			Path currPath2 = null;
			File newFile = null;
			File currFile = null;
			File[] fileRep = null;
			Date date = null;
			boolean[] exists = null;
			
			Message messOut = null;
			boolean[] ya = null;
			int[] versions = null;
			
			//criar path para o rep
			if (messIn.repName.contains("/")) {
				rep = new File(path + "/users/" + messIn.repName);
				//currPath = new File(path + "/" + messIn.repName).toPath();
			} else { 
				rep = new File(path + "/users/" + messIn.user + "/" + messIn.repName);
				//currPath = new File(path + "/" + messIn.repName).toPath();
			}
			//criar rep caso nao exista
			if (!rep.exists())
				rep.mkdir();
			//criar lista com todos os ficheiros
			else 
				fileRep = rep.listFiles();
			
			//caso o rep venha sem ficheiros (so para iniciar rep)
			if (messIn.fileName.length > 0) {
				ya = new boolean[messIn.fileName.length];
				versions = new int[messIn.fileName.length];
				//exists = new boolean[fileRep.length];

				//
				for (int i = 0; i < messIn.fileName.length; i++) {

					currFile = new File(rep + "/" + messIn.fileName[i]);

					if (currFile.exists()) {
						date = new Date(currFile.lastModified());

						
						//verificar quais os ficheiros que precisam de ser actualizados
						if (date.compareTo(messIn.fileDate[i]) < 0) {

							versions[i] = countNumVersions1(rep.toPath(), messIn.fileName[i]);
							//versions[i] = countNumVersions(path, messIn.fileName[i], messIn.user);

							ya[i] = true;
							
						}
						
						//verificar quais os ficheiros q foram "apagados"
						//if (fileRep.)
						
					} else {
						ya[i] = true;
						versions[i] = 0;
					}
					
				}

			}
			
						
			messOut = new Message(messIn.method, null, messIn.repName, null, ya, messIn.user, null);
			outStream.writeObject(messOut);
					
			//receber os ficheiros para actualizar	
			for (int i = 0; i < messIn.fileName.length; i++) {
				
				//saber quais os ficheiros q vai client vai mandar
				if (messOut.toBeUpdated[i] == true) {

					currFile = new File(rep + "/" + messIn.fileName[i]);

					if(!currFile.exists()){

						newFile = currFile;

						newFile.createNewFile();
					}else{

						//currFile.createNewFile();
						newFile = new File(currFile + ".temp");

						newFile.createNewFile();
					}
					//receber os ficheiros
					if (receiveFile(outStream, inStream, newFile) >= 0) {
						//saber se o ficheiro e novo nao tem versao

						if (!(versions[i] == 0)) {
							currFile.renameTo(new File(rep + "/" + messIn.fileName[i] + "." + Integer.toString(versions[i])));
							newFile.renameTo(new File(rep + "/" + messIn.fileName[i]));
						}

						result = 0;
						
					}
				}
			}
			return result;
		}
		
		
		public int countNumVersions1(Path path, String filename) throws IOException{

            
            File folder = new File(path.toString());

            int numVersions = 0;
                       
            final String nameOfFile = filename;

            String[] list = folder.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String nameA) {
                    return nameA.contains(nameOfFile);
                }
            });
            
            numVersions = list.length;

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
		
		public int sendFile(ObjectOutputStream  outStream, ObjectInputStream inStream, File file) throws IOException {
			int result = 0;
			//File pdf = new File(file);
			int lengthPdf = (int) file.length();
			byte[] buf = new byte[1024];
	        FileInputStream is = new FileInputStream(file);
	        
	        outStream.writeInt(lengthPdf);
	        
	        int n = 0;
	        
	        while(((n = is.read(buf, 0, buf.length)) != -1)) {
	        	outStream.write(buf, 0, n);
	        	outStream.flush();        
	        }
	        
	        is.close();
			//inStream.close();
			//outStream.close();
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
		
		public boolean createUser(User u, File f, Path path) throws IOException{
			boolean result = false;
			FileWriter fw = new FileWriter(f, true);
			
			try {
				if(checkUser(u.name, f))
					result = true;
				else{
					//writes the name and pass in the file
					fw.write(System.lineSeparator() + u.name + ":" + u.pass); 
				    fw.flush();
				    fw.close();
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