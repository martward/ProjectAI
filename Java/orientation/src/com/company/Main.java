package com.company;

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;

public class Main {

    public static void main(String[] args) throws IOException{

        System.out.println("Waiting for connection");
        System.out.println("blaaaaa");
        try (ServerSocket listener = new ServerSocket(9090)) {
            while (true) {

                try (Socket socket = listener.accept()) {

                    DataInputStream in = new DataInputStream(socket.getInputStream());

                    System.out.println(in.readUTF());

                }

                if( listener.isClosed() )
                    break;
            }
        }
    }
}
