/*
 * Copyright 2017 original authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.particleframework.runtime.http.codec;

import org.particleframework.context.annotation.Bean;
import org.particleframework.context.annotation.Factory;
import org.particleframework.context.annotation.Primary;
import org.particleframework.http.codec.MediaTypeCodec;
import org.particleframework.http.codec.MediaTypeCodecRegistry;

import javax.inject.Singleton;

/**
 * A bean for the default {@link MediaTypeCodecRegistry} used by the server
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@Factory
public class MediaTypeCodecRegistryFactory {

    @Singleton
    @Primary
    @Bean
    MediaTypeCodecRegistry mediaTypeCodecRegistry(MediaTypeCodec...codecs) {
        return MediaTypeCodecRegistry.of(codecs);
    }
}
