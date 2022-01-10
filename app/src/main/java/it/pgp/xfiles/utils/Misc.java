package it.pgp.xfiles.utils;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.pgp.xfiles.roothelperclient.ResponseCodes;

/**
 * Created by pgp on 23/06/17
 * Miscellaneous low-level util methods
 */

public class Misc {
    public static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static File internalStorageDir = Environment.getExternalStorageDirectory();
    public static final byte[] EOL = new byte[2]; // end-of-list indication in RH protocol

    public static String toHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static long castBytesToUnsignedNumber(byte[] b, Integer cut_) {
        long value = 0;
        int cut = b.length;
        if (cut_ != null) cut = cut_;
        for (int i = cut-1; i >= 0; i--)
            value = (value << 8) + (b[i] & 0xFF);
        return value;
    }

    public static long castBytesToUnsignedNumberWithBigInteger(byte[] b, Integer cut_) {
        BigInteger value = BigInteger.ZERO;
        int cut = b.length;
        if (cut_ != null) cut = cut_;
        for (int i = cut-1; i >= 0; i--) { // replace with decrement for for little endianness compliance
            value = value.shiftLeft(8);
            value = value.add(BigInteger.valueOf(b[i] & 0xFF));
        }
        return value.longValue();
    }

    public static byte[] castUnsignedNumberToBytes(long l, Integer cut_) {
        byte[] o = (cut_ == null)?new byte[8]:new byte[cut_];
        long mask = 0xFF;
        for (int i=0;i<o.length;i++) {
            o[i] = (byte) (l & mask);
            l >>= 8; // FIXME maybe better to replace with unsigned shift >>>
        }
        return o;
    }

    public static int receiveBaseResponse(DataInputStream i) throws IOException {
        byte resp = i.readByte();
        ResponseCodes c = ResponseCodes.getCode(resp);

        if(c != null) {
            switch (c) {
                case RESPONSE_OK:
                    return 0;
                case RESPONSE_ERROR:
                    byte[] errno_ = new byte[4];
                    i.readFully(errno_);
                    int errno = (int) Misc.castBytesToUnsignedNumber(errno_,4);
                    Log.e("roothelper", "Error returned from roothelper server: " + errno);
                    return errno;
                default:
                    throw new RuntimeException("Illegal response byte from roothelper server");
            }
        }
        else {
            throw new RuntimeException("Empty response from roothelper server");
        }
    }

    public static long receiveTotalOrProgress(DataInputStream i) throws IOException {
        byte[] b = new byte[8];
        i.readFully(b);
        return castBytesToUnsignedNumber(b,null);
    }

    public static String receiveStringWithLen(DataInputStream i) throws IOException {
        byte[] tmp = new byte[2];
        i.readFully(tmp);
        int len = (int) Misc.castBytesToUnsignedNumber(tmp,2);
        tmp = new byte[len];
        i.readFully(tmp);
        return new String(tmp);
    }

    public static void sendStringWithLen(OutputStream o, String s) throws IOException {
        byte[] b = s.getBytes();
        byte[] len = Misc.castUnsignedNumberToBytes(b.length,2);
        o.write(len);
        o.write(b);
    }

