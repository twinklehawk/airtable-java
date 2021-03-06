/*
 * Copyright 2020, Airtable-java Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package com.tryadhawk.airtable;

import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tryadhawk.airtable.internal.http.AirtableHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.proxy.ProxyServer;

public class Airtable {

    private final Configuration config;
    private final AirtableHttpClient airtableHttpClient;
    private final ObjectMapper objectMapper;

    public static AirtableBuilder builder() {
        return new AirtableBuilder();
    }

    private Airtable(Configuration config, AirtableHttpClient airtableHttpClient, ObjectMapper objectMapper) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.airtableHttpClient = Objects.requireNonNull(airtableHttpClient, "airtableHttpClient cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    /**
     * Build a new asynchronous client for accessing an Airtable table
     * @param baseId the id of the base containing the table
     * @param tableName the name of the table
     * @param clazz the class the table row data should be mapped to
     * @param <T> the type for {@code clazz}
     * @return the async table client
     */
    public <T> AsyncTable<T> buildAsyncTable(String baseId, String tableName, Class<T> clazz) {
        String tableUrl = config.getEndpointUrl() + "/" + baseId + "/" + tableName;
        return new AsyncTable<>(tableUrl, config.getApiKey(), clazz, airtableHttpClient, objectMapper);
    }

    /**
     * Build a new synchronous client for accessing an Airtable table
     * @param baseId the id of the base containing the table
     * @param tableName the name of the table
     * @param clazz the class the table row data should be mapped to
     * @param <T> the type for {@code clazz}
     * @return the sync table client
     */
    public <T> SyncTable<T> buildSyncTable(String baseId, String tableName, Class<T> clazz) {
        return new SyncTable<>(buildAsyncTable(baseId, tableName, clazz));
    }

    public static class AirtableBuilder {

        private Configuration config;
        private AirtableHttpClient airtableHttpClient;
        private ObjectMapper objectMapper;

        /**
         * Set the configuration, must be set before calling {@link #build()}
         * @param config the configuration for accessing Airtable
         * @return this builder
         */
        public AirtableBuilder config(Configuration config) {
            this.config = config;
            return this;
        }

        /**
         * Set the client to use to access Airtable, will use a default if not set
         * @param airtableHttpClient the client to use to access Airtable
         * @return this builder
         */
        public AirtableBuilder airtableHttpClient(AirtableHttpClient airtableHttpClient) {
            this.airtableHttpClient = airtableHttpClient;
            return this;
        }

        /**
         * Set the mapper to use to serialize and deserialize JSON, will use a default if not set
         * @param objectMapper the mapper to use
         * @return this builder
         */
        public AirtableBuilder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Airtable build() {
            Objects.requireNonNull(config, "config cannot be null");
            ObjectMapper mapper = objectMapper;
            AirtableHttpClient client = airtableHttpClient;
            if (mapper == null)
                mapper = new ObjectMapper();
            if (client == null)
                client = buildHttpClient(config, mapper);
            return new Airtable(config, client, mapper);
        }

        private AirtableHttpClient buildHttpClient(Configuration config, ObjectMapper objectMapper) {
            DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
            if (config.getTimeout() != null) {
                builder.setRequestTimeout(config.getTimeout());
                builder.setReadTimeout(config.getTimeout());
            }
            if (config.getProxy() != null)
                builder.setProxyServer(new ProxyServer.Builder(config.getProxy().getHost(), config.getProxy().getPort()).build());

            return new AirtableHttpClient(Dsl.asyncHttpClient(builder), objectMapper);
        }
    }
}
