package client;

import javax.swing.*;
import java.io.File;
public class AttachmentChooser {
    public static File[] chooseAttachments() {
        JFileChooser fileChooser = new JFileChooser();  //creates a dialog to allow users to navigate their files and select attachments
        fileChooser.setMultiSelectionEnabled(true);
        int option = fileChooser.showOpenDialog(null);
        if(option == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFiles();  //returns the selected files
        }

        return new File[] {};
    }
}
