package ir.xweb.test.module;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.testng.Assert.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestJsonPost extends TestModule {

    public void test() throws IOException {
        final String json = "{\"test\":[\"OK1\",\"OK2\"]}";

        // set up
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final StringReader reader = new StringReader(json);

        when(request.getSession()).thenReturn(getSession());
        when(request.getHeader("Content-Type")).thenReturn("application/json");
        when(request.getReader()).thenReturn(new BufferedReader(reader));

        final HttpServletResponse response = mock(HttpServletResponse.class);
        final StringWriter writer = new StringWriter();

        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        final EmptyModule module = getManager().getModule(EmptyModule.class);
        module.process(getServletContext(), request, response, null);

        assertEquals(json, writer.toString());
    }

}
