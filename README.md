![Application Icon](https://github.com/MajenkoProjects/AudiobookRecorder/raw/master/resources/uk/co/majenko/audiobookrecorder/icons/appIcon.png)

Audiobook Recorder
==================

A system for easing the task of recording and editing audiobooks.

* Zero editing
* MP3 export
* Chapter management
* Audio effect chains (biquad, delay line, etc)

Usage
-----

Step one is to open Tools -> Options and set the system up in the way you need. Mainly you will
want to set the audio settings - select the device or sound system you want to record from and
play back to.  For Linux you probably want to select "the ear-candy mixer" for PulseAudio.

Create yourself a new book either using File -> New Book or by pressing the New Book icon in  the toolbar (![toolbar icon](https://raw.githubusercontent.com/MajenkoProjects/AudiobookRecorder/master/resources/uk/co/majenko/audiobookrecorder/icons/new.png)).

Your first action with a new book should be to record the "room noise" (![toolbar icon](https://github.com/MajenkoProjects/AudiobookRecorder/raw/master/resources/uk/co/majenko/audiobookrecorder/icons/record-room.png)).  This is 5 seconds of silence recorded
from your microphone.  It is used to both calculate the "noise floor" for audio detection (see below) and also
for stitching the recorded phrases together.  You should keep as quiet as you can while it's recording.

From here on much is controlled by key presses.

* Press and hold "R" to record a new phrase - the screen flashes red while it's recording.  The phrase is
  appended to the currently selected chapter, or to the last chapter if none is selected.
* Press and hold "T" to record a new phrase that is the start of a new paragraph.  This adds the "post paragraph" gap to the previous sentence. Otherwise it does the same as "R".
* Press and hold "F" to record a "continuation" phrase. This sets the previous phrase's post-gap to be the "short" gap instead of the normal length gap.
* Press "D" to delete the last phrase you recorded.
* Press "E" to re-record the currently selected phrase.

Each phrase you record will be briefly analysed using FFT to find the start and end of the audio and set
crop marks appropriately.  These can be adjusted in the waveform display when a phrase is selected. You can also
re-run the analysis using either the default FFT method or using a peak detector method (finding the first and last points
where the audio amplitude rises above the backround noise).

The phrases also have a "post gap" associated with them.  This is the amount of room noise (in milliseconds) to place between
the current phrase and the next phrase when playing or exporting.

Speaking of playing - you have the option (in the toolbar) to either play the currently selected phrase in isolation (![toolbar icon](https://github.com/MajenkoProjects/AudiobookRecorder/raw/master/resources/uk/co/majenko/audiobookrecorder/icons/play.png)) or
to start playing from the currently selected phrase on to the end of the chapter(![toolbar icon](https://github.com/MajenkoProjects/AudiobookRecorder/raw/master/resources/uk/co/majenko/audiobookrecorder/icons/playon.png)).  This is good for testing your post gaps.

Exporting the project creates MP3 files for each chapter using the settings selected in Options.

Extra functions
---------------

The recordings are initially given a unique ID. You can
edit the text of this ID to identify the recordings. You
may, for instance, change it to have the same text as the
audio contains.  

To help with this the Haven On-Demand online speech recognition
service is integrated with the system and can be used to try and convert the 
audio into text.  Right clicking on a recording brings
up a menu which includes the option to try and convert
the audio into text.  The detected text is then used to
replace the current recording ID / text.

File layout
-----------

All data is stored in your "storage" directory (specified in Options).  Each book (which is a directory named after the
title of the book) has an associated XML file (audiobook.abk) and a directory "files" where all the audio (stored as WAV
files) is placed.

When you export the book as MP3 a new folder "export" is created within the book's folder where the MP3 files are placed.
MP3 files are all tagged with the book title, chapter title, chapter number and comment.


Building
========

1. Check out this repo
2. Install `ant` and `default-jdk`
3. Install [git LFS support](https://help.github.com/articles/installing-git-large-file-storage/)
4. Pull the large files with `git lfs pull`
5. Build with `ant build`
6. Run with `java -jar ./AudiobookRecorder.jar`

