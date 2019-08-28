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

import static org.gradle.api.reflect.TypeOf.typeOf;

import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.util.ConfigureUtil;

import groovy.lang.Closure;

/**
 * The implementation of the {@link org.openflexo.gradleplugin.SableCCSourceVirtualDirectory} contract.
 */
public class SableCCSourceVirtualDirectoryImpl implements SableCCSourceVirtualDirectory, HasPublicType {
	private final SourceDirectorySet sablecc;

	public SableCCSourceVirtualDirectoryImpl(String parentDisplayName, ObjectFactory objectFactory) {
		sablecc = objectFactory.sourceDirectorySet(parentDisplayName + ".sablecc", parentDisplayName + " SableCC source");
		sablecc.getFilter().include("**/*.sablecc");
	}

	@Override
	public SourceDirectorySet getSableCC() {
		return sablecc;
	}

	@Override
	public SableCCSourceVirtualDirectory sablecc(Closure configureClosure) {
		ConfigureUtil.configure(configureClosure, getSableCC());
		return this;
	}

	@Override
	public SableCCSourceVirtualDirectory sablecc(Action<? super SourceDirectorySet> configureAction) {
		configureAction.execute(getSableCC());
		return this;
	}

	@Override
	public TypeOf<?> getPublicType() {
		return typeOf(SableCCSourceVirtualDirectory.class);
	}
}
