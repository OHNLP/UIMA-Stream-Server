package edu.mayo.bsi.uima.stream.core;

import edu.mayo.bsi.uima.stream.api.UIMAServer;
import edu.mayo.bsi.uima.stream.api.UIMAServerPlugin;
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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Serves as a base class common to the various UIMA server implementations: loads plugins and creates relevant streams
 */
public abstract class UIMAServerBase extends URLClassLoader implements UIMAServer {

    protected Map<String, UIMAServerPlugin> plugins;

    public UIMAServerBase() {
        super(((URLClassLoader) UIMAServer.class.getClassLoader()).getURLs(), UIMAServer.class.getClassLoader());
        this.plugins = new HashMap<>();

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
            throw new RuntimeException("Either plugins were not accessible or at least one ");
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
}
