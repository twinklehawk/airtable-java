/*
 * The MIT License (MIT)
 * Copyright (c) 2017 Sybit GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 */
package com.sybit.airtable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.sybit.airtable.exception.AirtableException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Representation Class of Airtable.
 * It is the entry class to access Airtable data.
 *
 * The API key could be passed to the app by
 * + defining Java property <code>AIRTABLE_API_KEY</code> (e.g. <code>-DAIRTABLE_API_KEY=foo</code>).
 * + defining OS environment variable <code>AIRTABLE_API_KEY</code> (e.g. <code>export AIRTABLE_API_KEY=foo</code>).
 * + defining property file `credentials.properties` in root classpath containing key/value <code>AIRTABLE_API_KEY=foo</code>.
 * + On the other hand the API-key could also be added by using the method <code>Airtable.configure(String apiKey)</code>.
 *
 * @since 0.1
 */
public class Airtable {

    private static final Logger LOG = Logger.getLogger( Airtable.class.getName() );
    private static final String ENDPOINT_URL = "https://api.airtable.com/v0";
    private static final String AIRTABLE_API_KEY = "AIRTABLE_API_KEY";
    private static final String AIRTABLE_BASE = "AIRTABLE_BASE";

    private String  endpointUrl;
    private String apiKey;

    /**
     * Configure, <code>AIRTABLE_API_KEY</code> passed by Java property, enviroment variable
     * or within credentials.properties.
     *
     * @return configured Airtable object.
     */
    public Airtable configure() throws AirtableException {

        LOG.log(Level.CONFIG, "System-Property: Using Java property '-D" + AIRTABLE_API_KEY + "' to get apikey.");
        String airtableApi = System.getProperty(AIRTABLE_API_KEY);

        if(airtableApi == null) {
            LOG.log(Level.CONFIG, "Environment-Variable: Using OS environment '" + AIRTABLE_API_KEY + "' to get apikey.");
            airtableApi = System.getenv(AIRTABLE_API_KEY);
        }
        if(airtableApi == null) {
            airtableApi = getCredentialProperty(AIRTABLE_API_KEY);
        }

        return this.configure(airtableApi);
    }



    /**
     * Configure Airtable.
     *
     * @param apiKey API-Key of Airtable.
     * @return
     */
    public Airtable configure(String apiKey) throws AirtableException {
        return configure(apiKey, ENDPOINT_URL);
    }

    /**
     *
     * @param apiKey
     * @param endpointUrl
     * @return
     */
    public Airtable configure(String apiKey, String endpointUrl) throws AirtableException {
        if(apiKey == null) {
            throw new AirtableException("Missing Airtable API-Key");
        }
        if(endpointUrl == null) {
            throw new AirtableException("Missing endpointUrl");
        }

        this.apiKey = apiKey;
        this.endpointUrl = endpointUrl;


        final String httpProxy = System.getenv("http_proxy");
        if(httpProxy != null) {
            LOG.log( Level.INFO, "Use Proxy: Environment variable 'http_proxy' found and used: " + httpProxy);
            //Unirest.setProxy(HttpHost.create(httpProxy));
        }


        // Only one time
        Unirest.setObjectMapper(new ObjectMapper() {
            final Gson gson = new GsonBuilder().create();

            public <T> T readValue(String value, Class<T> valueType) {
                LOG.log(Level.INFO, "readValue: " +value);
                return gson.fromJson(value, valueType);
            }

            public String writeValue(Object value) {
                return gson.toJson(value);
            }
        });

        return this;
    }

    /**
     * Getting the base by given property <code>AIRTABLE_BASE</code>.
     *
     * @return the base object.
     */
    public Base base() throws AirtableException {

        LOG.log(Level.CONFIG, "Using Java property '-D" + AIRTABLE_BASE + "' to get key.");
        String val = System.getProperty(AIRTABLE_BASE);

        if(val == null) {
            LOG.log(Level.CONFIG, "Environment-Variable: Using OS environment '" + AIRTABLE_BASE + "' to get base name.");
            val = System.getenv(AIRTABLE_BASE);
        }
        if(val == null) {
            val = getCredentialProperty(AIRTABLE_BASE);
        }

        return base(val);
    }

    /**
     * Builder method to create base of given base id.
     * @param base the base id.
     * @return
     */
    public Base base(String base) throws AirtableException {
        if(base == null) {
            throw new AirtableException("base was null");
        }
        final Base b = new Base(base);
        b.setParent(this);

        return b;
    }

    /**
     *
     * @return
     */
    public String endpointUrl() {
        return endpointUrl;
    }

    /**
     *
     * @return
     */
    public String apiKey() {
        return apiKey;
    }

    /**
     * Get property value from <code>/credentials.properties</code> file.
     *
     * @param key key of property.
     * @return value of property.
     */
    private String getCredentialProperty(String key) {

        final String file = "/credentials.properties";
        LOG.log(Level.CONFIG, "credentials file: Using file '" + file + "' using key '" + key + "' to get value.");
        String value;

        InputStream in = null;
        try {
            final Properties prop = new Properties();
            in = getClass().getResourceAsStream(file);
            prop.load(in);
            value = prop.getProperty(key);
        } catch (IOException | NullPointerException e) {
            LOG.throwing(this.getClass().getName(), "configure", e);
            value = null;
        } finally {
            IOUtils.closeQuietly(in);
        }

        return value;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
}