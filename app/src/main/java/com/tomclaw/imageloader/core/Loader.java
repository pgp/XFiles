package com.tomclaw.imageloader.core;

import java.io.File;
import java.util.List;

public interface Loader {

    List<String> getSchemes();

    boolean load(String uriString, File file);
}
