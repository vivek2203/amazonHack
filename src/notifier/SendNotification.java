package notifier;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

import com.Messages;
import com.opencsv.CSVReader;

public class SendNotification {

	public static  void main(String args[]){
		
		SendNotification sn=new SendNotification();
		sn.notificationListener();
	}
	
	public void notificationListener() {
		System.out.println("in listerenr func"); //$NON-NLS-1$
		String sharedPath = Messages.getString("FileListener.NOTIFY_LISTENER_PATH"); //$NON-NLS-1$
		WatchService watcher = null;
		HashMap<String, HashMap> outerMap = new HashMap<String, HashMap>();
		Path dir = Paths.get(sharedPath);

		try {

			watcher = FileSystems.getDefault().newWatchService();
			WatchKey key = dir.register(watcher, ENTRY_MODIFY);// ,
																// ENTRY_DELETE,ENTRY_MODIFY);
			// }
		} catch (IOException x) {
			System.err.println(x);
		}
		for (;;) {

			// wait for key to be signaled
			WatchKey key;
			try {

				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();

				// This key is registered only
				// for ENTRY_CREATE events,
				// but an OVERFLOW event can
				// occur regardless if events
				// are lost or discarded.
				if (kind == OVERFLOW) {
					continue;
				}

				// The filename is the // context of the event.
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path filename = ev.context();

				try {
					Path child = dir.resolve(filename);
					// Files.probeContentType(child) check null

					if (child != null
							&& !Messages.getString("NotifyListener.FILE_EXTN").equals(Files.probeContentType(child))) { //$NON-NLS-1$
						System.out.format("New file '%s'" //$NON-NLS-1$
								+ " is a csv file .%n", filename); //$NON-NLS-1$


						buildContent(child);
	
				
						
						continue;
					}
				} catch (IOException x) {
					System.err.println(x);
					continue;
				}
			}

			// Reset the key -- this step is critical if you want to
			// receive further watch events. If the key is no longer valid,
			// the directory is inaccessible so exit the loop.
			boolean valid = key.reset();
			if (!valid) {
				break;
			}
		}
	}
	


  public String buildContent(Path filePath)
  {	  

		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(filePath.toString()));
			
			String[] nextLine;
			int count = 0;
			while ((nextLine = reader.readNext()) != null) {
				sendEmail(nextLine);	
			
			}
			
		}catch (Exception e){
			System.out.println(e.getMessage());
		}
		
	  return "Hackathon Subscriber";
  }
	
	
   public void sendEmail(String[] data)
   {    
	   StringBuffer content=new StringBuffer();
	   content.append("UserId=");
	   content.append(data[0]);
	   content.append("\n");
	   content.append("Requested Details : ");
	   content.append("\n");
	   content.append(data[1]);
	   content.append("  : ");
	   content.append(data[2]);
	   content.append("\n");
	   String to = data[3];// "amazonhacktest@gmail.com";	   
	   
      // Recipient's email ID needs to be mentioned.
      if( data[3]==null){
    	  to="amazonhacktest@gmail.com";
      }
	   
      
      // Sender's email ID needs to be mentioned
      String from = "amazonhacktest@gmail.com";

      // Assuming you are sending email from localhost
      String host = "smtp.gmail.com";

      // Get system properties
      Properties properties = System.getProperties();
      System.out.println("after getting prop");
      // Setup mail server
      properties.setProperty("mail.smtp.host", host);
      properties.setProperty("mail.smtp.port", "465");
      
      properties.put("mail.smtp.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
      
      //properties.setProperty("mail.smtp.socketFactory.port", "465");
      properties.put("mail.smtp.starttls.enable", "true");
      properties.put("mail.smtp.auth", "true");
      
      // Get the default Session object.
      //Session session = Session.getDefaultInstance(properties);

      Session session = Session.getDefaultInstance(properties, new Authenticator() {

          protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(
                      "amazonhacktest@gmail.com","amazon123");// Specify the Username and the PassWord
          }

      });
      
      
      try{
         // Create a default MimeMessage object.
         MimeMessage message = new MimeMessage(session);

         // Set From: header field of the header.
         message.setFrom(new InternetAddress(from));

         // Set To: header field of the header.
         message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        
         // Bulk Mails 
        /*void addRecipients(Message.RecipientType type, Address[] addresses)throws MessagingException{ 
        	 throw new MessagingException("Message cannot be sent");
         };*/

         // Set Subject: header field
         message.setSubject("Notification for Price");

         // Now set the actual message
         message.setText(content.toString());

         // Send message
         Transport.send(message);
         System.out.println("Sent message successfully....");
      }catch (MessagingException mex) {
         mex.printStackTrace();
      }
   }

}