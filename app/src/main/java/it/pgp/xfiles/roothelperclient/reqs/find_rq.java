package it.pgp.xfiles.roothelperclient.reqs;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import it.pgp.xfiles.io.FlushingBufferedOutputStream;
import it.pgp.xfiles.roothelperclient.ControlCodes;
import it.pgp.xfiles.utils.Misc;

public class find_rq extends BaseRHRequest {

    public List<byte[]> basepaths; // where to search into
    @Nullable private byte[] contentPattern;
    @Nullable private byte[] filenamePattern;
    private SearchBits searchBits;
    private FlagBits flagBits;

    // to be XORed over request byte
    public static class FlagBits {
        boolean cancelCurrentSearch = false;
        boolean searchOnlyCurrentFolder = false; // false means recurse search in subfolders
        boolean searchInArchives = false;

        public FlagBits(boolean cancelCurrentSearch, boolean searchOnlyCurrentFolder, boolean searchInArchives) {
            this.cancelCurrentSearch = cancelCurrentSearch;
            this.searchOnlyCurrentFolder = searchOnlyCurrentFolder;
            this.searchInArchives = searchInArchives;
        }

        // cancel search
        public FlagBits() {
            cancelCurrentSearch = true;
        }

        public FlagBits(boolean searchOnlyCurrentFolder) {
            this.searchOnlyCurrentFolder = searchOnlyCurrentFolder;
        }

        public byte getFlagBits() {
            byte bits = 0;
            if (cancelCurrentSearch) return 1;
            if (searchOnlyCurrentFolder) bits |= 2;
            if (searchInArchives) bits |= 4;
            return bits;
        }
    }

    public static class SearchBits {
        public SearchBits(boolean filenameRegex,
                          boolean filenameEscapes,
                          boolean filenameCaseInsensitive,
                          boolean filenameWholeWord,
                          boolean contentRegex,
                          boolean contentEscapes,
                          boolean contentCaseInsensitive,
                          boolean contentWholeWord,
                          boolean findAllContentOccurrences) {
            this.filenameRegex = filenameRegex;
            this.filenameEscapes = filenameEscapes;
            this.filenameCaseInsensitive = filenameCaseInsensitive;
            this.filenameWholeWord = filenameWholeWord;
            this.contentRegex = contentRegex;
            this.contentEscapes = contentEscapes;
            this.contentCaseInsensitive = contentCaseInsensitive;
            this.contentWholeWord = contentWholeWord;
            this.findAllContentOccurrences = findAllContentOccurrences;
        }

        boolean filenameRegex,filenameEscapes,filenameWholeWord,filenameCaseInsensitive;
        boolean contentRegex,contentEscapes,contentWholeWord,contentCaseInsensitive;
        boolean findAllContentOccurrences;

        byte[] getSearchBits() {
            byte[] searchBits = new byte[2];

            searchBits[0] |= filenameRegex ? 1 : 0;
            searchBits[0] |= (filenameEscapes ? 1 : 0) << 1;
            searchBits[0] |= (filenameCaseInsensitive ? 1 : 0) << 2;
            searchBits[0] |= (filenameWholeWord ? 1 : 0) << 3;

            searchBits[0] |= (contentRegex ? 1 : 0)<<4;
            searchBits[0] |= (contentEscapes ? 1 : 0) << 5;
            searchBits[0] |= (contentCaseInsensitive ? 1 : 0) << 6;
            searchBits[0] |= (contentWholeWord ? 1 : 0) << 7;

            searchBits[1] |= findAllContentOccurrences ? 1 : 0;

            return searchBits;
        }
    }

    public find_rq(
            List<byte[]> basepaths,
            @Nullable byte[] filenamePattern,
            @Nullable byte[] contentPattern,
            FlagBits flagBits,
            SearchBits searchBits) {
        super(ControlCodes.ACTION_FIND);
        this.basepaths = basepaths;
        this.filenamePattern = filenamePattern==null?new byte[0]:filenamePattern;
        this.contentPattern = contentPattern==null?new byte[0]:contentPattern;
        this.searchBits = searchBits;
        this.flagBits = flagBits;
    }

    // cancel search
    public find_rq() {
        super(ControlCodes.ACTION_FIND);
        flagBits = new FlagBits();
    }

    @Override
    public byte getRequestByteWithFlags() {
        byte rq = requestType.getValue();
        rq ^= (flagBits.getFlagBits() << (ControlCodes.rq_bit_length));
        return rq;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        try(FlushingBufferedOutputStream nbf = new FlushingBufferedOutputStream(outputStream)) {
            nbf.write(getRequestByteWithFlags());
            if (flagBits.cancelCurrentSearch) return;

            nbf.write(searchBits.getSearchBits());

            for(byte[] basepath : basepaths) {
                nbf.write(Misc.castUnsignedNumberToBytes(basepath.length,2));
                nbf.write(basepath);
            }
            nbf.write(new byte[2]); // eol

            nbf.write(Misc.castUnsignedNumberToBytes(filenamePattern.length,2));
            if (filenamePattern.length!=0) nbf.write(filenamePattern);

            nbf.write(Misc.castUnsignedNumberToBytes(contentPattern.length,2));
            if (contentPattern.length!=0) nbf.write(contentPattern);
        }
    }
}