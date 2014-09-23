package org.activestate.stackatojenkins;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.activestate.stackatojenkins.StackatoPushPublisher.OptionalManifest;

import java.io.IOException;
import java.util.Map;

public class DeploymentInfo {

    public static final Integer DEFAULT_MEMORY = 512;
    public static final Integer DEFAULT_INSTANCES = 1;
    public static final Integer DEFAULT_TIMEOUT = 60;

    private String appName;
    private int memory;
    private String hostname;
    private int instances;
    private int timeout;
    private boolean noRoute;
    private String appPath;
    private String buildpack;
    private String command;
    private String domain;

    public DeploymentInfo(AbstractBuild build, BuildListener listener, OptionalManifest optionalManifest,
                          String jenkinsBuildName, String defaultDomain)
            throws IOException, ManifestParsingException, InterruptedException {

        // Read manifest.yml
        if (optionalManifest == null) {
            ManifestReader manifestReader = new ManifestReader(build);
            Map<String, Object> applicationInfo = manifestReader.getApplicationInfo(null);
            listener.getLogger().println(applicationInfo.toString());

            // Important optional attributes, we should warn in case they are missing.

            appName = (String) applicationInfo.get("name");
            if (appName == null) {
                listener.getLogger().
                        println("WARNING: No application name. Using Jenkins build name: " + jenkinsBuildName);
                appName = jenkinsBuildName;
            }

            int memory = 0;
            String memString = (String) applicationInfo.get("mem");
            if (memString == null) {
                listener.getLogger().
                        println("WARNING: No manifest value for memory. Using default value: " + DEFAULT_MEMORY);
                memory = DEFAULT_MEMORY;
            } else if (memString.toLowerCase().endsWith("m")) {
                memory = Integer.parseInt(memString.substring(0, memString.length()-1));
            }
            this.memory = memory;

            hostname = (String) applicationInfo.get("host");
            if (hostname == null) {
                listener.getLogger().println("WARNING: No manifest value for hostname. Using app name: " + appName);
                hostname = appName;
            }

            // Non-important optional attributes, no need to warn.

            Integer instances = (Integer) applicationInfo.get("instances");
            if (instances == null) {
                instances = DEFAULT_INSTANCES;
            }
            this.instances = instances;

            Integer timeout = (Integer) applicationInfo.get("timeout");
            if (timeout == null) {
                timeout = DEFAULT_TIMEOUT;
            }
            this.timeout = timeout;

            Boolean noRoute = (Boolean) applicationInfo.get("no-route");
            if (noRoute == null) {
                noRoute = false;
            }
            this.noRoute = noRoute;

            String domain = (String) applicationInfo.get("domain");
            if (domain == null) {
                domain = defaultDomain;
            }
            this.domain = domain;

            // Optional attributes with no defaults, it's ok if those are null.

            this.buildpack = (String) applicationInfo.get("buildpack");
            this.command = (String) applicationInfo.get("command");
            this.appPath = (String) applicationInfo.get("path");
        }
        // Read Jenkins configuration
        else {
            this.appName = optionalManifest.appName;
            if (appName.equals("")) {
                listener.getLogger().
                        println("WARNING: No application name. Using Jenkins build name: " + jenkinsBuildName);
                appName = jenkinsBuildName;
            }
            this.memory = optionalManifest.memory;
            if (memory == 0) {
                listener.getLogger().
                        println("WARNING: Missing value for memory. Using default value: " + DEFAULT_MEMORY);
                memory = DEFAULT_MEMORY;
            }
            this.hostname = optionalManifest.hostname;
            if (hostname.equals("")) {
                listener.getLogger().println("WARNING: Missing value for hostname. Using app name: " + appName);
                hostname = appName;
            }
            this.instances = optionalManifest.instances;
            this.timeout = optionalManifest.timeout;
            this.noRoute = optionalManifest.noRoute;
            this.buildpack = optionalManifest.buildpack;
            this.command = optionalManifest.command;
            this.domain = optionalManifest.domain;
            this.appPath = optionalManifest.appPath;
        }

        // TODO: env vars and services
    }

    public String getAppName() {
        return appName;
    }

    public int getMemory() {
        return memory;
    }

    public String getHostname() {
        return hostname;
    }

    public int getInstances() {
        return instances;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isNoRoute() {
        return noRoute;
    }

    public String getAppPath() {
        return appPath;
    }

    public String getBuildpack() {
        return buildpack;
    }

    public String getCommand() {
        return command;
    }

    public String getDomain() {
        return domain;
    }
}