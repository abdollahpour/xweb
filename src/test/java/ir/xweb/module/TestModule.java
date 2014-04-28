
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Test module unit.
 */
public class TestModule {

    /**
     * test.
     */
    private final ServletContext servletContext;

    /**
     * test.
     */
    private final HttpSession session;

    /**
     * test.
     */
    private final Manager manager;

    /**
     * Create new instance with default params.
     */
    public TestModule() {
        this(null);
    }

    /**
     * Create new instance with custom params or null for default params.
     *
     * @param config custom param
     */
    public TestModule(final TestModuleConfig config) {

        // Setup for test
        System.setProperty("ir.xweb.test", "true");

        // Mock objects
        servletContext = mock(ServletContext.class);
        session = mock(HttpSession.class);

        // Setup configs
        final TestModuleConfig p;
        if (config != null) {
            p = config;
        }
        else {
            p = new DefaultTestModuleConfig();
        }

        // Mock methods
        when(servletContext.getInitParameterNames()).thenReturn(
            new IteratorEnumeration(p.servletInitParams().keySet().iterator()));
        doAnswer(new Answer<Object>() {
            public Object answer(final InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                final File file = new File(p.getContextDir(), args[0].toString());
                return file.getAbsolutePath();
            }
        }).when(servletContext).getRealPath(anyString());

        manager = new Manager(servletContext);
        try {
            manager.load(p.getXWebFile().toURI().toURL());
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }

        final Map<String, Module> modules = manager.getModules();
        for (Module module : modules.values()) {
            module.init(servletContext);
        }
        //for(Module module:module.values()) {
        //    module.initFilter();
        //}
    }

    /**
     * Get manager.
     *
     * @return manager
     */
    public final Manager getManager() {
        return manager;
    }

    /**
     * Get HTTP session.
     *
     * @return session
     */
    public final HttpSession getSession() {
        return session;
    }

    /**
     * Get mocked servlet context
     *
     * @return mocked servlet context
     */
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Module test configurations.
     */
    public interface TestModuleConfig {

        /**
         * Servlet initialize parameters.
         *
         * @return Map of params
         */
        Map<String, String> servletInitParams();

        /**
         * Get web ROOT folder.
         * @return web ROOT folder
         */
        File getContextDir();

        /**
         * xweb.xml config file.
         * @return file path
         */
        File getXWebFile();

    }

    /**
     * {@inheritDoc}.
     */
    public class DefaultTestModuleConfig implements TestModuleConfig {

        @Override
        public final Map<String, String> servletInitParams() {
            return Collections.EMPTY_MAP;
        }

        @Override
        public final File getContextDir() {
            final File inTest = new File("src/test/webapp");
            if (inTest.exists()) {
                return inTest;
            }

            final File inMain = new File("src/main/webapp");
            if (inMain.exists()) {
                return inMain;
            }

            // In resources
            return new File("src/main/resources");
        }

        @Override
        public final File getXWebFile() {
            final String[] files = new String[]{
                "src/test/resources/WEB-INF/xweb.xml",
                "src/test/webapp/WEB-INF/xweb.xml",
                "src/main/resources/WEB-INF/xweb.xml",
                "src/main/webapp/WEB-INF/xweb.xml"
            };

            for (String f:files) {
                final File file = new File(f);
                if(file.exists()) {
                    return file;
                }
            }

            return null;
        }
    }

    /**
     * Iterator to Enumeration.
     *
     * @param <E>
     */
    private class IteratorEnumeration<E> implements Enumeration<E> {

        /**
         * Iterator source.
         */
        private final Iterator<E> iterator;

        /**
         * constructor.
         *
         * @param iterator iterator source.
         */
        public IteratorEnumeration(final Iterator<E> iterator) {
            this.iterator = iterator;
        }

        /**
         * {@inheritDoc}
         */
        public E nextElement() {
            return this.iterator.next();
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasMoreElements() {
            return this.iterator.hasNext();
        }

    }

}

