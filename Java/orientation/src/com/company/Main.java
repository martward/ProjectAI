package com.company;

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.Buffer;
import java.util.Date;

public class Main {

    public static void main(String[] args) throws IOException{

        ServerSocket listener = new ServerSocket(9090);
        System.out.println("Waiting for connection");
        System.out.println("blaaaaa");
        try {
            while (true) {

                Socket socket = listener.accept();
                try {
                    //BufferedReader in =
                    //      new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //System.out.println("blablabla");
                    //System.out.println(socket.getInputStream());
                    DataInputStream in = new DataInputStream(socket.getInputStream());

                    System.out.println(in.readUTF());

                } finally {
                    socket.close();
                }
            }
        }
        finally {
            listener.close();
        }
    }

}
