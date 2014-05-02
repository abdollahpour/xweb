package ir.xweb.test.module;

import org.junit.Test;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestJsonPost extends TestModule {


    public TestJsonPost() throws IOException {
    }

    @Test
    public void testJsonPost() throws IOException {
        // set up
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getSession()).thenReturn(getSession());
        when(request.getHeader("Content-Type")).thenReturn("application/json");
        when(request.getInputStream()).thenReturn(new ServletInputStream() {

            StringReader reader = new StringReader("{\"test\": [\"OK1\", \"OK2\"]}");

            @Override
            public int read() throws IOException {
                return reader.read();
            }
        });

        final EmptyModule module = getManager().getModule(EmptyModule.class);

        module.process(getServletContext(), request, response, null);
    }

}
