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
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
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

	public final String SERVER_DIR = "/users"; 
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
		File usersDir = new File("bin/" + SERVER_DIR);
		if(!usersDir.exists())
			usersDir.mkdir();
		//creates the text file users if it does not exist
		File users = new File("bin/" + SERVER_DIR + "/" + USERS_FILE_CIF);
		if (!users.exists()) {
			users = new File("bin/" + SERVER_DIR + "/" + USERS_FILE);
			users.createNewFile();
		}
		else
			users = decipher(users, USERS_FILE_NAME);
		//creates the text file shareLog if it does not exist
		File shareLog = new File("bin/" + SERVER_DIR + "/" + SHARE_FILE);
		if(!shareLog.exists())
			shareLog.createNewFile();
		
		File users_mac = new File("bin/" + SERVER_DIR + "/" + USERS_MAC_FILE);
		File share_mac = new File("bin/" + SERVER_DIR + "/" + SHARE_MAC_FILE);
		
		verifyFileIntegrity(users, users_mac, USERS_FILE);
		
		cipher(users, USERS_FILE_NAME);
		
		verifyFileIntegrity(shareLog, share_mac, SHARE_FILE);
		
		ServerSocket sSoc = null;
		
		try {
			
		    System.setProperty("javax.net.ssl.keyStore", "myServer.keyStore");
			System.setProperty("javax.net.ssl.keyStorePassword", SERVER_PASS);
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

		if (!readFile.hasNextLine()){
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
			
		    FileOutputStream fos = new FileOutputStream("bin/" + SERVER_DIR + "/" + filename + ".cif");
		    CipherOutputStream cos = new CipherOutputStream(fos, c);
		     
		    while (i != -1) {
		        cos.write(b, 0, i);
		        i = fis.read(b);
		    }
		    cos.close();
		    fos.close();
		    fis.close();
		    
		    file.delete();
		    
		    return new File("bin/" + SERVER_DIR + "/" + filename + ".cif");
		    
		} else {
			File f = new File("bin/" + SERVER_DIR + "/" + filename + ".cif");
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
	    FileOutputStream fos = new FileOutputStream("bin/" + SERVER_DIR + "/" + filename + ".txt");
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
	    
	    return new File("bin/" + SERVER_DIR + "/" + filename + ".txt");
		
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
				
				File users = new File("bin/" + SERVER_DIR + "/" + USERS_FILE_CIF);
				
				File users_mac = new File("bin/" + SERVER_DIR + "/" + USERS_MAC_FILE);
				File share_mac = new File("bin/" + SERVER_DIR + "/" + SHARE_MAC_FILE);

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
			
		private int pushFile(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn) 
				throws IOException, ClassNotFoundException, UnrecoverableKeyException, KeyStoreException, 
				NoSuchAlgorithmException, CertificateException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
			int result = -1;
		
			StringBuilder sb = new StringBuilder();
			sb.append(messIn.fileName[0]);
			int firstI = sb.indexOf("/");
			int secondI = sb.indexOf("/", firstI+1);
			String[] split = messIn.fileName[0].split("/");
			
			File file = null;
			File sigFile = null;
			File cifFile = null;
			String cifFilename = null;
			Path pathFolder = null;
			String filename = null;
			String pathFile = null;
			
			if(secondI == -1) {
				pathFolder = new File("bin/" + SERVER_DIR + "/" + messIn.user[0] + "/" + split[0]).toPath();
				file = new File("bin/" + SERVER_DIR + "/" + messIn.user[0] + "/" + messIn.fileName[0]);
				filename = split[1];
				String[] splitName = filename.split("\\.");
				pathFile = "bin/" + SERVER_DIR + "/" + messIn.user[0] + "/" + split[0] + "/";
				sigFile = new File(pathFile + splitName[0] + ".sig");
				cifFilename = splitName[0] + ".cif";
				cifFile = new File(pathFile + cifFilename);
			} else {
				pathFolder = new File("bin/" + SERVER_DIR + "/" + split[0] + "/" + split[1]).toPath();
				file = new File("bin/" + SERVER_DIR + "/" + messIn.fileName[0]);
				filename = split[2];
				String[] splitName = filename.split("\\.");
				pathFile = "bin/" + SERVER_DIR + "/" + messIn.user[0] + "/" + split[0] + "/" 
				+ split[1] + "/";
				sigFile = new File(pathFile + splitName[0] + ".sig");
				cifFilename = splitName[0] + ".cif";
				cifFile = new File(pathFile + cifFilename);
			}
			
			File newFile = null;
		
			Date date = null;
			boolean exists = cifFile.exists();
			
			Message messOut = null;
			boolean[] toUpdated = new boolean[1];
			
			int versao = 0;
		
			if (exists) {
				//actualiza o ficheiro para uma versao mais recente
				date = new Date(cifFile.lastModified());
				
				if (date.before(messIn.fileDate[0])) {

					versao = countNumVersions1(pathFolder, cifFilename);
					newFile = new File(file + ".temp");
					
					toUpdated[0] = true;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, toUpdated, messIn.user, null, "-- O ficheiro " + filename + " foi atualizado no servidor");
					outStream.writeObject(messOut);
					newFile.createNewFile();
					receiveSecureFile(outStream, inStream, sigFile, newFile, filename, pathFile);
					cifFile.renameTo(new File(pathFolder.toString() + "/" + cifFilename + "." + String.valueOf(versao)));
					newFile.renameTo(new File(pathFolder.toString() + "/" + cifFilename));
					result = 0;
					
				} else {
					//nao actualiza o ficheiro porque nao he mais recente
					toUpdated[0] = false;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, toUpdated, messIn.user, null, "-- Nada a atualizar no servidor");
					outStream.writeObject(messOut);
					result = 0;
				}	
				
			//cria o ficheiro porque ainda existe
			} else {
				toUpdated[0] = true;
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, toUpdated, messIn.user, null, "-- O ficheiro " + filename + " foi enviado para o servidor");
				outStream.writeObject(messOut);
				cifFile.createNewFile();
				receiveSecureFile(outStream, inStream, sigFile, cifFile, filename, pathFile);
				result = 0;
									
			}
			return result;			
		}

		private int pushRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn) 
				throws IOException, ClassNotFoundException {
			int result = -1;
			File rep  = null;
			File newFile = null;
			File currFile = null;
			File[] fileRep = null;
			Date date = null;
			String nameAux = null;
			
			Message messOut = null;
			boolean[] toUpdated = null;
			int[] versions = null;
			
			String repName = null;
			String res = "";
			
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
		         fileRep = rep.listFiles(new FilenameFilter() {
		            @Override
		            public boolean accept(File dir, String nameA) {
		                return nameA.matches("([.[^.\\//]]+.\\D+)|([.[^.\\//]]+.[.[^.\\//]]+.\\D+)");
		            }
		         });
			}
			
			//caso o rep venha com ficheiros
			if (messIn.fileName != null) {
				toUpdated = new boolean[messIn.fileName.length];
				versions = new int[messIn.fileName.length];
				
				for (int i = 0; i < messIn.fileName.length; i++) {
					currFile = new File(rep.toString() + "/" + messIn.fileName[i]);
					if (fileRep != null && currFile.exists()) {
						date = new Date(currFile.lastModified());
						//verificar quais os ficheiros que precisam de ser actualizados
						if (date.before(messIn.fileDate[0])) {
							versions[i] = countNumVersions1(rep.toPath(), messIn.fileName[i]);
							toUpdated[i] = true;
							res += "-- O ficheiro " + messIn.fileName[i] + " foi atualizado no servidor" + System.lineSeparator();
						} else
							res = "-- Nada a atualizar no servidor";
					} else {
						toUpdated[i] = true;
						versions[i] = 0;
						res += "-- O ficheiro " + messIn.fileName[i] + " foi enviado para o servidor" + System.lineSeparator();
					}
				}
			}
			
			//saber quais os ficheiros a "eliminar"
			if (fileRep != null && toUpdated == null) {
				for (int j = 0; j < fileRep.length; j++)
					if (!Arrays.asList(messIn.repName).contains(fileRep[j].getName())) {
						nameAux = fileRep[j].getName();
						fileRep[j].renameTo(new File(rep.toPath() + "/" + nameAux + "." + String.valueOf(countNumVersions1(rep.toPath(), fileRep[j].getName()))));		
						res += "-- O ficheiro " + fileRep[j].getName() + " vai ser eliminado no servidor" + System.lineSeparator();
						eliminou = true;
					}
			}
						
			messOut = new Message(messIn.method, null, messIn.repName, null, toUpdated, messIn.user, null, res);
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

		private int pullFile(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, 
				File share_mac) throws IOException, InvalidKeyException, NoSuchAlgorithmException, 
		ClassNotFoundException {
			
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
			
			boolean[] toUpdated = new boolean[1];
			
			toUpdated[0] = false;
			
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
						toUpdated[0] = true;
						if(myrep)
							messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, toUpdated, messIn.user, null, "-- O repositório " + repName + " foi copiado do servidor");
						else
							messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, toUpdated, messIn.user, null, "-- O repositório " + repName + " do utilizador " + otherUser + " foi copiado do servidor");
						outStream.writeObject(messOut);
						sendFile(outStream, inStream, file);
						result = 0;				
					} else {
						//nao actualiza o ficheiro porque nao he mais recente
						toUpdated[0] = false;
						messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, toUpdated, messIn.user, null, "-- O ficheiro do seu repositório local é o mais recente");
						outStream.writeObject(messOut);
						result = 0;
					}	
				//erro o ficeiro nao existe
				} else {
					toUpdated[0] = false;
					messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, toUpdated, messIn.user, null, "-- O ficheiro " + filename + " não existe");
					outStream.writeObject(messOut);
				}
				
			} else {
				messOut = new Message(messIn.method, messIn.fileName, messIn.repName, messIn.fileDate, toUpdated, messIn.user, null, "-- O utilizador " + messIn.user[0] + " não tem acesso ao ficheiro " + filename + " do utilizador " + otherUser);
				outStream.writeObject(messOut);
				result = -1;
			}
			br.close();
			return result;
		}

		private int pullRep(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, 
				File share_mac) throws IOException, InvalidKeyException, NoSuchAlgorithmException, 
		ClassNotFoundException {
			
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
		
				boolean[] toUpdated = null;
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
					toUpdated = new boolean[fileRep.length];
					names = new String[fileRep.length];
					if(messIn.fileName != null)
					if (fileRep.length < messIn.fileName.length)
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
									toUpdated[i] = true;
															
							//ficheiros que o servidor ja nao tem	
							} else 
								delete[i] = true;	
							
						//ficheiros novos que o cliente nao tem	
						} else {
							for (int j = 0;j < names.length; j++)
								if (!Arrays.asList(names).contains(fileRep[j].getName())) {
									names[j] = fileRep[j].getName();
								}
						}
											
					}
					if(messIn.fileName != null)
					if (messIn.fileName.length > fileRep.length)
						Arrays.fill(delete, fileRep.length, delete.length-1, Boolean.TRUE);
						
					messOut = new Message(messIn.method, names, messIn.repName, messIn.fileDate, toUpdated, messIn.user, delete, res);
					outStream.writeObject(messOut);
					
					for (int i = 0; i < names.length; i++) 
						sendFile(outStream, inStream, new File(rep.toString() + "/" + names[i]));
				
				result = 0;
				
				} else if(rep.listFiles() != null){
					delete = new boolean[1];
					delete[0] = true;
					res = "-- O ficheiro " + messIn.fileName[0] + " existe localmente mas foi eliminado no servidor ";
				}
				//para inicializar o rep
				else {
					toUpdated = new boolean[fileRep.length];
					names = new String[fileRep.length];
					Arrays.fill(toUpdated, 0, toUpdated.length-1, Boolean.TRUE);
					for(int i = 0; i < toUpdated.length; i++)
						names[i] = fileRep[i].getName();
					messOut = new Message(messIn.method, names, messIn.repName, messIn.fileDate, toUpdated, messIn.user, null, res);
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

		private void remove(ObjectOutputStream outStream, ObjectInputStream inStream, Message messIn, 
				File users, File users_mac, File share_mac) throws InvalidKeyException, 
		NoSuchAlgorithmException, ClassNotFoundException, InvalidKeySpecException, 
		NoSuchPaddingException, InvalidAlgorithmParameterException {
			
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
			
			FileInputStream is = new FileInputStream("myServer.keystore");

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
		
		public void receiveSecureFile(ObjectOutputStream outStream, ObjectInputStream inStream, File sigFile, File cifFile, 
				String filename, String pathFile) 
						throws IOException, ClassNotFoundException, UnrecoverableKeyException, 
						KeyStoreException, NoSuchAlgorithmException, CertificateException, 
						NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
			
			//saves file's digital signature received from client
			receiveFile(outStream, inStream, sigFile);
			
			//receives secret key
			SecretKey secKey = (SecretKey) inStream.readObject();
			
			//receives ciphered file
			receiveFile(outStream, inStream, cifFile);
			
			//getting public key
			PublicKey pubKey = getKeyPair().getPublic();
			
			Cipher c = Cipher.getInstance("RSA"); 
			c.init(Cipher.ENCRYPT_MODE, pubKey);
			
			FileOutputStream fos; 
			CipherOutputStream cos;
			
			fos = new FileOutputStream(pathFile + filename + ".key.server");
			cos = new CipherOutputStream(fos, c); 
			
			//ciphers secret key using public key
			cos.write(secKey.getEncoded());
			
			cos.close();
			fos.close();
			
		}
		
	}
}