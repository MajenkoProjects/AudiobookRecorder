package uk.co.majenko.audiobookrecorder;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import it.sauronsoftware.jave.MultimediaInfo;
import it.sauronsoftware.jave.EncoderProgressListener;
import java.awt.Dialog;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.event.WindowEvent;

public class ProgressDialog extends JDialog implements EncoderProgressListener {
    JLabel message;
    JLabel icon;

    JProgressBar progress;

    int spin = 0;



    public ProgressDialog(String m) {
        super();

        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

        message = new JLabel(m);
        icon = new JLabel(Icons.spinner0);
        progress = new JProgressBar(0, 1000);

        setLayout(new BorderLayout());
        add(message, BorderLayout.CENTER);

        icon.setBorder(new EmptyBorder(10, 10, 10, 10));

        add(icon, BorderLayout.WEST);


        add(progress, BorderLayout.SOUTH);

        setLocationRelativeTo(AudiobookRecorder.window);

        setPreferredSize(new Dimension(300, 100));

        pack();

        setSize(new Dimension(300, 100));
        setResizable(false);
        
//        setVisible(true);
    }

    public void setMessage(String m) {
        message.setText(m);
    }

    public void closeDialog() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    public void spin() {
        spin++;
        if (spin == 4) spin = 0;

        switch (spin) {
            case 0: icon.setIcon(Icons.spinner0); break;
            case 1: icon.setIcon(Icons.spinner1); break;
            case 2: icon.setIcon(Icons.spinner2); break;
            case 3: icon.setIcon(Icons.spinner3); break;
        }
    }

    public void progress(int p) { 
        progress.setValue(500 + (p / 2));
        progress.setString((50 + p / 20) + "%");
        spin();
    }

    public void setProgress(int p) {
        progress.setValue(p / 2);
        progress.setString((p / 20) + "%");
        spin();
    }

    public void message(String m) {
    }
    
    public void sourceInfo(MultimediaInfo i) {
    }
}
