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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility for trimming the offset media linked to ELAN transcripts.
 */
public class Trimmer {
   
   /** Program entrypoint */
   public static void main(String[] argv) {
      try {
         Trimmer trimmer = new Trimmer();
         trimmer.processArguments(argv);
         if (trimmer.transcripts != null && trimmer.transcripts.size() > 0) {
            // process transcripts
            trimmer.processTranscripts();
         } else { // no transcripts, try interactive mode
            new TrimmerGui()
               .setTrimmer(trimmer)
               .start();
         }
      } catch(Throwable exception) {
         System.err.println("Unexpected error: " + exception);
         exception.printStackTrace(System.err);
      }
   }

   private DocumentBuilderFactory builderFactory;
   private DocumentBuilder builder;
   private XPathFactory xpathFactory;
   private XPath xpath;
   private TransformerFactory transformerFactory;
   private Transformer transformer;
   
   /**
    * Whether to print verbose output.
    * @see #getVerbose()
    * @see #setVerbose(boolean)
    */
   protected boolean verbose;
   /**
    * Getter for {@link #verbose}: Whether to print verbose output.
    * @return Whether to print verbose output.
    */
   public boolean getVerbose() { return verbose; }
   /**
    * Setter for {@link #verbose}: Whether to print verbose output.
    * @param newVerbose Whether to print verbose output.
    */
   public Trimmer setVerbose(boolean newVerbose) { verbose = newVerbose; return this; }
   
   /**
    * A list of .eaf files to process.
    * @see #getTranscripts()
    * @see #setTranscripts(List<File>)
    */
   protected List<File> transcripts;
   /**
    * Getter for {@link #transcripts}: A list of .eaf files to process.
    * @return A list of .eaf files to process.
    */
   public List<File> getTranscripts() { return transcripts; }
   /**
    * Setter for {@link #transcripts}: A list of .eaf files to process.
    * @param newTranscripts A list of .eaf files to process.
    */
   public Trimmer setTranscripts(List<File> newTranscripts) { transcripts = newTranscripts; return this; }

   /** Constructor */
   public Trimmer() throws ParserConfigurationException, TransformerConfigurationException {
      builderFactory = DocumentBuilderFactory.newInstance();
      builder = builderFactory.newDocumentBuilder();
      xpathFactory = XPathFactory.newInstance();
      xpath = xpathFactory.newXPath();
      transformerFactory = TransformerFactory.newInstance();
      transformer = transformerFactory.newTransformer();
   }
   
   /**
    * Processes the given command line arguments.
    * @param argv
    */
   public void processArguments(String[] argv)
   {      
      if (argv.length == 0) {
         printUsage();
      } else {
         
         // interpret command line arguments
         transcripts = new Vector<File>();
         for (String arg : argv) {
            if (arg.equals("-v")) {
               verbose = true;
            } else {
               File transcript = new File(arg);
               if (!transcript.exists()) {
                  System.err.println(arg + ": not found.");
               } else if (!transcript.getName().toLowerCase().endsWith(".eaf")) {
                  System.err.println(arg + ": not an ELAN transcript.");
               } else if (transcript.getName().matches(".*-original\\.eaf$")) {
                  System.err.println(arg + ": Ignoring previously created backup.");
               } else {
                  transcripts.add(transcript);
               }
            }
         } // next argument
      }      
   } // end of processArguments()
   
   /**
    * Process the transcripts.
    */
   public void processTranscripts() {
      for (File transcript : transcripts) {
         processTranscript(transcript);
      }
   } // end of processTranscripts()
   
