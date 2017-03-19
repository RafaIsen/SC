/***************************************************************************
*   Seguranca e Confiabilidade 2016/17
*dd
*
***************************************************************************/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
							shareRep();
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
		
			
		private void shareRep() {
			// TODO Auto-generated method stub
			
		}


		private void removeRep() {
			// TODO Auto-generated method stub
			
		}


		private int pullRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) throws IOException {
			int result = -1;
			File rep  = null;
			File currFile = null;
			File[] fileRep = null;
			
			Date date = null;
			boolean[] delete = null;
			
			Message messOut = null;
			boolean[] ya = null;
			String[] names = null;
			
			//criar path para o rep
			if (messIn.repName.contains("/")) 
				rep = new File(path + "/users/" + messIn.repName);
				
			//rep do user
			else 
				rep = new File(path + "/users/" + messIn.user + messIn.repName);
				
			//erro caso o rep nao exista
			if (!rep.exists())
				return -1;
			//criar lista com todos os ficheiros

			else {
				//final String nameOfFile = filename;

	             fileRep = rep.listFiles(new FilenameFilter() {
	                @Override
	                public boolean accept(File dir, String nameA) {
	                    return !nameA.matches(".[1-9]+");
	                }
	             });
			}
			//so para actualizar os ficheiros
			if (fileRep.length > 0) {
				ya = new boolean[fileRep.length];
				names = new String[fileRep.length];
				if (fileRep.length > messIn.fileName.length)
					delete = new boolean[fileRep.length];
				else
					delete = new boolean[messIn.fileName.length];
				//
				for (int i = 0; i < fileRep.length; i++) {
					if (i < messIn.fileName.length) {
						names[i] = messIn.fileName[i];
						currFile = new File(rep.toString() + "/" + messIn.fileName[i]);
						
						//se o ficheiro ainda existe no servidor
						if (currFile.exists()) {
							date = new Date(currFile.lastModified());
							
							//verificar se o ficheiro precisa de ser actualizado
							if (date.compareTo(messIn.fileDate[i]) > 0) 
								ya[i] = true;
														
						//ficheiros que o servidor ja nao tem	
						} else 
							delete[i] = true;	
						
					//ficheiros novos que o cliente nao tem	
					} else {
						for (int j = 0;j < names.length; j++)
							if (!Arrays.asList(names).contains(fileRep[j].getName()))
								names[i] = fileRep[j].getName();
				
						ya[i] = true;
					}
										
				}
				if (messIn.fileName.length > fileRep.length)
					Arrays.fill(delete, fileRep.length, messIn.fileName.length-1, Boolean.TRUE);
					
				messOut = new Message(messIn.method, names, messIn.repName, messIn.fileDate, ya, messIn.user, delete);
				outStream.writeObject(messOut);
				
				for (int i = 0; i < ya.length; i++) {
					if (ya[i] == true)
						sendFile(outStream, inStream, new File(rep.toString() + "/" + names[i]));
				}
				
				result = 0;
				
				
			//para inicializar o rep
			} else {
				ya = new boolean[fileRep.length];
				names = new String[fileRep.length];
				Arrays.fill(ya, 0, ya.length-1, Boolean.TRUE);
				for(int i = 0; i < ya.length; i++)
					names[i] = fileRep[i].getName();
				messOut = new Message(messIn.method, names, messIn.repName, messIn.fileDate, ya, messIn.user, null);
				outStream.writeObject(messOut);
				
				for (int i = 0; i < fileRep.length; i++) 
					sendFile(outStream, inStream, fileRep[i]);
								
				result = 0;
			}
				
			
			return result;
		}


		private int pullFile(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) throws IOException {
			int result = -1;
			File file = null;
			String[] split = messIn.fileName[0].split("/");
			
			Date date = null;
			Message messOut = null;
			boolean[] ya = new boolean[1];
			
			//criar path para o ficheiro
			
			if (split.length == 3) 
				file = new File(path + "/users/" + messIn.fileName[0]);
				
			 else  
				file = new File(path + "/users/" + messIn.user + "/" + messIn.fileName[0]);
						
			
								
			if (file.exists()) {
				//actualiza o ficheiro para uma versao mais recente
				date = new Date(file.lastModified());
				
				if (date.compareTo(messIn.fileDate[0]) > 0) {
					ya[0] = true;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null);
					outStream.writeObject(messOut);
					sendFile(outStream, inStream, file);
					result = 0;
										
				} else {
					//nao actualiza o ficheiro porque nao he mais recente
					ya[0] = false;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null);
					outStream.writeObject(messOut);
					result = 0;
				}	
				
			//erro o ficeiro nao existe
			} else {
				ya[0] = false;
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null);
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
			
			File file = null;
			Path pathFolder = null;
			
			if(secondI == -1) {
				pathFolder = new File(path + "/users/" + messIn.user + "/" + split[0]).toPath();
				file = new File(path + "/users/" + messIn.user + "/" + messIn.fileName[0]);
			
			} else {
				pathFolder = new File(path + "/users/" + messIn.user + "/" + split[0] + "/" + split[1]).toPath();
				file = new File(path + "/users/" + messIn.fileName[0]);
			}
			
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
					
					versao = countNumVersions1(pathFolder, split[split.length-1]);
					newFile = new File(file + ".temp");
					
					ya[0] = true;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null);
					outStream.writeObject(messOut);
					newFile.createNewFile();
					receiveFile(outStream, inStream, newFile);
					file.renameTo(new File(pathFolder.toString() + "/" + split[split.length-1] + "." + String.valueOf(versao)));
					newFile.renameTo(new File(pathFolder.toString() + "/" + split[split.length-1]));
					result = 0;
					
					
				} else {
					//nao actualiza o ficheiro porque nao he mais recente
					ya[0] = false;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null);
					outStream.writeObject(messOut);
					result = 0;
				}	
				
			//cria o ficheiro porque ainda existe
			} else {
				ya[0] = true;
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null);
				outStream.writeObject(messOut);
				file.createNewFile();
				receiveFile(outStream, inStream, file);
				result = 0;
									
			}
			return result;			
		}


		private int pushRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) throws IOException {
			int result = -1;
			File rep  = null;
			File newFile = null;
			File currFile = null;
			File[] fileRep = null;
			Date date = null;
			String nameAux = null;
			
			Message messOut = null;
			boolean[] ya = null;
			int[] versions = null;
			
			//criar path para o rep
			if (messIn.repName.contains("/")) 
				rep = new File(path + "/users/" + messIn.repName);
				
			else  
				rep = new File(path + "/users/" + messIn.user + "/" + messIn.repName);
			
			//criar rep caso nao exista
			if (!rep.exists())
				rep.mkdir();
			else {
				//final String nameOfFile = filename;

	             fileRep = rep.listFiles(new FilenameFilter() {
	                @Override
	                public boolean accept(File dir, String nameA) {
	                    return nameA.endsWith(".[1-9]+");
	                }
	             });
			}
			
			//caso o rep venha sem ficheiros (so para iniciar rep)
			if (messIn.fileName.length > 0) {
				ya = new boolean[messIn.fileName.length];
				versions = new int[messIn.fileName.length];

				//
				
					for (int i = 0; i < messIn.fileName.length; i++) {
						//currFile = new File(rep.toString() + "/" + messIn.fileName[i]);
						if (fileRep != null && fileRep[i].exists()) {
							date = new Date(fileRep[i].lastModified());
							
							//verificar quais os ficheiros que precisam de ser actualizados
							if (date.compareTo(messIn.fileDate[i]) < 0) {
								versions[i] = countNumVersions1(rep.toPath(), messIn.fileName[i]);
								ya[i] = true;	
							}
							
						} else {
							ya[i] = true;
							versions[i] = 0;
						}
						
					}

			}
			
			//saber quais os ficheiros a "eliminar"
			if (fileRep != null)
				for (int j = 0; j < messIn.fileName.length; j++)
					if (Arrays.binarySearch(messIn.fileName, fileRep[j].toString()) > 0) {
						nameAux = fileRep[j].toString();
						fileRep[j].renameTo(new File(rep.toPath() + "/" + nameAux + "." + String.valueOf(countNumVersions1(rep.toPath(), fileRep[j].toString()))));
					}
						
			messOut = new Message(messIn.method, null, messIn.repName, null, ya, messIn.user, null);
			outStream.writeObject(messOut);
					
			//receber os ficheiros para actualizar	
			for (int i = 0; i < messIn.fileName.length; i++) {
				
				//saber quais os ficheiros q vai client vai mandar
				if (messOut.toBeUpdated[i] == true) {
					currFile = new File(rep.toString() + "/" + messIn.fileName[i]);
					if(!currFile.exists()){
						newFile = currFile;
						newFile.createNewFile();
					}else{
						//currFile.createNewFile();
						newFile = new File(fileRep[i] + ".temp");
						newFile.createNewFile();
					}
					//receber os ficheiros
					receiveFile(outStream, inStream, newFile);
					//saber se o ficheiro e novo nao tem versao
					if (!(versions[i] == 0)) {
						fileRep[i].renameTo(new File(rep.toString() + "/" + messIn.fileName[i] + "." + Integer.toString(versions[i])));
						newFile.renameTo(new File(rep.toString() + "/" + messIn.fileName[i]));
					}
					result = 0;
						
					
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