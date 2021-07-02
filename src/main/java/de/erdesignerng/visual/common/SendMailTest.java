/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.erdesignerng.visual.common;

import de.erdesignerng.visual.*;
import de.erdesignerng.visual.jgraph.JGraphEditor;
import de.erdesignerng.visual.jgraph.export.ImageExporter;
import static javax.swing.JOptionPane.showMessageDialog;
import java.io.UnsupportedEncodingException;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import java.io.File;


/**
 *
 * @author Bima
 */
public class SendMailTest extends UICommand{

    private String reciever;
    private JGraphEditor editor;

    public SendMailTest(JGraphEditor ed) {
        editor = ed;
    }

    @Override
    public void execute(){
            showMessageDialog(null, "Awal export");
            editor.getImageExport(new ImageExporter("png"));
            showMessageDialog(null, "Akhir export");

            //showMessageDialog(null, "Execute Terpanggil");
            GetReciever mailFrame = new GetReciever();

            reciever = mailFrame.showPop();

            if(reciever != null){
                //showMessageDialog(null, "Last !");
                sendMail();
            }
            
    }

    public void sendMail() {
        final String username = "bmaputrazzzz@gmail.com";   
          
        // change accordingly
        final String password = "passwordnya";
          
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
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com"); 
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
          
        // TLS Port
        props.put("mail.smtp.port", "587");
        props.put("mail.debug", "true");
        
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
            try {
                message.setFrom(new InternetAddress(session.getProperty("mail.from"), session.getProperty("mail.from.alias"), "UTF8"));              
            }
            catch(UnsupportedEncodingException e){
                showMessageDialog(null, "UTF8 Fail");
                throw new AssertionError("UTF-8 is unknown");
            }
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(reciever));
            
            message.setSubject("Extacted File Image MOGWAI ERD Design");

            // This mail has 2 part, the BODY and the embedded image
            MimeMultipart multipart = new MimeMultipart("related");

            // first part (the html)
            BodyPart messageBodyPart = new MimeBodyPart();
            String htmlText = "<H3>[Do not reply this message !]</H3>" +
                "<H3>This is image that extracted by Mogwai ERD Design and sended to your email. \n</H3>" +
                "<img src=\"cid:image\">" +
                "<br><br><br><H4>This Module is Created By : Bima Putra S (191524038) | " +
                "Politeknik Negeri Bandung | D4-B Teknik Informatika '19 | bimaputras.sz14@gmail.com</H4>";
            messageBodyPart.setContent(htmlText, "text/html");
            // add it
            multipart.addBodyPart(messageBodyPart);
            // second part (the image)
            messageBodyPart = new MimeBodyPart();
            String fileLocs = System.getProperty("user.dir") + "/export.png";
            DataSource fds = new FileDataSource(fileLocs);

            messageBodyPart.setDataHandler(new DataHandler(fds));
            messageBodyPart.setHeader("Content-ID", "<image>");

            // add image to the multipart
            multipart.addBodyPart(messageBodyPart);

            // put everything together
            message.setContent(multipart);


            // Send message text (testing)
            /* 
            message.setText("[Do not reply this message ! \n " +
                    "This is image that extracted by Mogwai and sended to your email. \n");
            */

            Transport.send(message);         //send Message

            showMessageDialog(null, "Success send email to : " + reciever);

            // Delete File After Sending
            File myFile = new File(fileLocs);
            myFile.delete();
            
            //System.out.println("Done");
  
        }
        catch (MessagingException e) {
            showMessageDialog(null, e + " | Fail");
            e.printStackTrace();
        }
    }
    
}
