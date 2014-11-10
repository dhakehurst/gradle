/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.play.internal;

import org.gradle.platform.base.internal.toolchain.ToolProvider;
import org.gradle.play.platform.PlayPlatform;

public class DefaultPlayToolChain implements PlayToolChainInternal {

    private final PlayPlatform platform;

    public DefaultPlayToolChain(PlayPlatform playPlatform) {
        this.platform = playPlatform;
    }

    public String getName() {
        return String.format("PlayToolchain%s", platform.getPlayVersion());
    }

    public String getDisplayName() {
        return String.format("Play Toolchain (Play %s, Scala %s, JDK %s (%s))", platform.getPlayVersion(), platform.getScalaVersion(), platform.getJavaVersion().getMajorVersion(), platform.getJavaVersion());
    }

    public ToolProvider select(PlayPlatform targetPlatform) {
        return null;
    }
}
