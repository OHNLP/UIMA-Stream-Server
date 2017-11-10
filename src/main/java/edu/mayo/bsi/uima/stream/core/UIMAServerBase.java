package edu.mayo.bsi.uima.stream.core;

import edu.mayo.bsi.uima.stream.api.UIMANLPResultSerializer;
import edu.mayo.bsi.uima.stream.api.UIMAServer;
import edu.mayo.bsi.uima.stream.api.UIMAServerPlugin;
import edu.mayo.bsi.uima.stream.api.UIMAStream;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.pear.util.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Serves as a base class common to the various UIMA server implementations: loads plugins and creates relevant streams
 */
public abstract class UIMAServerBase extends URLClassLoader implements UIMAServer {

    private Map<String, UIMAServerPlugin> plugins;
    private Map<String, UIMAStream> streams;
    private Map<String, UIMANLPResultSerializer> serializers;


    public UIMAServerBase() {
        super(((URLClassLoader) UIMAServer.class.getClassLoader()).getURLs(), UIMAServer.class.getClassLoader());
        this.plugins = new HashMap<>();
        this.streams = new HashMap<>();
        this.serializers = new HashMap<>();
        init();
    }

    private void init() {
        loadPlugins();
        start();
    }

    // Initialization
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
        File[] pluginJars = pluginDir.listFiles(new FileUtil.NameFileFilter("jar"));
        if (pluginJars == null || pluginJars.length == 0) {
            throw new RuntimeException("Either plugins were not accessible or at least one must exist");
        }
        for (File jar : pluginJars) {
            try {
                addURL(jar.toURI().toURL());
            } catch (MalformedURLException e) {
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
                if (element.getName().equalsIgnoreCase("plugin.info")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(element)));
                    String next = reader.readLine();
                    if (next != null) {
                        mainClass = next.substring(12);
                        break;
                    }
                }
            }
            if (mainClass != null) {
                Class<?> pluginClazz = Class.forName(mainClass, true, this);
                Constructor<?> ctor = pluginClazz.getConstructor(UIMAServer.class);
                jar.close();
                return (UIMAServerPlugin) ctor.newInstance(this);
            } else {
                return null;
            }
        } catch (IOException | ClassNotFoundException | NoSuchMethodException |
                IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
            return null;
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
