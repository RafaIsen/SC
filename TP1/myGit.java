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
import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;

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
		java.nio.file.Path path = java.nio.file.Paths.get(myGitPath);
		
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
						executeCommand(args[4], args[5], args[6]);
					else if(args.length > 4)
						executeCommand(args[4], args[5], "N");
				else
					if(args.length == 5)
						executeCommand(args[2], args[3], args[4]);
					else if(args.length > 2)
						executeCommand(args[2], args[3], "N");
				
				cSoc.close();
			
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//autenticate(scan, outStream, inStream);
		
		//sendFile(outStream, inStream);
		
	}
	
	private void removeRep() {
		// TODO Auto-generated method stub
		
	}

	private void pushFile(ObjectOutputStream  outStream, ObjectInputStream inStream) throws IOException {
		outStream.writeObject("pushFile ya");
	}

	private void pushRep() {
		// TODO Auto-generated method stub
		
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

	public boolean sendFile(ObjectOutputStream  outStream, ObjectInputStream inStream) throws IOException {
		boolean result = false;
		File pdf = new File("C:\\Users\\HP\\Desktop\\a.pdf");
		int lengthPdf = (int) pdf.length();
		byte[] buf = new byte[1024];
        FileInputStream is = new FileInputStream(pdf);
        
        outStream.writeInt(lengthPdf);
        
        int n = 0;
        
        while(((n = is.read(buf, 0, buf.length)) != -1)) {
        	outStream.write(buf, 0, n);
        	outStream.flush();        
        }
        
        is.close();
		inStream.close();
		outStream.close();
		return result;
	}
	
	public String confirmPwd(String username, String command, String pwd){
		
		Scanner scan = new Scanner(System.in);
		String pass = " ";
		String passConf = " ";
		
		if (command.equals("-p")) {
			
			System.out.println("Confirmar password do utilizador " + username + ":");
		
			passConf = scan.nextLine();
			
			while(!pass.equals(pwd) && !pass.equals(passConf)) {
				
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
		
		return pass;
		
	}
	
	public void executeCommand(String command, String param1, String param2){
		
		switch (command) {
		
			case "-push":
				if (param1.contains(".")) {
					System.out.println("asd");
					//pushFile(outStream, inStream);
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