package it.pgp.xfiles.items;

import java.nio.ByteBuffer;

import it.pgp.xfiles.utils.Misc;

public class FileCreationAdvancedOptions {

    public enum CreationStrategy {
        FALLOCATE,ZEROS,RANDOM // FALLOCATE not used, but don't remove that, needed offset till ZEROS and RANDOM ordinal shifted back at protocol level to 0 and 1 instead of 1 and 2
    }

    CreationStrategy strategy;
    long size;

    public FileCreationAdvancedOptions(long size) {
        this.strategy = CreationStrategy.FALLOCATE;
        this.size = size;
    }

    public FileCreationAdvancedOptions(long size, CreationStrategy strategy) {
        this.strategy = strategy;
        this.size = size;
    }

    public byte[] toRootHelperRequestOptions() {
        ByteBuffer bb = ByteBuffer.allocate(9);
        bb.put(Misc.castUnsignedNumberToBytes(strategy.ordinal(),1));
        bb.put(Misc.castUnsignedNumberToBytes(size,8));
        return bb.array();
    }
}
