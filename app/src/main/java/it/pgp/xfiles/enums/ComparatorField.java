package it.pgp.xfiles.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by pgp on 26/10/16
 * Last modified on 30/10/16
 */

public enum ComparatorField {
    FILENAME,
    DATE,
    SIZE,
    TYPE,
    DIR;

    public static final Map<Locale,Map<ComparatorField,String>> localizedLabels;

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
}
