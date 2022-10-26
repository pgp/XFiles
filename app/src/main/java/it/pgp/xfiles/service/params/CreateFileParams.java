package it.pgp.xfiles.service.params;

import java.io.Serializable;

import it.pgp.xfiles.items.FileCreationAdvancedOptions;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

public class CreateFileParams implements Serializable {
    public BasePathContent path;
    public FileCreationAdvancedOptions opts;

    public CreateFileParams(BasePathContent path, FileCreationAdvancedOptions opts) {
        this.path = path;
        this.opts = opts;
    }
}
