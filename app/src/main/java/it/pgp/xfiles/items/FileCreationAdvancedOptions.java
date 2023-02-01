package it.pgp.xfiles.items;

import java.io.Serializable;
import java.nio.ByteBuffer;

import it.pgp.xfiles.utils.Misc;

public class FileCreationAdvancedOptions implements Serializable {

    public enum CreationStrategy {
        FALLOCATE,ZEROS,RANDOM,RANDOM_CUSTOM_SEED // FALLOCATE not used, but don't remove that, needed offset till ZEROS and RANDOM ordinal shifted back at protocol level to 0 and 1 instead of 1 and 2
    }

    CreationStrategy strategy;
    public String seed = "";
    public long size;

    public FileCreationAdvancedOptions(long size, CreationStrategy strategy) {
        this.strategy = size == 0 ? CreationStrategy.ZEROS : strategy; // fallocate returns errno 22 (invalid argument) if called with zero size
        this.size = size;
    }

    public byte[] toRootHelperRequestOptions() {
        int a = 0;
        byte[] bs = seed.getBytes();
        if(strategy == CreationStrategy.RANDOM_CUSTOM_SEED) a = 2 + bs.length;
        ByteBuffer bb = ByteBuffer.allocate(9+a);
        bb.put(Misc.castUnsignedNumberToBytes(strategy.ordinal(),1));
        if(strategy == CreationStrategy.RANDOM_CUSTOM_SEED) {
            bb.put(Misc.castUnsignedNumberToBytes(bs.length,2));
            bb.put(bs);
        }
        bb.put(Misc.castUnsignedNumberToBytes(size,8));
        return bb.array();
    }
}
