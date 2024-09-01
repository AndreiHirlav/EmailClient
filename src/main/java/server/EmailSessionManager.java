package server;

import javax.mail.*;
import java.util.*;
public class EmailSessionManager {
    private Session emailSession;
    private Store store;
    private Folder emailFolder;
    private static EmailSessionManager instance;
    private static String currentUsername = "";
    private static String currentPassword = "";

    //follows a singleton pattern, hence the private constructor
    private EmailSessionManager(String username, String password) throws MessagingException {
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", "imap.gmail.com");
        properties.put("mail.imaps.port", "993");
        properties.put("mail.imaps.ssl.enable", "true");
        this.emailSession = Session.getInstance(properties, null);
        this.store = emailSession.getStore("imaps");
        this.store.connect(username, password);

        //store the credentials after a successful connection
        currentUsername = username;
        currentPassword = password;
    }

    //to get the singleton instance of this class before logging-in
    public static EmailSessionManager getInstance(String username, String password) throws MessagingException {
        if (instance == null) {
            instance = new EmailSessionManager(username, password);
            System.out.println("server.EmailSessionManager initialized");
        }
        return instance;
    }

    //to acces the instance after the login has occured
    public static EmailSessionManager getInstance() throws IllegalStateException {
        if (instance == null) {
            throw new IllegalStateException("server.EmailSessionManager is not initialied. Please login first.");
        }

        return instance;
    }

    public static String getUsername() {
        return currentUsername;
    }

    public static String getPassword() {
        return currentPassword;
    }


    //method used to fetch the emails from the inbox
    public Message[] receiveEmail() throws MessagingException {
        if(emailFolder == null || !emailFolder.isOpen()) {
            emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);
        }

        return emailFolder.getMessages();
    }


    //method which closes the emailFolder and store
    public void close() throws MessagingException {
        if (emailFolder != null) {
            emailFolder.close(false);
            emailFolder = null;
        }

        if(store != null) {
            store.close();
            store = null;
        }
        instance = null;

        currentPassword = "";
        currentUsername = "";
    }
}
