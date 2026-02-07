package com.mycompany.psr;

import com.mycompany.UI.ventanaPrincipal;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class Cliente {

    private final String HOST = "192.168.1.133";
    private final int PUERTO = 5000;
    private DataInputStream in;
    private DataOutputStream out;
    private ventanaPrincipal VP;

    public Cliente() {
        try {
            Socket sc = new Socket(HOST, PUERTO);
            in = new DataInputStream(sc.getInputStream());
            out = new DataOutputStream(sc.getOutputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void enviarDato(String dato){
        try{
            out.writeUTF(dato);
        }catch(IOException ex){
            System.out.println("Error al enviar el dato");
        }
    }

    public String recibirDato() throws IOException {
        return in.readUTF();
    }
}
   
