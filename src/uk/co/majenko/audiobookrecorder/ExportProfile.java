package uk.co.majenko.audiobookrecorder;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;


public class ExportProfile {
	String name;
	String code;
	int export_bitrate;
	int export_channels;
	int export_samples;
	String export_format;
	int gap_pre_chapter;
	int gap_post_chapter;
	int gap_post_sentence;
	int gap_followon;
	int gap_post_paragraph;
	int gap_post_section;
	int audio_rms;

	public ExportProfile() {
		name = "Default";
		code = "default";
		export_bitrate = 128000;
		export_channels = 1;
		export_samples = 44100;
		export_format = "{book.title} - {chapter.number} - {chapter.title}";
		gap_pre_chapter = 1000;
		gap_post_chapter = 1000;
		gap_post_sentence = 500;
		gap_followon = 300;
		gap_post_paragraph = 800;
		gap_post_section = 1200;
		audio_rms = -18;
	}

	public ExportProfile(String filename) {
		loadProfileFromFile(new File(filename));
	}

	public ExportProfile(File f) {
		loadProfileFromFile(f);
	}

	public void loadProfileFromFile(File inputFile) {
		if (inputFile.exists()) {
			try {
	            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	            Document doc = dBuilder.parse(inputFile);
	            doc.getDocumentElement().normalize();
	            Element root = doc.getDocumentElement();
				Element export = getNode(root, "export");
				Element gaps = getNode(root, "gaps");
				Element audio = getNode(root, "audio");

				name = getTextNode(root, "name");
				code = getTextNode(root, "code");
				export_bitrate = Utils.s2i(getTextNode(export, "bitrate"));
				export_channels = Utils.s2i(getTextNode(export, "channels"));
				export_samples = Utils.s2i(getTextNode(export, "samples"));
				export_format = getTextNode(export, "format");
    			gap_pre_chapter = Utils.s2i(getTextNode(gaps, "pre-chapter"));
    			gap_post_chapter = Utils.s2i(getTextNode(gaps, "post-chapter"));
    			gap_post_sentence = Utils.s2i(getTextNode(gaps, "post-sentence"));
    			gap_followon = Utils.s2i(getTextNode(gaps, "followon"));
    			gap_post_paragraph = Utils.s2i(getTextNode(gaps, "post-paragraph"));
    			gap_post_section = Utils.s2i(getTextNode(gaps, "post-section"));
    			audio_rms = Utils.s2i(getTextNode(audio, "rms"));

			} catch (ParserConfigurationException ex) {
				System.err.println("Badly formatted XML file: " + inputFile.getPath());
			} catch (SAXException ex) {
				System.err.println("Badly formatted XML file: " + inputFile.getPath());
			} catch (IOException ex) {
				System.err.println("Error reading file: " + inputFile.getPath());
			}
		}

	}

    public Element getNode(Element r, String n) {
        Debug.trace();
        NodeList nl = r.getElementsByTagName(n);
        if (nl == null) return null;
        if (nl.getLength() == 0) return null;
        return (Element)nl.item(0); 
    }   
        
    public String getTextNode(Element r, String n) {
        Debug.trace();
        return getTextNode(r, n, "");
    }   

    public String getTextNode(Element r, String n, String d) {
        Debug.trace();
        Element node = getNode(r, n);
        if (node == null) return d;
        return node.getTextContent();
    }

	public String getName() { return name; }
	public String getCode() { return code; }
	public int getExportBitrate() { return export_bitrate; }
	public int getExportChannels() { return export_channels; }
	public int getExportSamples() { return export_samples; }
	public String getExportFormat() { return export_format; }
	public int getGapPreChapter() { return gap_pre_chapter; }
	public int getGapPostChapter() { return gap_post_chapter; }
	public int getGapFollowon() { return gap_followon; }
	public int getGapPostSentence() { return gap_post_sentence; }
	public int getGapPostParagraph() { return gap_post_paragraph; }
	public int getGapPostSection() { return gap_post_section; }
	public int getAudioRMS() { return audio_rms; }
        

	public String toString() {
		return getName();
	}

}
