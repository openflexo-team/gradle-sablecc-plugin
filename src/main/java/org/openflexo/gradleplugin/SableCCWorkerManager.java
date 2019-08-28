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

import java.io.File;

import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.process.internal.worker.SingleRequestWorkerProcessBuilder;
import org.gradle.process.internal.worker.WorkerProcessFactory;

public class SableCCWorkerManager {

	public SableCCResult runWorker(File workingDir, WorkerProcessFactory workerFactory, SableCCSpec spec) {

		SableCCWorker sableccWorker = createWorkerProcess(workingDir, workerFactory, spec);
		return sableccWorker.runSableCC(spec);
	}

	private SableCCWorker createWorkerProcess(File workingDir, WorkerProcessFactory workerFactory, SableCCSpec spec) {
		SingleRequestWorkerProcessBuilder<SableCCWorker> builder = workerFactory.singleRequestWorker(SableCCWorker.class,
				SableCCExecuter.class);
		builder.setBaseName("Gradle SABLECC Worker");

		builder.sharedPackages(new String[] { "sablecc", "org.sablecc" });
		JavaExecHandleBuilder javaCommand = builder.getJavaCommand();
		javaCommand.setWorkingDir(workingDir);
		javaCommand.setMaxHeapSize(spec.getMaxHeapSize());
		javaCommand.systemProperty("SABLECC_DO_NOT_EXIT", "true");
		javaCommand.redirectErrorStream();
		return builder.build();
	}
}
