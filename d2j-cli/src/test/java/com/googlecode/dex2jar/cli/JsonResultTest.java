package com.googlecode.dex2jar.cli;

import org.junit.Test;
import static org.junit.Assert.*;

public class JsonResultTest {

    @Test
    public void testOk() {
        JsonResult r = JsonResult.ok("dex2jar", "d2j dex2jar app.apk", "done", "");
        assertTrue(r.success);
        assertEquals("dex2jar", r.tool);
        assertEquals("d2j dex2jar app.apk", r.command);
        assertEquals(0, r.exitCode);
        assertEquals("done", r.stdout);
        assertEquals("", r.stderr);
    }

    @Test
    public void testFail() {
        JsonResult r = JsonResult.fail("dex2jar", "d2j dex2jar app.apk", 1, "error msg");
        assertFalse(r.success);
        assertEquals("dex2jar", r.tool);
        assertEquals(1, r.exitCode);
        assertEquals("error msg", r.stderr);
    }

    @Test
    public void testError() {
        JsonResult r = JsonResult.error("test", "something went wrong");
        assertFalse(r.success);
        assertEquals(-1, r.exitCode);
        assertEquals("something went wrong", r.stderr);
    }

    @Test
    public void testToJson_containsFields() {
        JsonResult r = JsonResult.ok("dex2jar", "d2j dex2jar app.apk", "ok", "");
        String json = r.toJson();
        assertTrue(json.contains("\"success\": true"));
        assertTrue(json.contains("\"tool\": \"dex2jar\""));
        assertTrue(json.contains("\"exitCode\": 0"));
    }
}
