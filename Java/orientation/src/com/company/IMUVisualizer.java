package com.company;

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;

public class IMUVisualizer {

    GUI theGUI;

    public static void main(String[] args) throws IOException{

        IMUVisualizer visualizer = new IMUVisualizer();

    }

    public IMUVisualizer()
    {
        theGUI = new GUI();

        System.out.println("Opening socket");

        handleConnection();
    }

    private void handleConnection()
    {
        try (ServerSocket listener = new ServerSocket(9090)) {
            while (true) {
                try (Socket socket = listener.accept()) {

                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String msg;
                    while(true)
                    {
                        while( (msg = in.readUTF()) != null )
                        {
                            Transform transform = parseMessage(msg);
                            theGUI.setRotationMatrix(transform);
                        }
                    }

                    //String msg = in.readUTF();



                } catch (Exception e)
                {
                    e.printStackTrace();
                }

                if( listener.isClosed() )
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Transform parseMessage(String msg)
    {
        System.out.println(msg);
        System.out.flush();
        String[] arguments = msg.split("/");

        Transform transform = new Transform(Double.parseDouble(arguments[1]),
                Double.parseDouble(arguments[2]),
                Double.parseDouble(arguments[3]) );

        if (arguments[0].equals("relative"))
        {
            transform.mode = Transform.Mode.RELATIVE;
        }
        else if ( arguments[0].equals("absolute"))
        {
            transform.mode = Transform.Mode.ABSOLUTE;
        }

        //System.out.println(transform);
        return transform;
    }


}
