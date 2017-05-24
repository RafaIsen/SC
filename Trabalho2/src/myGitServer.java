/*****************************************
*   Seguranca e Confiabilidade 2016/17
*
*			myGitServer.java
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
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
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

	public final String SERVER_DIR = "server"; 
	public final String USERS_DIR = "users"; 
	public final String SERVER_PASS = "sc1617";
	public final String USERS_FILE = "users.txt"; 
	public final String USERS_FILE_CIF = "users.cif"; 
	public final String USERS_FILE_NAME = "users"; 
	public final String USERS_MAC_FILE = "users_mac.txt";
	public final String SHARE_FILE = "shareLog.txt";
	public final String SHARE_MAC_FILE = "shareLog_mac.txt";
	
	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, 
	NoSuchPaddingException, IOException, ClassNotFoundException, 
	InvalidKeySpecException, InvalidAlgorithmParameterException {
		System.out.println("servidor: main");
		myGitServer server = new myGitServer();
		server.startServer();
	}

	public void startServer () throws InvalidKeyException, NoSuchAlgorithmException, 
	NoSuchPaddingException, IOException, ClassNotFoundException, 
	InvalidKeySpecException, InvalidAlgorithmParameterException{
        
		//authenticate user
		System.out.println("Digite a password do servidor:");
		Scanner scan = new Scanner(System.in);
		if(!scan.nextLine().equals(SERVER_PASS)){
			do System.out.println("Errado! Digite novamente a password do servidor:");
			while(!scan.nextLine().equals(SERVER_PASS));
		}
		scan.close();
		System.out.println("Entrou no servidor...");
		
		//creates the directory users if it does not exist
		File usersDir = new File(SERVER_DIR + "/" + USERS_DIR);
		if(!usersDir.exists())
			usersDir.mkdir();
		//creates the text file users if it does not exist
		File users = new File(SERVER_DIR + "/" + USERS_DIR + "/" + USERS_FILE_CIF);
		if (!users.exists()) {
			users = new File(SERVER_DIR + "/" + USERS_DIR + "/" + USERS_FILE);
			users.createNewFile();
		}
		else
			users = decipher(users, USERS_FILE_NAME);
		//creates the text file shareLog if it does not exist
		File shareLog = new File(SERVER_DIR + "/" + USERS_DIR + "/" + SHARE_FILE);
		if(!shareLog.exists())
			shareLog.createNewFile();
		
		File users_mac = new File(SERVER_DIR + "/" + USERS_DIR + "/" + USERS_MAC_FILE);
		File share_mac = new File(SERVER_DIR + "/" + USERS_DIR + "/" + SHARE_MAC_FILE);
		
		verifyFileIntegrity(users, users_mac, USERS_FILE);
		
		cipher(users, USERS_FILE_NAME);
		
		verifyFileIntegrity(shareLog, share_mac, SHARE_FILE);
		
		ServerSocket sSoc = null;
		
		try {
			
		    System.setProperty("javax.net.ssl.keyStore", SERVER_DIR + "/myServer.keyStore");
			System.setProperty("javax.net.ssl.keyStorePassword", SERVER_PASS);
			System.setProperty("javax.net.ssl.trustStore", SERVER_DIR + "/myServer.keyStore");
			System.setProperty("javax.net.ssl.trustStorePassword", SERVER_PASS);
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
	
	public final class SessionIdentifierGenerator {
		  private SecureRandom random = new SecureRandom();

		  public int nextSessionId() {
		    return new BigInteger(130, random).intValue();
		  }
		}
	
	public void verifyFileIntegrity(File file, File macs, String filename) throws IOException, 
	NoSuchAlgorithmException, InvalidKeyException {
		
		Scanner readFile = new Scanner(new BufferedReader(new FileReader(file)));
		
		if (!macs.exists() && readFile.hasNextLine()) {
			System.out.println("AVISO!!! Não existe nenhum MAC a proteger o sistema! Será criado e adicionado um ao sistema.");
			macs.createNewFile();
		} else if (!macs.exists())
			macs.createNewFile();

		BufferedReader readMacs = new BufferedReader(new FileReader(macs));

		if (!readFile.hasNextLine() && !readMacs.ready()){
			readMacs.close();
			readFile.close();
		}
		else if (readMacs.ready()){
				
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
			
			byte[] macOrig = Files.readAllBytes(macs.toPath());
			
			if (!Arrays.equals(mac, macOrig)) {
				readMacs.close();
				readFile.close();
				System.out.println("AVISO!!! O ficheiro " + filename + " foi corrompido!" 
				+ System.lineSeparator() + "A desligar servidor...");
				System.exit(-1);
			}
			
			readMacs.close();
			readFile.close();
		}
		else {
			System.out.println("AVISO!!! Não existe nenhum MAC a proteger o ficheiro " + filename + "!");
			System.out.println("Escolha uma das opções: Terminar (n) ou Calcular MAC "
					+ "e adiciona-lo ao sistema (y)");
			
			Scanner sc = new Scanner(System.in);
			   String resposta = null;
			   while (sc.hasNextLine()) {
			    resposta = sc.nextLine();
			    if (resposta.equals("y") || resposta.equals("Y")) {
			    	System.out.println("A calcular MAC e adiciona-lo ao sistema");
			    	createMac(file, macs);
			    } else {
			    	System.out.println("A terminar o servidor...");
			    	System.exit(-1);
			    }
			   }
			   sc.close();
		}
	}
	
	public void createMac(File file, File macs) throws NoSuchAlgorithmException, 
	InvalidKeyException, IOException{
		
		Scanner readFile = new Scanner(new BufferedReader(new FileReader(file))); 
		
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
		
		Scanner readFile = new Scanner(new BufferedReader(new FileReader(file))); 
		
		File macsTemp;
		
		if (filename.equals(USERS_FILE)) 
			macsTemp = new File(SERVER_DIR + "/" + USERS_DIR + "/users_mac.temp");
		else 
			macsTemp = new File(SERVER_DIR + "/" + USERS_DIR + "/share_mac.temp");	
		
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
	
	public File cipher(File file, String filename) throws NoSuchAlgorithmException, 
	InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, 
	InvalidAlgorithmParameterException, IOException{
		
		FileInputStream fis = new FileInputStream(file);
		byte[] b = new byte[16]; 
		int i = fis.read(b);
		
		if (i != -1) {
		
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
			
		    FileOutputStream fos = new FileOutputStream(SERVER_DIR + "/" + USERS_DIR + "/" + filename + ".cif");
		    CipherOutputStream cos = new CipherOutputStream(fos, c);
		     
		    while (i != -1) {
		        cos.write(b, 0, i);
		        i = fis.read(b);
		    }
		    cos.close();
		    fos.close();
		    fis.close();
		    
		    file.delete();
		    
		    return new File(SERVER_DIR + "/" + USERS_DIR + "/" + filename + ".cif");
		    
		} else {
			File f = new File(SERVER_DIR + "/" + USERS_DIR + "/" + filename + ".cif");
			f.createNewFile();
			fis.close();
			file.delete();
			return f;
		}
	}
	
	public File decipher(File file, String filename) throws NoSuchAlgorithmException, 
	InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, 
	InvalidAlgorithmParameterException, IOException{
		
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
		c.init(Cipher.DECRYPT_MODE, key, spec);
		
		FileInputStream fis = new FileInputStream(file);
	    FileOutputStream fos = new FileOutputStream(SERVER_DIR + "/" + USERS_DIR + "/" + filename + ".txt");
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
	    
	    return new File(SERVER_DIR + "/" + USERS_DIR + "/" + filename + ".txt");
		
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
			private Object pass;
			
			public User(String name, Object pass){
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
				
				File users = new File(SERVER_DIR + "/" + USERS_DIR + "/" + USERS_FILE_CIF);
				
				File users_mac = new File(SERVER_DIR + "/" + USERS_DIR + "/" + USERS_MAC_FILE);
				File share_mac = new File(SERVER_DIR + "/" + USERS_DIR + "/" + SHARE_MAC_FILE);

				boolean foundU = checkUser(username, users, users_mac, USERS_FILE);
				
				outStream.writeObject(foundU);
				
				Object pass = null;
								
				if (!foundU) { 
					
					//create user
					pass = inStream.readObject();
					User newUser = new User(username, pass);
					outStream.writeObject(createUser(newUser, users, users_mac));
					
				} else {
					
					//generating nonce
					SessionIdentifierGenerator sig = new SessionIdentifierGenerator();
					int nonce = sig.nextSessionId();
					
					//sending nonce to client
					outStream.writeObject(nonce);
					
					//receive/confirm password
					boolean authentic = false;
					while(!authentic){
						pass = inStream.readObject();
						User user = new User(username, pass);
						authentic = authenticate(user, users, users_mac, nonce);
						outStream.writeObject(authentic);
					}
				}

				//verifies if it has any methods in args
				if ((param_p && num_args > 4) || (!param_p && num_args > 2)) {
						
					Message messIn = (Message) inStream.readObject();
										
					switch (messIn.method) {
					
						case "pushFile":
							pushFile(outStream, inStream, messIn, share_mac);
							break;
							
						case "pushRep":
							pushRep(outStream, inStream, messIn, share_mac);
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
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				e.printStackTrace();
			} catch (UnrecoverableKeyException e) {
				e.printStackTrace();
			} catch (KeyStoreException e) {
				e.printStackTrace();
			} catch (CertificateException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			}
		}
			
		private int pushFile(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, 
				File share_mac) 
				throws IOException, ClassNotFoundException, UnrecoverableKeyException, KeyStoreException, 
				NoSuchAlgorithmException, CertificateException, NoSuchPaddingException, InvalidKeyException, 
				IllegalBlockSizeException, BadPaddingException {
			
			int result = -1;
		
			StringBuilder sb = new StringBuilder();
			sb.append(messIn.fileName[0]);
			int firstI = sb.indexOf("/");
			int secondI = sb.indexOf("/", firstI+1);
			String[] split = messIn.fileName[0].split("/");
			
			File file = null;
			File cifFile = null;
			String cifFilename = null;
			Path pathFolder = null;
			String filename = null;
			String filePath = null;
			String user = null;
			File rep = new File(SERVER_DIR + "/" + USERS_DIR + "/" + messIn.user[0] + "/" + split[0]);
			
			//criar rep caso nao exista
			if (!rep.exists()){
				rep.mkdir();
				System.out.println("-- O repositório " + split[0] + " foi criado no servidor");
			}
			
			if(secondI == -1) {
				pathFolder = new File(SERVER_DIR + "/" + USERS_DIR + "/" + messIn.user[0] + "/" + split[0]).toPath();
				file = new File(SERVER_DIR + "/" + USERS_DIR + "/" + messIn.user[0] + "/" + messIn.fileName[0]);
				filename = split[1];
				String[] splitName = filename.split("\\.");
				filePath = SERVER_DIR + "/" + USERS_DIR + "/" + messIn.user[0] + "/" + split[0] + "/";
				cifFilename = splitName[0] + ".cif";
				cifFile = new File(filePath + cifFilename);
			} else {
				user = split[0];
				pathFolder = new File(SERVER_DIR + "/" + USERS_DIR + "/" + split[0] + "/" + split[1]).toPath();
				file = new File(SERVER_DIR + "/" + USERS_DIR + "/" + messIn.fileName[0]);
				filename = split[2];
				String[] splitName = filename.split("\\.");
				filePath = SERVER_DIR + "/" + USERS_DIR + "/" + split[0] + "/" + split[1] + "/";
				cifFilename = splitName[0] + ".cif";
				cifFile = new File(filePath + cifFilename);
			}
			
			boolean hasAccess = userHasAccess(user, messIn, share_mac);
			
			File newFile = null;
		
			Date date = null;
			boolean exists = cifFile.exists();
			
			Message messOut = null;
			boolean[] toUpdate = new boolean[1];
			
			int versao = 0;
				
			if (hasAccess) {
			
				if (exists) {
					//actualiza o ficheiro para uma versao mais recente
					date = new Date(cifFile.lastModified());
					
					if (date.before(messIn.fileDate[0])) {
	
						versao = countNumVersions1(pathFolder, cifFilename);
						newFile = new File(file + ".temp");
						
						toUpdate[0] = true;
						messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, toUpdate, messIn.user, null, "-- O ficheiro " + filename + " foi atualizado no servidor");
						outStream.writeObject(messOut);
						newFile.createNewFile();
						receiveSecureFile(outStream, inStream, newFile, filename, filePath);
						cifFile.renameTo(new File(pathFolder.toString() + "/" + cifFilename + "." + String.valueOf(versao)));
						newFile.renameTo(new File(pathFolder.toString() + "/" + cifFilename));
						result = 0;
						
					} else {
						//nao actualiza o ficheiro porque nao he mais recente
						toUpdate[0] = false;
						messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, toUpdate, messIn.user, null, "-- Nada a atualizar no servidor");
						outStream.writeObject(messOut);
						result = 0;
					}	
					
				//cria o ficheiro porque ainda existe
				} else {
					toUpdate[0] = true;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, toUpdate, messIn.user, null, "-- O ficheiro " + filename + " foi enviado para o servidor");
					outStream.writeObject(messOut);
					cifFile.createNewFile();
					receiveSecureFile(outStream, inStream, cifFile, filename, filePath);
					result = 0;
										
				}
			} else {
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, 
						toUpdate, messIn.user, null, "-- O utilizador " + messIn.user[0] + 
						" não tem acesso ao ficheiro " + filename + " do utilizador " + user);
				outStream.writeObject(messOut);
				result = -1;
			}
			return result;			
		}

		private int pushRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn,
				File share_mac) 
				throws IOException, ClassNotFoundException, UnrecoverableKeyException, InvalidKeyException,
				KeyStoreException, NoSuchAlgorithmException, CertificateException, NoSuchPaddingException, 
				IllegalBlockSizeException, BadPaddingException {
			int result = -1;
			File rep  = null;
			File newFile = null;
			File currFile = null;
			File[] fileRep = null;
			Date date = null;
			
			Message messOut = null;
			boolean[] toUpdate = null;
			int[] versions = null;
			
			String repName = null;
			String res = "";
			
			String[] split = messIn.repName.split("/"); 
			String filename = null;
			String user = null;
			
			boolean eliminou = false;
			
			//criar path para o rep
			if (messIn.repName.contains("/")) {
				rep = new File(SERVER_DIR + "/" + USERS_DIR + "/" + messIn.repName);
				user = split[0];
				repName = split[1];
			}	
			else {
				rep = new File(SERVER_DIR + "/" + USERS_DIR + "/" + messIn.user[0] + "/" + messIn.repName);
				repName = messIn.repName;
			}
			
			boolean hasAccess = userHasAccess(user, messIn, share_mac);
			
			if (hasAccess) {
				
				//criar rep caso nao exista
				if (!rep.exists()){
					rep.mkdir();
					res = "-- O repositório " + repName + " foi criado no servidor";
				}
				else {
			         fileRep = rep.listFiles(new FilenameFilter() {
			            @Override
			            public boolean accept(File dir, String nameA) {
			                return nameA.matches("([.[^.\\//]]+.\\D+)|([.[^.\\//]]+.[.[^.\\//]]+.\\D+)");
			            }
			         });
				}
				
				//number of cif files
				int numFiles = 0;
				
				//file rep with cif files
				ArrayList<File> newFileRep = new ArrayList<File>();
				
				if(fileRep != null)
					for (int i = 0; i < fileRep.length; i++)
						if (fileRep[i].getName().contains(".cif")) {
							numFiles++;
							newFileRep.add(fileRep[i]);
						}
				
				//file rep with client files
				ArrayList<String> clientFileRep = new ArrayList<String>(messIn.fileName.length);
				
				for (int i = 0; i < messIn.fileName.length; i++)
					clientFileRep.add(messIn.fileName[i]);
				
				//file rep with key server files
				ArrayList<File> keyFileRep = new ArrayList<File>(numFiles);
				
				String cifFileName;
				String keyFileName;
				String keyExt;
				String[] keySplit;
				
				//get key files
				if(fileRep != null)
					for (int j = 0; j < fileRep.length; j++) {
						cifFileName = fileRep[j].getName().split("\\.")[0];
						keySplit = fileRep[j].getName().split("\\.");
						keyFileName = keySplit[0];
						if(keySplit.length > 2) {
							keyExt = fileRep[j].getName().split("\\.")[2];
							if(cifFileName.equals(keyFileName) && keyExt.equals("key"))
								keyFileRep.add(fileRep[j]);
						}
					}
					
				//caso o rep venha com ficheiros
				if (messIn.fileName != null) {
					toUpdate = new boolean[messIn.fileName.length];
					versions = new int[messIn.fileName.length];
					
					for (int i = 0; i < messIn.fileName.length; i++) {
						split = messIn.fileName[i].split("\\.");
						filename = split[0] + ".cif";
						currFile = new File(rep.toString() + "/" + filename);
						//verificar quais os ficheiros a enviar e que mensagem escrita enviar
						if (fileRep != null && currFile.exists()) {
							date = new Date(currFile.lastModified());
							//verificar quais os ficheiros que precisam de ser actualizados
							if (date.before(messIn.fileDate[i])) {
								versions[i] = countNumVersions1(rep.toPath(), filename);
								toUpdate[i] = true;
								res += "-- O ficheiro " + messIn.fileName[i] + " foi atualizado no servidor" + System.lineSeparator();
							} else
								res += "-- O ficheiro " + messIn.fileName[i] + " já está atualizado no servidor" + System.lineSeparator();
						} else {
							toUpdate[i] = true;
							versions[i] = 0;
							res += "-- O ficheiro " + messIn.fileName[i] + " foi enviado para o servidor" + System.lineSeparator();
						}
					}
				}
				
				//saber quais os ficheiros a "eliminar"
				if (fileRep != null && toUpdate == null) {
					for (int i = 0; i < numFiles; i++){
						split = keyFileRep.get(i).getName().split("\\.");
						String fileExt = split[1];
						split = newFileRep.get(i).getName().split("\\.");
						filename = split[0] + ".cif";
						if (!Arrays.asList(messIn.repName).contains(newFileRep.get(i))) {
							newFileRep.get(i).renameTo(new File(newFileRep.get(i) + "." 
						+ String.valueOf(countNumVersions1(rep.toPath(), newFileRep.get(i).getName()))));		
							res += "-- O ficheiro " + split[0] + "." + fileExt 
									+ " vai ser eliminado no servidor" + System.lineSeparator();
							eliminou = true;
						}
					}
				}
							
				messOut = new Message(messIn.method, null, messIn.repName, null, toUpdate, messIn.user, null, res);
				outStream.writeObject(messOut);
						
				//receber os ficheiros para actualizar
				if(messIn.fileName != null && !eliminou)
					for (int i = 0; i < messIn.fileName.length; i++) {
						//saber quais os ficheiros q vai client vai mandar
						if (messOut.toBeUpdated[i] == true) {
							
							split = messIn.fileName[i].split("\\.");
							filename = split[0] + ".cif";
							currFile = new File(rep.toString() + "/" + filename);
							
							if (!currFile.exists()) {
								newFile = new File(rep.toString() + "/" + filename);
								newFile.createNewFile();
								receiveSecureFile(outStream, inStream, newFile, messIn.fileName[i], rep.toString() + "/");
							} else {
								newFile = new File(rep.toString() + "/" + messIn.fileName[i] + ".temp");
								newFile.createNewFile();
								receiveSecureFile(outStream, inStream, newFile, messIn.fileName[i], rep.toString() + "/");
								newFileRep.get(i).renameTo(new File(rep.toString() + "/" + filename + "." + Integer.toString(versions[i])));
								newFile.renameTo(new File(rep.toString() + "/" + filename));
							}
						}
					}
			} else {
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, 
						messIn.toBeUpdated, messIn.user, null, "-- O utilizador " + messIn.user[0] 
								+ " não tem acesso ao repositório " + repName + " do utilizador " + user);
				outStream.writeObject(messOut);
				result = -1;
			} 
			return result;
		}

		private int pullFile(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, 
				File share_mac) throws IOException, InvalidKeyException, NoSuchAlgorithmException, 
		ClassNotFoundException, UnrecoverableKeyException, KeyStoreException, CertificateException,
		NoSuchPaddingException {
			
			String[] split = messIn.fileName[0].split("/");
			
			String user = null;
			String repName = null;
			String filename = null;
			String name = null;
			String filePath = null;
			
			int result = -1;
			
			Message messOut = null;
			
			boolean hasAccess = true;
			
			boolean[] toUpdate = new boolean[1];
			
			toUpdate[0] = false;
			
			if(split.length > 2){
				user = split[0];
				repName = split[1];
				filename = split[2];
				name = filename.split("\\.")[0];
			} else { 
				repName = split[0];
				filename = split[1];
				name = filename.split("\\.")[0];	
			}
			
			hasAccess = userHasAccess(user, messIn, share_mac);
			
			if (hasAccess) {
			
				File file = null;
				
				Date date = null;
				
				boolean myrep = false;
				
				File keyFile;
				
				//criar path para o ficheiro
				if (split.length == 3) {
					filePath = SERVER_DIR + "/" + USERS_DIR + "/" + user + "/" + repName + "/";
					file = new File(filePath + name + ".cif");
					keyFile = new File(filePath + filename + ".key.server");
				}
				else {
					filePath = SERVER_DIR + "/" + USERS_DIR + "/" + messIn.user[0] + "/" + repName + "/";
					file = new File(filePath + name + ".cif");
					keyFile = new File(filePath + filename + ".key.server");
					myrep = true;
				}
				
				if (file.exists() && keyFile.exists()) {
					//actualiza o ficheiro para uma versao mais recente
					date = new Date(file.lastModified());
					
					if (date.compareTo(messIn.fileDate[0]) > 0) {
						toUpdate[0] = true;
						if(myrep)
							messOut = new Message(messIn.method, messIn.fileName, messIn.repName, 
									messIn.fileDate, toUpdate, messIn.user, null, "-- O ficheiro " + filename 
									+ " foi copiado do servidor");
						else
							messOut = new Message(messIn.method, messIn.fileName, messIn.repName, 
									messIn.fileDate, toUpdate, messIn.user, null, "-- O ficheiro " 
							+ filename + " do utilizador " + user + " foi copiado do servidor");
						outStream.writeObject(messOut);
						sendSecureFile(outStream, inStream, file, filename, filePath);
						result = 0;				
					} else {
						//nao actualiza o ficheiro porque nao he mais recente
						toUpdate[0] = false;
						messOut = new Message(messIn.method, messIn.fileName, messIn.repName, 
								messIn.fileDate, toUpdate, messIn.user, null, 
								"-- O ficheiro do seu repositório local é o mais recente");
						outStream.writeObject(messOut);
						result = 0;
					}	
				//erro o ficeiro nao existe
				} else {
					toUpdate[0] = false;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, 
							toUpdate, messIn.user, null, "-- O ficheiro " + filename + " não existe no servidor");
					outStream.writeObject(messOut);
				}
				
			} else {
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, 
						toUpdate, messIn.user, null, "-- O utilizador " + messIn.user[0] + 
						" não tem acesso ao ficheiro " + filename + " do utilizador " + user);
				outStream.writeObject(messOut);
				result = -1;
			}
			return result;
		}

		private int pullRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, 
				File share_mac) throws IOException, InvalidKeyException, NoSuchAlgorithmException, 
		ClassNotFoundException, UnrecoverableKeyException, KeyStoreException, CertificateException, 
		NoSuchPaddingException {
			
			String[] split1 = messIn.repName.split("/");
			
			String user = null;
			String repName = null;
			
			int result = -1;
			
			Message messOut = null;
			
			boolean hasAccess = false;
			
			boolean[] delete = null;
			
			if(split1.length > 1){
				user = split1[0];
				repName = split1[1];
			} else
				repName = split1[0];
			
			hasAccess = userHasAccess(user, messIn, share_mac);
			
			if (hasAccess) {
		
				File rep  = null;
				File currFile = null;
				File[] fileRep = null;
				
				Date date = null;
		
				boolean[] toUpdate = null;
				String[] names = null;
				
				String res = null;
				
				//criar path para o rep
				if (messIn.repName.contains("/")) {
					rep = new File(SERVER_DIR + "/" + USERS_DIR + "/" + messIn.repName);
					res = "-- O respositório " + messIn.repName.split("/")[1] + " do utilizador " 
					+ messIn.repName.split("/")[0] + " foi copiado do servidor";
				}
				//rep do user
				else {
					rep = new File(SERVER_DIR + "/" + USERS_DIR + "/" + messIn.user[0] + "/" + messIn.repName);
					res = "-- O respositório " + messIn.repName + " do utilizador " + messIn.user[0] 
							+ " foi copiado do servidor";
				}
				//erro caso o rep nao exista
				if (!rep.exists()) {
					return -1;
				}
				//criar lista com todos os ficheiros
				else {
		             fileRep = rep.listFiles(new FilenameFilter() {
		                @Override
		                public boolean accept(File dir, String nameA) {
		                    return nameA.matches("([.[^.\\//]]+.\\D+)|([.[^.\\//]]+.[.[^.\\//]]+.\\D+)");
		                }
		             });
				}
				
				//number of cif files
				int numFiles = 0;
				
				//file rep with cif files
				ArrayList<File> newFileRep = new ArrayList<File>();
				
				for (int i = 0; i < fileRep.length; i++)
					if (fileRep[i].getName().contains(".cif")) {
						numFiles++;
						newFileRep.add(fileRep[i]);
					}
				
				//file rep with client files
				ArrayList<String> clientFileRep = new ArrayList<String>();
				
				if (messIn.fileName != null)
					for (int i = 0; i < messIn.fileName.length; i++)
						clientFileRep.add(messIn.fileName[i]);
				
				//file rep with key server files
				ArrayList<File> keyFileRep = new ArrayList<File>(numFiles);
				
				String cifFileName;
				String keyFileName;
				String keyExt;
				String[] keySplit;
				
				//get key files
				for (int j = 0; j < fileRep.length; j++) {
					cifFileName = fileRep[j].getName().split("\\.")[0];
					keySplit = fileRep[j].getName().split("\\.");
					keyFileName = keySplit[0];
					if(keySplit.length > 2) {
						keyExt = fileRep[j].getName().split("\\.")[2];
						if(cifFileName.equals(keyFileName) && keyExt.equals("key"))
							keyFileRep.add(fileRep[j]);
					}
				}
				
				//so para actualizar os ficheiros
				if (numFiles > 0) {
					toUpdate = new boolean[numFiles];
					names = new String[numFiles];
					if(messIn.fileName != null)
					if (numFiles < messIn.fileName.length)
						delete = new boolean[numFiles];
					else
						delete = new boolean[messIn.fileName.length];
					if(messIn.fileName != null) {
						
						HashMap<String, Date> mapaDatas = new HashMap<String, Date>();
						
						for (int i = 0; i < numFiles; i++) {
							if(messIn.fileName.length > i)
								names[i] = messIn.fileName[i];
							currFile = new File(newFileRep.get(i).getPath());
							
							//se o ficheiro ainda existe no servidor
							if (currFile.exists()) {
								date = new Date(currFile.lastModified());
								
								String currName = currFile.getName().split("\\.")[0];
								String currExt = keyFileRep.get(i).getName().split("\\.")[1];
								String currFileName = currName + "." + currExt;
								
									if(messIn.fileName.length > i) {
										if(!names[i].equals(currFileName)) {
											//caso em que ficheiro não existe no cliente
											//guarda a data e o nome do ficheiro para verificar mais tarde
											mapaDatas.put(names[i], messIn.fileDate[i]);
											names[i] = currFileName;
										}
										else
											//verificar se o ficheiro precisa de ser actualizado
											if (date.compareTo(messIn.fileDate[i]) > 0) 
												toUpdate[i] = true;
									} else {
										names[i] = currFileName;
	
										//verificar se o ficheiro precisa de ser actualizado
										if(messIn.fileName.length > i) {
											if (date.compareTo(messIn.fileDate[i]) > 0) 
												toUpdate[i] = true;
										} else 
											if(mapaDatas.containsKey(currFileName))
												if (date.compareTo(mapaDatas.get(currFileName)) > 0) 
													toUpdate[i] = true;
									}
							//ficheiros que o servidor ja nao tem	
							} else 
								delete[i] = true;		
						}
						
					}
					else
						//ficheiros novos que o cliente nao tem	
						for (int i = 0; i < names.length; i++)
							if (!Arrays.asList(names).contains(newFileRep.get(i).getName())) {
								String name = newFileRep.get(i).getName().split("\\.")[0];
								String ext = keyFileRep.get(i).getName().split("\\.")[1];
								names[i] = name + "." + ext;
							}
					
					if(messIn.fileName != null)
						if (messIn.fileName.length > numFiles)
							Arrays.fill(delete, numFiles, delete.length-1, Boolean.TRUE);
						
					messOut = new Message(messIn.method, names, messIn.repName, messIn.fileDate, toUpdate, 
							messIn.user, delete, res);
					outStream.writeObject(messOut);
					
					for (int i = 0; i < names.length; i++)
						if (!(clientFileRep.contains(names[i]) && !toUpdate[i]))
							sendSecureFile(outStream, inStream, new File(newFileRep.get(i).getPath()), 
									names[i], rep.toString() + "/");
					
					result = 0;
				
				} else if(messIn.fileName != null){
					res = "";
					for(int i = 0; i < messIn.fileName.length; i++) {
						delete = new boolean[messIn.fileName.length];
						delete[i] = true;
						res += "-- O ficheiro " + messIn.fileName[i] + " existe localmente mas foi eliminado no servidor "
								+ System.lineSeparator();
					}
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, 
							messIn.toBeUpdated, messIn.user, null, res);
				}
				//para inicializar o rep
				else {
					toUpdate = new boolean[numFiles];
					names = new String[numFiles];
					Arrays.fill(toUpdate, 0, toUpdate.length-1, Boolean.TRUE);
					for(int i = 0; i < toUpdate.length; i++)
						names[i] = newFileRep.get(i).getName();
					messOut = new Message(messIn.method, names, messIn.repName, messIn.fileDate, toUpdate,
							messIn.user, null, res);
					outStream.writeObject(messOut);
					
					for (int i = 0; i < numFiles; i++) 
						sendFile(outStream, inStream, newFileRep.get(i));
									
					result = 0;
				}
			} else {
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, 
						messIn.toBeUpdated, messIn.user, null, "-- O utilizador " + messIn.user[0] 
								+ " não tem acesso ao repositório " + repName + " do utilizador " + user);
				outStream.writeObject(messOut);
				return -1;
			} 
			outStream.writeObject(messOut);
			return result;
		}

		private void shareRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, 
				File users, File users_mac, File share_mac) throws InvalidKeyException, 
		NoSuchAlgorithmException, ClassNotFoundException, InvalidKeySpecException, 
		NoSuchPaddingException, InvalidAlgorithmParameterException {
					
			try {

				boolean foundUser = checkUser(messIn.user[1], users, users_mac, USERS_FILE);

				String res = null;

				String repName = messIn.repName;

				if (foundUser) {

					String userToAdd = messIn.user[1];

					File shareLog = new File(SERVER_DIR + "/" + USERS_DIR + "/shareLog.txt");
					
					verifyFileIntegrity(shareLog, share_mac, SHARE_FILE);

					BufferedReader reader = new BufferedReader(new FileReader(shareLog));

					String currLine = reader.readLine();
					
					if (currLine == null) {
						FileWriter fw = new FileWriter(shareLog);
						fw.write(messIn.user[0] + ":" + userToAdd + System.lineSeparator());
						res = "-- O repositório " + repName + " foi partilhado com o utilizador " + messIn.user[1];
						fw.close();
					} else {
						File tempFile = new File(SERVER_DIR + "/" + USERS_DIR + "/shareLog.temp");
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

		private void remove(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, 
				File users, File users_mac, File share_mac) throws InvalidKeyException, 
		NoSuchAlgorithmException, ClassNotFoundException, InvalidKeySpecException, 
		NoSuchPaddingException, InvalidAlgorithmParameterException {
			
			try {
				
				boolean foundUser = checkUser(messIn.user[1], users, users_mac, USERS_FILE);
				
				String res = null;
				
				String repName = messIn.repName;
				
				if (foundUser) {
					
					File shareLog = new File(SERVER_DIR + "/" + USERS_DIR + "/shareLog.txt");
						
					if (checkUser(messIn.user[0], shareLog, share_mac, SHARE_FILE)) {
						
						File tempFile = new File(SERVER_DIR + "/" + USERS_DIR + "/shareLog.temp");
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

		public int receiveFile(ObjectOutputStream  outStream, ObjectInputStream inStream, File file) 
				throws IOException, ClassNotFoundException{
			int result = -1;
				
			FileOutputStream pdfOut = new FileOutputStream(file);
				
			int lengthFile = (int) inStream.readObject();
				
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
		
		public int sendFile(ObjectOutputStream  outStream, ObjectInputStream inStream, File file) 
				throws IOException {
			int result = 0;
			int lengthPdf = (int) file.length();
			byte[] buf = new byte[1024];
	        FileInputStream is = new FileInputStream(file);
	        
	        outStream.writeObject(lengthPdf);
	        
	        int n = 0;
	        
	        while(((n = is.read(buf, 0, buf.length)) != -1)) {
	        	outStream.write(buf, 0, n);
	        	outStream.flush();        
	        }
	        
	        is.close();
			return result;
		}
		
		public boolean userHasAccess(String user, Message messIn, File share_mac) 
				throws InvalidKeyException, NoSuchAlgorithmException, IOException{
			
			boolean hasAccess = false;
			
			if (user != null) {
				
				File shareLog = new File(SERVER_DIR + "/" + USERS_DIR + "/shareLog.txt");
				verifyFileIntegrity(shareLog, share_mac, SHARE_FILE);
				
				BufferedReader br = new BufferedReader(new FileReader(shareLog));
				String currentLine = null;
				
				while ((currentLine = br.readLine()) != null && !hasAccess) {
					
					String[] split = currentLine.split(",");
				    String[] split2 = split[0].split(":");
				    
				    if (split2[0].equals(user)) {
				    	
				    	if (split2[1].equals(messIn.user[0]))
				    		hasAccess = true;
				    	else
				    		for(int i = 1; i < split.length && !hasAccess; i++)
				    			if(split[i].equals(messIn.user[0]))
				    				hasAccess = true;
				    }
				}
				br.close();
			} else
				hasAccess = true;
			return hasAccess;
		}
		
		public boolean authenticate(User u, File f, File m, int nonce) throws IOException, InvalidKeyException, 
		NoSuchAlgorithmException, ClassNotFoundException, InvalidKeySpecException, 
		NoSuchPaddingException, InvalidAlgorithmParameterException{
			
			boolean autenticado = false;
			
			f = decipher(f, USERS_FILE_NAME);
			
			verifyFileIntegrity(f, m, USERS_FILE);
	
			Scanner scan = new Scanner(new BufferedReader(new FileReader(f)));
			
			//getting hash
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			
			String text = null;
			byte[] buf = null; 
			byte[] hash = null; 
			
			while (scan.hasNextLine()) {
				String[] split = scan.nextLine().split(":");
				if (split[0].equals(u.name)) {
					text = split[1] + nonce;
					buf = text.getBytes();
					hash = md.digest(buf);
					byte[] pass = (byte[]) u.pass;
					if(Arrays.equals(hash, pass))
						autenticado = true; 
				}
			}
			
			scan.close();
			
			cipher(f, USERS_FILE_NAME);
			
			return autenticado;
		}
		
		public boolean createUser(User u, File f, File m) throws IOException, InvalidKeyException, 
		NoSuchAlgorithmException, ClassNotFoundException, InvalidKeySpecException, 
		NoSuchPaddingException, InvalidAlgorithmParameterException{
			
			boolean result = false;
			FileWriter fw = null;
			Scanner scan = new Scanner(new BufferedReader(new FileReader(f)));
			boolean empty = false;
			
			if(!scan.hasNext())
				empty = true;
			
			try {
				if(checkUser(u.name, f, m, USERS_FILE))
					result = true;
				else{
					f = decipher(f, USERS_FILE_NAME);
					fw = new FileWriter(f, true);
					//writes the name and pass in the file
					fw.write(u.name + ":" + u.pass + System.lineSeparator()); 
				    fw.flush();
				    fw.close();
					//creates a directory to the user
					new File(SERVER_DIR + "/" + USERS_DIR + "/" + u.name).mkdir();
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
			cipher(f, USERS_FILE_NAME);
			return result;
		}
		
		public boolean checkUser(String username, File f, File m, String filename) 
				throws InvalidKeyException, NoSuchAlgorithmException, ClassNotFoundException, 
				IOException, InvalidKeySpecException, NoSuchPaddingException, 
				InvalidAlgorithmParameterException {
			
			if(filename.equals(USERS_FILE))
				f = decipher(f, USERS_FILE_NAME);
			
			verifyFileIntegrity(f, m, filename);
			
			boolean result = false;
			Scanner scan = new Scanner(new BufferedReader(new FileReader(f)));
			
			while (scan.hasNextLine()) {
				String[] split = scan.nextLine().split(":");
				if(split[0].equals(username))
					result = true; 
			}
			scan.close();
		
			if (filename.equals(USERS_FILE))
				cipher(f, USERS_FILE_NAME);

			return result;
		}
		
		public KeyPair getKeyPair() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, 
		IOException, UnrecoverableKeyException {
			
			FileInputStream is = new FileInputStream(SERVER_DIR + "/myServer.keystore");

		    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
		    keystore.load(is, "sc1617".toCharArray());

		    String alias = "myServer";

		    Key key = keystore.getKey(alias, "sc1617".toCharArray());
		    if (key instanceof PrivateKey) {
		      //getting certificate of public key
		      Certificate cert = keystore.getCertificate(alias);

		      //getting public key
		      PublicKey publicKey = cert.getPublicKey();

		      return new KeyPair(publicKey, (PrivateKey) key);
		    }
		    return null;
		}
		
		public void receiveSecureFile(ObjectOutputStream outStream, ObjectInputStream inStream, File cifFile, 
				String filename, String filePath) 
						throws IOException, ClassNotFoundException, UnrecoverableKeyException, 
						KeyStoreException, NoSuchAlgorithmException, CertificateException, 
						NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{

			//save file's digital signature received from client
			String[] splitName = filename.split("\\.");
			File sigFile = new File(filePath + splitName[0] + ".sig");
			receiveFile(outStream, inStream, sigFile);
			
			//receive secret key
			SecretKey secKey = (SecretKey) inStream.readObject();
			
			//receive ciphered file
			receiveFile(outStream, inStream, cifFile);
			
			//get public key
			PublicKey pubKey = getKeyPair().getPublic();
			
			Cipher c = Cipher.getInstance("RSA"); 
			c.init(Cipher.ENCRYPT_MODE, pubKey);
			
			FileOutputStream fos; 
			CipherOutputStream cos;
			
			fos = new FileOutputStream(filePath + filename + ".key.server");
			cos = new CipherOutputStream(fos, c); 
			
			//cipher secret key using public key
			cos.write(secKey.getEncoded());
			
			cos.close();
			fos.close();
		}
		
		public void sendSecureFile(ObjectOutputStream outStream, ObjectInputStream inStream, File cifFile, 
				String filename, String filePath) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, NoSuchPaddingException, InvalidKeyException{
			
			//get private key
			PrivateKey privKey = getKeyPair().getPrivate();
			
			Cipher c = Cipher.getInstance("RSA"); 
			c.init(Cipher.DECRYPT_MODE, privKey);
			
			FileInputStream fis; 
			CipherInputStream cis;
			byte[] secKeyEncoded = new byte[16];
			
			fis = new FileInputStream(filePath + filename + ".key.server");
			cis = new CipherInputStream(fis, c); 
			
			//decipher secret key using private key
			cis.read(secKeyEncoded);
			
			//get secret key
			SecretKey secKey = new SecretKeySpec(secKeyEncoded, 0, secKeyEncoded.length, "AES");
			
			cis.close();
			fis.close();
			
			//send to client the ciphered file
			sendFile(outStream, inStream, cifFile);
			
			//send to client private key
			outStream.writeObject(secKey);
			
			//get file's digital signature 
			String[] splitName = filename.split("\\.");
			File sigFile = new File(filePath + splitName[0] + ".sig");
			
			//send file's digital signature
			sendFile(outStream, inStream, sigFile);
			
		}
		
	}
}