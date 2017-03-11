/***************************************************************************
*   Seguranca e Confiabilidade 2016/17
*
*
***************************************************************************/

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;

//Cliente myClient

public class myClient{
	
	public static void main(String[] args) throws IOException, ClassNotFoundException{
		System.out.println("cliente: main");
		myClient client = new myClient();
		client.startClient();
	}

	public void startClient() throws ClassNotFoundException, IOException{
			
		Scanner scan = new Scanner(System.in);
		Socket cSoc = null;
		ObjectOutputStream outStream = null;
		ObjectInputStream inStream = null;
		
		
		while(true) {
			String[] split = scan.nextLine().split(" ");
			String[] ip = split[1].split(":");
			int port = Integer.parseInt(ip[1]);
			
			if(split[0].equals("-init"))
				initRep();
			else {
							
				try {
					cSoc = new Socket(ip[0],port);
					outStream = new ObjectOutputStream(cSoc.getOutputStream());
					inStream = new ObjectInputStream(cSoc.getInputStream());
					//PrintWriter pw = new PrintWriter(socket1.getOutputStream(), true);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				switch (split[2]) {
				
					case "-push":
						if (split[3].contains("."))
							pushFile();
						else
							pushRep();
						break;
					
					case "-pull":
						if (split[3].contains("."))
							pullFile();
						else
							pullRep();
						break;
						
					case "-share":
						shareRep();
						break;
						
					default:
						System.out.println("Comando errado");
						break;
				}
			}
			
			break;		
		}
		
		autenticate(scan, outStream, inStream);
		
		sendFile(outStream, inStream);
		
		//initRep();
		scan.close();
		cSoc.close();
		
	}
	
	private void pushFile() {
		// TODO Auto-generated method stub
		
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
	
	public boolean initRep() throws IOException {
		
		Path dir = Paths.get("C:\\Users\\HP\\Documents\\SC1\\1");
		Files.createDirectory(dir);
		return false;
		
	}
	
}