package com.googlecode.dex2jar.cli;

import org.junit.Test;
import static org.junit.Assert.*;

public class CommandRegistryTest {

    @Test
    public void testGetCommand_dex2jar() {
        assertNotNull(CommandRegistry.getCommand("dex2jar"));
    }

    @Test
    public void testGetCommand_baksmali() {
        assertNotNull(CommandRegistry.getCommand("baksmali"));
    }

    @Test
    public void testGetCommand_jar2dex() {
        assertNotNull(CommandRegistry.getCommand("jar2dex"));
    }

    @Test
    public void testGetCommand_unknown() {
        assertNull(CommandRegistry.getCommand("nonexistent"));
    }

    @Test
    public void testGetCommandNames() {
        String[] names = CommandRegistry.getCommandNames();
        assertTrue(names.length >= 14);
    }

    @Test
    public void testGetCommands_containsKeyTools() {
        assertTrue(CommandRegistry.getCommands().containsKey("dex2jar"));
        assertTrue(CommandRegistry.getCommands().containsKey("baksmali"));
        assertTrue(CommandRegistry.getCommands().containsKey("apk-sign"));
        assertTrue(CommandRegistry.getCommands().containsKey("jar-access"));
    }
}
