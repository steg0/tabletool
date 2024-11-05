package de.steg0.deskapps.tabletool;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlaceHolderInputControllerTest
{
    @Test
    public void selectStatement_withoutComments()
    {
        String stmt = """
                select * from d.apps where sys = 'DAC' and app = 'some.app' and env = 'pre'
                ;""";
        String actual = PlaceholderInputController.stripComments(stmt);
        assertEquals(stmt,actual);
    }

    @Test
    public void selectStatement_withComment1()
    {
        String stmt = """
                select * from d.apps where sys = 'DAC' and app = 'some.app' /*and env = 'pre'*/
                ;""";
        String expected = "select * from d.apps where sys = 'DAC' and app = 'some.app' \n;";
        String actual = PlaceholderInputController.stripComments(stmt);
        assertEquals(expected,actual);
    }

    @Test
    public void selectStatement_withComment2()
    {
        String stmt = """
                select * from d.apps where sys = 'DAC' and app = 'some.app' --and env = 'pre'
                ;""";
        /* we swallow the newline but this shouldn't be a problem. */
        String expected = "select * from d.apps where sys = 'DAC' and app = 'some.app' ;";
        String actual = PlaceholderInputController.stripComments(stmt);
        assertEquals(expected,actual);
    }

    @Test
    public void selectStatement_withComment3()
    {
        String stmt = """
                select * from d.apps where sys = 'DAC' and app = 'some.app' /*and env = 'pre'
                ;*/""";
        String expected = "select * from d.apps where sys = 'DAC' and app = 'some.app' ";
        String actual = PlaceholderInputController.stripComments(stmt);
        assertEquals(expected,actual);
    }

    @Test
    public void selectStatement_withQuotedSection()
    {
        String stmt = """
                select * from d.apps where sys = 'DAC' and app = 'some.app /*and env = ''pre''*/
                ';""";
        String expected = "select * from d.apps where sys = 'DAC' and app = 'some.app /*and env = ''pre''*/\n';";
        String actual = PlaceholderInputController.stripComments(stmt);
        assertEquals(expected,actual);
    }

    @Test
    public void selectStatement_with2Comments()
    {
        String stmt = """
                select * from d.apps where sys = 'DAC' and app = 'some.app '/*and env*/ --pre
                ;""";
        String expected = "select * from d.apps where sys = 'DAC' and app = 'some.app ' ;";
        String actual = PlaceholderInputController.stripComments(stmt);
        assertEquals(expected,actual);
    }

    @Test
    public void selectStatement_withQuotesInComments()
    {
        String stmt = """
                / */
                */* * /' -- */select * from x where y = z --'/*
                ;""";
        String expected = "/ */\n*select * from x where y = z ;";
        String actual = PlaceholderInputController.stripComments(stmt);
        assertEquals(expected,actual);
    }
}
