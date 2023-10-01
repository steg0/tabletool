package de.steg0.deskapps.tabletool;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CallableStatementMatchers
{
    private static final Pattern CALLABLE_STATEMENT_PATTERN = Pattern.compile(
        "^(?:\\-\\-[^\\n]*\\n|\\s*\\n)*(begin|declare|create|call|\\{|)(.*)$",
        Pattern.CASE_INSENSITIVE|Pattern.DOTALL);

    static Matcher prefixMatch(String s)
    {
        var m = CALLABLE_STATEMENT_PATTERN.matcher(s);
        var matches = m.matches();
        assert matches : "the pattern is designed to match always";
        return m;
    }
}