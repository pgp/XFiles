package it.pgp.xfiles.service.params;

import android.support.annotation.Nullable;

import java.util.List;

import it.pgp.xfiles.utils.pathcontent.BasePathContent;

public class TestParams extends ExtractParams {
    public TestParams(BasePathContent srcArchive, @Nullable String password, @Nullable List<String> filenames) {
        super(srcArchive, null, password, filenames, false);
    }
}
