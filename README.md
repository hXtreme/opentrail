OpenTrail
========

Android app focusing on countryside walking in England and Wales, showing
rights-of-way. See http://www.free-map.org.uk/common/opentrail.html
for more details.

Current state
-------------
OpenTrail 0.4 being worked on; this will use VTM rather than Mapsforge.

OpenTrail 0.3 (previous version) available at
http://www.free-map.org.uk/downloads/android/opentrail03-030617.apk

Building
--------

Many of the dependencies are on Maven Central and are thus downloaded and
included automatically.

However the following libraries (available as Maven projects in most cases, or,
in the case of andromaps, an Android library project) must be downloaded and
built. Once built they will be installed into your local Maven repository and
OpenTrail will build.

freemaplib: general geo-oriented Java library
https://github.com/nickw1/freemaplib.git

freemaplibProj: wrapper round Proj4Java and JCoord libraries (see below)
https://github.com/nickw1/freemaplibProj.git

javaproj: fork of the Proj4Java project, with AWT references removed
https://github.com/nickw1/javaproj.git

JCoord: Jonathan Stott's jcoord library for conversion between lat/lon and
other projections, notably OSGB, converted to a Maven project
https://github.com/nickw1/JCoord.git

andromaps: general purpose Android mapping stuff (Android library project)
https://github.com/nickw1/andromaps.git

New app icon by Jason Butwell, www.jasonbutwell.com
