package client;

import server.EmailSessionManager;

import javax.mail.*;

public class EmailReceiver {
   public static Message[] receiveEmail() throws MessagingException {
       EmailSessionManager manager = EmailSessionManager.getInstance();
       Message[] messages = manager.receiveEmail();
       return messages;
   }
}
