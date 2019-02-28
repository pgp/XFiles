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
- Roothelper native executable (embedded as binaries in the libs folder)

### Releases
Prebuilt APKs are available in the [Releases](https://github.com/pgp/XFiles/releases) section of this repo. RootHelper standalone binaries will be available soon.

### Demos
Usage demos are available [here](https://github.com/pgp/XFilesDemos).

### Building from source 
## Prerequisites

1. [Optional] [Botan 2.9.0](https://botan.randombit.net/releases/Botan-2.9.0.tgz) 
2. [Python 2.7](https://www.python.org) (already included in any recent Linux distro)
3. [Android Studio 3.0+](https://developer.android.com/studio/index.html)

Build has been tested on Linux Mint 19 and Manjaro (x64)

### Build instructions

#### Roothelper
0. Clone the [Roothelper](https://github.com/pgp/XFilesRootHelper) repository
1. [Optional] Botan amalgamated source code files for all needed target platforms are already provided in the botanAm folder, however if you want to regenerate them after having downloaded the official source: just run the script **_genBotanAmalgamations.sh_** after having set the variables $BOTAN_SRC_DIR to the extracted Botan source dir
2. Point a terminal to the roothelper source base dir, then run the **_genRootHelpersAndroid.sh_** script, after having set the $NDKDIR variable to the extracted path (the default should already be fine under Linux), and the $XFILES_ASSET_DIR one to the root path of the XFiles project
3. If you want to build RootHelper for your desktop platform (Windows/Mac/Linux/BSD), just run **_genRootHelperDesktopFull.sh_** which starts a CMake configure+build, or **_genRootHelperDesktop.sh_** which rebuilds a preconfigured CMake project

#### XFiles
1. Open Android Studio, then open the XFiles project (open, not import)
2. If you plan to build Roothelper from source, ensure to install also latest NDK; install also CMake from the SDK Manager
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
