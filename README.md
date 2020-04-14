# elan-media-trimmer

Utility for trimming the offset media linked to ELAN transcripts.

[ELAN](https://archive.mpi.nl/tla/elan)
is a 3rd-party transcription/annotation tool for media files.

When ELAN is used to transcribe interactions with multiple media files (e.g. an mp4 video
recording and a wav audio recording), often the media files are not synchronized with each
other. ELAN allows you to synchronize the media files for transcription/annotation
purposes by allowing to to line them up by assigning "Relative Offsets" for lining them up
with the 'master' track.

If you want to edit the media files so they all line up with the master 'master' track
without any "Relative Offsets", in preparation for upload to
[LaBB-CAT](https://labbcat.canterbury.ac.nz/),
you can use *elan-media-trimmer* to do so. 

Given an .eaf ELAN transcript, *elan-media-trimmer*:
1. Analyzes the .eaf looking for tracks with a relative offset.
2. Edits each such media file, trimming off corresponding content at the start of the media.
3. Resamples mp4 videos for web delivery.
4. Updates the .eaf file so that the tracks no longer specify relative offsets.
5. Renames media files to match the name of the transcript.
6. Saves all changed files in a subdirectory called `trimmer` (no original files are
   harmed in the process of editing transcripts/media).

## How to use the utility

### Prerequisites:

* Java
* ffmpeg

### Usage

*elan-media-trimmer* works as a command-line application:

The following command:

```
java -jar elan-media-trimmer.jar *.eaf
```

...will process all ELAN transcripts in the current directory.

## How to build from source

### Prerequisites:

* JDK
* Apache ant

## Building

Use the following command to build:

```
ant
```

The output is:
`bin/elan-media-trimmer.jar`
