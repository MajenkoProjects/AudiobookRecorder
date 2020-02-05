package uk.co.majenko.audiobookrecorder;

import java.lang.Runnable;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.json.JSONObject;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class VersionChecker implements Runnable {
    public VersionChecker() {
    }

    public void run() {
        try {
            URL url = new URL("https://api.github.com/repos/MajenkoProjects/AudiobookRecorder/releases/latest");
            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            InputStream is = conn.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String inputLine;
            StringBuilder jsonData = new StringBuilder();

            while ((inputLine = br.readLine()) != null) {
                jsonData.append(inputLine);
            }

            br.close();

            JSONObject job = new JSONObject(jsonData.toString());

            String installed = AudiobookRecorder.config.getProperty("version");
            String available = job.getString("tag_name");
            if (available.startsWith("v")) {
                available = available.substring(1);
            }
            String website = job.getString("html_url");


            String[] installedParts = installed.split("\\.");
            String[] availableParts = available.split("\\.");
            // Must be x.y.z

            if (installedParts.length != 3) return;
            if (availableParts.length != 3) return;

            // Convert to xxxyyyzzz
            String installedVersion = String.format("%03d%03d%03d", Utils.s2i(installedParts[0]), Utils.s2i(installedParts[1]), Utils.s2i(installedParts[2]));
            String availableVersion = String.format("%03d%03d%03d", Utils.s2i(availableParts[0]), Utils.s2i(availableParts[1]), Utils.s2i(availableParts[2]));

            if (Utils.s2i(installedVersion) >= Utils.s2i(availableVersion)) return;
            
            JButton upgrade = new JButton("A new version is available.");
            upgrade.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    Utils.browse(website);
                }
            });

            AudiobookRecorder.window.statusBar.add(upgrade);
            AudiobookRecorder.window.statusBar.revalidate();

        } catch (Exception ignored) {
            ignored.printStackTrace();
        }

    }
}
