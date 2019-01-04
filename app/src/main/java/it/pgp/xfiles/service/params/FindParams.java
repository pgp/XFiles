package it.pgp.xfiles.service.params;

import android.support.annotation.Nullable;

import java.io.Serializable;

/**
 * Created by pgp on 11/12/17
 */

public class FindParams implements Serializable {

    public String targetFolder;
    public String expr;

    public FindParams(@Nullable String targetFolder, @Nullable String expr) {
        this.targetFolder = targetFolder;
        this.expr = expr;
    }
}
