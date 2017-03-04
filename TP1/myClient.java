/***************************************************************************
*   Seguranca e Confiabilidade 2016/17
*
*
***************************************************************************/

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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

	public void startClient() throws IOException, ClassNotFoundException{
		Socket cSoc = new Socket("127.0.0.1",23456);
		
		ObjectOutputStream outStream = new ObjectOutputStream(cSoc.getOutputStream());
		ObjectInputStream inStream = new ObjectInputStream(cSoc.getInputStream());
		
		Scanner keyb = new Scanner(System.in);
		
		boolean resposta = false;
		
		while(resposta == false){
			System.out.println("Username: ");
			String user = keyb.nextLine();
			outStream.writeObject(user);
			
			System.out.println("Password: ");
			String pass = keyb.nextLine();
			outStream.writeObject(pass);
			
			resposta = (boolean) inStream.readObject();
			
			if(resposta == true) 
				System.out.println("Entraste!");
			else
				System.out.println("Erro! Tenta novamente:");
		}
		
		File pdf = new File("C:\\Users\\rafae\\Desktop\\SC\\a.pdf");
		int lengthPdf = (int) pdf.length();
		byte[] buf = new byte[1024];
        FileInputStream is = new FileInputStream(pdf);
        
        outStream.writeInt(lengthPdf);
        
        int n = 0;
        
        while(((n = is.read(buf, 0, buf.length)) != -1)){
        	outStream.write(buf, 0, n);
        	outStream.flush();        
        }
        
        is.close();
		keyb.close();
		inStream.close();
		outStream.close();
		cSoc.close();
	}
	
}