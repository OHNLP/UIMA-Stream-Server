package edu.mayo.bsi.uima.server.core;

import edu.mayo.bsi.uima.server.api.UIMANLPResultSerializer;
import edu.mayo.bsi.uima.server.api.UIMAServer;
import edu.mayo.bsi.uima.server.api.UIMAServerPlugin;
import edu.mayo.bsi.uima.server.api.UIMAStream;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.pear.util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Serves as a base class common to the various UIMA server implementations: loads plugins and creates relevant streams
 */
public abstract class UIMAServerBase implements UIMAServer {

    private Map<String, UIMAServerPlugin> plugins;
    private Map<String, UIMAStream> streams;
    private Map<String, UIMANLPResultSerializer> serializers;
    private URLClassLoader classLoader;


    protected UIMAServerBase() {
        this.classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        this.plugins = new HashMap<>();
        this.streams = new HashMap<>();
        this.serializers = new HashMap<>();
        init();
    }

    private void init() {
        loadLibs();
        loadPlugins();
        enablePlugins();
        start();
    }

    private void enablePlugins() {
        for (UIMAServerPlugin p : plugins.values()) {
            p.onEnable(this);
        }
    }

    private void loadLibs() {
        File libDir = new File("libs");
        if (!libDir.exists()) {
            if (!libDir.mkdirs()) {
                throw new RuntimeException("Could not create a lib folder");
            }
        }
        if (!libDir.isDirectory()) {
            throw new RuntimeException("/lib is reserved and is not a directory");
        }
        File[] libJars = libDir.listFiles(new FileUtil.ExtFilenameFilter("jar"));
        if (libJars == null || libJars.length == 0) {
            return;
        }
        for (File jar : libJars) {
            try {
                addURL(jar.toURI().toURL());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadPlugins() {
        // Plugin loading
        File pluginDir = new File("plugins");
        if (!pluginDir.exists()) {
            if (!pluginDir.mkdirs()) {
                throw new RuntimeException("Could not create a plugin folder");
            }
        }
        if (!pluginDir.isDirectory()) {
            throw new RuntimeException("/plugins is reserved and is not a directory");
        }
        File[] pluginJars = pluginDir.listFiles(new FileUtil.ExtFilenameFilter("jar"));
        if (pluginJars == null || pluginJars.length == 0) {
            throw new RuntimeException("Either plugins were not accessible or at least one must exist");
        }
        for (File jar : pluginJars) {
            try {
                addURL(jar.toURI().toURL());
            } catch (IOException e) {
                e.printStackTrace();
            }
            UIMAServerPlugin plugin = loadUIMAServerPlugin(jar);
            if (plugin == null) {
                continue;
            }
            plugins.put(plugin.getName().toLowerCase(), plugin);
        }
    }

    private UIMAServerPlugin loadUIMAServerPlugin(File file) {
        try {
            JarFile jar = new JarFile(file);
            Enumeration<JarEntry> entries = jar.entries();
            String mainClass = null;
            while (entries.hasMoreElements()) {
                JarEntry element = entries.nextElement();

                if (element.getName().equalsIgnoreCase("plugin-class.info")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(element)));
                    String next = reader.readLine();
                    if (next != null) {
                        mainClass = next;
                        break;
                    }
                }
            }
            if (mainClass != null) {
                Class<?> pluginClazz = Class.forName(mainClass, true, classLoader);
                Constructor<?> ctor = pluginClazz.getConstructor();
                jar.close();
                return (UIMAServerPlugin) ctor.newInstance();
            } else {
                return null;
            }
        } catch (IOException | ClassNotFoundException | NoSuchMethodException |
                IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addURL(URL url) throws IOException {
        Class<?> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, url);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
    }

    @Override
    public UIMAServerPlugin getPlugin(String pluginName) {
        return plugins.get(pluginName.toLowerCase());
    }

    public UIMAStream getStream(String streamName) {
        return streams.get(streamName.toLowerCase());
    }

    public UIMAStream registerStream(String streamName, AnalysisEngineDescription metadataDesc, AnalysisEngineDescription pipelineDesc) {
        if (getStream(streamName) != null) {
            throw new IllegalStateException("A stream with " + streamName + " has already been registered!");
        }
        try {
            UIMAStream ret = UIMAStream.build(streamName.toLowerCase(), metadataDesc, pipelineDesc).get();
            streams.put(streamName.toLowerCase(), ret);
            return ret;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Building of stream " + streamName + " failed!", e);
        }
    }

    public UIMANLPResultSerializer getSerializer(String serializerName) {
        return serializers.get(serializerName.toLowerCase());
    }

    public void registerSerializer(String streamName, UIMANLPResultSerializer serializer) {
        if (getSerializer(streamName) != null) {
            throw new IllegalStateException("A stream with " + streamName + " has already been registered!");
        }
        serializers.put(streamName.toLowerCase(), serializer);
    }
}
