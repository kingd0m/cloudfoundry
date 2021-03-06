/**
 * Copyright (c) ActiveState 2014 - ALL RIGHTS RESERVED.
 */

package com.activestate.cloudfoundryjenkins;

import hudson.FilePath;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DeploymentInfo {

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

    private Map<String, String> envVars = new HashMap<String, String>();
    private List<String> servicesNames = new ArrayList<String>();

    public DeploymentInfo(PrintStream logger, FilePath manifestFile, CloudFoundryPushPublisher.OptionalManifest optionalManifest,
                          String jenkinsBuildName, String defaultDomain)
            throws IOException, ManifestParsingException, InterruptedException {

        if (optionalManifest == null) {
            // Read manifest.yml
            ManifestReader manifestReader = new ManifestReader(manifestFile);
            Map<String, Object> applicationInfo = manifestReader.getApplicationInfo(null);
            readManifestFile(logger, applicationInfo, jenkinsBuildName, defaultDomain);
        } else {
            // Read Jenkins configuration
            readOptionalJenkinsConfig(logger, optionalManifest, jenkinsBuildName, defaultDomain);
        }
    }

    private void readManifestFile(PrintStream logger, Map<String, Object> manifestJson,
                                  String jenkinsBuildName, String defaultDomain) {

        // Important optional attributes, we should warn in case they are missing

        appName = (String) manifestJson.get("name");
        if (appName == null) {
            logger.println("WARNING: No application name. Using Jenkins build name: " + jenkinsBuildName);
            appName = jenkinsBuildName;
        }

        int memory = 0;
        String memString = (String) manifestJson.get("memory");
        if (memString == null) {
            logger.println("WARNING: No manifest value for memory. Using default value: " + CloudFoundryPushPublisher.DescriptorImpl.DEFAULT_MEMORY);
            memory = CloudFoundryPushPublisher.DescriptorImpl.DEFAULT_MEMORY;
        } else if (memString.toLowerCase().endsWith("m")) {
            memory = Integer.parseInt(memString.substring(0, memString.length() - 1));
        }
        this.memory = memory;

        hostname = (String) manifestJson.get("host");
        if (hostname == null) {
            logger.println("WARNING: No manifest value for hostname. Using app name: " + appName);
            hostname = appName;
        }

        // Non-important optional attributes, no need to warn

        Integer instances = (Integer) manifestJson.get("instances");
        if (instances == null) {
            instances = CloudFoundryPushPublisher.DescriptorImpl.DEFAULT_INSTANCES;
        }
        this.instances = instances;

        Integer timeout = (Integer) manifestJson.get("timeout");
        if (timeout == null) {
            timeout = CloudFoundryPushPublisher.DescriptorImpl.DEFAULT_TIMEOUT;
        }
        this.timeout = timeout;

        Boolean noRoute = (Boolean) manifestJson.get("no-route");
        if (noRoute == null) {
            noRoute = false;
        }
        this.noRoute = noRoute;

        String domain = (String) manifestJson.get("domain");
        if (domain == null) {
            domain = defaultDomain;
        }
        this.domain = domain;

        String appPath = (String) manifestJson.get("path");
        if (appPath == null) {
            appPath = ".";
        }
        this.appPath = appPath;

        // Optional attributes with no defaults, it's ok if those are null
        this.buildpack = (String) manifestJson.get("buildpack");
        this.command = (String) manifestJson.get("command");

        // Env vars and services
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> envVarsSuppressed = (Map<String, String>) manifestJson.get("env");
            if (envVarsSuppressed != null) {
                this.envVars = envVarsSuppressed;
            }
        } catch (ClassCastException e) {
            logger.println("WARNING: Could not parse env vars into a map. Ignoring env vars.");
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> servicesSuppressed = (List<String>) manifestJson.get("services");
            if (servicesSuppressed != null) {
                this.servicesNames = servicesSuppressed;
            }
        } catch (ClassCastException e) {
            logger.println("WARNING: Could not parse services into a list. Ignoring services.");
        }
    }

    private void readOptionalJenkinsConfig(PrintStream logger, CloudFoundryPushPublisher.OptionalManifest optionalManifest,
                                           String jenkinsBuildName, String defaultDomain) {

        this.appName = optionalManifest.appName;
        if (appName.equals("")) {
            logger.println("WARNING: No application name. Using Jenkins build name: " + jenkinsBuildName);
            appName = jenkinsBuildName;
        }
        this.memory = optionalManifest.memory;
        if (memory == 0) {
            logger.println("WARNING: Missing value for memory. Using default value: " + CloudFoundryPushPublisher.DescriptorImpl.DEFAULT_MEMORY);
            memory = CloudFoundryPushPublisher.DescriptorImpl.DEFAULT_MEMORY;
        }
        this.hostname = optionalManifest.hostname;
        if (hostname.equals("")) {
            logger.println("WARNING: Missing value for hostname. Using app name: " + appName);
            hostname = appName;
        }

        this.instances = optionalManifest.instances;
        if (instances == 0) {
            instances = CloudFoundryPushPublisher.DescriptorImpl.DEFAULT_INSTANCES;
        }

        this.timeout = optionalManifest.timeout;
        if (timeout == 0) {
            timeout = CloudFoundryPushPublisher.DescriptorImpl.DEFAULT_TIMEOUT;
        }

        // noRoute's default value is already false, which is acceptable
        this.noRoute = optionalManifest.noRoute;

        this.domain = optionalManifest.domain;
        if (domain.equals("")) {
            domain = defaultDomain;
        }

        // These must be null, not just empty string
        this.buildpack = optionalManifest.buildpack;
        if (buildpack.equals("")) {
            buildpack = null;
        }
        this.command = optionalManifest.command;
        if (command.equals("")) {
            command = null;
        }
        this.appPath = optionalManifest.appPath;
        if (appPath.equals("")) {
            appPath = ".";
        }

        List<CloudFoundryPushPublisher.EnvironmentVariable> manifestEnvVars = optionalManifest.envVars;
        if (manifestEnvVars != null) {
            for (CloudFoundryPushPublisher.EnvironmentVariable var : manifestEnvVars) {
                this.envVars.put(var.key, var.value);
            }
        }

        List<CloudFoundryPushPublisher.ServiceName> manifestServicesNames = optionalManifest.servicesNames;
        if (manifestServicesNames != null) {
            for (CloudFoundryPushPublisher.ServiceName service : manifestServicesNames) {
                this.servicesNames.add(service.name);
            }
        }
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

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public List<String> getServicesNames() {
        return servicesNames;
    }

}
