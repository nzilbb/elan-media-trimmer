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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Properties;
import java.util.Vector;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Proxy for ffmpeg invocation.
 */
public class Ffmpeg extends Execution {
   
   /**
    * InputFile media.
    * @see #getInputFile()
    * @see #setInputFile(File)
    */
   protected File inputFile;
   /**
    * Getter for {@link #inputFile}: InputFile media.
    * @return InputFile media.
    */
   public File getInputFile() { return inputFile; }
   /**
    * Setter for {@link #inputFile}: InputFile media.
    * @param newInputFile InputFile media.
    */
   public Ffmpeg setInputFile(File newInputFile) { inputFile = newInputFile; return this; }

   /**
    * OutputFile media.
    * @see #getOutputFile()
    * @see #setOutputFile(File)
    */
   protected File outputFile;
   /**
    * Getter for {@link #outputFile}: OutputFile media.
    * @return OutputFile media.
    */
   public File getOutputFile() { return outputFile; }
   /**
    * Setter for {@link #outputFile}: OutputFile media.
    * @param newOutputFile OutputFile media.
    */
   public Ffmpeg setOutputFile(File newOutputFile) { outputFile = newOutputFile; return this; }

   /**
    * Configuration file.
    * @see #getConfigFile()
    * @see #setConfigFile(File)
    */
   protected File configFile;
   /**
    * Getter for {@link #configFile}: Configuration file.
    * @return Configuration file.
    */
   public File getConfigFile() { return configFile; }
   /**
    * Setter for {@link #configFile}: Configuration file.
    * @param newConfigFile Configuration file.
    */
   public Ffmpeg setConfigFile(File newConfigFile) { configFile = newConfigFile; return this; }   

   /**
    * Overridden setter for {@link #exe}: Executable file. Save the location in the
    * {@link #configFile}
    * @param newExe Executable file.
    */
   public Execution setExe(File newExe) {
      if (configFile != null) {
         Properties config = new Properties();
         config.setProperty("ffmpeg", newExe.getPath());
         try {
            config.storeToXML(new FileOutputStream(configFile),"");
            if (verbose) System.out.println("Saved config to: " + configFile.getPath());
         } catch(IOException exception) {
            System.err.println("Could not save config file: " + exception);
            exception.printStackTrace(System.err);
         }
      }
      return super.setExe(newExe);
   }
   
   /**
    * Constructor.
    */
   public Ffmpeg() {
      try {
         URL thisClassUrl = getClass().getResource(getClass().getSimpleName() + ".class");
         if (thisClassUrl.toString().startsWith("jar:")) {
            URI thisJarUri = new URI(thisClassUrl.toString().replaceAll("jar:(.*)!.*","$1"));
            File thisJarFile = new File(thisJarUri);
            configFile = new File(thisJarFile.getParentFile(),
                                  thisJarFile.getName().replace(".jar", ".xml"));
            if (verbose) System.out.println("Config: " + configFile.getPath());
         }
      }
      catch(Exception exception) {
         System.err.println("Could not determine config file: " + exception);
         exception.printStackTrace(System.err);
      }
   }
   
   /**
    * Add arguments for resampling an MP4 to have 720px wide, for web delivery.
    * @return A reference to this object.
    */
   public Ffmpeg resampleForWeb() {
      return resampleForWeb(720);
   }
   /**
    * Add arguments for resampling an MP4 for web delivery.
    * @param width Width, in pixels, of the outputFile video
    * @return A reference to this object.
    */
   public Ffmpeg resampleForWeb(int width) {
      // e.g. ... -filter:v scale=720:-1 -codec:v libx264 -strict -2 ...
      arg("-filter:v");
      arg("scale="+width+":-1");
      arg("-codec:v");
      arg("libx264");
      arg("-strict");
      arg("-2");
      return this;
   } // end of resampleForWeb()
   