    @SafeVarargs
    public static <T> T[] concatAll(T[] first, T[]... rest) {
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    public static boolean writeStringToFilePath(String s, String path) {
        try (OutputStream o = new BufferedOutputStream(
                new FileOutputStream(path))) {
            o.write(s.getBytes());
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // TODO TO BE TESTED
//    public static String getLongestCommonPrefix(String... strings) {
//        if (strings.length == 0) return "";
//        if (strings.length == 1) return strings[0];
//        int commonPrefixLength = 0;
//        while (allCharactersAreSame(strings, commonPrefixLength)) {
//            commonPrefixLength++;
//        }
//        return strings[0].substring(0, commonPrefixLength);
//    }
//
//    private static boolean allCharactersAreSame(String[] strings, int pos) {
//        String first = strings[0];
//        for (String curString : strings) {
//            if (curString.length() <= pos
//                    || curString.charAt(pos) != first.charAt(pos)) {
//                return false;
//            }
//        }
//        return true;
//    }

    public static String getLongestCommonPrefix(List<String> strings) {
        if (strings.size() == 0) return "";
        if (strings.size() == 1) return strings.get(0);
        int commonPrefixLength = 0;
        while (allCharactersAreSame(strings, commonPrefixLength)) {
            commonPrefixLength++;
        }
        return strings.get(0).substring(0, commonPrefixLength);
    }

    private static boolean allCharactersAreSame(List<String> strings, int pos) {
        String first = strings.get(0);
        for (String curString : strings) {
            if (curString.length() <= pos
                    || curString.charAt(pos) != first.charAt(pos)) {
                return false;
            }
        }
        return true;
    }

    public static String getLongestCommonPathFromPrefix(String s) {
        if (s.equals("") || s.equals("/")) return "/";
        int i = s.lastIndexOf('/');
        if (i < 0) throw new RuntimeException("Malformed common path prefix");
        return s.substring(0,i); // also valid for single selection, if not malformed (e.g. ending with /)
    }

    public static List<String> splitByteArrayOverByteAndEncode(byte[] b, byte targetByte) {
        List<String> outs = new ArrayList<>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (byte value : b) {
            if (value != targetByte) baos.write(value);
            else {
                outs.add(new String(baos.toByteArray())); // assumed default charset: UTF-8
                baos.reset();
            }
        }
        return outs;
    }

    // CSV escape (for checksum export)
    public static final String[] csvToBeEscaped = {"\"",",","\n"};

    public static final byte[] crlf = new byte[]{'\r','\n'};

    /**
     * - if filename contains any between { \n " , }, use enclosing quotes
     * - if filename contains " escape it as ""
     */
    public static String escapeForCSV(String filename) {
        boolean enclosingQuotes = false;
        for(String x : csvToBeEscaped)
            if(filename.contains(x)) {
                enclosingQuotes = true;
                break;
            }

        return enclosingQuotes?"\""+filename.replace("\"","\"\"")+"\"":filename;
    }

    public static void csvWriteRow(OutputStream o, List<String> row) throws IOException {
        for(int i=0;i<row.size()-1;i++)
            o.write((escapeForCSV(row.get(i))+",").getBytes(StandardCharsets.UTF_8));

        // fine to have IndexOutOfBounds with empty list
        o.write(escapeForCSV(row.get(row.size()-1)).getBytes(StandardCharsets.UTF_8));
        o.write(crlf);
    }

    public static View getViewByPosition(int pos, AbsListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    /**
     * Web source:
     * https://stackoverflow.com/questions/1247772/is-there-an-equivalent-of-java-util-regex-for-glob-type-patterns
     *
     * Converts a standard POSIX Shell globbing pattern into a regular expression
     * pattern. The result can be used with the standard {@link java.util.regex} API to
     * recognize strings which match the glob pattern.
     * <p/>
     * See also, the POSIX Shell language:
     * http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01
     *
     * @param pattern A glob pattern.
     * @return A regex pattern to recognize the given glob pattern.
     */
    public static String convertGlobToRegex(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
                case '\\':
                    if (++i >= arr.length) {
                        sb.append('\\');
                    } else {
                        char next = arr[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                sb.append('\\');
                            default:
                                sb.append('\\');
                        }
                        sb.append(next);
                    }
                    break;
                case '*':
                    if (inClass == 0)
                        sb.append(".*");
                    else
                        sb.append('*');
                    break;
                case '?':
                    if (inClass == 0)
                        sb.append('.');
                    else
                        sb.append('?');
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i+1;
                    sb.append('[');
                    break;
                case ']':
                    inClass--;
                    sb.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                        sb.append('\\');
                    sb.append(ch);
                    break;
                case '!':
                    if (firstIndexInClass == i)
                        sb.append('^');
                    else
                        sb.append('!');
                    break;
                case '{':
                    inGroup++;
                    sb.append('(');
                    break;
                case '}':
                    inGroup--;
                    sb.append(')');
                    break;
                case ',':
                    if (inGroup > 0)
                        sb.append('|');
                    else
                        sb.append(',');
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    // discriminant for item duplication is type T's hashCode implementation
    public static <T> Map<T, Set<Integer>> createOccurrencesMap(Iterable<T> input) {
        Map<T,Set<Integer>> m = new HashMap<>();
        int i=0;
        for(T t : input) {
            Set<Integer> li = m.get(t);
            if(li==null) {
                li = new HashSet<>();
                li.add(i);
                m.put(t,li);
            }
            else li.add(i);
            i++;
        }
        return m;
    }

    // web source:
    // https://ssaurel.medium.com/create-a-blink-effect-on-android-3c76b5e0e36b
    public static void highlightListViewItem(int pos, AbsListView absListView) {
        final View view = getViewByPosition(pos, absListView);
        final Drawable oldBg = view.getBackground();
        int oldBgColor = (oldBg instanceof ColorDrawable) ? ((ColorDrawable) oldBg).getColor() : Color.TRANSPARENT;
        ObjectAnimator anim = ObjectAnimator.ofInt(view,"backgroundColor", oldBgColor, Color.GREEN, oldBgColor);
        anim.setDuration(2000);
        anim.setEvaluator(new ArgbEvaluator());
        anim.setRepeatCount(0);
        anim.start();
    }

    public static String escapeHtml(String s) {
        byte[] bb = s.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            for(byte b : bb) {
                char c = (char)b;
                switch(c) {
                    case '&':  baos.write("&amp;".getBytes(StandardCharsets.UTF_8)); break;
                    case '\"': baos.write("&quot;".getBytes(StandardCharsets.UTF_8)); break;
                    case '\'': baos.write("&apos;".getBytes(StandardCharsets.UTF_8)); break;
                    case '<':  baos.write("&lt;".getBytes(StandardCharsets.UTF_8)); break;
                    case '>':  baos.write("&gt;".getBytes(StandardCharsets.UTF_8)); break;
                    default:   baos.write(b); break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(baos.toByteArray(),StandardCharsets.UTF_8);
    }
}
