package ir.xweb.data;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

public class InstrumentHook {

    public static void premain(String agentArgs, Instrumentation inst) {
        //System.out.println("Premain");
        if (agentArgs != null) {
            System.getProperties().put(AGENT_ARGS_KEY, agentArgs);
        }
        System.getProperties().put(INSTRUMENTATION_KEY, inst);
    }

    public static Instrumentation getInstrumentation() {
        return (Instrumentation) System.getProperties().get(INSTRUMENTATION_KEY);
    }

    // Needn't be a UUID - can be a String or any other object that
    // implements equals().
    private static final Object AGENT_ARGS_KEY =
            UUID.fromString("887b43f3-c742-4b87-978d-70d2db74e40e");

    private static final Object INSTRUMENTATION_KEY =
            UUID.fromString("214ac54a-60a5-417e-b3b8-772e80a16667");

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws java.io.IOException
     */
    public static Class<?>[] getClasses(String packageName)
            throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        for (File directory : dirs) {
            try {
                classes.addAll(findClasses(directory, packageName));
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    public static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

}
