/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

import javax.swing.Timer;

/**
 *
 * @author Eng.Orwa Nader
 */
public class Server implements ActionListener {
    static ServerLog logger;
    static BufferedReader BR;
    static BufferedWriter BW;
    static InetAddress client_ip;
    // states..............
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;

    static String URL = "";
    // request messages......
    final static int SETUP = 4;
    final static int PLAY = 5;
    final static int TEARDOWN = 7;
    final static int OK = 9;
    
    // video Information
    final static int MJPEG_TYPE = 26;
    final static int FRAME_PERIOD = 100;
    final static int VIDEO_LENGTH = 500;

    // server components
    Timer timer;
    byte[] buf;
    int state;
    Socket RTSPsocket;
    DatagramSocket RTPsocket;
    DatagramPacket send_p;
    String VideoFileName;
    RTP.VideoStream video;
    int SeqNb = 0;
    int RTP_dest_port = 0;
    int sessionID = 123456;
    int imagenb = 1;
    String names = "movie.Mjpeg,D:/movie1.Mjpeg,qqq.mjpeg,movie3.Mjpeg,movie4.Mjpeg";
    // Add you own path to movies here
    String urls = " , , , ";

    public Server(Socket s) {
        RTSPsocket = s;
        // buf = new byte[15000];
        timer = new Timer(10, this);
        timer.setInitialDelay(0);
        timer.setCoalesce(true);
        imagenb = 0;
        SeqNb = 0;
    }

