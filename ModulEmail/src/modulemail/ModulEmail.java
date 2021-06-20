/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modulemail;

import java.io.UnsupportedEncodingException;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

/**
 *
 * @author Bima
 */
public class ModulEmail {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws UnsupportedEncodingException {
        // TODO code application logic here
        // change accordingly
        
        final String username = "bmaputrazzzz@gmail.com";   
          
        // change accordingly
        final String password = "passwordnyaapaya, jgn liat anjay";
          
        // or IP address
        final String host = "localhost"; 
  
        // Get system properties
        Properties props = new Properties();             
          
        // enable authentication
        props.put("mail.smtp.auth", "true");               
          
        // enable STARTTLS
        props.put("mail.smtp.starttls.enable", "true");    
          
        // Setup mail server
        props.put("mail.smtp.host", "smtp.gmail.com");     
          
        // TLS Port
        props.put("mail.smtp.port", "587");         
        
        // Alias Mail
        props.put("mail.from", username); //blabla@mail.com
        props.put("mail.from.alias", "SYSTEM [BIMA] | Don't Reply This Message !");//"joao Ninguem"
  
        // creating Session instance referenced to 
        // Authenticator object to pass in 
        // Session.getInstance argument
        Session session = Session.getInstance(props,
          new javax.mail.Authenticator() {
             
            //override the getPasswordAuthentication method
            protected PasswordAuthentication 
                           getPasswordAuthentication() {
                                         
                return new PasswordAuthentication(username, 
                                                 password);
            }
          });
  
        try {
              
            // compose the message
            // javax.mail.internet.MimeMessage class is 
            // mostly used for abstraction.
            Message message = new MimeMessage(session);    
              
            // header field of the header.
            message.setFrom(new InternetAddress(session.getProperty("mail.from"), session.getProperty("mail.from.alias"), "UTF8"));              
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse("bimaputras.sz14@gmail.com"));
            message.setSubject("Extacted File Image MOGWAI ERD Design");
            message.setText("Ceritanya isinya email nya cenah ayeyeyeye");
  
            Transport.send(message);         //send Message
  
            System.out.println("Done");
  
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
    
}
