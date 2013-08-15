package org.bittero.foldering;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileSystemView;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FolderPanel extends JPanel {
	private final class ChangeShowMouseListener implements MouseListener {
		private final FolderPanel parent;

		private ChangeShowMouseListener(FolderPanel parent) {
			this.parent = parent;
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			parent.changeShowing();
		}
	}

	private static final long serialVersionUID = 1L;
	private GridBagConstraints contentsGridBagConstraints = new GridBagConstraints();
	private final static String[] SAVE_PREFIXES = { "<FolderTreeInfo><version>1</version>", };
	private String[] SAVE_POSTFIXES = { "</FolderTreeInfo>", };
	private List<FolderPanel> folders = new ArrayList<FolderPanel>();
	private List<JLabel> files = new ArrayList<JLabel>();
	private JLabel expandLabel = new JLabel();
	private JPanel contents = new JPanel(new GridBagLayout());
	private JLabel nameLabel;
	private boolean showing;
	private String name;

	public FolderPanel(String name) {
		this(name, false, null);
	}

	public FolderPanel(String name, boolean showing) {
		this(name, showing, null);
	}

	public FolderPanel(String name, Icon icon) {
		this(name, false, icon);
	}

	public FolderPanel(String name, boolean showing, Icon icon) {
		this.name = name;
		this.showing = showing;
		ChangeShowMouseListener csml = new ChangeShowMouseListener(this);
		nameLabel = icon == null ? new JLabel(name) : new JLabel(name, icon, JLabel.LEFT);
		expandLabel.addMouseListener(csml);
		nameLabel.addMouseListener(csml);

		JPanel expandPanel = new JPanel(new BorderLayout(0, 0));
		expandPanel.add(expandLabel, BorderLayout.NORTH);
		this.setLayout(new BorderLayout(5, 0));
		JPanel namePanel = new JPanel(new BorderLayout(0, 0));
		this.add(expandPanel, BorderLayout.WEST);
		this.add(namePanel, BorderLayout.CENTER);
		namePanel.add(nameLabel, BorderLayout.NORTH);
		JPanel contentsPanel = new JPanel(new BorderLayout(0, 0));
		namePanel.add(contentsPanel, BorderLayout.WEST);
		contentsPanel.add(contents, BorderLayout.NORTH);
		refreshView();
		contentsGridBagConstraints.gridx = 1;
		contentsGridBagConstraints.gridwidth = 1;
		contentsGridBagConstraints.weightx = 0;
		contentsGridBagConstraints.weighty = 0;
		contentsGridBagConstraints.fill = GridBagConstraints.NONE;
		contentsGridBagConstraints.anchor = GridBagConstraints.LINE_START;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		nameLabel.setText(name);
	}

	public void changeShowing() {
		setShowing(!showing);
	}

	public void setShowing(boolean showing) {
		this.showing = showing && (folders.size() + files.size() > 0);
		refreshView();
		if (showing) {
			for (FolderPanel folder : folders) {
				contents.add(folder, contentsGridBagConstraints);
			}
			for (JLabel file : files) {
				contents.add(file, contentsGridBagConstraints);
			}
		} else {
			contents.removeAll();
		}
	}

	public void addfolder(FolderPanel folder) {
		int i = 0;
		while (i < folders.size() && folders.get(i).getName().compareTo(folder.getName()) < 0) {
			i++;
		}
		folders.add(i, folder);
		if (showing) {
			contents.add(folder, contentsGridBagConstraints, i);
		}
		refreshView();
	}

	public void addfile(String file) {
		addfile(file, null);
	}

	public void addfile(String file, Icon icon) {
		int i = 0;
		while (i < files.size() && files.get(i).getText().compareTo(file) < 0) {
			i++;
		}
		JLabel fileLabel = icon == null ? new JLabel(file) : new JLabel(file, icon, JLabel.LEFT);
		files.add(i, fileLabel);
		if (showing) {
			contents.add(fileLabel, contentsGridBagConstraints, folders.size() + i);
		}
		refreshView();
	}

	public StringBuilder toFoldersXMLTree() {
		StringBuilder ret = new StringBuilder("<folder>");
		ret.append("<name>");
		ret.append(StringEscapeUtils.escapeXml(name));
		ret.append("</name>");
		ret.append("<folders>");
		for (FolderPanel folder : folders) {
			ret.append(folder.toFoldersXMLTree());
		}
		ret.append("</folders>");
		ret.append("<files>");
		for (JLabel file : files) {
			ret.append("<file>");
			ret.append(StringEscapeUtils.escapeXml(file.getText()));
			ret.append("</file>");
		}
		ret.append("</files>");
		ret.append("</folder>");
		return ret;
	}

	public void save(File selectedFile) throws IOException {
		saveXML_V2(selectedFile);
	}

	public void saveXML_V1(File selectedFile) throws IOException {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(selectedFile), Charset.forName("UTF-8")));
		bw.write(toFoldersXMLTree() + "\r\n");
		bw.flush();
		bw.close();
	}

	public void saveXML_V2(File selectedFile) throws IOException {
		ArrayList<String> treeInfoIcons = new ArrayList<String>();
		StringBuilder treeInfo_xmlTree = getTreeInfo(treeInfoIcons);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(selectedFile), Charset.forName("UTF-8")));
		bw.write(SAVE_PREFIXES[0] + treeInfo_xmlTree + "<Icons>");
		for (String icon_str : treeInfoIcons) {
			bw.write("<Icon>");
			bw.write(/* StringEscapeUtils.escapeXml */(icon_str));
			bw.write("</Icon>");
		}
		bw.write("</Icons>");
		bw.write(SAVE_POSTFIXES[0]);
		bw.flush();
		bw.close();
	}

	public static FolderPanel extract(File dir) {
		FolderPanel ret = new FolderPanel(dir.getName(), FileSystemView.getFileSystemView().getSystemIcon(dir));
		File[] files = dir.listFiles();
		if (files != null)
			for (File file : files) {
				if (file.isDirectory()) {
					ret.addfolder(extract(file));
				} else {
					ret.addfile(file.getName(), FileSystemView.getFileSystemView().getSystemIcon(file));
				}
			}
		return ret;
	}

	public static FolderPanel load(File loadFile) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(loadFile), Charset.forName("utf-8")));
		char[] cbuf = new char[200];
		int i = br.read(cbuf);
		br.close();
		if (new String(cbuf, 0, i).startsWith("<folder>")) {
			return loadXML_V1(loadFile);
		}
		if (new String(cbuf, 0, i).startsWith(SAVE_PREFIXES[0])) {
			return loadXML_V2(loadFile);
		}
		return null;
	}

	private void refreshView() {
		expandLabel.setText(folders.size() + files.size() > 0 ? showing ? "-" : "+" : "*");
		nameLabel.setForeground(showing ? Color.magenta : Color.magenta.darker());
		expandLabel.setForeground(showing ? Color.orange : Color.orange.darker());
		for (JLabel file : files) {
			file.setForeground(showing ? new Color(30, 60, 40) : new Color(255, 220, 100));
		}
	}

	private StringBuilder getTreeInfo(List<String> icons) throws IOException {
		Icon icn = nameLabel.getIcon();
		String icon_str = getIconStr(icn);
		int index = findIndex(icons, icon_str);
		StringBuilder xmlTree = new StringBuilder("<folder icon='" + index);
		if (icn instanceof ImageIcon) {
			xmlTree.append("' desc='" + ((ImageIcon) icn).getDescription());
		}
		xmlTree.append("'>");
		xmlTree.append("<name>");
		xmlTree.append(StringEscapeUtils.escapeXml(name));
		xmlTree.append("</name>");
		xmlTree.append("<folders>");
		for (FolderPanel folder : folders) {
			xmlTree.append(folder.getTreeInfo(icons));
		}
		xmlTree.append("</folders>");
		xmlTree.append("<files>");
		for (JLabel file : files) {
			icn = file.getIcon();
			icon_str = getIconStr(icn);
			index = findIndex(icons, icon_str);
			if (icn instanceof ImageIcon) {
				xmlTree.append("<file icon='" + index + "' desc='" + ((ImageIcon) icn).getDescription() + "'>");
			} else {
				xmlTree.append("<file icon='" + index + "'>");
			}
			xmlTree.append(StringEscapeUtils.escapeXml(file.getText()));
			xmlTree.append("</file>");
		}
		xmlTree.append("</files>");
		xmlTree.append("</folder>");
		return xmlTree;
	}

	private int findIndex(List<String> icons, String icon_str) {
		for (int i = 0; i < icons.size(); i++) {
			if (icons.get(i).equals(icon_str)) {
				return i;
			}
		}
		icons.add(icon_str);
		return icons.size() - 1;
	}

	private String getIconStr(Icon icon) throws IOException {
		BufferedImage bufferedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR_PRE);
		icon.paintIcon(nameLabel, bufferedImage.getGraphics(), 0, 0);
		final List<byte[]> streamData = new ArrayList<byte[]>();
		ImageIO.write(bufferedImage, "png", new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				streamData.add(new byte[] { (byte) b });
			}
		});
		byte[] bs = new byte[streamData.size()];
		for (int i = 0; i < bs.length; i++) {
			bs[i] = streamData.get(i)[0];
		}
		String icon_str = new String(Hex.encodeHex(bs));
		return icon_str;
	}

	private static FolderPanel loadXML_V1(File xmlFile) throws FileNotFoundException, SAXException, IOException,
			ParserConfigurationException {
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		saxParserFactory.setValidating(true);
		SAXParser saxParser = saxParserFactory.newSAXParser();
		final Vector<FolderPanel> fps = new Vector<FolderPanel>();
		fps.add(new FolderPanel(""));
		saxParser.parse(new FileInputStream(xmlFile), new DefaultHandler() {
			Vector<FolderPanel> cur = new Vector<FolderPanel>(fps);
			Vector<String> curTag = new Vector<String>();

			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
				curTag.add(qName);
				if (qName.equals("folder")) {
					cur.add(new FolderPanel(""));
				} else if (qName.equals("name")) {
				} else if (qName.equals("folders")) {
				} else if (qName.equals("files")) {
				} else if (qName.equals("file")) {
				}
				super.startElement(uri, localName, qName, attributes);
			}

			@Override
			public void characters(char[] ch, int start, int length) throws SAXException {
				if (curTag.lastElement().equals("folder")) {
				} else if (curTag.lastElement().equals("name")) {
					cur.lastElement().setName(StringEscapeUtils.unescapeXml(new String(ch, start, length)));
				} else if (curTag.lastElement().equals("folders")) {
				} else if (curTag.lastElement().equals("files")) {
				} else if (curTag.lastElement().equals("file")) {
					cur.lastElement().addfile(StringEscapeUtils.unescapeXml(new String(ch, start, length)));
				}
				super.characters(ch, start, length);
			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {
				curTag.remove(curTag.size() - 1);
				if (qName.equals("folder")) {
					FolderPanel f = cur.lastElement();
					cur.remove(cur.size() - 1);
					cur.lastElement().addfolder(f);
				} else if (qName.equals("name")) {
				} else if (qName.equals("folders")) {
				} else if (qName.equals("files")) {
				} else if (qName.equals("file")) {
				}
				super.endElement(uri, localName, qName);
			}
		});
		return fps.get(0).folders.get(0);
	}

	private static FolderPanel loadXML_V2(File xmlFile) throws FileNotFoundException, SAXException, IOException,
			ParserConfigurationException {
		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		saxParserFactory.setValidating(true);
		SAXParser saxParser = saxParserFactory.newSAXParser();
		final Vector<FolderPanel> fps = new Vector<FolderPanel>();
		saxParser.parse(new FileInputStream(xmlFile), new DefaultHandler() {
			Vector<LinkedIconFolderData> cur = new Vector<LinkedIconFolderData>();
			Vector<String> curTag = new Vector<String>();
			Vector<ImageIcon> icons = new Vector<ImageIcon>();

			class LinkedIconFolderData {
				Vector<LinkedIconFolderData> childFolders = new Vector<LinkedIconFolderData>();
				Vector<LinkedIconFileData> childFiles = new Vector<LinkedIconFileData>();
				String name = "";
				int icon;
				String description;

				FolderPanel getFolderPanel() {
					FolderPanel folderPanel = new FolderPanel(StringEscapeUtils.unescapeXml(name), getIcon());
					for (LinkedIconFolderData childFolder : childFolders) {
						folderPanel.addfolder(childFolder.getFolderPanel());
					}
					for (LinkedIconFileData childFile : childFiles) {
						folderPanel.addfile(StringEscapeUtils.unescapeXml(childFile.name), childFile.getIcon());
					}
					return folderPanel;
				}

				private ImageIcon getIcon() {
					return new ImageIcon(icons.get(icon).getImage(), description);
				}
			}

			class LinkedIconFileData {
				String name = "";
				int icon;
				String description;

				private ImageIcon getIcon() {
					return new ImageIcon(icons.get(icon).getImage(), description);
				}
			}

			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
				curTag.add(qName);
				if (qName.equalsIgnoreCase("FolderTreeInfo")) {
					cur.add(new LinkedIconFolderData());
				} else if (qName.equalsIgnoreCase("name")) {
				} else if (qName.equalsIgnoreCase("folders")) {
				} else if (qName.equalsIgnoreCase("folder")) {
					cur.add(new LinkedIconFolderData());
					cur.lastElement().icon = Integer.parseInt(attributes.getValue("icon"));
					cur.lastElement().description = attributes.getValue("desc");
				} else if (qName.equalsIgnoreCase("files")) {
				} else if (qName.equalsIgnoreCase("file")) {
					cur.lastElement().childFiles.add(new LinkedIconFileData());
					cur.lastElement().childFiles.lastElement().icon = Integer.parseInt(attributes.getValue("icon"));
					cur.lastElement().childFiles.lastElement().description = attributes.getValue("desc");
				} else if (qName.equalsIgnoreCase("Icon")) {
				}
				super.startElement(uri, localName, qName, attributes);
			}

			@Override
			public void characters(char[] ch, int start, int length) throws SAXException {
				if (curTag.lastElement().equalsIgnoreCase("folder")) {
				} else if (curTag.lastElement().equalsIgnoreCase("name")) {
					cur.lastElement().name += new String(ch, start, length);
				} else if (curTag.lastElement().equalsIgnoreCase("folders")) {
				} else if (curTag.lastElement().equalsIgnoreCase("files")) {
				} else if (curTag.lastElement().equalsIgnoreCase("file")) {
					cur.lastElement().childFiles.lastElement().name += new String(ch, start, length);
				} else if (curTag.lastElement().equalsIgnoreCase("Icon")) {
					char[] charData = new char[length];
					System.arraycopy(ch, start, charData, 0, length);
					try {
						icons.add(new ImageIcon(Hex.decodeHex(charData)));
					} catch (DecoderException e) {
						e.printStackTrace();
					}
				}
				super.characters(ch, start, length);
			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {
				curTag.remove(curTag.size() - 1);
				if (qName.equalsIgnoreCase("FolderTreeInfo")) {
					fps.add(cur.get(0).childFolders.get(0).getFolderPanel());
				} else if (qName.equalsIgnoreCase("folder")) {
					LinkedIconFolderData f = cur.lastElement();
					cur.remove(cur.size() - 1);
					cur.lastElement().childFolders.add(f);
				} else if (qName.equalsIgnoreCase("name")) {
				} else if (qName.equalsIgnoreCase("folders")) {
				} else if (qName.equalsIgnoreCase("files")) {
				} else if (qName.equalsIgnoreCase("file")) {
				} else if (qName.equalsIgnoreCase("Icon")) {
				}
				super.endElement(uri, localName, qName);
			}
		});
		return fps.get(0);
	}
}