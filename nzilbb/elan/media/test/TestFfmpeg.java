//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of nzilbb.ag.
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

package nzilbb.elan.media.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import nzilbb.elan.media.*;

public class TestFfmpeg
{
   @Test public void getAllArguments() {
      File input = new File(getDir(), "input.mp4");
      File output = new File(getDir(), "output.mp4");
      Ffmpeg ffmpeg = new Ffmpeg()
         .setInputFile(input)
         .setOutputFile(output);

      assertEquals("input", input.getPath(), ffmpeg.getInputFile().getPath());
      assertEquals("output", output.getPath(), ffmpeg.getOutputFile().getPath());

      // test with no other arguments
      Iterator<String> args = ffmpeg.getAllArguments().iterator();
      assertEquals("-i", "-i", args.next());
      assertEquals("input file", input.getPath(), args.next());
      assertEquals("output file", output.getPath(), args.next());
      assertFalse("no extra args", args.hasNext());

      // add an argument
      ffmpeg.arg("other-arg");
      args = ffmpeg.getAllArguments().iterator();
      assertEquals("-i", "-i", args.next());
      assertEquals("input file", input.getPath(), args.next());
      assertEquals("other-arg", "other-arg", args.next());
      assertEquals("output file", output.getPath(), args.next());
      assertFalse("no extra args", args.hasNext());
   }
   
   @Test public void resampleForWeb() {
      Ffmpeg ffmpeg = new Ffmpeg()
         .resampleForWeb();
      Iterator<String> args = ffmpeg.getArguments().iterator();
      assertEquals("-filter:v", args.next());
      assertEquals("scale=720:-1", args.next());
      assertEquals("-codec:v", args.next());
      assertEquals("libx264", args.next());
      assertEquals("-strict", args.next());
      assertEquals("-2", args.next());
      assertFalse("no extra args", args.hasNext());
   }
   
   @Test public void resampleForWeb1024() {
      Ffmpeg ffmpeg = new Ffmpeg()
         .resampleForWeb(1024);
      Iterator<String> args = ffmpeg.getArguments().iterator();
      assertEquals("-filter:v", args.next());
      assertEquals("no thousands separator", "scale=1024:-1", args.next());
      assertEquals("-codec:v", args.next());
      assertEquals("libx264", args.next());
      assertEquals("-strict", args.next());
      assertEquals("-2", args.next());
      assertFalse("no extra args", args.hasNext());
   }

   @Test public void trimStartMS1000() {
      Ffmpeg ffmpeg = new Ffmpeg()
         .trimStartMS(1000);
      Iterator<String> args = ffmpeg.getArguments().iterator();
      assertEquals("-ss", args.next());
      assertEquals("00:00:01", args.next());
      assertEquals("-async", args.next());
      assertEquals("1", args.next());
      assertFalse("no extra args", args.hasNext());
   }

   @Test public void trimStartMS1() {
      Ffmpeg ffmpeg = new Ffmpeg()
         .trimStartMS(1);
      Iterator<String> args = ffmpeg.getArguments().iterator();
      assertEquals("-ss", args.next());
      assertEquals("00:00:00.001", args.next());
      assertEquals("-async", args.next());
      assertEquals("1", args.next());
      assertFalse("no extra args", args.hasNext());
   }
   
   @Test public void trimStartMS900() {
      Ffmpeg ffmpeg = new Ffmpeg()
         .trimStartMS(900);
      Iterator<String> args = ffmpeg.getArguments().iterator();
      assertEquals("-ss", args.next());
      assertEquals("00:00:00.900", args.next());
      assertEquals("-async", args.next());
      assertEquals("1", args.next());
      assertFalse("no extra args", args.hasNext());
   }

   @Test public void trimStartMSMinutes() {
      Ffmpeg ffmpeg = new Ffmpeg()
         .trimStartMS(61001);
      Iterator<String> args = ffmpeg.getArguments().iterator();
      assertEquals("-ss", args.next());
      assertEquals("00:01:01.001", args.next());
      assertEquals("-async", args.next());
      assertEquals("1", args.next());
      assertFalse("no extra args", args.hasNext());
   }

   @Test public void trimStartMSHours() {
      Ffmpeg ffmpeg = new Ffmpeg()
         .trimStartMS(1*60*60*1000);
      Iterator<String> args = ffmpeg.getArguments().iterator();
      assertEquals("-ss", args.next());
      assertEquals("01:00:00", args.next());
      assertEquals("-async", args.next());
      assertEquals("1", args.next());
      assertFalse("no extra args", args.hasNext());
   }

   /**
    * Directory for text files.
    * @see #getDir()
    * @see #setDir(File)
    */
   protected File fDir;
   /**
    * Getter for {@link #fDir}: Directory for text files.
    * @return Directory for text files.
    */
   public File getDir() { 
      if (fDir == null) {
	 try {
	    URL urlThisClass = getClass().getResource(getClass().getSimpleName() + ".class");
	    File fThisClass = new File(urlThisClass.toURI());
	    fDir = fThisClass.getParentFile();
	 } catch(Throwable t) {
	    System.out.println("" + t);
	 }
      }
      return fDir; 
   }

   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.elan.media.test.TestFfmpeg");
   }
}