   /**
    * Process a single transcript.
    * @param eaf Transcript file.
    * @return null if successful, an error message otherwise.
    */
   public String processTranscript(File eaf) {
      verboseMessage("Transcript: " + eaf.getPath());
      String nameWithoutExtension = eaf.getName().replaceAll("\\.[^.]+$", "");
      File dir = new File(eaf.getParentFile(), "trimmer");
      if (!dir.exists()) {
         if (!dir.mkdir()) {
            String error = "ERROR: could not create output directory " + dir.getPath();
            System.err.println(error);
            // this is fatal
            return error; 
         }
      }
      
      try {
         // parse XML
         Document document = builder.parse(new FileInputStream(eaf));
         
         // get MEDIA_DESCRIPTOR elements
         NodeList mediaDescriptors = (NodeList)xpath.evaluate(
            "//MEDIA_DESCRIPTOR", document, XPathConstants.NODESET);
         
         // for each media file
         for (int d = 0; d < mediaDescriptors.getLength(); d++) {
            
            Node descriptor = mediaDescriptors.item(d);

            // get the URLs and origin
            Attr mediaUrl = (Attr)descriptor.getAttributes().getNamedItem("MEDIA_URL");
            Attr relativeMediaUrl = (Attr)descriptor.getAttributes().getNamedItem("RELATIVE_MEDIA_URL");
            Attr timeOrigin =  (Attr)descriptor.getAttributes().getNamedItem("TIME_ORIGIN");
            
            // Find media file...
            
            File media = findMedia(eaf, mediaUrl, relativeMediaUrl);
            
            if (media == null) {
               if (timeOrigin == null) {
                  System.err.println(
                     "WARNING: could not find media " + mediaUrl + " ("+relativeMediaUrl+")");
               } else {
                  String error = "ERROR: could not find media " + mediaUrl
                     + " ("+relativeMediaUrl+")";
                  System.err.println(error);
                  // this is fatal - the file needs to be edited and we can't find it
                  return error; 
               }
            } else { // media found
               
               String extension = media.getName().replaceAll(".*(\\.[^.]+)$","$1");
               File newMediaFile = new File(dir, nameWithoutExtension + extension);
               verboseMessage("New media file name: " + newMediaFile.getPath());

               // create the media file...
               if (timeOrigin == null) { // no time origin...
                  // if it's video, resample for web
                  if (media.getName().endsWith(".mp4")) {
                     verboseMessage("Resample: " + media.getPath());
                     Ffmpeg ffmpeg = new Ffmpeg()
                        .setInputFile(media)
                        .setOutputFile(newMediaFile);
                     ffmpeg.setVerbose(verbose);
                     
                     ffmpeg.resampleForWeb();
                     
                     ffmpeg.run();
                     if (ffmpeg.getExecutionError() != null) {
                        System.err.println(ffmpeg.getExecutionError());
                        // this is fatal
                        return ffmpeg.getExecutionError(); 
                     }
                  } else { // not mp4
                     // just copy the file
                     verboseMessage("Copy: " + media.getPath());
                     Files.copy(media.toPath(), newMediaFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                  }
               } else {
                  // trim
                  verboseMessage("Trim: " + media.getPath());
                  Ffmpeg ffmpeg = new Ffmpeg()
                     .setInputFile(media)
                     .setOutputFile(newMediaFile);
                  ffmpeg.setVerbose(verbose);

                  // if it's video, resample for web
                  if (media.getName().endsWith(".mp4")) {
                     ffmpeg.resampleForWeb();
                  }

                  ffmpeg.trimStartMS(Long.parseLong(timeOrigin.getValue()));
                  
                  ffmpeg.run();
                  if (ffmpeg.getExecutionError() != null) {
                     System.err.println(ffmpeg.getExecutionError());
                     // this is fatal
                     return ffmpeg.getExecutionError(); 
                  }
               }

               // update the descriptor
               if (mediaUrl != null) {
                  mediaUrl.setValue(newMediaFile.toURI().toString());
               }
               if (relativeMediaUrl != null) {
                  relativeMediaUrl.setValue("./" + newMediaFile.getName());
               }
               if (timeOrigin != null) {
                  descriptor.getAttributes().removeNamedItem("TIME_ORIGIN");
               }
               
            } // media found
         } // next media descriptor

         // save .eaf with new media files and no TIME_ORIGINs
         File newEaf = new File(dir, eaf.getName());
         DOMSource source = new DOMSource(document);
         FileWriter fw = new FileWriter(newEaf);
         StreamResult result = new StreamResult(fw);
         transformer.transform(source, result);
         
      } catch (Exception x) {
         String error = "ERROR: " + eaf.getName() + ": " + x;
         System.err.println(error);
         x.printStackTrace(System.err);
         return error;
      }
      return null;
   } // end of processTranscript()
   
   /**
    * Finds the given media file for the given transcript.
    * @param eaf
    * @param mediaUrl
    * @param relativeMediaUrl
    * @return The media file, or null if an existing file cannot be located.
    */
   public File findMedia(File eaf, Attr mediaUrl, Attr relativeMediaUrl) {

      File media = null;
      
      // try MEDIA_URL
      if (mediaUrl != null) {
         try {
            media = new File(new URI(mediaUrl.getValue()));
            
            // check it's accessible
            if (media.exists()) {
               verboseMessage("MEDIA_URL " + mediaUrl.getValue() + " -> " + media.getPath());
            } else {
               media = null;
            }
            
         } catch(Exception x) {
            System.err.println("Invalid MEDIA_URL: " + mediaUrl.getValue() + ": " + x);
         }
      } // try MEDIA_URL
      
      if (media == null && relativeMediaUrl != null) {
         
         // try RELATIVE_MEDIA_URL
         try {
            media = new File(eaf.toURI().resolve(relativeMediaUrl.getValue()));
            
            // check it's accessible
            if (media.exists()) {
               verboseMessage("RELATIVE_MEDIA_URL "
                              + relativeMediaUrl.getValue() + " -> " + media.getPath());
            } else {
               media = null;
            }
            
         } catch(Exception x) {
            System.err.println(
               "Invalid RELATIVE_MEDIA_URL: " + relativeMediaUrl.getValue() + ": " + x);
         }               
      } // try MEDIA_URL

      return media;
   } // end of findMedia()
   
   /**
    * Prints the command-line parameter info for the utility.
    */
   private void printUsage() {
      System.err.println("Utility for trimming the offset media linked to ELAN transcripts.");
      System.err.println("Usage:");
      System.err.println("java -jar elan-media-trimmer.jar file1.eaf [file2.eaf ...]");
   } // end of printUsage()
   
   /**
    * Print a message if verbose == true.
    * @param message
    */
   public void verboseMessage(String message) {
      if (verbose) System.out.println(message);
   } // end of verboseMessage()


}
