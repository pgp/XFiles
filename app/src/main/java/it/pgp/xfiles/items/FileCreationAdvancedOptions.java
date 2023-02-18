package it.pgp.xfiles.items;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

import it.pgp.xfiles.utils.Misc;

public class FileCreationAdvancedOptions implements Serializable {

    public enum FileCreationMode {
        FALLOCATE,ZEROS,RANDOM // FALLOCATE not used, but don't remove that, needed offset till ZEROS and RANDOM ordinal shifted back at protocol level to 0 and 1 instead of 1 and 2
    }

    public static class CreationStrategyAndOptions {
        public final FileCreationMode mode;
        public String customSeed;
        public String outputHashType;
        public String backendCipher;

        public CreationStrategyAndOptions(FileCreationMode mode, String customSeed, String outputHashType, String backendCipher) {
            this.mode = mode;
            this.customSeed = customSeed;
            this.outputHashType = outputHashType;
            this.backendCipher = backendCipher;
        }

        public int getByte() {
            int ret = mode.ordinal(); // 0,1,2
            if(customSeed != null) ret |= 4;
            if(outputHashType != null) ret |= 8;
            if(backendCipher != null) ret |= 16;
            return ret;
        }
    }

    public CreationStrategyAndOptions strategy;
    public long size;

    public FileCreationAdvancedOptions(long size, CreationStrategyAndOptions strategy) {
        this.strategy = size == 0 ? new CreationStrategyAndOptions(FileCreationMode.ZEROS,null,null, null) : strategy; // fallocate returns errno 22 (invalid argument) if called with zero size
        this.size = size;
    }

    public byte[] toRootHelperRequestOptions() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(strategy.getByte());
        baos.write(Misc.castUnsignedNumberToBytes(size,8));
        if(strategy.mode == FileCreationMode.RANDOM) {
            if(strategy.customSeed != null) Misc.sendStringWithLen(baos, strategy.customSeed);
            if(strategy.outputHashType != null) Misc.sendStringWithLen(baos, strategy.outputHashType);
            if(strategy.backendCipher != null) Misc.sendStringWithLen(baos, strategy.backendCipher);
        }
        return baos.toByteArray();
    }
}
