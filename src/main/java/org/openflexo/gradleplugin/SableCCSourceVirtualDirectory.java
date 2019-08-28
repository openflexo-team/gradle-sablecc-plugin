/*
 * Copyright 2019 Openflexo
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

package org.openflexo.gradleplugin;

import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;

import groovy.lang.Closure;

/**
 * Contract for a Gradle "convention object" that acts as a handler for what I call a virtual directory mapping, injecting a virtual
 * directory named 'sablecc' into the project's various {@link org.gradle.api.tasks.SourceSet source sets}.
 */
public interface SableCCSourceVirtualDirectory {
	String NAME = "sablecc";

	/**
	 * All SableCC source for this source set.
	 *
	 * @return The SableCC source. Never returns null.
	 */
	SourceDirectorySet getSableCC();

	/**
	 * Configures the SableCC source for this set. The given closure is used to configure the {@link org.gradle.api.file.SourceDirectorySet}
	 * (see {@link #getSableCC}) which contains the SableCC source.
	 *
	 * @param configureClosure
	 *            The closure to use to configure the SableCC source.
	 * @return this
	 */
	SableCCSourceVirtualDirectory sablecc(Closure configureClosure);

	/**
	 * Configures the SableCC source for this set. The given action is used to configure the {@link org.gradle.api.file.SourceDirectorySet}
	 * (see {@link #getSableCC}) which contains the SableCC source.
	 *
	 * @param configureAction
	 *            The action to use to configure the SableCC source.
	 * @return this
	 * @since 3.5
	 */
	SableCCSourceVirtualDirectory sablecc(Action<? super SourceDirectorySet> configureAction);
}
