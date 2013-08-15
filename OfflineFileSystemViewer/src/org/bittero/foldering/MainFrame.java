package org.bittero.foldering;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private JFileChooser openDirChooser = new JFileChooser("");
	private JFileChooser XMLChooser = new JFileChooser("");
	private FolderPanel folder;

	public MainFrame() {
		final MainFrame parent = this;
		final JScrollPane jScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		openDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		XMLChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		XMLChooser.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		this.getContentPane().setLayout(new BorderLayout(0, 0));
		JPanel optionPanel = new JPanel(new GridLayout(1, 3));
		JButton selectButton = new JButton("Select Directory");
		selectButton.addMouseListener(new MouseListener() {
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
				int openDialogStatus = openDirChooser.showOpenDialog(parent);
				if (openDialogStatus == JFileChooser.APPROVE_OPTION) {
					File selectedFile = openDirChooser.getSelectedFile();
					folder = FolderPanel.extract(selectedFile);
					folder.setShowing(true);
					jScrollPane.setViewportView(folder);
				}
			}
		});
		optionPanel.add(selectButton, BorderLayout.WEST);
		JButton saveButton = new JButton("Save Foldering");
		saveButton.addMouseListener(new MouseListener() {
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
				XMLChooser.setSelectedFile(new File(openDirChooser.getCurrentDirectory(), folder.getName() + ".xml"));
				int openDialogStatus = XMLChooser.showSaveDialog(parent);
				if (openDialogStatus == JFileChooser.APPROVE_OPTION) {
					File selectedFile = XMLChooser.getSelectedFile();
					if (!selectedFile.getName().endsWith(".xml")) {
						selectedFile = new File(selectedFile.getAbsolutePath() + ".xml");
					}
					try {
						folder.save(selectedFile);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		optionPanel.add(saveButton, BorderLayout.EAST);
		JButton openButton = new JButton("Open Foldering");
		openButton.addMouseListener(new MouseListener() {
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
				int openDialogStatus = XMLChooser.showOpenDialog(parent);
				if (openDialogStatus == JFileChooser.APPROVE_OPTION) {
					File selectedFile = XMLChooser.getSelectedFile();
					try {
						folder = FolderPanel.load(selectedFile);
						folder.setShowing(true);
						jScrollPane.setViewportView(folder);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		});
		optionPanel.add(openButton, BorderLayout.EAST);
		this.getContentPane().add(optionPanel, BorderLayout.NORTH);
		this.getContentPane().add(jScrollPane, BorderLayout.CENTER);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setTitle("Offline File System Viewer");
		this.pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int w = 400, h = 600;
		this.setBounds((screenSize.width - w) / 2, (screenSize.height - h) / 2, w, h);
		this.setVisible(true);
	}

	public static void main(String[] args) {
		new MainFrame();
	}
}
