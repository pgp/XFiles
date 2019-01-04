# XFiles

A general-purpose file explorer for (rooted) Android, with support for compressed archives, remote file servers, device-to-device file transfers, checksums, etc...

## Getting Started

Two main components:
- XFiles application (that will be built as APK)
- Roothelper native executable (embedded as binaries in the libs folder)

### Releases
Prebuilt APKs and RootHelper binaries will be available soon.

### Demos
Usage demos will be available soon.

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
