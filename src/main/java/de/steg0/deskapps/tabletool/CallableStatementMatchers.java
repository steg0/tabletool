package de.steg0.deskapps.tabletool;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

class CallableStatementMatchers
{
    private static final Pattern CALLABLE_STATEMENT_PATTERN = compile(
            "^(?:\\-\\-[^\\n]*\\n|\\s*\\n)*(begin|declare|call|\\{|)(.*)$",
            Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
    private static final Pattern CALLABLE_STATEMENT_BLOCK_PATTERN = compile(
            "^(?:\\-\\-[^\\n]*\\n|\\s*\\n)*(begin|declare|\\{|)(.*)$",
            Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
        
    static Matcher prefixMatch(String s)
    {
        var m = CALLABLE_STATEMENT_PATTERN.matcher(s);
        var matches = m.matches();
        assert matches : "the pattern is designed to match always";
        return m;
    }

    static Matcher blockPrefixMatch(String s)
    {
        var m = CALLABLE_STATEMENT_BLOCK_PATTERN.matcher(s);
        var matches = m.matches();
        assert matches : "the pattern is designed to match always";
        return m;
    }
}