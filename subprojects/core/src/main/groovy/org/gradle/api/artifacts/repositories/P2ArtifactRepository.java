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
package org.gradle.api.artifacts.repositories;

import java.net.URI;

/**
 * An artifact repository which uses P2 format to store artifacts and meta-data.
 * <p>
 * Repositories of this type are created by the {@link org.gradle.api.artifacts.dsl.RepositoryHandler#p2(org.gradle.api.Action)} group of methods.
 */
public interface P2ArtifactRepository extends ArtifactRepository, AuthenticationSupported {

	/**
	 * The group name assigned to artifacts from this p2 repository.
	 * 
	 * @return
	 */
	String getGroup();
	
	/**
	 * Sets the group name for artifacts from this p2 repository
	 * 
	 * @param value
	 */
	void setGroup(String value);
	
	/**
	 * Sets the group name for artifacts from this p2 repository
	 * 
	 * @param value
	 */
	void group(String value);
	
    /**
     * The base URI of this repository. This URI is used to find (p2) artifact files.
     *
     * @return The URI.
     */
    URI getUri();

    /**
     * Sets the base URI of this repository. This URL is used to find (p2) artifact files.
     *
     * <p>The provided value is evaluated as per {@link org.gradle.api.Project#uri(Object)}. This means, for example, you can pass in a {@code File} object, or a relative path to be evaluated relative
     * to the project directory.
     *
     * @param url The base URI.
     */
    void setUri(Object url);

}
