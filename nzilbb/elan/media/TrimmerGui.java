//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of elan-media-trimmer.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.elan.media;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Graphical user interface for {@link Trimmer}.
 */
@SuppressWarnings("serial")
public class TrimmerGui {

   // attributes

   /**
    * The trimmer.
    * @see #getTrimmer()
    * @see #setTrimmer(Trimmer)
    */
   protected Trimmer trimmer;
   /**
    * Getter for {@link #trimmer}: The trimmer.
    * @return The trimmer.
    */
   public Trimmer getTrimmer() { return trimmer; }
   /**
    * Setter for {@link #trimmer}: The trimmer.
    * @param newTrimmer The trimmer.
    */
   public TrimmerGui setTrimmer(Trimmer newTrimmer) { trimmer = newTrimmer; return this; }
   
   /**
    * The window the application runs in.
    * @see #getFrame()
    * @see #setFrame(JFrame)
    */
   protected JFrame frame;
   /**
    * Getter for {@link #frame}: The window the application runs in.
    * @return The window the application runs in.
    */
   public JFrame getFrame() { return frame; }
   /**
    * Setter for {@link #frame}: The window the application runs in.
    * @param newFrame The window the application runs in.
    */
   public TrimmerGui setFrame(JFrame newFrame) { frame = newFrame; return this; }

   // UI
   protected JButton btnAdd = new JButton("+");
   protected JButton btnRemove = new JButton("-");
   protected JList<File> files = new JList<File>(new DefaultListModel<File>());
   protected JButton btnProcess = new JButton("Trim");
   protected JProgressBar progress = new JProgressBar();

   /** Constructor */
   public TrimmerGui() {
   }
   
   /**
    * Initialize the graphical components.
    */
   public void init()
   {
      // create window...
      frame = new JFrame("ELAN Media Trimmer ("+trimmer.getVersionInformation()+")");
      frame.getContentPane().setLayout(new BorderLayout());
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      Toolkit toolkit = frame.getToolkit();
      Dimension screenSize = toolkit.getScreenSize();
      int defaultHeight = screenSize.height / 2;
      int defaultWidth = screenSize.width / 2;
      int top = (screenSize.height - defaultHeight) / 2;
      int left = (screenSize.width - defaultWidth) / 2;
      // icon
      try {
         URL imageUrl = getClass().getResource(getClass().getSimpleName() + ".png");
         if (imageUrl != null) {
            frame.setIconImage(toolkit.createImage(imageUrl));
         }
      } catch(Exception exception) {}
      frame.setSize(defaultWidth, defaultHeight);
      frame.setLocation(left, top);

      // build GUI
      JPanel pnlEast = new JPanel(new FlowLayout());
      btnAdd.setToolTipText("Add a file to the list");
      pnlEast.add(btnAdd);
      btnRemove.setToolTipText("Remove selected files from the list");
      pnlEast.add(btnRemove);
      frame.getContentPane().add(pnlEast, BorderLayout.EAST);
      
      files.setToolTipText("Drop/drop .eaf transcripts here");
      files.setCellRenderer(new DefaultListCellRenderer() {
	    public Component getListCellRendererComponent(
	       JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
	       super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	       setText(((File)value).getName());
	       return this;
	    }
	 });
      frame.getContentPane().add(new JScrollPane(files), BorderLayout.CENTER);
      
      JPanel pnlSouth = new JPanel(new BorderLayout());
      progress.setStringPainted(true);
      pnlSouth.add(progress, BorderLayout.CENTER);
      btnProcess.setToolTipText("Process all transcripts");
      pnlSouth.add(btnProcess, BorderLayout.EAST);
      frame.getContentPane().add(pnlSouth, BorderLayout.SOUTH);
      
      // events

      frame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) { System.exit(0); }});
      
      final FileNameExtensionFilter fileFilter
         = new FileNameExtensionFilter("ELAN transcripts", "eaf");
      btnAdd.addActionListener(new ActionListener() {
            @SuppressWarnings("unchecked")
	    public void actionPerformed(ActionEvent e) {
	       JFileChooser chooser = new JFileChooser();
	       chooser.setFileFilter(fileFilter);
	       chooser.setMultiSelectionEnabled(true);
	       
	       int returnVal = chooser.showOpenDialog(frame);
	       if(returnVal == JFileChooser.APPROVE_OPTION) {
		  for (File file : chooser.getSelectedFiles()) {
		     ((DefaultListModel)files.getModel()).add(files.getModel().getSize(), file);
		  }
	       }
	    }
	 });
      
      btnRemove.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	       for (int i : files.getSelectedIndices()) {
		  ((DefaultListModel)files.getModel()).remove(i);
	       }
	    }
	 });
      
      btnProcess.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) { processTranscripts(); }
	 });
      
      DropTarget target = new DropTarget(files, new DropTargetAdapter() {
	    public void dragEnter(DropTargetDragEvent dtde) 
	    {
	       if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
		  dtde.rejectDrag();
	       }
	    }
            @SuppressWarnings({"rawtypes","unchecked"})
	    public void drop(DropTargetDropEvent dtde) 
	    {
	       try {
		  if (dtde.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
		     dtde.acceptDrop(dtde.getDropAction());
		     List droppedFiles = (List) dtde.getTransferable()
			.getTransferData(DataFlavor.javaFileListFlavor);
		     ListIterator f = droppedFiles.listIterator();
		     while(f.hasNext()) {
			File file = (File)f.next();
			if (fileFilter.accept(file) && !file.isDirectory()) {
			   ((DefaultListModel)files.getModel()).add(files.getModel().getSize(), file);
			} 
		     } // next file
		     dtde.dropComplete(true);
		  } else { // not a file list
		     dtde.rejectDrop();
		  }
	       } catch(Exception e) {
		  dtde.rejectDrop();
		  System.err.println("ERROR dropping file: " + e.getMessage());
		  e.printStackTrace(System.err);
	       }
	    }	    
	 });
      target.setActive(true);

      // redirect stdout and stderr to a file
      File log = new File("elan-media-trimmer.log");
      try {
         PrintStream out = new PrintStream(new FileOutputStream(log), true);
         System.setOut(out);
         System.setErr(out);
         System.out.println("Started: " + new Date());
      } catch(FileNotFoundException exception) {
      }
      
      frame.setVisible(true);
   } // end of init()
   
   /**
    * Start the GUI.
    */
   public void start() {
      init();
   } // end of start()

   /**
    * Process the transcripts in the <var>files</var> list.
    */
   public void processTranscripts() {
      new Thread(new Runnable() {
	    public void run() {
	       btnProcess.setEnabled(false);
               progress.setMaximum(((DefaultListModel)files.getModel()).size());
               progress.setValue(0);
               progress.setString("");
	       for (Object f : ((DefaultListModel)files.getModel()).toArray()) {
                  File eaf = (File)f;
                  progress.setString(eaf.getName());
                  String error = trimmer.processTranscript(eaf);
                  if (error != null) {
                     // display error
                     JOptionPane.showMessageDialog(
                        frame, error, "Error", JOptionPane.ERROR_MESSAGE);
                     progress.setString(eaf.getName() + ": " + error);
                     return;
                  }
                  progress.setValue(progress.getValue() + 1);
               } // next file
               progress.setString("Finished.");
	       btnProcess.setEnabled(true);
	    }
	 }).start();
   } // end of processTranscripts()
}
