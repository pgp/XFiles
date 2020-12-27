package it.pgp.xfiles.service.params;

import android.support.annotation.Nullable;

import java.util.List;

import it.pgp.xfiles.utils.pathcontent.BasePathContent;

public class TestParams extends ExtractParams {
    public TestParams(List<BasePathContent> srcArchives, @Nullable String password, @Nullable List<String> filenames) {
        super(srcArchives, null, password, filenames, false);
    }
}
