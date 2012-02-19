/******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.  If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for 
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is: Jsoda
 * The Initial Developer of the Original Code is: William Wong (williamw520@gmail.com)
 * Portions created by William Wong are Copyright (C) 2012 William Wong, All Rights Reserved.
 *
 ******************************************************************************/


package wwutil.model;


import java.util.*;


/** Validate a String field matches the mask expression.
 * digitMask is the mask character for matching any digit.
 * alphaMask is the mask character for matching any alphabet.
 * anyMask is the mask character for matching any character.
 * pattern is a the string expression contains the literal characters and the mask characters.
 *
 *<pre> 
 * e.g. MaskMatcher( pattern = "(###) ###-####" )
 * will match "(415) 555-1212" or "(408) 121-5555"
 *
 * e.g. MaskMatcher( pattern = "800-@@@-####" )
 * will match "800-ABC-1212" or "800-EFG-4567"
 *
 * e.g. MaskMatcher( pattern = "800-***-****" )
 * will match "800-A12-3[?D"
 *
 * e.g. MaskMatcher( pattern = "## $$/$$/$$$$ ##", digitMask = '$' )
 * redefine the digitMask to $ and will match "## 07/20/1999 ##"
 *</pre> 
 */
public class MaskMatcher
{
    private static final byte   LITERAL = (byte)0;      // exact literal match
    private static final byte   DIGIT   = (byte)1;      // match any digit
    private static final byte   LETTER  = (byte)2;      // match any alphabet
    private static final byte   ANY     = (byte)3;      // match any character

    public String           pattern;
    public char             digitMask = '#';
    public char             letterMask = '@';
    public char             anyMask = '*';
    private byte[]          types;


	public MaskMatcher(String pattern) {
        this.pattern = pattern;
        parse(pattern);
    }

	public MaskMatcher(String pattern, char digitMask, char letterMask, char anyMask) {
        this.pattern = pattern;
        this.digitMask = digitMask;
        this.letterMask = letterMask;
        this.anyMask = anyMask;
        parse(pattern);
	}

    public boolean matches(String str) {
        if (str.length() != pattern.length())
            return false;

        for (int i = 0; i < str.length(); i++) {
            switch (types[i]) {
            case LITERAL:
                if (str.charAt(i) != pattern.charAt(i))
                    return false;
                break;
            case DIGIT:
                if (!Character.isDigit(str.charAt(i)))
                    return false;
                break;
            case LETTER:
                if (!Character.isLetter(str.charAt(i)))
                    return false;
                break;
            default:
                break;
            }
        }

        return true;
    }

    private void parse(String pattern) {
        types = new byte[pattern.length()];
        for (int i = 0; i < pattern.length(); i++) {
            types[i] = getType(pattern.charAt(i));
        }
    }

    private byte getType(char ch) {
        if (ch == digitMask)
            return DIGIT;
        else if (ch == letterMask)
            return LETTER;
        else if (ch == anyMask)
            return ANY;
        else
            return LITERAL;
    }

}
