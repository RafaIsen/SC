/*****************************************
*   Seguranca e Confiabilidade 2016/17
*
*				Grupo 34
*****************************************/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

//Servidor myGitServer

public class myGitServer{

	public final String SERVER_DIR = "/users"; 
	public final String SERVER_PASS = "sc1617";
	public final String USERS_FILE = "users.txt"; 
	public final String USERS_MAC_FILE = "users_mac.txt";
	public final String SHARE_FILE = "shareLog.txt";
	public final String SHARE_MAC_FILE = "shareLog_mac.txt";
	
	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, URISyntaxException, IOException, ClassNotFoundException {
		System.out.println("servidor: main");
		myGitServer server = new myGitServer();
		server.startServer();
	}

	public void startServer () throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, URISyntaxException, IOException, ClassNotFoundException{
        
		//autenticate user
		System.out.println("Digite a password do servidor:");
		Scanner scan = new Scanner(System.in);
		if(!scan.nextLine().equals(SERVER_PASS)){
			do System.out.println("Errado! Digite novamente a password do servidor:");
			while(!scan.nextLine().equals(SERVER_PASS));
		}
		scan.close();
		System.out.println("Entrou no servidor...");
		
		//creates the directory users if it does not exist
		File usersDir = new File("bin/" + SERVER_DIR);
		if(!usersDir.exists())
			usersDir.mkdir();
		//creates the text file users if it does not exist
		File users = new File("bin/" + SERVER_DIR + "/" + USERS_FILE);
		if(!users.exists()){
			users.createNewFile();
		}
		//creates the text file shareLog if it does not exist
		File shareLog = new File("bin/" + SERVER_DIR + "/" + SHARE_FILE);
		if(!shareLog.exists()){
			shareLog.createNewFile();
		}
		
		File users_mac = new File("bin/" + SERVER_DIR + "/" + USERS_MAC_FILE);
		File share_mac = new File("bin/" + SERVER_DIR + "/" + SHARE_MAC_FILE);
		
		verifyFileIntegrity(users, users_mac, USERS_FILE);
		verifyFileIntegrity(shareLog, share_mac, SHARE_FILE);
		
		ServerSocket sSoc = null;
		
		try {
			
		    System.setProperty("javax.net.ssl.keyStore", "myServer.keyStore");
			System.setProperty("javax.net.ssl.keyStorePassword", "12345678");
		    ServerSocketFactory ssf = SSLServerSocketFactory.getDefault( );
		    sSoc = ssf.createServerSocket(23456);
		    
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
	
	public void verifyFileIntegrity(File file, File macs, String filename) throws NoSuchAlgorithmException, 
													InvalidKeyException, IOException, ClassNotFoundException{
		
		Scanner readFile = new Scanner(file);
		
		if (!macs.exists() && readFile.hasNextLine()) {
			System.out.println("AVISO!!! Não existe nenhum MAC a proteger o sistema! Será criado e adicionado um ao sistema.");
			macs.createNewFile();
		} else if (!macs.exists())
			macs.createNewFile();

		Scanner readMacs = new Scanner(macs);
			
		if (!readFile.hasNextLine()){
			readMacs.close();
			readFile.close();
		}
		else if (readMacs.hasNextLine()){
				
			//obtaining secret key through the user's pass
			byte[] bytePass = SERVER_PASS.getBytes();
			SecretKey key = new SecretKeySpec(bytePass, "HmacSHA256");
			
			//obtaining the MAC through the secret key above
			Mac m;
			byte[] mac = null;
			
			m = Mac.getInstance("HmacSHA256");
			m.init(key);
			
			while(readFile.hasNextLine())
				m.update(readFile.nextLine().getBytes());
			
			mac = m.doFinal();
			
			byte[] macOrig = readMacs.nextLine().getBytes();
			
			if (!Arrays.equals(mac, macOrig)) {
				readMacs.close();
				readFile.close();
				System.out.println("AVISO!!! O ficheiro " + filename + " foi corrompido! A desligar servidor...");
				System.exit(-1);
			}
			
			readMacs.close();
			readFile.close();
		}
		else {
			System.out.println("AVISO!!! Não existe nenhum MAC a proteger o ficheiro " + filename + "! Será criado e adicionado um MAC ao sistema.");
			createMac(file, macs);
		}
	}
	
	public void createMac(File file, File macs) throws NoSuchAlgorithmException, InvalidKeyException, 
														IOException{
		
		Scanner readFile = new Scanner(file);
		
		//opening macs file
		FileOutputStream macsOut = new FileOutputStream(macs);
		
		//obtaining secret key through the user's pass
		byte[] bytePass = SERVER_PASS.getBytes();
		SecretKey key = new SecretKeySpec(bytePass, "HmacSHA256");
		
		//obtaining the MAC through the secret key above
		Mac m;
		byte[] mac = null;
		
		m = Mac.getInstance("HmacSHA256");
		m.init(key);
		
		while(readFile.hasNextLine())
			m.update(readFile.nextLine().getBytes());
		
		mac = m.doFinal();
		
		macsOut.write(mac);
		
		readFile.close();
		macsOut.close();
	}
	
	public void updateMac(File file, File macs, String filename) throws NoSuchAlgorithmException, 
																		InvalidKeyException, IOException{
		
		Scanner readFile = new Scanner(file);
		
		File macsTemp;
		
		if (filename.equals(USERS_FILE)) 
			macsTemp = new File("bin" + SERVER_DIR + "/users_mac.temp");
		else 
			macsTemp = new File("bin" + SERVER_DIR + "/share_mac.temp");	
		
		//opening macs file
		FileOutputStream macsOut = new FileOutputStream(macsTemp);
		
		//remove antique macs file
		macs.delete();
		
		//obtaining secret key through the user's pass
		byte[] bytePass = SERVER_PASS.getBytes();
		SecretKey key = new SecretKeySpec(bytePass, "HmacSHA256");
		
		//obtaining the MAC through the secret key above
		Mac m;
		byte[] mac = null;
		
		m = Mac.getInstance("HmacSHA256");
		m.init(key);
		
		while(readFile.hasNextLine())
			m.update(readFile.nextLine().getBytes());
		
		mac = m.doFinal();
		
		macsOut.write(mac);
		
		readFile.close();
		macsOut.close();
		
		macsTemp.renameTo(macs);
	}
	
	public void ciphers(File file, String filename) throws NoSuchAlgorithmException, InvalidKeySpecException, 
	NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException{
		
		PBEKeySpec keySpec = new PBEKeySpec(SERVER_PASS.toCharArray());
		SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
		SecretKey key = kf.generateSecret(keySpec);

		// obs: o salt, count e iv não têm de ser definidos explicitamenta na cifra 
		// mas os mesmos valores têm de ser usados para cifrar e decifrar 
		// os seus valores podem ser obtidos pelo método getParameters da classe Cipher

		byte[] ivBytes = {0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,
		0x19,0x1A,0x1B,0x1C,0x1D,0x1E,0x1F,0x20};
		IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
		byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99,
				(byte) 0x52, (byte) 0x3e, (byte) 0xea, (byte) 0xf2 };
		PBEParameterSpec spec = new PBEParameterSpec(salt, 20, ivSpec);

		Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
		c.init(Cipher.ENCRYPT_MODE, key, spec);
		
		FileInputStream fis = new FileInputStream(file);
	    FileOutputStream fos = new FileOutputStream("bin/" + SERVER_DIR + "/" + filename + ".cif");
	    CipherOutputStream cos = new CipherOutputStream(fos, c);
	    
	    byte[] b = new byte[16];  
	    int i = fis.read(b);
	    while (i != -1) {
	        cos.write(b, 0, i);
	        i = fis.read(b);
	    }
	    cos.close();
	    fos.close();
	    fis.close();
	    
	    file.delete();
		
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
				
				File users = new File("bin/" + SERVER_DIR + "/" + USERS_FILE);
				
				File users_mac = new File("bin/" + SERVER_DIR + "/" + USERS_MAC_FILE);
				File share_mac = new File("bin/" + SERVER_DIR + "/" + SHARE_MAC_FILE);

				boolean foundU = checkUser(username, users, users_mac, USERS_FILE);
				
				outStream.writeObject(foundU);
				
				String pass = null;
								
				if (!foundU) { //create user
					pass = (String) inStream.readObject();
					User newUser = new User(username, pass);
					outStream.writeObject(createUser(newUser, users, users_mac));
				} else { //receive/confirm password
					boolean autentic = false;
					while(!autentic){
						pass = (String) inStream.readObject();
						User user = new User(username, pass);
						autentic = autenticate(user, users, users_mac);
						outStream.writeObject(autentic);
					}
				}

				//verifies if it has any methods in args
				if ((param_p && num_args > 4) || (!param_p && num_args > 2)) {
						
					Message messIn = (Message) inStream.readObject();
										
					switch (messIn.method) {
					
						case "pushFile":
							pushFile(outStream, inStream, messIn);
							break;
							
						case "pushRep":
							pushRep(outStream, inStream, messIn);
							break;
							
						case "pullFile":
							pullFile(outStream, inStream, messIn, share_mac);
							break;
							
						case "pullRep":
							pullRep(outStream, inStream, messIn, share_mac);
							break;
						
						case "shareRep":
							shareRep(outStream, inStream, messIn, users, users_mac, share_mac);
							break;
							
						case "remove":
							remove(outStream, inStream, messIn, users, users_mac, share_mac);
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
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
			
		private int pushFile(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn) throws IOException {
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
				pathFolder = new File("bin/" + SERVER_DIR + "/" + messIn.user[0] + "/" + split[0]).toPath();
				file = new File("bin/" + SERVER_DIR + "/" + messIn.user[0] + "/" + messIn.fileName[0]);
				filename = split[1];
			} else {
				pathFolder = new File("bin/" + SERVER_DIR + "/" + split[0] + "/" + split[1]).toPath();
				file = new File("bin/" + SERVER_DIR + "/" + messIn.fileName[0]);
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

		private int pushRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn) throws IOException {
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
			
			String repName = null;
			String res = null;
			
			boolean eliminou = false;
			
			//criar path para o rep
			if (messIn.repName.contains("/")) {
				rep = new File("bin/" + SERVER_DIR + "/" + messIn.repName);
				repName = messIn.repName.split("/")[1];
			}	
			else {
				rep = new File("bin/" + SERVER_DIR + "/" + messIn.user[0] + "/" + messIn.repName);
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
					if (!Arrays.asList(messIn.repName).contains(fileRep[j].getName())) {
						nameAux = fileRep[j].getName();
						fileRep[j].renameTo(new File(rep.toPath() + "/" + nameAux + "." + String.valueOf(countNumVersions1(rep.toPath(), fileRep[j].getName()))));		
						res = "-- O ficheiro " + fileRep[j].getName() + " vai ser eliminado no servidor";
						eliminou = true;
					}
						
			messOut = new Message(messIn.method, null, messIn.repName, null, ya, messIn.user, null, res);
			outStream.writeObject(messOut);
					
			//receber os ficheiros para actualizar
			if(messIn.fileName != null && !eliminou)
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

		private int pullFile(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, File share_mac) throws IOException, InvalidKeyException, NoSuchAlgorithmException, ClassNotFoundException {
			
			File shareLog = new File("bin/" + SERVER_DIR + "/shareLog.txt");
			
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
				
				verifyFileIntegrity(shareLog, share_mac, SHARE_FILE);
				
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
					file = new File("bin/" + SERVER_DIR + "/" + messIn.fileName[0]);
					
				 else {
					file = new File("bin/" + SERVER_DIR + "/" + messIn.user[0] + "/" + messIn.fileName[0]);
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
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- O ficheiro " + filename + " não existe");
					outStream.writeObject(messOut);
				}
				
			} else {
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, ya, messIn.user, null, "-- O utilizador " + messIn.user[0] + " não tem acesso ao ficheiro " + filename + " do utilizador " + otherUser);
				outStream.writeObject(messOut);
				result = -1;
			}
			br.close();
			return result;
		}

		private int pullRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, File share_mac) throws IOException, InvalidKeyException, NoSuchAlgorithmException, ClassNotFoundException {
			
			File shareLog = new File("bin/" + SERVER_DIR + "/shareLog.txt");
			
			BufferedReader br = new BufferedReader(new FileReader(shareLog));
			
			String currentLine = null;
			
			String[] split1 = messIn.repName.split("/");
			
			String otherUser = null;
			String repName = null;
			
			int result = -1;
			
			Message messOut = null;
			
			boolean hasAccess = false;
			
			boolean[] delete = null;
			
			if(split1.length > 1){
				otherUser = split1[0];
				repName = split1[1];
			} else
				repName = split1[0];
			
			if (otherUser != null) {
				
				verifyFileIntegrity(shareLog, share_mac, SHARE_FILE);
				
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
		
				boolean[] ya = null;
				String[] names = null;
				
				String res = null;
				
				//criar path para o rep
				if (messIn.repName.contains("/")) {
					rep = new File("bin/" + SERVER_DIR + "/" + messIn.repName);
					res = "-- O respositório " + messIn.repName.split("/")[1] + " do utilizador " + messIn.repName.split("/")[0] + " foi copiado do servidor";
				}
				//rep do user
				else {
					rep = new File("bin/" + SERVER_DIR + "/" + messIn.user[0] + "/" + messIn.repName);
					res = "-- O respositório " + messIn.repName + " do utilizador " + messIn.user[0] + " foi copiado do servidor";
				}
				//erro caso o rep nao exista
				if (!rep.exists()) {
				    br.close();
					return -1;
				}
				//criar lista com todos os ficheiros
				else {
					//final String nameOfFile = filename;
		
		             fileRep = rep.listFiles(new FilenameFilter() {
		                @Override
		                public boolean accept(File dir, String nameA) {
		                    return nameA.matches("([.[^.\\//]]+.\\D+)|([.[^.\\//]]+.[.[^.\\//]]+.\\D+)");
		                	//return !nameA.matches("(\\w*.\\w+.\\d)|(\\w+.\\d)");
		                }
		             });
				}
				//so para actualizar os ficheiros
				if (fileRep.length > 0) {
					ya = new boolean[fileRep.length];
					names = new String[fileRep.length];
					if(messIn.fileName != null)
					if (fileRep.length > messIn.fileName.length)
						delete = new boolean[fileRep.length];
					else
						delete = new boolean[messIn.fileName.length];
					//
					for (int i = 0; i < fileRep.length; i++) {
						if(messIn.fileName != null)
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
					if(messIn.fileName != null)
					if (messIn.fileName.length > fileRep.length)
						Arrays.fill(delete, fileRep.length, delete.length-1, Boolean.TRUE);
						
					messOut = new Message(messIn.method, names, messIn.repName, messIn.fileDate, ya, messIn.user, delete, res);
					outStream.writeObject(messOut);
					
					for (int i = 0; i < ya.length; i++) {
						if (ya[i] == true)
							sendFile(outStream, inStream, new File(rep.toString() + "/" + names[i]));
					}
				
				result = 0;
				
				} else if(rep.listFiles() != null){
					delete = new boolean[1];
					delete[0] = true;
					res = "-- O ficheiro " + messIn.fileName[0] + " existe localmente mas foi eliminado no servidor ";
				}
				//para inicializar o rep
				else {
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
				br.close();
				return -1;
			} 
			messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, messIn.toBeUpdated, messIn.user, delete, "-- O utilizador " + messIn.user[0] + " não tem acesso ao repositório " + repName + " do utilizador " + otherUser);
			outStream.writeObject(messOut);
			br.close();
			return result;
		}

		private void shareRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, File users, File users_mac, File share_mac) throws InvalidKeyException, NoSuchAlgorithmException, ClassNotFoundException {
					
			try {

				boolean foundUser = checkUser(messIn.user[1], users, users_mac, USERS_FILE);

				String res = null;

				String repName = messIn.repName;

				if (foundUser) {

					String userToAdd = messIn.user[1];

					File shareLog = new File("bin/" + SERVER_DIR + "/shareLog.txt");
					
					verifyFileIntegrity(shareLog, share_mac, SHARE_FILE);

					BufferedReader reader = new BufferedReader(new FileReader(shareLog));

					String currLine = reader.readLine();
					
					if (currLine == null) {
						FileWriter fw = new FileWriter(shareLog);
						fw.write(messIn.user[0] + ":" + userToAdd + System.lineSeparator());
						res = "-- O repositório " + repName + " foi partilhado com o utilizador " + messIn.user[1];
						fw.close();
					} else {
						File tempFile = new File("bin/" + SERVER_DIR + "/shareLog.temp");
						tempFile.createNewFile();
						BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
						do {
							String[] split = currLine.split(",");
							String[] split2 = split[0].split(":");

							if (split2[0].equals(messIn.user[0])) {

								for (String s : split)
									if (s.equals(userToAdd))
										res = "-- O utilizador " + userToAdd + " já tem acesso ao repositório " + repName;

								if (split2[1].equals(userToAdd))
									res = "-- O utilizador " + userToAdd + " já tem acesso ao repositório " + repName;
								else {

									writer.write(currLine + "," + userToAdd + System.getProperty("line.separator"));
									res = "-- O repositório " + repName + " foi partilhado com o utilizador " + messIn.user[1];

								}

							} else
								writer.write(currLine + System.getProperty("line.separator"));

						} while ((currLine = reader.readLine()) != null);
						
						//writer.write(messIn.user[0] + ":" + userToAdd);
 
						writer.close(); 
						reader.close();
		    			shareLog.delete();
						tempFile.renameTo(shareLog);
					}
					updateMac(shareLog, share_mac, SHARE_FILE);
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

		private void remove(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, File users, File users_mac, File share_mac) throws InvalidKeyException, NoSuchAlgorithmException, ClassNotFoundException {
			
			try {
				
				boolean foundUser = checkUser(messIn.user[1], users, users_mac, USERS_FILE);
				
				String res = null;
				
				String repName = messIn.repName;
				
				if (foundUser) {
					
					File shareLog = new File("bin/" + SERVER_DIR + "/shareLog.txt");
						
					if (checkUser(messIn.user[0], shareLog, share_mac, SHARE_FILE)) {
						
						File tempFile = new File("bin/" + SERVER_DIR + "/shareLog.temp");
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
						tempFile.renameTo(shareLog);
						
						updateMac(shareLog, share_mac, SHARE_FILE);
						
						res = "-- Foi retirado o acesso previamente dado ao utilizador " + messIn.user[1];
						
						Message messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, messIn.toBeUpdated, messIn.user, messIn.delete, res);
						outStream.writeObject(messOut);
					} else {
						res = "-- O utilizador a quem quer retirar o acesso não tem acesso ao repositório " + repName;	
					
						Message messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, messIn.toBeUpdated, messIn.user, messIn.delete, res);
						outStream.writeObject(messOut);
					}
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
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
		
		public boolean autenticate(User u, File f, File m) throws IOException, InvalidKeyException, NoSuchAlgorithmException, ClassNotFoundException{
			
			boolean autenticado = false;
			
			verifyFileIntegrity(f, m, USERS_FILE);
	
			Scanner scan = new Scanner(f);
			
			while (scan.hasNextLine()) {
				String[] split = scan.nextLine().split(":");
				if(split[0].equals(u.name) && split[1].equals(u.pass))
					autenticado = true; 
			}
			
			scan.close();
			
			return autenticado;
		}
		
		public boolean createUser(User u, File f, File m) throws IOException, InvalidKeyException, NoSuchAlgorithmException, ClassNotFoundException{
			
			boolean result = false;
			FileWriter fw = new FileWriter(f, true);
			Scanner scan = new Scanner(f);
			boolean empty = false;
			
			if(!scan.hasNext())
				empty = true;
			
			try {
				if(checkUser(u.name, f, m, USERS_FILE))
					result = true;
				else{
					//writes the name and pass in the file
					fw.write(u.name + ":" + u.pass + System.lineSeparator()); 
				    fw.flush();
				    fw.close();
					//creates a directory to the user
					new File("bin/" + SERVER_DIR + "/" + u.name).mkdir();
					result = true;				
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(empty)
				createMac(f, m);
			else
				updateMac(f, m, USERS_FILE);
			
			scan.close();
			return result;
		}
		
		public boolean checkUser(String username, File f, File m, String filename) throws InvalidKeyException, NoSuchAlgorithmException, ClassNotFoundException, IOException {
			
			verifyFileIntegrity(f, m, filename);
			
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