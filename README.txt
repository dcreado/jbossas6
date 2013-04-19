JBoss Application Server
$Id: README.txt 109019 2010-10-29 18:54:01Z pgier $

Directory Structure
-------------------
build - Contains the build scripts for creating the complete JBoss AS distribution.
component-matrix - Contains Maven configuration which controls dependency versions.
testsuite - Contains code and build scripts for testing the application server
thirdparty - Used during the build process
tools - Various files used by the build (ant, maven, etc.)

The remaining directories contain the various components of the application server.


Building
-------------------
From the root directory, run the build script "./build.sh" or "build.bat"
If you want to use an installation of Maven other than the one included with the
AS build, you can use "mvn install".

For slightly faster builds, the maven enforcer plugin can be skipped.
./build.sh -P-enforce

Generation of the source jars can be skipping by deactivating the sources
profile.
./build.sh -P-sources

During development you may want to build only a single module and update the 
distribution build.  This can be done using the property "module".
For example, to build the "ejb3" module and update the dist build, run the following:
./build.sh -Dmodule=ejb3

A full zip of the distribution build can be created using the "dist-zip" profile.
./build.sh -Pdist-zip


Running the Testsuite
--------------------
Change the the testsuite directory "cd testsuite"
Build the tests "./build.sh"
Run the tests "./build.sh tests"

Using Eclipse
-------------
To use eclipse you need to use the m2eclipse plugin (http://m2eclipse.sonatype.org/).
The following steps are recommended:
1. Install the latest version of eclipse
2. Set Xmx in eclipse.ini to be at least 512M, and make sure it's using java 6
3. On the command line run ./build.sh eclipse:m2eclipse
4. launch eclipse and install the m2eclipse plugin, and make sure it uses your repo configs
5. In eclipse preferences Java->Compiler->Errors/Warnings->Deprecated and restricted set forbidden reference to WARNING 
6. Use import on the root pom, which will pull in all modules
7. Wait (m2eclipse takes awhile on initial import, especially if you did not do step 3)
