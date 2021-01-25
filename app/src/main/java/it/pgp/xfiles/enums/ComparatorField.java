package it.pgp.xfiles.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.Pair;

public enum ComparatorField {
    FILENAME,
    DATE,
    SIZE,
    TYPE,
    DIR;

    public static final Map<Locale,Map<ComparatorField,String>> localizedLabels;

    public static final Map<Integer, Pair<ComparatorField,Boolean>> fromResMap;

    static {
        Map<Locale,Map<ComparatorField,String>> localizedLabels_ = new HashMap<>();

        // IT
        Map<ComparatorField,String> itMap = new HashMap<>();
        itMap.put(FILENAME,"Nome file");
        itMap.put(DATE,"Data");
        itMap.put(SIZE,"Dimensione");
        itMap.put(TYPE,"Tipo");
        itMap.put(DIR,"Cartella");
        localizedLabels_.put(Locale.ITALIAN,Collections.unmodifiableMap(itMap));

        // EN
        Map<ComparatorField,String> enMap = new HashMap<>();
        enMap.put(FILENAME,"Filename");
        enMap.put(DATE,"Date");
        enMap.put(SIZE,"Size");
        enMap.put(TYPE,"Type");
        enMap.put(DIR,"Directory");
        localizedLabels_.put(Locale.ENGLISH,Collections.unmodifiableMap(enMap));

        // ...Add other languages if needed...

        localizedLabels = Collections.unmodifiableMap(localizedLabels_);

        fromResMap = new HashMap<>();
        fromResMap.put(R.id.sortByFilename,new Pair<>(ComparatorField.FILENAME,false));
        fromResMap.put(R.id.sortByFilenameDesc,new Pair<>(ComparatorField.FILENAME,true));
        fromResMap.put(R.id.sortByDate,new Pair<>(ComparatorField.DATE,false));
        fromResMap.put(R.id.sortByDateDesc,new Pair<>(ComparatorField.DATE,true));
        fromResMap.put(R.id.sortBySize,new Pair<>(ComparatorField.SIZE,false));
        fromResMap.put(R.id.sortBySizeDesc,new Pair<>(ComparatorField.SIZE,true));
        fromResMap.put(R.id.sortByType,new Pair<>(ComparatorField.TYPE,false));
        fromResMap.put(R.id.sortByTypeDesc,new Pair<>(ComparatorField.TYPE,true));
    }

    public static String getLocalizedString(Locale locale,ComparatorField comparatorField) {
        try {
            return localizedLabels.get(locale).get(comparatorField);
        }
        catch (NullPointerException n) {
            return null;
        }
    }

    public String getLocalized(Locale locale) {
        try {
            return localizedLabels.get(locale).get(this);
        }
        catch (NullPointerException n) {
            return localizedLabels.get(Locale.ENGLISH).get(this);
        }
    }

    // DEBUG
//    public String name1() {
//        switch(this) {
//            case FILENAME:
//                return "FILE";
//            case DATE:
//                return "DATE";
//            case SIZE:
//                return "SIZE";
//            case TYPE:
//                return "TYPE";
//            case DIR:
//                return "DIR1";
//        }
//        return null;
//    }
}