    // the use of the code that sends the data packets in different locations
    // has to led to the creation of this function
    private void writeFrame(int index, byte[] data) {
        try {
            RTP.RTPpacket rtp_packet = new RTP.RTPpacket(MJPEG_TYPE, index,
                    index * FRAME_PERIOD, data, data.length);
            int packet_length = rtp_packet.getlength();
            logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+ "Packet Length: " + packet_length + " Timestamp: " + rtp_packet.gettimestamp());
            System.out.println("Packet Length: " + packet_length
                    + " Timestamp: " + rtp_packet.gettimestamp());
            byte[] packet_bits = new byte[packet_length];
            rtp_packet.getpacket(packet_bits);
            send_p = new DatagramPacket(packet_bits, packet_length, client_ip,
                    RTP_dest_port);

            RTPsocket.send(send_p);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+"Send frame # " + imagenb);
        System.out.println("Send frame #" + imagenb);
    }

    private void startServer() {
        try {
            client_ip = RTSPsocket.getInetAddress();
            logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+"Accept Client Socket - Client IP : "
                    + client_ip.toString());
            System.out.println("Accept Client Socket - Client IP : "
                    + client_ip.toString());

            BR = new BufferedReader(new InputStreamReader(
                    RTSPsocket.getInputStream()));
            BW = new BufferedWriter(new OutputStreamWriter(
                    RTSPsocket.getOutputStream()));

            state = INIT;
                logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+"state:"+state);
            System.out.println("State : " + state);
            int request_type = parse_RTSP_request();

            if (request_type == OK) {
                state = INIT;
                logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+"sending video information");
                System.out.println("sending video information");
                try {
                    send_RTSp_response(true);
                    BW.write(names);
                    BW.newLine();
                    BW.flush();
                    BW.write(urls);
                    BW.newLine();
                    BW.flush();
                } catch (Exception e) {
                    logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+"cannot send the video info");
                    System.out.println("cannot send the video info");
                    e.printStackTrace();
                }

            }

            while (true) {
                request_type = parse_RTSP_request();

                if (request_type == SETUP) {
                    state = READY;
                    logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+"State: READY");
                    System.out.println("State: READY");
                    send_RTSp_response(true);
                    video = new RTP.VideoStream(VideoFileName);
                    RTPsocket = new DatagramSocket();
                    // request_type = PLAY;
                }// states: Paused,Fast Playing and playing can handle a direct
                // backward request and thus shifting to back playing status
                else if ((request_type == PLAY)
                        && state == READY) {
                    // to start playing we have to manually reset imagenb to 0
                    imagenb = 0;
                    send_RTSp_response(true);
                    timer.start();
                    state = PLAYING;
                    logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+"State: PLAYING");
                    System.out.println("State: PLAYING");
                } else if (request_type == PLAY && state == PLAYING) {
                    //incase we want to play while we are playing
                    send_RTSp_response(false);
                } else if (request_type == TEARDOWN) {
                    send_RTSp_response(true);
                    timer.stop();
                    RTPsocket.close();
                    state = -1;
                    break;
                }

            }
        } catch (Exception e) {
            logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+"Connection terminated");
            System.out.println("Connection terminated");
        }
    }

    public static void main(String args[]) throws Exception {
        Server theServer;
        int port = 2500;
        ServerSocket ss = new ServerSocket(port);
        logger=new ServerLog();
        logger.setVisible(true);
        logger.setLocation(800, 50);
        
        while (true) {
            logger.jTextArea1.setText( "Ready For Clients");
            System.out.println("Ready For Clients");
            Socket RTSPsocket = ss.accept();
            theServer = new Server(RTSPsocket);
            //start
            theServer.startServer();
        }
    }

    private int parse_RTSP_request() {
        int request_type = -1;
        try {
            String RequestLine = BR.readLine();
            logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+ "RTSP Server - Received from Client:");
            System.out.println("RTSP Server - Received from Client:");
            logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+ RequestLine);
            System.out.println(RequestLine);
            StringTokenizer tokens = new StringTokenizer(RequestLine);
            String request_type_string = tokens.nextToken();
            if (request_type_string.compareTo("OK") == 0) {
                request_type = OK;
            } else if (request_type_string.compareTo("SETUP") == 0) {
                request_type = SETUP;
            } else if (request_type_string.compareTo("PLAY") == 0) {
                request_type = PLAY;
            } else if (request_type_string.compareTo("TEARDOWN") == 0) {
                request_type = TEARDOWN;

            } 
            if (request_type == SETUP ) {
                VideoFileName = tokens.nextToken();

            }

            String SeqNumLine = BR.readLine();
            logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+ SeqNumLine);
            System.out.println(SeqNumLine);
            tokens = new StringTokenizer(SeqNumLine);
            tokens.nextToken();
            SeqNb = Integer.parseInt(tokens.nextToken());
            String LastLine = BR.readLine();
            logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+ LastLine);
            System.out.println(LastLine);
            if (request_type == OK) {
                tokens = new StringTokenizer(LastLine);
                for (int i = 0; i < 3; i++) {
                    tokens.nextToken();
                }
                RTP_dest_port = Integer.parseInt(tokens.nextToken());

            }
        } catch (Exception ex) {
            logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+ "Exception caught: " + ex);
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
        return request_type;
    }

    private void send_RTSp_response(boolean ok) {
        try {
            if (ok) {
                BW.write("RTSP/1.0 200 OK");
            } else {
                BW.write("RTSP/1.0 201 INCORRECT REQUEST");
            }
            BW.newLine();
            BW.write("CSeq: " + SeqNb);
            BW.newLine();
            BW.write("Session: " + sessionID);
            BW.newLine();
            //sending the length of the movie
            BW.write("Length: " + VIDEO_LENGTH);
            BW.newLine();
            BW.flush();
            logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+ "Server - Sent response to Client.");
            System.out.println("Server - Sent response to Client.");
        } catch (Exception ex) {
            logger.jTextArea1.setText(logger.jTextArea1.getText()+"\n"+ "Exception caught: " + ex);
            System.out.println("Exception caught: " + ex);
            System.exit(0);
        }
    }

    public void actionPerformed(ActionEvent e) {
        // no meaning of sending frames to a disconnected client
        if (RTSPsocket.isConnected()) {
            try {
                // playing stops when imagenb= video length
                buf=null;
                if (imagenb < VIDEO_LENGTH) {
                    if (state == PLAYING) {

                        buf = video.getnextframe();

                    }
                    imagenb++;
                }
                // when frame is null that means no more frames in
                // file are availabe so we use a a check value
                if (buf != null) {
                    writeFrame(imagenb, buf);

                } else {
                    timer.stop();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(0);
            }

        } else {
            timer.stop();
        }
    }
}
