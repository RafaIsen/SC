/***************************************************************************
*   Seguranca e Confiabilidade 2016/17
*
*
***************************************************************************/

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;

//Cliente myGit

public class myGit{
	
	public static void main(String[] args) throws IOException, ClassNotFoundException{
		System.out.println("cliente: main");
		myGit client = new myGit();
		client.startClient();
	}

	public void startClient() throws ClassNotFoundException, IOException{
			
		Scanner scan = new Scanner(System.in);
		Socket cSoc = null;
		ObjectOutputStream outStream = null;
		ObjectInputStream inStream = null;
		int continua = 1;
		
		while (continua == 1) {
			
			System.out.println("O seu comando ---->>>");
			String in = scan.nextLine();
			String[] split = in.split(" ");
			String[] ip = null;
			int port = 0;
			
			
			if (split[0].equals("-init"))
				initRep(split[1]);
			else {	
				
				if (split[2] != null) {
					ip = split[1].split(":");
					port = Integer.parseInt(ip[1]);
					
					try {
						if (cSoc == null) {
							cSoc = new Socket(ip[0],port);
							outStream = new ObjectOutputStream(cSoc.getOutputStream());
							inStream = new ObjectInputStream(cSoc.getInputStream());
						}
					} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					}
					
					switch (split[2]) {
					
						case "-push":
							if (split[3].contains(".")) {
								pushFile(outStream, inStream);
							}
							else
								pushRep();
							
							break;
						
						case "pull":
							if (split[3].contains("."))
								pullFile();
							else
								pullRep();
							
							break;
							
						case "-share":
							shareRep();
							break;
							
						case "remove":
							removeRep();
							break;
							
						default:
							System.out.println("Comando errado");
							break;
					}
				}
			}
		}
		
		//autenticate(scan, outStream, inStream);
		
		//sendFile(outStream, inStream);
		
		
		scan.close();
		cSoc.close();
		
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
	
	public int autenticate(Scanner keyb, ObjectOutputStream  outStream, ObjectInputStream inStream) throws IOException, ClassNotFoundException {
		int result = -1;
		boolean resposta = false;
				
				while (resposta == false) {
					System.out.println("Username: ");
					String user = keyb.nextLine();
					outStream.writeObject(user);
					
					System.out.println("Password: ");
					String pass = keyb.nextLine();
					outStream.writeObject(pass);
					
					resposta = (boolean) inStream.readObject();
					
					if (resposta == true) { 
						System.out.println("Entraste!");
						result = 0;
					}
					else
						System.out.println("Erro! Tenta novamente:");
				}
		return result;
	}
	
	public void initRep(String repName) throws IOException {
		Path dir = Paths.get("C:\\Users\\rafae\\Desktop\\SC\\Trabalho1\\" + repName);
		Files.createDirectory(dir);
	}
	
}