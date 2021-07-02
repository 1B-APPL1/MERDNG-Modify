/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.erdesignerng.visual.common;

import static javax.swing.JOptionPane.showMessageDialog;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author Bima
 */

public class GetReciever{
    private String reciever;
    private JTextField field1 = new JTextField("");
    private JPanel panel = new JPanel(new GridLayout(0, 1));

    public Boolean statusPop = false;

   
    public GetReciever(){
        //
    }

    public String showPop(){
        panel.add(new JLabel("Email : "));
        panel.add(field1);

        int result = JOptionPane.showConfirmDialog(null, panel, "Masukkan Email Penerima : ",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            reciever = field1.getText();
            //showMessageDialog(null, "Penerima Masuk !" + reciever);
            return reciever;
        }
        else {
            //showMessageDialog(null, "Cancelled");
            return null;
        }
    }
}