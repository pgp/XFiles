# XFiles

A general-purpose file explorer for (rooted) Android, with support for compressed archives, remote file servers, device-to-device file transfers, checksums, etc...

[![Build Status](https://travis-ci.org/pgp/XFiles.svg?branch=master)](https://travis-ci.org/pgp/XFiles)

[<img src="https://i.ibb.co/74VcyM3/00mainview.jpg" alt="00mainview" width="120" border="0">](https://ibb.co/wsCH6Gn)
[<img src="https://i.ibb.co/HG8Fz6r/00mainviewgrid.jpg" alt="00mainviewgrid" width="120" border="0">](https://ibb.co/nQJgc9D)
[<img src="https://i.ibb.co/tDDGPZH/001quicksearch.jpg" alt="001quicksearch" width="120" border="0">](https://ibb.co/899C4zm)
[<img src="https://i.ibb.co/nDDWbKL/002xreserver.jpg" alt="002xreserver" width="120" border="0">](https://ibb.co/LCCG8sZ)
[<img src="https://i.ibb.co/58qddy6/003advancedsort.jpg" alt="003advancedsort" width="120" border="0">](https://ibb.co/bBpxxZP)
[<img src="https://i.ibb.co/wJDhJzj/004compress.jpg" alt="004compress" width="120" border="0">](https://ibb.co/jV7kVrs)
[<img src="https://i.ibb.co/566G6H4/005compressbrowse.jpg" alt="005compressbrowse" width="120" border="0">](https://ibb.co/pxxbxFw)
[<img src="https://i.ibb.co/sHYmZjx/006contsel.jpg" alt="006contsel" width="120" border="0">](https://ibb.co/ySjfr0v)
[<img src="https://i.ibb.co/XjFMPtv/007contselgrid.jpg" alt="007contselgrid" width="120" border="0">](https://ibb.co/gmth8JB)
[<img src="https://i.ibb.co/6RX3Zvq/008advancedsearch.jpg" alt="008advancedsearch" width="120" border="0">](https://ibb.co/5WKN9YJ)
[<img src="https://i.ibb.co/JHLMnKS/009checksum.jpg" alt="009checksum" width="120" border="0">](https://ibb.co/M7T3nsJ)
[<img src="https://i.ibb.co/7YrwsnB/010godir.jpg" alt="010godir" width="120" border="0">](https://ibb.co/SsVhFtH)

## Getting Started

Two main components:
- XFiles application (that will be built as APK)
- Roothelper native executable (which gets cloned from [here](https://github.com/pgp/XFilesRootHelper) and built automatically by the Android gradle script)

### Releases
Prebuilt APKs are available in the [Releases](https://github.com/pgp/XFiles/releases) section of this repo. RootHelper standalone binaries will be available soon.

### Demos
Usage demos are available [here](https://github.com/pgp/XFilesDemos).

### Building from source 
## Prerequisites

1. Windows, Linux or MacOS with git preinstalled
1. [Android Studio 3.4+](https://developer.android.com/studio/index.html) with CMake and NDK plugins (it is recommended to install them from SDK Manager BEFORE importing the project)

### Build instructions

#### Roothelper
0. The build script for RootHelper is invoked automatically as inner task of the Android gradle build script, and the library binaries are put in the lib folder of the project, in order to be packed into the APK. Follow the below steps if you want to perform a manual build instead.
1. Clone the [Roothelper](https://github.com/pgp/XFilesRootHelper) repository, or change dir into the automatically cloned repo from this project's root.
1. Botan amalgamated source code files for all needed target platforms are already provided in the botanAm folder, however if you want to regenerate them after having downloaded/cloned the official source from [here](https://github.com/randombit/botan): just run the script **_genBotanAmalgamations.sh_** ( or **_genBotanAmalgamations.bat_** for Windows ) after having set the variables $BOTAN_SRC_DIR to the Botan source dir
2. Point a terminal to the roothelper source base dir, then run the **_genRootHelpersAndroid.sh_** / **_genRootHelpersAndroid.bat_** script, after having set the $NDKDIR variable to the extracted path (the default should already be fine under Linux), and the $XFILES_ASSET_DIR one to the root path of the XFiles project
3. If you want instead to build RootHelper for your desktop platform (Windows/Mac/Linux/BSD), just run **_genRootHelperDesktopFull.sh_** / **_genRootHelperDesktopFull.bat_** which starts a CMake configure+build, or **_genRootHelperDesktop.sh_** / **_genRootHelperDesktop.bat_** which rebuilds a preconfigured CMake project

#### XFiles
1. Open Android Studio, and then the SDK Manager
2. Install latest NDK and CMake
3. Open the project (the first sync will last a few minutes, due to the native library build step)
3. Build the project/Build APK from the IDE


## Development tools used

* [Android Studio](https://developer.android.com/studio/index.html) - Main IDE
* [PyCharm](https://www.jetbrains.com/) - For miscellaneous test cases, especially for RootHelper
* [Geany](https://geany.org) - My favourite text editor on non-Windows systems
* [Notepad++](https://notepad-plus-plus.org) - My favourite text editor on Windows
* [Meld](http://meldmerge.org) - An advanced DIFF tool
* [Irfanview](https://www.irfanview.com),[Paint.NET](https://www.getpaint.net),[Inkscape](https://inkscape.org),[ImageMagick](https://www.imagemagick.org/script/index.php) for icon design and processing

## Contributing
* Suggestions, bugfixes, pull-requests are welcome

## License
Both XFiles and RootHelper are released under the LGPL license, with unRAR clause for some files of p7zip source in RootHelper. See the [LICENSE](LICENSE) and [LICENSE_UNRAR](LICENSE_UNRAR) files for further details.
