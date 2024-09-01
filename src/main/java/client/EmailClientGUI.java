package client;

import server.EmailSessionManager;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class EmailClientGUI extends JFrame{
    private JTextField usernameField = new JTextField(20);
    private JPasswordField passwordField = new JPasswordField(20);
    private DefaultListModel<String> emailListModel = new DefaultListModel<>();
    private JList<String> emailList = new JList<>(emailListModel);  //component to display emails
    private JTextArea emailContent = new JTextArea();
    private Message[] messages;
    private static final Color BACKGROUND_COLOR = new Color(230, 240, 250);
    private static final Color ACTION_PANEL_COLOR = new Color(200, 220, 240);
    private static final Color BUTTON_COLOR = new Color(180, 220, 240);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font EMAIL_LIST_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font EMAIL_CONTENT_FONT = new Font("SansSerif", Font.PLAIN, 14);
    public EmailClientGUI() {
        setTitle("Java Email Client");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initUI();
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (EmailSessionManager.getInstance() != null) {
                        EmailSessionManager.getInstance().close();
                    }
                } catch (MessagingException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void initUI() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setOneTouchExpandable(true);

        emailList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        emailList.addListSelectionListener(this::emailListSelectionChanged);
        emailList.setFont(EMAIL_LIST_FONT);
        JScrollPane listScrollPane = new JScrollPane(emailList);
        listScrollPane.setBackground(BACKGROUND_COLOR);

        emailContent.setEditable(false);
        emailContent.setFont(EMAIL_CONTENT_FONT);
        JScrollPane contentScrollPane = new JScrollPane(emailContent);
        contentScrollPane.setBackground(BACKGROUND_COLOR);

        splitPane.setLeftComponent(listScrollPane);
        splitPane.setRightComponent(contentScrollPane);

        getContentPane().setBackground(BACKGROUND_COLOR);
        getContentPane().add(splitPane, BorderLayout.CENTER);

        JButton replyButton = new JButton("Reply");
        JButton forwardButton = new JButton("Forward");
        replyButton.setFont(BUTTON_FONT);
        forwardButton.setFont(BUTTON_FONT);
        replyButton.setBackground(BUTTON_COLOR);
        forwardButton.setBackground(BUTTON_COLOR);

        replyButton.addActionListener(e -> prepareEmailAction("Reply"));
        forwardButton.addActionListener(e -> prepareEmailAction("Forward"));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(ACTION_PANEL_COLOR);
        actionPanel.add(replyButton);
        actionPanel.add(forwardButton);

        add(actionPanel, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel(new GridLayout(1, 2));
        bottomPanel.setBackground(ACTION_PANEL_COLOR);
        JButton composeButton = new JButton("Compose");
        JButton refreshButton = new JButton("Refresh Inbox");
        composeButton.setBackground(BUTTON_COLOR);
        refreshButton.setBackground(BUTTON_COLOR);
        composeButton.setFont(BUTTON_FONT);
        refreshButton.setFont(BUTTON_FONT);

        composeButton.addActionListener(e -> showComposeDialog("", "", ""));
        refreshButton.addActionListener(e -> refreshInbox());
        bottomPanel.add(composeButton);
        bottomPanel.add(refreshButton);
        add(bottomPanel, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(this::showLoginDialog);
    }

    private void showComposeDialog(String to, String subject, String body) {
        JDialog composeDialog = new JDialog(this, "Compose Email", true);
        composeDialog.setLayout(new BorderLayout(5, 5));
        composeDialog.getContentPane().setBackground(BACKGROUND_COLOR);

        Box fieldsPanel = Box.createVerticalBox();
        fieldsPanel.setBackground(BACKGROUND_COLOR);

        JTextField toField = new JTextField(to);
        toField.setFont(EMAIL_CONTENT_FONT);
        JTextField subjectField = new JTextField(subject);
        subjectField.setFont(EMAIL_CONTENT_FONT);
        JTextArea bodyArea = new JTextArea(10, 20);
        bodyArea.setText(body);
        bodyArea.setFont(EMAIL_CONTENT_FONT);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);

        JLabel toLabel= new JLabel("To: ");
        toLabel.setFont(BUTTON_FONT);
        fieldsPanel.add(toLabel);
        fieldsPanel.add(toField);
        JLabel subjectLabel= new JLabel("Subject: ");
        subjectLabel.setFont(BUTTON_FONT);
        fieldsPanel.add(subjectLabel);
        fieldsPanel.add(subjectField);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(BACKGROUND_COLOR);

        JButton attachButton = new JButton("Attach Files");
        attachButton.setFont(BUTTON_FONT);
        attachButton.setBackground(BUTTON_COLOR);

        JButton sendButton = new JButton("Send");
        sendButton.setFont(BUTTON_FONT);
        sendButton.setBackground(BUTTON_COLOR);

        JLabel attachedFilesLabel = new JLabel("No files attached");
        attachedFilesLabel.setFont(BUTTON_FONT);

        List<File> attachedFiles = new ArrayList<>();
        attachButton.addActionListener(e -> {
            File[] files = AttachmentChooser.chooseAttachments();
            attachedFiles.addAll(Arrays.asList(files));
            attachedFilesLabel.setText(attachedFiles.size() + " files attached");
        });

        sendButton.addActionListener(e -> {
            EmailSender.sendEmailWithAttachment(toField.getText(), subjectField.getText(), bodyArea.getText(), attachedFiles.toArray(new File[0]));
            composeDialog.dispose();
        });

        bottomPanel.add(attachButton);
        bottomPanel.add(sendButton);

        composeDialog.add(fieldsPanel, BorderLayout.NORTH);
        composeDialog.add(new JScrollPane(bodyArea), BorderLayout.CENTER);
        composeDialog.add(bottomPanel, BorderLayout.SOUTH);

        composeDialog.pack();
        composeDialog.setLocationRelativeTo(this);
        composeDialog.setVisible(true);
    }

    private void emailListSelectionChanged(ListSelectionEvent listSelectionEvent) {  //when a user selects and email from the list, the application fetches/displays the email
        if(!emailList.getValueIsAdjusting() && emailList.getSelectedIndex() != -1) {
            try {
                Message selectedMessage = messages[emailList.getSelectedIndex()];
                emailContent.setText(""); //Clear previous content
                emailContent.append("Subject:" + selectedMessage.getSubject() + "\n\n");
                emailContent.append("From: " + InternetAddress.toString(selectedMessage.getFrom()) + "\n\n");
                emailContent.append(getTextFromMessage(selectedMessage));
            } catch(MessagingException | IOException ex) {
                emailContent.setText("Error reading email content: " + ex.getMessage());
            }
        }
    }

    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        if(message.isMimeType("text/plain")) {
            return (String) message.getContent();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            for(int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if(bodyPart.isMimeType("text/plain")) {
                    return (String) bodyPart.getContent();
                }
            }
        }

        return "No readable content found.";
    }

    private void showLoginDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.setBackground(BACKGROUND_COLOR);

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(BUTTON_FONT);
        panel.add(emailLabel);
        usernameField.setFont(EMAIL_LIST_FONT);
        panel.add(usernameField);

        JLabel passwordLabel = new JLabel("App Password:");
        passwordLabel.setFont(BUTTON_FONT);
        panel.add(passwordLabel);
        passwordField.setFont(EMAIL_LIST_FONT);
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        System.out.println("Dialog result: " + result);
        if(result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            try {
                EmailSessionManager.getInstance(username, password);
                System.out.println("Login successfull");
                SwingUtilities.invokeLater(this::refreshInbox);

            } catch (MessagingException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Failed to initialize email session: " + e.getMessage(), "Login Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        } else {
            System.out.println("Login cancelled");
        }
    }

    private void refreshInbox() {
        try {
            messages = EmailSessionManager.getInstance().receiveEmail();
            emailListModel.clear();
            for (Message message : messages) {
                emailListModel
                        .addElement(message.getSubject() + " - From: " + InternetAddress.toString(message.getFrom()));
            }
        } catch (MessagingException e) {
            JOptionPane.showMessageDialog(this, "Failed to fetch emails: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void prepareEmailAction(String actionType) {
        if(emailList.getSelectedIndex() == -1) {
            JOptionPane.showMessageDialog(this, "No email selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Message selectedMessage = messages[emailList.getSelectedIndex()];
            String to = actionType.equals("Reply") ? InternetAddress.toString(selectedMessage.getFrom()) : "";
            String subjectPrefix = actionType.equals("Reply") ? "Re: " : "Fwd: ";
            String subject = subjectPrefix + selectedMessage.getSubject();
            String body = getTextFromMessage(selectedMessage);

            showComposeDialog(to, subject, body);
        } catch (MessagingException | IOException ex) {
            JOptionPane.showMessageDialog(this, "Error preparing email action.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EmailClientGUI());
    }
}
