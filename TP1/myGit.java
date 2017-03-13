/***************************************************************************
*   Seguranca e Confiabilidade 2016/17
*
*
***************************************************************************/

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

//Cliente myGit

public class myGit{
	
	public static void main(String[] args) throws IOException, ClassNotFoundException, URISyntaxException{
		System.out.println("cliente: main");
		myGit client = new myGit();
		client.startClient(args);
	}

	public void startClient(String[] args) throws ClassNotFoundException, IOException, URISyntaxException{
			
		String[] ip = null;
		int port = 0;
		
		/*Trying to get the path of the client*/
		URI myGitPath = myGit.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		Path path = Paths.get(myGitPath);
		
		//creates a local repository
		if (args[0].equals("-init")){
			new File(path + "/" + args[1]).mkdir();
			System.out.println("-- O repositório " + args[1] + " foi criado localmente ");
		}
		else {
			try {		
				
				Scanner scan = new Scanner(System.in);
				
				boolean param_p = false;
				
				if(args.length > 2)
					param_p = args[2].equals("-p");
				
				ip = args[1].split(":");
				port = Integer.parseInt(ip[1]);
					
				Socket cSoc = new Socket(ip[0],port);
				ObjectOutputStream outStream = new ObjectOutputStream(cSoc.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(cSoc.getInputStream());
				
				//gives the server the number of args
				outStream.writeObject(args.length);
				
				//warn the server that -p is a parameter
				outStream.writeObject(param_p);
				
				/*autenticate user*/
				//sends user name to the server
				outStream.writeObject(args[0]);
				
				//obtains response of the server
				boolean exists = (boolean) inStream.readObject();
				
				boolean autentic = false;
				
				if (!exists) {
					
					System.out.println("-- O utilizador " + args[0] + " vai ser criado ");
					String pass = null;
					if(param_p)
						pass = confirmPwd(args[0], args[2], args[3]);
					else 
						pass = confirmPwd(args[0], " ", " ");
					outStream.writeObject(pass);
					if((boolean) inStream.readObject())
						System.out.println("-- O utilizador " + args[0] + " foi criado");
				
				} else if (param_p) {
					
					while(!autentic){
						
						outStream.writeObject(args[3]);
						autentic = (boolean) inStream.readObject();
						if(!autentic){
							System.out.println("Password errada!");
							System.out.println("Tenta novamente:");
							outStream.writeObject(scan.nextLine());
							autentic = (boolean) inStream.readObject();
						}
						
					}
						
				} else {
						
						System.out.println("Password: ");
						outStream.writeObject(scan.nextLine());
						autentic = (boolean) inStream.readObject();
						
						while(!autentic){	
							
							if(!autentic){
								System.out.println("Password errada!");
								System.out.println("Tenta novamente:");
								outStream.writeObject(scan.nextLine());
								autentic = (boolean) inStream.readObject();
								
						}	
						
					}
					
				}

				//executes the command
				if(param_p)
					if(args.length == 7)
						executeCommand(args[4], args[5], args[6], outStream, inStream, path);
					else if(args.length > 4)
						executeCommand(args[4], args[5], "N", outStream, inStream, path);
				else
					if(args.length == 5)
						executeCommand(args[2], args[3], args[4], outStream, inStream, path);
					else if(args.length > 2)
						executeCommand(args[2], args[3], "N", outStream, inStream, path);
				
				cSoc.close();
				scan.close();
			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
				
	}
	
	private void removeRep() {
		// TODO Auto-generated method stub
		
	}

	private int pushFile(ObjectOutputStream  outStream, ObjectInputStream inStream, String fileName, Path path) throws IOException, ClassNotFoundException {
		int result = -1; 
		File file = new File(path + "/" + fileName);
		
		String[] name = new String[1];
		name[0] = fileName;
		
		Date[] dates = new Date[1];
		Date date = new Date(file.lastModified());
		dates[0] = date;
		
		Message messOut = new Message("pushFile", name, null, dates, null);
		Message messIn = null;
		
		outStream.writeObject(messOut);
		
		messIn = (Message) inStream.readObject();
		
		if (messIn == null)
			result = -1;
		else if (messIn.toBeUpdated[0] == true){
			sendFile(outStream, inStream, file);
			result = 0;
		} else
			result = 0;
		
		return result;		
	}

	
	private void pushRep() {
		
		
	}

	private void pullFile() {
		// TODO Auto-generated method stub
		
	}

	private void pullRep() {
		// TODO Auto-generated method stub
		
	}

	private void shareRep() {
		// TODO Auto-generated method stub
		
	}

	public boolean sendFile(ObjectOutputStream  outStream, ObjectInputStream inStream, File file) throws IOException {
		boolean result = false;
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
	
	public void executeCommand(String command, String param1, String param2, ObjectOutputStream  outStream, ObjectInputStream inStream, Path path) throws IOException, ClassNotFoundException{
		
		switch (command) {
		
			case "-push":
				if (param1.contains(".")) {
					System.out.println("asd");
					pushFile(outStream, inStream, param1, path);
				}
				else
					pushRep();
				
				break;
			
			case "-pull":
				if (param1.contains("."))
					pullFile();
				else
					pullRep();
				
				break;
				
			case "-share":
				shareRep();
				break;
				
			case "-remove":
				removeRep();
				break;
				
			default:
				System.out.println("Esse commando não existe!");
				break;
			
		}
		
	}

}