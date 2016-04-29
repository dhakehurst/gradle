/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.artifacts.repositories;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.gradle.api.artifacts.repositories.AuthenticationContainer;
import org.gradle.api.artifacts.repositories.P2ArtifactRepository;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ConfiguredModuleComponentRepository;
import org.gradle.api.internal.artifacts.repositories.p2.DefaultP2Resolver;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.internal.reflect.Instantiator;

public class DefaultP2ArtifactRepository extends AbstractAuthenticationSupportedRepository implements P2ArtifactRepository, ResolutionAwareRepository {
    
	private final FileResolver fileResolver;
    private Object url;
    private String group;
    private URI localCacheUri;
    
    public DefaultP2ArtifactRepository(FileResolver fileResolver, Instantiator instantiator,
                                          AuthenticationContainer authenticationContainer) {
        super(instantiator, authenticationContainer);
        this.fileResolver = fileResolver;
    }

    
	
	public String getGroup() {
		return this.group;
	}
	public void setGroup(String value) {
		this.group = value;
	}
	public void group(String value) {
		this.setGroup(value);
	}
    
    
    public URI getUri() {
        return url == null ? null : fileResolver.resolveUri(url);
    }

    public void setUri(Object url) {
        this.url = url;
    }

    
	
	public URI getLocalCacheUri() {
		if (null==localCacheUri) {
			//use default value
			String userHome = System.getProperty("user.home");
			String defaultValue = userHome+"/.gradle-p2/";
			this.localCacheUri = Paths.get(defaultValue).toUri();
		}
		return this.localCacheUri;
	}
	public void localCacheUri(String value) {
		this.setLocalCacheUri(value);
	}
	public void setLocalCacheUri(String value) {
		try {
			this.localCacheUri = new URI(value);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Cannot create URI for "+value,e);
		}	
	}
    

    public ConfiguredModuleComponentRepository createResolver() {
		URI p2Uri = this.getUri();
		URI lcUri = this.getLocalCacheUri();
		return new DefaultP2Resolver(group, p2Uri, lcUri);
    }
    
    
}
