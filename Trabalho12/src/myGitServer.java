/***************************************************************************
*   Seguranca e Confiabilidade 2016/17
*dd
*
***************************************************************************/

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.nio.file.Path;
import java.util.Arrays;
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
						
						case "shareRep":
							shareRep(outStream, inStream, messIn, path);
							break;
							
						case "remove":
							remove(outStream, inStream, messIn, path);
							break;
							
						default:
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
					
			try {

				File users = new File(path + "/users/users.txt");

				boolean foundUser = checkUser(messIn.user[1], users);

				String res = null;

				String repName = messIn.repName;

				if (foundUser) {

					String userToAdd = messIn.user[1];

					File shareLog = new File(path + "/users/shareLog.txt");

					if (!shareLog.exists()) {

						shareLog.createNewFile();
						FileWriter fw = new FileWriter(shareLog);
						fw.write(messIn.user[0] + ":" + userToAdd + System.lineSeparator());
						res = "-- O repositório " + repName + " foi partilhado com o utilizador " + messIn.user[1];
						fw.close();

					} else {

						BufferedReader reader = new BufferedReader(new FileReader(shareLog));

						if (reader.readLine() == null) {

							FileWriter fw = new FileWriter(shareLog);
							fw.write(messIn.user[0] + ":" + userToAdd + System.lineSeparator());
							res = "-- O repositório " + repName + " foi partilhado com o utilizador " + messIn.user[1];
							fw.close();

						} else {

							File tempFile = new File(path + "/users/shareLog_temp.txt");
							tempFile.createNewFile();
							BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

							String currentLine = null;

							while ((currentLine = reader.readLine()) != null) {

								String[] split = currentLine.split(",");
								String[] split2 = split[0].split(":");

								if (split2[0].equals(messIn.user[0])) {

									for (String s : split)
										if (s.equals(userToAdd))
											res = "-- O utilizador " + userToAdd + " já tem acesso ao repositório " + repName;

									if (split2[1].equals(userToAdd))
										res = "-- O utilizador " + userToAdd + " já tem acesso ao repositório " + repName;
									else {

										writer.write(currentLine + "," + userToAdd + System.getProperty("line.separator"));
										res = "-- O repositório " + repName + " foi partilhado com o utilizador " + messIn.user[1];

									}

								} else
									writer.write(currentLine + System.getProperty("line.separator"));

							}

							writer.close(); 
							reader.close(); 
							shareLog.delete();
							tempFile.renameTo(shareLog);

						}

					}

					Message messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, messIn.toBeUpdated, messIn.user, messIn.delete, res);
					outStream.writeObject(messOut);

				} else {	

					Message messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, messIn.toBeUpdated, messIn.user, messIn.delete, "Erro: O utilizador " + messIn.user[1] + " não existe");
					outStream.writeObject(messOut);

				}

			} catch(IOException e) {

				e.printStackTrace();

			}
					
		}

		private void remove(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) {
			
			try {
				
				File users = new File(path + "/users/users.txt");
				
				boolean foundUser = checkUser(messIn.user[1], users);
				
				String res = null;
				
				String repName = messIn.repName;
				
				if (foundUser) {
					
					File shareLog = new File(path + "/users/shareLog.txt");
					
					if (!shareLog.exists()) {
						
						res = "-- O utilizador a quem quer retirar o acesso não tem acesso ao repositório " + repName; 
						Message messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, messIn.toBeUpdated, messIn.user, messIn.delete, res);
						outStream.writeObject(messOut);
					
					} else {
						
						if (checkUser(messIn.user[0], shareLog)) {
							
							File tempFile = new File(path + "/users/shareLog.temp");
							tempFile.createNewFile();
		
							BufferedReader reader = new BufferedReader(new FileReader(shareLog));
							BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
		
							String userToRemove = messIn.user[1];
							String currentLine = null;
		
							while ((currentLine = reader.readLine()) != null) {
								
								String[] split = currentLine.split(",");
							    String[] split2 = split[0].split(":");
							    
							    if (split2[0].equals(messIn.user[0])) {
							    	
							    	if (split2[1].equals(userToRemove)) {
							    		
							    		if (!(split.length == 1)) {
							    			
							    			writer.write(split2[0] + ":");
							    			writer.write(split[0]);
								    		for(int i = 1; i < split.length; i++)
								    			writer.write("," + split[i]);
								    		
							    		}
							    		
							    	} else {
							    		
							    		writer.write(split2[0] + ":" + split2[1]);
							    		
							    		for(String s : split)
							    			writer.write("," + s);
							    		
							    	}
							    	
							    } else
							    	writer.write(currentLine + System.getProperty("line.separator"));
							}
							
							writer.close(); 
							reader.close();
			    			shareLog.delete();
			    			shareLog = new File(path + "/users/shareLog.txt");
			    			shareLog.createNewFile();
							tempFile.renameTo(shareLog);
							res = "-- Foi retirado o acesso previamente dado ao utilizador " + messIn.user[1];
							
							Message messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, messIn.toBeUpdated, messIn.user, messIn.delete, res);
							outStream.writeObject(messOut);
							
						} else {
							
							res = "-- O utilizador a quem quer retirar o acesso não tem acesso ao repositório " + repName;	
						
							Message messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, messIn.toBeUpdated, messIn.user, messIn.delete, res);
							outStream.writeObject(messOut);
					
						}
						
					}
					
				}
				
			} catch(IOException e) {
				
				e.printStackTrace();
				
			}
			
		}


		private int pullRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) throws IOException {
			
			File shareLog = new File(path + "/users/shareLog.txt");
			
			BufferedReader br = new BufferedReader(new FileReader(shareLog));
			
			String currentLine = null;
			
			String[] split1 = messIn.repName.split("/");
			
			String otherUser = null;
			String repName = null;
			
			int result = -1;
			
			Message messOut = null;
			
			boolean hasAccess = false;
			
			if(split1.length > 1){
				otherUser = split1[0];
				repName = split1[1];
			} else
				repName = split1[0];
			
			if (otherUser != null) {
				
				while ((currentLine = br.readLine()) != null && !hasAccess) {
					
					String[] split = currentLine.split(",");
				    String[] split2 = split[0].split(":");
				    
				    if (split2[0].equals(otherUser)) {
				    	
				    	if (split2[1].equals(messIn.user[0]))
				    		hasAccess = true;
				    	else
				    		for(int i = 1; i < split.length && !hasAccess; i++)
				    			if(split[i].equals(messIn.user[0]))
				    				hasAccess = true;
				    }
				    	
				}
				    
			}
			
			if (hasAccess) {

				File rep  = null;
				File currFile = null;
				File[] fileRep = null;
				
				Date date = null;
				boolean[] delete = null;

				boolean[] ya = null;
				String[] names = null;
				
				String res = null;
				
				//criar path para o rep
				if (messIn.repName.contains("/")) {
					rep = new File(path + "/users/" + messIn.repName);
					res = "-- O respositório " + messIn.repName.split("/")[1] + " do utilizador " + messIn.repName.split("/")[0] + " foi copiado do servidor";
				}
				//rep do user
				else {
					rep = new File(path + "/users/" + messIn.user[0] + "/" + messIn.repName);
					res = "-- O respositório " + messIn.repName + " do utilizador " + messIn.user[0] + " foi copiado do servidor";
				}
				//erro caso o rep nao exista
				if (!rep.exists())
					return -1;
				//criar lista com todos os ficheiros
	
				else {
					//final String nameOfFile = filename;
	
		             fileRep = rep.listFiles(new FilenameFilter() {
		                @Override
		                public boolean accept(File dir, String nameA) {
		                    return nameA.matches("([.[^.\\//]]+.\\D+)|([.[^.\\//]]+.[.[^.\\//]]+.\\D+)");
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
						Arrays.fill(delete, fileRep.length, delete.length-1, Boolean.TRUE);
						
					messOut = new Message(messIn.method, names, messIn.repName, messIn.fileDate, ya, messIn.user, delete, res);
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
					messOut = new Message(messIn.method, names, messIn.repName, messIn.fileDate, ya, messIn.user, null, res);
					outStream.writeObject(messOut);
					
					for (int i = 0; i < fileRep.length; i++) 
						sendFile(outStream, inStream, fileRep[i]);
									
					result = 0;
				}
			} else {
				
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, messIn.toBeUpdated, messIn.user, null, "-- O utilizador " + messIn.user[0] + " não tem acesso ao repositório " + repName + " do utilizador " + otherUser);
				outStream.writeObject(messOut);
				result = -1;
				
			}
			
			messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, messIn.toBeUpdated, messIn.user, null, "-- O utilizador " + messIn.user[0] + " não tem acesso ao repositório " + repName + " do utilizador " + otherUser);
			outStream.writeObject(messOut);
			return result;
		}


		private int pullFile(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, Path path) throws IOException {
			
			File shareLog = new File(path + "/users/shareLog.txt");
			
			BufferedReader br = new BufferedReader(new FileReader(shareLog));
			
			String currentLine = null;
			
			String[] split1 = messIn.fileName[0].split("/");
			
			String otherUser = null;
			String repName = null;
			String filename = null;
			
			int result = -1;
			
			Message messOut = null;
			
			boolean hasAccess = true;
			
			boolean[] ya = new boolean[1];
			
			ya[0] = false;
			
			if(split1.length > 2){
				otherUser = split1[0];
				repName = split1[1];
				filename = split1[2];
			} else { 
				repName = split1[0];
				filename = split1[1];
			}
			
			if (otherUser != null) {
				
				while ((currentLine = br.readLine()) != null && !hasAccess) {
					
					String[] split = currentLine.split(",");
				    String[] split2 = split[0].split(":");
				    
				    if (split2[0].equals(otherUser)) {
				    	
				    	if (split2[1].equals(messIn.user[0]))
				    		hasAccess = true;
				    	else
				    		for(int i = 1; i < split.length && !hasAccess; i++)
				    			if(split[i].equals(messIn.user[0]))
				    				hasAccess = true;
				    }
				    	
				}
				    
			}
			
			if (hasAccess) {
			
				File file = null;
				String[] split = messIn.fileName[0].split("/");
				
				Date date = null;
				
				boolean myrep = false;
				
				//criar path para o ficheiro
				
				if (split.length == 3) 
					file = new File(path + "/users/" + messIn.fileName[0]);
					
				 else {
					file = new File(path + "/users/" + messIn.user[0] + "/" + messIn.fileName[0]);
					myrep = true;
				 }
							
				
									
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
						sendFile(outStream, inStream, file);
						result = 0;
											
					} else {
						//nao actualiza o ficheiro porque nao he mais recente
						ya[0] = false;
						messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- O ficheiro do seu repositório local é o mais recente");
						outStream.writeObject(messOut);
						result = 0;
					}	
					
				//erro o ficeiro nao existe
				} else {
					ya[0] = false;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- O ficheiro " + filename + " foi copiado do servidor");
					outStream.writeObject(messOut);
				}
				
			} else {
				
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- O utilizador " + messIn.user[0] + " não tem acesso ao ficheiro " + filename + " do utilizador " + otherUser);
				outStream.writeObject(messOut);
				result = -1;
				
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
			String filename = null;
			
			if(secondI == -1) {
				pathFolder = new File(path + "/users/" + messIn.user[0] + "/" + split[0]).toPath();
				file = new File(path + "/users/" + messIn.user[0] + "/" + messIn.fileName[0]);
				filename = split[1];
			} else {
				pathFolder = new File(path + "/users/" + messIn.user[0] + "/" + split[0] + "/" + split[1]).toPath();
				file = new File(path + "/users/" + messIn.fileName[0]);
				filename = split[2];
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
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- O ficheiro " + filename + " foi atualizado no servidor");
					outStream.writeObject(messOut);
					newFile.createNewFile();
					receiveFile(outStream, inStream, newFile);
					file.renameTo(new File(pathFolder.toString() + "/" + split[split.length-1] + "." + String.valueOf(versao)));
					newFile.renameTo(new File(pathFolder.toString() + "/" + split[split.length-1]));
					result = 0;
					
					
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
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- O ficheiro " + filename + " foi enviado para o servidor");
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
			boolean[] ya = new boolean[1];
			ya[0] = false;
			int[] versions = null;
			
			String repName = null;
			String res = null;
			
			//criar path para o rep
			if (messIn.repName.contains("/")) {
				rep = new File(path + "/users/" + messIn.repName);
				repName = messIn.repName.split("/")[1];
			}	
			else {
				rep = new File(path + "/users/" + messIn.user[0] + "/" + messIn.repName);
				repName = messIn.repName;
			}
			//criar rep caso nao exista
			if (!rep.exists()){
				rep.mkdir();
				res = "-- O repositório " + repName + " foi criado no servidor";
			}
			else {
				//final String nameOfFile = filename;

	             fileRep = rep.listFiles(new FilenameFilter() {
	                @Override
	                public boolean accept(File dir, String nameA) {
	                    return nameA.matches("([.[^.\\//]]+.\\D+)|([.[^.\\//]]+.[.[^.\\//]]+.\\D+)");
	                }
	             });
			}
			
			//caso o rep venha com ficheiros
			if (messIn.fileName != null) {
				ya = new boolean[messIn.fileName.length];
				versions = new int[messIn.fileName.length];
				
				
					for (int i = 0; i < messIn.fileName.length; i++) {
						currFile = new File(rep.toString() + "/" + messIn.fileName[i]);
						if (fileRep != null && currFile.exists()) {
							date = new Date(currFile.lastModified());
							
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
					
				res += System.lineSeparator() + "-- O ficheiro " + messIn.fileName[0] + " foi enviado para o servidor";

			}
			
			//saber quais os ficheiros a "eliminar"
			if (fileRep != null)
				for (int j = 0; j < fileRep.length; j++)
					if (!Arrays.asList(fileRep).contains(fileRep[j].getName())) {
						nameAux = fileRep[j].getName();
						fileRep[j].renameTo(new File(rep.toPath() + "/" + nameAux + "." + String.valueOf(countNumVersions1(rep.toPath(), fileRep[j].getName()))));		
						res = "-- O ficheiro myGit.java vai ser eliminado no servidor ";
					}
						
			messOut = new Message(messIn.method, null, messIn.repName, null, ya, messIn.user, null, res);
			outStream.writeObject(messOut);
					
			//receber os ficheiros para actualizar	
			if(ya[0] != false)
				for (int i = 0; i < messIn.fileName.length; i++) {
					
					//saber quais os ficheiros q vai client vai mandar
					if (messOut.toBeUpdated[i] == true) {
						currFile = new File(rep.toString() + "/" + messIn.fileName[i]);
						if(!currFile.exists()){
							newFile = currFile;
							newFile.createNewFile();
							receiveFile(outStream, inStream, newFile);
							
						}else{
							
							newFile = new File(messIn.fileName[i] + ".temp");
							newFile.createNewFile();
							receiveFile(outStream, inStream, newFile);
							fileRep[i].renameTo(new File(rep.toString() + "/" + messIn.fileName[i] + "." + Integer.toString(versions[i])));
							newFile.renameTo(new File(rep.toString() + "/" + messIn.fileName[i]));
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
					fw.write(u.name + ":" + u.pass + System.lineSeparator()); 
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