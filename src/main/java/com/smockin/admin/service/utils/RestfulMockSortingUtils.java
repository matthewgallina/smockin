package com.smockin.admin.service.utils;

import com.smockin.admin.persistence.entity.RestfulMock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mgallina.
 */
@Component
public class RestfulMockSortingUtils {

    private final String ZERO_NUMERIC_RANGE_REGEX = "[0-9]+";
    private final String NON_ZERO_NUMERIC_RANGE_REGEX = "[1-9]+";
    private final String ALPHA_RANGE_REGEX = "[a-zA-Z]+";
    private final String WILDCARD = "*";

    @Transactional
    public void autoOrderEndpointPaths(final List<RestfulMock> mocks) {

        int maxLen = 0;

        for (RestfulMock m : mocks) {
            if (m.getPath().length() > maxLen) {
                maxLen = m.getPath().length();
            }
        }

        Collections.sort(mocks, new AlphaNumericComparator(maxLen));
        Collections.sort(mocks, new WildcardComparator(maxLen));

        // Update the initialisation order
        int index = 1;

        for (RestfulMock m : mocks) {
            m.setInitializationOrder(index++);
        }

    }

    private class AlphaNumericComparator implements Comparator<RestfulMock> {

        private final int maxLen;

        public AlphaNumericComparator(final int maxLen) {
            this.maxLen = maxLen;
        }

        @Override
        public int compare(RestfulMock obj1, RestfulMock obj2) {

            final String o1 = obj1.getPath();
            final String o2 = obj2.getPath();

            if (o1.matches(NON_ZERO_NUMERIC_RANGE_REGEX) && o2.matches(NON_ZERO_NUMERIC_RANGE_REGEX)) {
                Integer integer1 = Integer.valueOf(o1);
                Integer integer2 = Integer.valueOf(o2);
                return integer1.compareTo(integer2);
            }

            if (o1.matches(ALPHA_RANGE_REGEX) && o2.matches(ALPHA_RANGE_REGEX)) {
                return o1.compareTo(o2);
            }

            final int result =  handleRegexComparison(o1, o2, maxLen);

            if (o1.contains(WILDCARD) && o2.contains(WILDCARD)) {
                return result;
            } else if (o1.contains(WILDCARD) && result < 0) {
                return Math.abs(result);
            } else if (o2.contains(WILDCARD) && result > 0) {
                return ( result - (result * 2) );
            }

            return result;
        }
    }

    private class WildcardComparator implements Comparator<RestfulMock> {

        private final int maxLen;

        public WildcardComparator(final int maxLen) {
            this.maxLen = maxLen;
        }

        @Override
        public int compare(RestfulMock obj1, RestfulMock obj2) {

            final String o1 = obj1.getPath();
            final String o2 = obj2.getPath();

            if (!o1.contains(WILDCARD) && !o2.contains(WILDCARD)) {
                return 0;
            }

            final int result = handleRegexComparison(o1, o2, maxLen);

            final int pos = o2.indexOf(WILDCARD);

            if ( ( pos == -1  || o1.length() < (pos + 1) ) ) {

                final int pos2 = o1.indexOf(WILDCARD);

                if ( ( pos2 == -1  || o2.length() < (pos2 + 1) ) ) {
                    return result;
                }

                final String s1 = o2.substring(0, pos2 + 1);
                final String s2 = o1.substring(0, pos2);
                final String s22 = s2 + o2.substring(pos2, pos2 + 1);

                if (s1.equalsIgnoreCase(s22) && result < 0) {
                    return Math.abs(result);
                }

                return result;
            }

            final String s1 = o1.substring(0, pos + 1);
            final String s2 = o2.substring(0, pos);
            final String s22 = s2 + o1.substring(pos, pos + 1);

            if (s1.equalsIgnoreCase(s22) && result > 0) {
                return result - (result * 2);
            }

            return result;
        }
    }

    private String leftPad(String stringToPad, String padder, Integer size) {

        final StringBuilder sb = new StringBuilder(size.intValue());
        final StringCharacterIterator sci = new StringCharacterIterator(padder);

        while (sb.length() < (size.intValue() - stringToPad.length())) {
            for (char ch = sci.first(); ch != CharacterIterator.DONE; ch = sci.next()) {
                if (sb.length() < (size.intValue() - stringToPad.length())) {
                    sb.insert(sb.length(), String.valueOf(ch));
                }
            }
        }

        return sb.append(stringToPad).toString();
    }

    private int handleRegexComparison(final String o1, final String o2, final int maxLen) {

        final String padding = "0";
        final Pattern p = Pattern.compile(ZERO_NUMERIC_RANGE_REGEX);
        final Matcher m1 = p.matcher(o1);
        final Matcher m2 = p.matcher(o2);

        final List<String> list = new ArrayList<String>();

        while (m1.find()) {
            list.add(m1.group());
        }

        for (String string : list) {
            o1.replaceFirst(string, leftPad(string, padding, maxLen));
        }

        list.clear();

        while (m2.find()) {
            list.add(m2.group());
        }

        for (String string : list) {
            o2.replaceFirst(string, leftPad(string, padding, maxLen));
        }

        return o1.compareTo(o2);
    }

}
