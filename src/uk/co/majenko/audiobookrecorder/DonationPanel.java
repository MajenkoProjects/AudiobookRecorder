package uk.co.majenko.audiobookrecorder;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DonationPanel extends JPanel {
    public DonationPanel() {
        super();
        setLayout(new BorderLayout());
        JLabel icon = new JLabel(Icons.dollar);
        icon.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(icon, BorderLayout.WEST);

        JTextArea label = new JTextArea(
            "Thank you for using AudiobookRecorder by\n" +
            "Majenko Technologies. This software is\n" +
            "Free Open Source Software (FOSS). It is\n" +
            "created and maintained voluntarily with\n" +
            "no possibility of any profits from it.\n" +
            "If you enjoy using this software and end\n" +
            "up making millions of dollars from using\n" +
            "it we hope that you would maybe kindly\n" +
            "donate a couple of those dollars to the\n" +
            "developer to help with the costs of maintaining\n" +
            "the software.\n" +
            "\n" +
            "You can donate by going to:\n" +
            "\n" +
            "https://paypal.me/majenko"
        );

        label.setEditable(false);
        label.setFocusable(false);
        label.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(label, BorderLayout.CENTER);

        JButton donate = new JButton("Donate!");
        donate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Utils.browse("https://paypal.me/majenko");
            }
        });

        add(donate, BorderLayout.SOUTH);


    }
}