   /**
    * Add arguments for trimming a given number of milliseconds from the start.
    * @param milliseconds The number of milliseconds to trim.
    * @return A reference to this object.
    */
   public Ffmpeg trimStartMS(long milliseconds) {
      // e.g. ... -ss 00:00:01.234 -async 1 ...
      arg("-ss");
      Duration toTrim = Duration.ofMillis(milliseconds);
      if (toTrim.getNano() > 0) { // include milliseconds
         arg(String.format(
                "%02d:%02d:%02d.%03d",
                toTrim.getSeconds() / 3600,
                (toTrim.getSeconds() % 3600) / 60,
                toTrim.getSeconds() % 60,
                toTrim.getNano() / 1000000));
      } else { // no milliseconds required
         arg(String.format(
                "%02d:%02d:%02d",
                toTrim.getSeconds() / 3600,
                (toTrim.getSeconds() % 3600) / 60,
                toTrim.getSeconds() % 60));
      }
      arg("-async");
      arg("1");
      return this;
   } // end of trimStartMS()
   
   /**
    * Generate runtime arguments, including those which specify input and output files.
    * @return All command-line arguments, including those which specify input and output files.
    */
   public Vector<String> getAllArguments()
   {
      Vector<String> arguments = new Vector<String>();
      
      // set inputFile file
      arguments.add("-i"); // input file next
      arguments.add(inputFile.getPath());
      arguments.add("-y");  // overwrite output without asking
      
      // add any other arguments
      if (getArguments() != null) arguments.addAll(getArguments());
      
      // set the outputFile file
      arguments.add(outputFile.getPath());

      return arguments;
   } // end of getAllArguments()

   /**
    * Determines the executable file, by using the configuration file, the system path, or
    * asking the user. 
    */
   public Execution setExe() {
      if (exe != null) return this;

      // check the current directory
      File ffmpegHere = new File("ffmpeg");
      if (ffmpegHere.exists()) {
         setExe(ffmpegHere);
      } else {
         // windows?
         ffmpegHere = new File("ffmpeg.exe");
         if (ffmpegHere.exists()) {
            if (verbose) System.out.println("Found local ffmpeg: " + ffmpegHere.getPath());
            setExe(ffmpegHere);
         }
      }

      if (exe == null) {
         // saved in the configuration file?
         if (configFile != null && configFile.exists()) {
            Properties config = new Properties();
            try {
               config.loadFromXML(new FileInputStream(configFile));
               if (config.containsKey("ffmpeg")) {
                  File f = new File(config.getProperty("ffmpeg"));
                  if (f.exists()) {
                     if (verbose) System.out.println("Using configured ffmpeg: " + f.getPath());
                     setExe(f);
                  } else {
                     if (verbose) System.out.println("Configured ffmpeg not found: " + f.getPath());
                  }
               }
            } catch(IOException exception) {
               System.err.println("Could not determine config file: " + exception);
               exception.printStackTrace(System.err);
            }
         }
      }

      if (exe == null) {
         // ask the system?
         Vector<String> whichArgs = new Vector<String>();
         whichArgs.add("ffmpeg");
         Execution which = new Execution(new File("which"), whichArgs);
         which.run();
         if (which.getInput().length() > 0) {
            setExe(new File(which.getInput().toString().trim()));
            if (verbose) System.out.println("Using system location: " + exe.getPath());
         }
      }

      // ask the user? TODO

      return this;
   }

   /**
    * Ensures the inputFile and outputFile arguments are set, and then calls
    * {@link Executable#run()}
    */
   public void run() {
      // determine executable
      setExe();
      if (exe == null) {
         System.err.println("Cannot execute ffmpeg: its location is unknown.");
         return;
      }
      
      Vector<String> originalArguments = arguments;
      try {
         // create a new arguments collection
         arguments = getAllArguments();
         // execute
         super.run();

      } finally {
         // restore the original arguments, so we can re-run
         arguments = originalArguments;
      }
   }

}
