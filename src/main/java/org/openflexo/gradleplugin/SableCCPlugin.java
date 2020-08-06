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

import static org.gradle.api.plugins.JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME;

import java.io.File;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import groovy.lang.Closure;

/**
 * A plugin for adding SableCC support to {@link JavaPlugin java projects}.
 */
public class SableCCPlugin implements Plugin<Project> {
	private final ObjectFactory objectFactory;

	@Inject
	public SableCCPlugin(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}

	@Override
	public void apply(final Project project) {
		project.getPluginManager().apply(JavaPlugin.class);

		final Configuration sableccConfiguration = project.getConfigurations().create("sablecc").setVisible(false)
				.setDescription("The SableCC libraries to be used for this project.");

		sableccConfiguration.defaultDependencies(new Action<DependencySet>() {
			@Override
			public void execute(DependencySet dependencies) {
				dependencies.add(project.getDependencies().create("com.peterlavalle:sablecc-maven.sablecc:3.7@jar"));
			}
		});

		project.getConfigurations().getByName(IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(sableccConfiguration);

		project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(new Action<SourceSet>() {
			@Override
			public void execute(final SourceSet sourceSet) {
				// for each source set we will:
				// 1) Add a new 'sablecc' virtual directory mapping
				final SableCCSourceVirtualDirectoryImpl sableccDirectoryDelegate = new SableCCSourceVirtualDirectoryImpl(
						((DefaultSourceSet) sourceSet).getDisplayName(), objectFactory);
				new DslObject(sourceSet).getConvention().getPlugins().put(SableCCSourceVirtualDirectory.NAME, sableccDirectoryDelegate);
				final String srcDir = "src/" + sourceSet.getName() + "/sablecc";
				sableccDirectoryDelegate.getSableCC().srcDir(srcDir);
				sourceSet.getAllSource().source(sableccDirectoryDelegate.getSableCC());

				// 2) create an SableCCTask for this sourceSet following the gradle
				// naming conventions via call to sourceSet.getTaskName()
				final String taskName = sourceSet.getTaskName("generate", "GrammarSource");

				// 3) Set up the SableCC output directory (adding to javac inputs!)
				final String outputDirectoryName = project.getBuildDir() + "/generated-sources/sablecc/" + sourceSet.getName();
				final File outputDirectory = new File(outputDirectoryName);
				sourceSet.getJava().srcDir(outputDirectory);
				sourceSet.getResources().srcDir(outputDirectory);
				Closure<Boolean> closure = new Closure<Boolean>(null) {
					public Boolean call(Object arg) {
						FileTreeElement e = (FileTreeElement) arg;
						return e.getPath().contains("generated-sources") && !e.getPath().endsWith(".dat");
					}
				};
				sourceSet.getResources().exclude(closure);

				project.getTasks().register(taskName, SableCCTask.class, new Action<SableCCTask>() {
					@Override
					public void execute(SableCCTask sableccTask) {
						sableccTask.setDescription("Processes the " + sourceSet.getName() + " SableCC grammars.");
						// 4) set up convention mapping for default sources (allows user to not have to specify)
						sableccTask.setSource(sableccDirectoryDelegate.getSableCC());
						sableccTask.setOutputDirectory(outputDirectory);
					}
				});

				// 5) register fact that sablecc should be run before compiling
				project.getTasks().named(sourceSet.getCompileJavaTaskName(), new Action<Task>() {
					@Override
					public void execute(Task task) {
						task.dependsOn(taskName);
					}
				});
			}
		});
	}
}
