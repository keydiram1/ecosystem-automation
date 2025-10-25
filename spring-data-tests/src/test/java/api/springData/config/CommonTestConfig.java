/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package api.springData.config;

import api.springData.BaseIntegrationTests;
import api.springData.cache.AerospikeCacheManagerIntegrationTests.CachingComponent;
import api.springData.query.queryEngine.QueryEngineTestDataPopulator;
import com.aerospike.client.IAerospikeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.aerospike.cache.AerospikeCacheConfiguration;
import org.springframework.data.aerospike.cache.AerospikeCacheKeyProcessor;
import org.springframework.data.aerospike.cache.AerospikeCacheManager;
import org.springframework.data.aerospike.convert.MappingAerospikeConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Taras Danylchuk
 */
@Configuration
@EnableCaching
@EnableAutoConfiguration
public class CommonTestConfig {

    @Value("${spring.data.aerospike.namespace}")
    protected String namespace;

    @Bean
    @Primary
    public CacheManager cacheManager(IAerospikeClient aerospikeClient, MappingAerospikeConverter aerospikeConverter,
                                     AerospikeCacheKeyProcessor cacheKeyProcessor) {
        AerospikeCacheConfiguration defaultCacheConfiguration = new AerospikeCacheConfiguration(namespace,
                BaseIntegrationTests.DEFAULT_SET_NAME);
        AerospikeCacheConfiguration configurationWithTTL = new AerospikeCacheConfiguration(namespace,
                BaseIntegrationTests.DEFAULT_SET_NAME, 2);
        AerospikeCacheConfiguration differentCacheConfiguration = new AerospikeCacheConfiguration(namespace,
                BaseIntegrationTests.DIFFERENT_SET_NAME);
        Map<String, AerospikeCacheConfiguration> aerospikeCacheConfigurationMap = new HashMap<>();
        aerospikeCacheConfigurationMap.put(BaseIntegrationTests.CACHE_WITH_TTL, configurationWithTTL);
        aerospikeCacheConfigurationMap.put(BaseIntegrationTests.DIFFERENT_EXISTING_CACHE, differentCacheConfiguration);
        return new AerospikeCacheManager(aerospikeClient, aerospikeConverter, defaultCacheConfiguration,
                aerospikeCacheConfigurationMap, cacheKeyProcessor);
    }

    @Bean
    public CacheManager anotherCacheManager(IAerospikeClient aerospikeClient,
                                            MappingAerospikeConverter aerospikeConverter,
                                            AerospikeCacheKeyProcessor cacheKeyProcessor) {
        AerospikeCacheConfiguration defaultCacheConfiguration = new AerospikeCacheConfiguration(namespace,
                BaseIntegrationTests.DIFFERENT_SET_NAME);
        return new AerospikeCacheManager(aerospikeClient, aerospikeConverter, defaultCacheConfiguration,
                cacheKeyProcessor);
    }

    @Bean
    public CachingComponent cachingComponent() {
        return new CachingComponent();
    }

    @Bean
    public QueryEngineTestDataPopulator queryEngineTestDataPopulator(IAerospikeClient client) {
        return new QueryEngineTestDataPopulator(namespace, client);
    }

    // Uncomment to use debug mode with embedded.aerospike.enabled=false in bootstrap.properties
    /*
    @Bean
    public GenericContainer<?> aerospikeGenericContainer() {
        return new GenericContainer<>();
    }
    */
}
