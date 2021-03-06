//
// Copyright 2017-2020 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of elan-media-trimmer.
//
//    nzilbb.ag is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 3 of the License, or
//    (at your option) any later version.
//
//    nzilbb.ag is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with nzilbb.ag; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.util;

import java.util.Vector;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Manages the execution of an external program, ensuring that streams are processed, etc.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Execution implements Runnable {
   
   // Attributes:
   
   /**
    * Executable file.
    * @see #getExe()
    * @see #setExe(File)
    */
   protected File exe;
   /**
    * Getter for {@link #exe}: Executable file.
    * @return Executable file.
    */
   public File getExe() { return exe; }
   /**
    * Setter for {@link #exe}: Executable file.
    * @param newExe Executable file.
    */
   public Execution setExe(File newExe) { exe = newExe; return this; }
   
   /**
    * Command line arguments.
    * @see #getArguments()
    * @see #setArguments(Vector)
    */
   protected Vector<String> arguments;
   /**
    * Getter for {@link #arguments}: Command line arguments.
    * @return Command line arguments.
    */
   public Vector<String> getArguments() { return arguments; }
   /**
    * Setter for {@link #arguments}: Command line arguments.
    * @param newArguments Command line arguments.
    */
   public Execution setArguments(Vector<String> newArguments) { arguments = newArguments; return this; }

   /**
    * Whether to print verbose output.
    * @see #getVerbose()
    * @see #setVerbose(boolean)
    */
   protected boolean verbose = false;
   /**
    * Getter for {@link #verbose}: Whether to print verbose output.
    * @return Whether to print verbose output.
    */
   public boolean getVerbose() { return verbose; }
   /**
    * Setter for {@link #verbose}: Whether to print verbose output.
    * @param newVerbose Whether to print verbose output.
    */
   public Execution setVerbose(boolean newVerbose) { verbose = newVerbose; return this; }

   /**
    * The executed process.
    * @see #getProcess()
    * @see #setProcess(Process)
    */
   protected Process process;
   /**
    * Getter for {@link #process}: The executed process.
    * @return The executed process.
    */
   public Process getProcess() { return process; }
   /**
    * Setter for {@link #process}: The executed process.
    * @param newProcess The executed process.
    */
   public Execution setProcess(Process newProcess) { process = newProcess; return this; }
   
   /**
    * Text from stdout
    * @see #getInput()
    * @see #setInput(StringBuffer)
    */
   protected StringBuffer input;
   /**
    * Getter for {@link #input}: Text from stdout
    * @return Text from stdout
    */
   public StringBuffer getInput() { return input; }
   /**
    * Setter for {@link #input}: Text from stdout
    * @param newInput Text from stdout
    */
   public Execution setInput(StringBuffer newInput) { input = newInput; return this; }

   /**
    * Text from stderr
    * @see #getError()
    * @see #setError(StringBuffer)
    */
   protected StringBuffer error;
   /**
    * Getter for {@link #error}: Text from stderr
    * @return Text from stderr
    */
   public StringBuffer getError() { return error; }
   /**
    * Setter for {@link #error}: Text from stderr
    * @param newError Text from stderr
    */
   public Execution setError(StringBuffer newError) { error = newError; return this; }

   /**
    * Whether the execution is currently running.
    * @see #getRunning()
    */
   protected boolean running = false;
   /**
    * Getter for {@link #running}: Whether the execution is currently running.
    * @return Whether the execution is currently running.
    */
   public boolean getRunning() { return running; }

   /**
    * Whether the execution has completed.
    * @see #getFinished()
    */
   protected boolean finished = false;
   /**
    * Getter for {@link #finished}: Whether the execution has completed.
    * @return Whether the execution has completed.
    */
   public boolean getFinished() { return finished; }

   /**
    * Error preventing execution.
    * @see #getExecutionError()
    * @see #setExecutionError(String)
    */
   protected String executionError;
   /**
    * Getter for {@link #executionError}: Error preventing execution.
    * @return Error preventing execution.
    */
   public String getExecutionError() { return executionError; }
   /**
    * Setter for {@link #executionError}: Error preventing execution.
    * @param newExecutionError Error preventing execution.
    */
   public Execution setExecutionError(String newExecutionError) { executionError = newExecutionError; return this; }


   // Methods:
   
   /**
    * Default constructor.
    */
   public Execution() {
   } // end of constructor

   /**
    * Constructor from attributes.
    */
   public Execution(File exe, Vector<String> arguments) {
      setExe(exe);
      setArguments(arguments);
   } // end of constructor
   
   /**
    * Builder-style method for adding an argment to {@link #arguments}.
    * @param argument The argument to add.
    * @return A reference to this object.
    */
   public Execution arg(String argument)
   {
      if (argument != null) {
         if (arguments == null) {
            arguments = new Vector<String>();
         }
         arguments.add(argument);
      }
      return this;
   } // end of arg()

   /**
    * Runs the executable, monitors it, and returns when done.
    */
   public void run() {
      running = true;
      finished = false;
      executionError = null;
      input = new StringBuffer();
      error = new StringBuffer();
       
      Vector<String> vArguments = new Vector<String>();
      if (exe == null) {
         executionError = "No executable file set.";
      } else {
         vArguments.add(exe.getPath());
         vArguments.addAll(arguments);
         if (verbose) System.out.println("Execution: " + vArguments);
         try {
            
            setProcess(Runtime.getRuntime().exec(vArguments.toArray(new String[0])));
            
            InputStream inStream = process.getInputStream();
            InputStream errStream = process.getErrorStream();
            byte[] buffer = new byte[1024];
            
            // loop waiting for the process to exit, all the while reading from
            //  the input stream to stop it from hanging
            // there seems to be some overhead in querying the input streams,
            // so we need sleep while waiting to not barrage the process with
            // requests. However, we don't want to sleep too long for processes
            // that terminate quickly or we'll be needlessly waiting.
            // So we start with short sleeps, and exponentially increase the 
            // wait time, with a maximum sleep of 30 seconds
            int iMSSleep = 1;
            while (running) {
               try
               {
                  int iReturnValue = process.exitValue();		     
                  // if exitValue returns, the process has finished
                  running = false;
               }
               catch(IllegalThreadStateException exception) { // still executing		     
                  // sleep for a while
                  try {
                     Thread.sleep(iMSSleep);
                  } catch(Exception sleepX) {
                     System.err.println(
                        "Execution: " + exe.getName() + " Exception while sleeping: "
                        + sleepX.toString() + "\r\n");	
                  }
                  iMSSleep *= 2; // backoff exponentially
                  if (iMSSleep > 10000) iMSSleep = 10000; // max 10 sec
               }
               
               try {
                  // data ready?
                  int bytesRead = inStream.available();
                  String sMessages = "";
                  while(bytesRead > 0) {
                     // if there's data coming, sleep a shorter time
                     iMSSleep = 1;		     
                     // write to the log file
                     bytesRead = inStream.read(buffer);
                     input.append(new String(buffer, 0, bytesRead));
                     // data ready?
                     bytesRead = inStream.available();
                  } // next chunk of data	       
               } catch(IOException exception) {
                  System.err.println("Execution: ERROR reading conversion input stream: "
                                     + exe.getName() + " - " + exception);
               }
               
               try {
                  // data ready from error stream?
                  int bytesRead = errStream.available();
                  while(bytesRead > 0) {
                     // if there's data coming, sleep a shorter time
                     iMSSleep = 1;	    
                     bytesRead = errStream.read(buffer);
                     error.append(new String(buffer, 0, bytesRead));
                     System.err.println("Execution: " + exe.getName() + ": " + new String(buffer, 0, bytesRead));
                     // data ready?
                     bytesRead = errStream.available();
                  } // next chunk of data
               } catch(IOException exception) {
                  System.err.println("Execution: ERROR reading conversion error stream: "
                                     + exe.getName() + " - " + exception);
               }
            } // running
         } catch(IOException exception) {
            executionError = exception.getMessage();
         }
      } // exe is set
      finished = true;
   } // end of run()

} // end of class Execution
