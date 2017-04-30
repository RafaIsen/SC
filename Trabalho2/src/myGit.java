/*****************************************
*   Seguranca e Confiabilidade 2016/17
*
*				Grupo 34
*****************************************/

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

//Cliente myGit
public class myGit{
	
	public static void main(String[] args) throws IOException, ClassNotFoundException, 
	URISyntaxException, NoSuchAlgorithmException, UnrecoverableKeyException,
	InvalidKeyException, KeyStoreException, CertificateException, SignatureException, NoSuchPaddingException{
		System.out.println("cliente: main");
		myGit client = new myGit();
		client.startClient(args);
	}

	public void startClient(String[] args) throws ClassNotFoundException, IOException, 
	URISyntaxException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeyException, 
	KeyStoreException, CertificateException, SignatureException, NoSuchPaddingException{
			
		String[] ip = null;
		int port = 0;
		File rep = null;
		
		//creates a local repository
		if (args[0].equals("-init")){

			rep = new File(args[1] + "/keys");
			
			if (rep.exists())
				System.out.println("-- O repositório " + args[1] + 
						" ja existe. Escolha outro nome por favor.");
			else {
				rep.mkdirs();
				System.out.println("-- O repositório " + args[1] + " foi criado localmente ");
			}
		}
		else {
			try {		
				
				Scanner scan = new Scanner(System.in);
				
				boolean param_p = false;
				
				//verifies if the password is written in the initial args
				if(args.length > 2)
					param_p = args[2].equals("-p");
				
				//splits ip address
				ip = args[1].split(":");
				port = Integer.parseInt(ip[1]);
				
				//verifies the certificate sent by the server
				System.setProperty("javax.net.ssl.trustStore", args[0] + "/keys/myClient.keyStore");
	
				//creates the socket
				SocketFactory sf = SSLSocketFactory.getDefault();
				Socket cSoc = sf.createSocket(ip[0], port);
				
				ObjectOutputStream outStream = new ObjectOutputStream(cSoc.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(cSoc.getInputStream());
				
				//gives the server the number of args
				outStream.writeObject(args.length);
				
				//warn the server that -p is a parameter
				outStream.writeObject(param_p);
				
				/*authenticate user*/
				//sends user name to the server
				outStream.writeObject(args[0]);
				
				//obtains response of the server
				boolean exists = (boolean) inStream.readObject();
				
				authenticateUser(exists, param_p, args[0], args[2], args[3], outStream , inStream, scan);

				//executes the command
				if (param_p) {
					if(args.length == 7)
						executeCommand(args[4], args[5], args[6], args[0], outStream, inStream);
					else if(args.length > 4)
						executeCommand(args[4], args[5], "N", args[0], outStream, inStream);
				}
				else
					if(args.length == 5)
						executeCommand(args[2], args[3], args[4], args[0], outStream, inStream);
					else if(args.length > 2)
						executeCommand(args[2], args[3], "N", args[0], outStream, inStream);
				
				cSoc.close();
				scan.close();
			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
				
	}
	
	public void authenticateUser(boolean exists, boolean param_p, String username, String command, 
			String pwd, ObjectOutputStream outStream, ObjectInputStream inStream, Scanner scan) 
					throws IOException, ClassNotFoundException, NoSuchAlgorithmException{
		
		boolean authentic = false;
		
		if (!exists) {
			
			System.out.println("-- O utilizador " + username + " vai ser criado ");
			if(param_p)
				pwd = confirmPwd(username, command, pwd);
			else 
				pwd = confirmPwd(username, " ", " ");
			outStream.writeObject(pwd);
			if((boolean) inStream.readObject())
				System.out.println("-- O utilizador " + username + " foi criado");
		
		} else if (param_p) {
			
			//receiving the nonce from the server
			int nonce = (int) inStream.readObject();
			
			//getting hash
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			
			//user's password concatenated with nonce sent by the server 
			String text = pwd + nonce;
			byte[] buf = text.getBytes();
			byte[] hash = md.digest(buf);
			
			outStream.writeObject(hash);
			authentic = (boolean) inStream.readObject();
			
			while(!authentic){
				System.out.println("Password errada!");
				System.out.println("Tenta novamente:");
				pwd = scan.nextLine();
				text = pwd + nonce;
				buf = text.getBytes();
				hash = md.digest(buf);
				outStream.writeObject(hash);
				authentic = (boolean) inStream.readObject();
			}
		} else {
			
			//receiving the nonce from the server
			int nonce = (int) inStream.readObject();
			
			//getting hash
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			
			System.out.println("Password:");
			pwd = scan.nextLine();
			//user's password concatenated with nonce sent by the server 
			String text = pwd + nonce;
			byte[] buf = text.getBytes();
			byte[] hash = md.digest(buf);
			
			outStream.writeObject(hash);
			authentic = (boolean) inStream.readObject();
			
			while(!authentic){
				System.out.println("Password errada!");
				System.out.println("Tenta novamente:");
				pwd = scan.nextLine();
				text = pwd + nonce;
				buf = text.getBytes();
				hash = md.digest(buf);
				outStream.writeObject(hash);
				authentic = (boolean) inStream.readObject();
			}
		}
	}
	
	private void shareRep(ObjectOutputStream outStream, ObjectInputStream inStream, String repName, 
			String userSharedWith, String user) throws IOException, ClassNotFoundException {
		String[] users = new String[2];
		users[0] = user;
		users[1] = userSharedWith;
		
		Message messOut = new Message("shareRep", null, repName, null, null, users, null, null);
		Message messIn = null;
		
		outStream.writeObject(messOut);
		messIn = (Message) inStream.readObject();
		
		System.out.println(messIn.result);
	}
	
	private void remove(ObjectOutputStream outStream, ObjectInputStream inStream, String repName, 
			String userToRemove, String user) throws IOException, ClassNotFoundException {
		String[] users = new String[2];
		users[0] = user;
		users[1] = userToRemove;
		
		Message messOut = new Message("remove", null, repName, null, null, users, null, null);
		Message messIn = null;
		
		outStream.writeObject(messOut);
		messIn = (Message) inStream.readObject();
		
		System.out.println(messIn.result);
	}

	private int pushFile(ObjectOutputStream  outStream, ObjectInputStream inStream, String filename, 
			String user) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, 
	UnrecoverableKeyException, InvalidKeyException, KeyStoreException, CertificateException, 
	SignatureException, NoSuchPaddingException {
		int result = -1; 
		Message messIn = null;
		
		File file = new File(user + "/" + filename);
		String repName;
		
		String[] split = filename.split("/");
		
		if (file.exists()) {
			String[] name = new String[1];
			split = filename.split("/");
			name[0] = filename;
			
			Date[] dates = new Date[1];
			Date date = new Date(file.lastModified());
			dates[0] = date;
			
			String[] users = new String[1];
			users[0] = user;
			repName = split[split.length-2];
			
			Message messOut = new Message("pushFile", name, repName, dates, null, users, null, null);
			
			outStream.writeObject(messOut);
			
			messIn = (Message) inStream.readObject();
			
			if (messIn == null)
				result = -1;
			
			else if (messIn.toBeUpdated[0] == true) {
				sendSecureFile(outStream, inStream, file, filename.split("/")[1], user + "/" + repName);
				result = 0;
			} else
				result = -1;
			
			
		} else {
			System.out.println("-- O ficheiro não existe no repositório");
			Message messOut = new Message("", null, null, null, null, null, null, null);
			outStream.writeObject(messOut);
		}
		if(messIn != null)
			System.out.println(messIn.result);
		return result;
	}

	private int pushRep(ObjectOutputStream  outStream, ObjectInputStream inStream, String repPath, 
			String user) throws IOException, ClassNotFoundException, UnrecoverableKeyException, 
	InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, 
	SignatureException, NoSuchPaddingException {
		int result = -1; 
		File rep = null;
		String repName;
		Message messIn = null;

		rep = new File(user + "/" + repPath);
		
		if (rep.exists()) {
			File[] repFiles = rep.listFiles(new FileFilter() {
			    @Override
			    public boolean accept(File pathname) {
			        return pathname.isFile();
			    }
			});
			int numFiles = repFiles.length;
			
			String[] name = null;
			Date[] dates = null;
			
			String[] users = new String[1];
			users[0] = user;
			
			String[] splitRep = repPath.split("/");
			repName = splitRep[splitRep.length-1];
			
			if (numFiles != 0) {
				name = rep.list(new FilenameFilter() {
					@Override
					public boolean accept(File current, String name) {
						return new File(current, name).isFile();
					}
					});
				dates = new Date[numFiles];
			}	
			
			for (int i = 0; i < numFiles; i++)
				dates[i] = new Date(repFiles[i].lastModified());

			Message messOut = new Message("pushRep", name, repPath, dates, null, users, null, null);
			
			outStream.writeObject(messOut);
			
			messIn = (Message) inStream.readObject();
			
			if (messIn == null)
				result = -1;
			else if (messIn.toBeUpdated != null) {
				for(int i = 0; i < messIn.toBeUpdated.length; i++) {
					if (messIn.toBeUpdated[i] == true) 
						sendSecureFile(outStream, inStream, repFiles[i], name[i], user + "/" + repName);
					}
				result = 0;
			} 
		} else {
			System.out.println("O repositorio nao existe");
			Message messOut = new Message("", null, null, null, null, null, null, null);
			outStream.writeObject(messOut);
		}
		System.out.println(messIn.result);
		return result;			
	}

	private int pullFile(ObjectOutputStream  outStream, ObjectInputStream inStream, String filename, 
			String user) throws IOException, ClassNotFoundException, InvalidKeyException, 
	UnrecoverableKeyException, NoSuchAlgorithmException, NoSuchPaddingException, KeyStoreException, 
	CertificateException, SignatureException {
		int result = 0;
		File file = new File(user + "/" + filename);
		File tempFile = null;
		String[] split = filename.split("/");
		String[] name = new String[1];
		name[0] = filename;
		String repName = split[split.length-2];
		
		Date[] dates = new Date[1];
		Date date = new Date(file.lastModified());
		dates[0] = date;
		
		String[] users = new String[1];
		users[0] = user;
		
		Message messOut = new Message("pullFile", name, repName, dates, null, 
				users, null, null);
		Message messIn = null;
		
		outStream.writeObject(messOut);
		
		messIn = (Message) inStream.readObject();
		
		if (messIn == null)
			result = -1;
		
		else if (messIn.toBeUpdated[0] == true) {
			tempFile = new File(user + "/" + filename + ".temp");
			tempFile.createNewFile();
			receiveSecureFile(outStream, inStream, tempFile, user + "/" + filename, user + "/" + repName);
			tempFile.delete();
			result = 0;
		} 
		System.out.println(messIn.result);
		return result;
	}

	private int pullRep(ObjectOutputStream  outStream, ObjectInputStream inStream, String repPath, 
			String user) throws IOException, ClassNotFoundException, InvalidKeyException, UnrecoverableKeyException, 
	NoSuchAlgorithmException, NoSuchPaddingException, KeyStoreException, CertificateException, SignatureException {
		int result = -1; 
		File rep = new File(user + "/" + repPath);
		String path = user + "/" + repPath + "/";
		//String[] splitRep = repPath.split("/");
		//String repName = splitRep[splitRep.length-1];
		File newFile = null;
		File[] repFiles = null;
		String[] names = null;
		Date[] dates = null;
		String[] users = new String[1];
		users[0] = user;
		
		if (rep.exists()) {
			repFiles = rep.listFiles(new FileFilter() {
			    @Override
			    public boolean accept(File pathname) {
			        return pathname.isFile();
			    }
			});
			int numFiles = repFiles.length;
			
			if (numFiles != 0) {
				names = new String[numFiles];
				dates = new Date[numFiles];
			}	
			
			for(int i = 0; i < numFiles; i++) {
				names[i] = repFiles[i].getName();
				dates[i] = new Date(repFiles[i].lastModified());
			}
		} else {
			names = new String[0];
			dates = new Date[0];
		}
		Message messOut = new Message("pullRep", names, repPath, dates, null, users, null, null);
		Message messIn = null;
		
		outStream.writeObject(messOut);
		
		messIn = (Message) inStream.readObject();
		
		boolean haAtualizar = false;
		
		if (messIn == null)
			result = -1;
		else {
			if (!rep.exists())
				rep.mkdirs();
			if(messIn.toBeUpdated != null)
				//a actualizar os ficheiros novos q o cliente nao tem
				for (int i = 0; i < messIn.toBeUpdated.length; i++) {
					//so a actualizar os ficheiros antigos
					if(names != null) {
						if (i < messIn.fileName.length)
							if (messIn.toBeUpdated[i] == true) {
								haAtualizar = true;
								if(names.length > i)
									receiveSecureFile(outStream, inStream, newFile, user + "/" + repPath + "/" + names[i], 
											user + "/" + repPath);
								else
									receiveSecureFile(outStream, inStream, newFile, user + "/" + repPath + "/" + messIn.fileName[i], 
											user + "/" + repPath);
							}
							//receber ficheiros novos
							else {
								newFile = new File(path + messIn.fileName[i]);
								if (!newFile.exists()) {
									haAtualizar = true;
									newFile.createNewFile();
									receiveSecureFile(outStream, inStream, newFile, user + "/" + repPath + "/" 
											+ messIn.fileName[i], user + "/" + repPath);
								}
							}
					} //receber ficheiros novos
					else {
						haAtualizar = true;
						receiveSecureFile(outStream, inStream, newFile, user + "/" + repPath + "/" + messIn.fileName[i], 
								user + "/" + repPath);
					}
				}
		}
		if(haAtualizar)
			System.out.println(messIn.result);
		else
			System.out.println("-- Nada há a atualizar");
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
	
	public String confirmPwd(String username, String command, String pwd){
		
		Scanner scan = new Scanner(System.in);
		String pass = pwd;
		String passConf = null;
		
		if (command.equals("-p")) {
			
			System.out.println("Confirmar password do utilizador " + username + ":");
		
			passConf = scan.nextLine();
			
			if(!passConf.equals(pwd))
				while(!pass.equals(passConf)) {
					
					System.out.println("Essa não foi a password que escreveu primeiro");
					System.out.println("Escreva a password:");
					pass = scan.nextLine();
					
					System.out.println("Confirmar password do utilizador " + username + ":");
					passConf = scan.nextLine();
					
				}
			
		} else {
			
			System.out.println("Escreva uma password para a sua conta:");
			
			pass = scan.nextLine();
			
			System.out.println("Confirmar password do utilizador " + username + ":");
			
			passConf = scan.nextLine();
			
			while(!pass.equals(passConf)) {
				
				System.out.println("Essa não foi a password que escreveu primeiro");
				System.out.println("Escreva a password:");
				pass = scan.nextLine();
				
				System.out.println("Confirmar password do utilizador " + username + ":");
				passConf = scan.nextLine();
				
			}

		}
		scan.close();
		return pass;
		
	}
	
	public void executeCommand(String command, String param1, String param2, String user, 
			ObjectOutputStream  outStream, ObjectInputStream inStream) throws IOException, 
	ClassNotFoundException, NoSuchAlgorithmException, UnrecoverableKeyException, 
	InvalidKeyException, KeyStoreException, CertificateException, SignatureException, NoSuchPaddingException{
		
		switch (command) {
		
			case "-push":
				if (param1.contains(".")) {
					pushFile(outStream, inStream, param1, user);
				}
				else
					pushRep(outStream, inStream, param1, user);
				
				break;
			
			case "-pull":
				if (param1.contains("."))
					pullFile(outStream, inStream, param1, user);
				else
					pullRep(outStream, inStream, param1, user);
				
				break;
				
			case "-share":
				shareRep(outStream, inStream, param1, param2, user);
				break;
				
			case "-remove":
				remove(outStream, inStream, param1, param2, user);
				break;
			
			case "-p":
				break;
				
			default:
				System.out.println("Esse commando não existe!");
				break;
			
		}
		
	}
	
	public PrivateKey getPrivateKey(String user) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, 
	IOException, UnrecoverableKeyException {
		
		FileInputStream is = new FileInputStream(user + "/keys/myClient.keystore");

	    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	    keystore.load(is, "client1617".toCharArray());

	    Enumeration<String> enumeration = keystore.aliases();
	    PrivateKey key = null;
	    String alias = null;
        while(enumeration.hasMoreElements()) {
            alias = (String)enumeration.nextElement();
            key = (PrivateKey) keystore.getKey(alias, "client1617".toCharArray());
            if (key instanceof PrivateKey)
            	return key;
        }
	    return key;
	}
	
	public ArrayList<PublicKey> getPublicKey(String user) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, 
	IOException, UnrecoverableKeyException {
		
		FileInputStream is = new FileInputStream(user + "/keys/myClient.keystore");

	    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	    keystore.load(is, "client1617".toCharArray());
	    
	    ArrayList<PublicKey> pubKeys = new ArrayList<PublicKey>(); 

	    Enumeration<String> enumeration = keystore.aliases();
	    String alias = null;
        while(enumeration.hasMoreElements()) {
            alias = (String)enumeration.nextElement();
            Certificate c = keystore.getCertificate(alias);
            pubKeys.add(c.getPublicKey());
 
        }
        return pubKeys;
	}
	
	public File signFile(File f, String filename, String repName) throws NoSuchAlgorithmException, IOException, 
	UnrecoverableKeyException, KeyStoreException, CertificateException, InvalidKeyException, 
	SignatureException { 

		PrivateKey key = getPrivateKey(repName.split("/")[0]);
		
		Signature s = Signature.getInstance("SHA256withRSA"); 
		s.initSign(key);
		
		FileInputStream fis = new FileInputStream(f);
		BufferedInputStream bufin = new BufferedInputStream(fis);
		byte[] buffer = new byte[256];
		int len;
		while ((len = bufin.read(buffer)) >= 0) {
		    s.update(buffer, 0, len);
		};
		bufin.close();
		
		FileOutputStream fos = new FileOutputStream(repName + "/" + filename + ".sig"); 
		fos.write(buffer);
		fos.close();
		
		return new File(repName + "/" + filename + ".sig");
	}
	
	public boolean verifySignedFile(File decifFile, File sigFile, String repName) throws NoSuchAlgorithmException, IOException, 
	UnrecoverableKeyException, KeyStoreException, CertificateException, InvalidKeyException, 
	SignatureException { 
		
		FileInputStream sigfis = new FileInputStream(sigFile);
		byte[] sigToVerify = new byte[sigfis.available()]; 
		sigfis.read(sigToVerify);
		sigfis.close();
		
		ArrayList<PublicKey> pubKeys = getPublicKey(repName.split("/")[0]);
		Iterator<PublicKey> iterator = pubKeys.iterator();
		
		boolean passed = false;
		
		while(iterator.hasNext() && !passed) {
		
			Signature s = Signature.getInstance("SHA256withRSA"); 
			s.initVerify(iterator.next()); 
			
			FileInputStream datafis = new FileInputStream(decifFile);
			BufferedInputStream bufin = new BufferedInputStream(datafis);
	
			byte[] buffer = new byte[256];
			int len;
			while (bufin.available() != 0) {
			    len = bufin.read(buffer);
			    s.update(buffer, 0, len);
			};
			bufin.close();
			passed = Arrays.equals(buffer, sigToVerify);
		}
			
		return passed;
	}

	public void sendSecureFile(ObjectOutputStream outStream, ObjectInputStream inStream, 
			File file, String filename, String repName) throws UnrecoverableKeyException, InvalidKeyException, 
	NoSuchAlgorithmException, KeyStoreException, CertificateException, SignatureException, 
	IOException, NoSuchPaddingException{
		
		//generates file's digital signature
		String[] splitName = filename.split("\\.");
		File sigFile = signFile(file, filename, repName);
		
		//send file's digital signature
		sendFile(outStream, inStream, sigFile);
		sigFile.delete();
		
		//generate random shared secret key
		KeyGenerator kg = KeyGenerator.getInstance("AES"); 
		kg.init(128); 
		SecretKey secKey = kg.generateKey();
		
		Cipher c = Cipher.getInstance("AES"); 
		c.init(Cipher.ENCRYPT_MODE, secKey);
		
		FileInputStream fis; 
		FileOutputStream fos; 
		CipherOutputStream cos;
		
		fis = new FileInputStream(file); 
		fos = new FileOutputStream(repName + "/" + splitName[0] + ".cif");
		cos = new CipherOutputStream(fos, c); 
		
		byte[] b = new byte[16]; 
		int i = 0;
		
		//cipher file using secret key
		while ( (i = fis.read(b)) != -1) 
			cos.write(b, 0, i); 
		
		cos.close();
		fos.close();
		fis.close();
		
		//send secret key to the server
		outStream.writeObject(secKey);
		
		//send ciphered file to the server
		File cifFile = new File(repName + "/" + splitName[0] + ".cif");
		sendFile(outStream, inStream, cifFile);
		cifFile.delete();
	}
	
	public void receiveSecureFile(ObjectOutputStream outStream, ObjectInputStream inStream, 
			File file, String filename, String repName) throws ClassNotFoundException, IOException, NoSuchAlgorithmException, 
	NoSuchPaddingException, InvalidKeyException, UnrecoverableKeyException, KeyStoreException, 
	CertificateException, SignatureException{
		
		String[] splitName = filename.split("\\.");
		File cifFile = new File(splitName[0] + ".cif");
		
		//receive ciphered file from server
		receiveFile(outStream, inStream, cifFile);
		
		//receive secret key from server
		SecretKey secKey = (SecretKey) inStream.readObject();
		
		Cipher c = Cipher.getInstance("AES"); 
		c.init(Cipher.DECRYPT_MODE, secKey);
		
		FileInputStream fis; 
		FileOutputStream fos; 
		CipherOutputStream cos;
		
		fis = new FileInputStream(cifFile); 
		fos = new FileOutputStream(filename + ".temp");
		cos = new CipherOutputStream(fos, c); 
		
		byte[] b = new byte[16]; 
		int i = 0;
		
		//decipher file using secret key
		while ( (i = fis.read(b)) != -1) 
			cos.write(b, 0, i); 
		
		cos.close();
		fos.close();
		fis.close();
		
		//prepare file's digital signature
		File sigFile = new File(splitName[0] + ".server.sig");
		
		//receive file's digital signature 
		receiveFile(outStream, inStream, sigFile);
		
		//get deciphered file
		File decifFile = new File(filename + ".temp");
		
		//verify file signature
		if (!verifySignedFile(decifFile, sigFile, repName)) {
			System.out.println("-- Erro! O ficheiro " + filename + " foi corrompido durante o envio do servidor."
					+ System.lineSeparator() + "O cliente irá terminar...");
			//delete all files created in this method except the original file
			decifFile.delete();
			sigFile.delete();
			cifFile.delete();
			System.exit(-1);
		}
		new File(filename).delete();
		decifFile.renameTo(new File(filename));
		sigFile.delete();
		cifFile.delete();
	}
	
}