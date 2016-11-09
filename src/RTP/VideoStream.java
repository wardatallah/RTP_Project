/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RTP;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Eng.Orwa Nader
 */
public class VideoStream {

    FileInputStream fis;
    int frame_nb;
    private byte[] frame;
    // we need to store previously read frames in order to move backwards
  
    // -----------------------------------
    // constructor
    // -----------------------------------
    public VideoStream(String filename) throws Exception {
        fis = new FileInputStream(new File(filename));
        frame_nb = 0;

    }

    // -----------------------------------
    // getnextframe
    // returns the next frame as an array of byte and the size of the frame
    // -----------------------------------
   
    public byte[] getnextframe() throws Exception {
        int length = 0;
        String length_string;
        byte[] frame_length = new byte[5];

        if (fis.available() == 0) {
            return null;
        }
        fis.read(frame_length, 0, 5);
        length_string = new String(frame_length);
        length = Integer.parseInt(length_string);
        frame = new byte[length];
        fis.read(frame, 0, length);
     
        return frame;
    }
}
