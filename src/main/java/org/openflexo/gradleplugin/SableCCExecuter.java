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
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.internal.reflect.JavaMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SableCCExecuter implements SableCCWorker {

	private static final Logger LOGGER = LoggerFactory.getLogger(SableCCExecuter.class);

	public static String SABLECC_MAIN_CLASS = "org.sablecc.sablecc.SableCC";

	@Override
	public SableCCResult runSableCC(SableCCSpec spec) {
		SableCCTool antlrTool = new SableCCTool();
		if (antlrTool.available()) {
			LOGGER.info("Processing with SableCC");
			return antlrTool.process(spec);
		}

		throw new IllegalStateException("No SableCC implementation available");
	}

	static class SableCCTool {
		void invoke(List<String> arguments, File inputFile, File outputDir) throws ClassNotFoundException {
			final Class<?> sableccClass = loadTool(SABLECC_MAIN_CLASS);
			JavaMethod.ofStatic(sableccClass, Void.class, "processGrammar", File.class, File.class).invokeStatic(inputFile, outputDir);
		}

		public boolean available() {
			try {
				loadTool(SABLECC_MAIN_CLASS);
			} catch (ClassNotFoundException cnf) {
				return false;
			}
			return true;
		}

		/**
		 * Utility method to create an instance of the Tool class.
		 *
		 * @throws ClassNotFoundException
		 *             if class was not on the runtime classpath.
		 */
		static Class<?> loadTool(String className) throws ClassNotFoundException {
			try {
				return Class.forName(className); // ok to use caller classloader
			} catch (ClassNotFoundException cnf) {
				throw cnf;
			} catch (Exception e) {
				throw new GradleException("Failed to load SableCC", e);
			}
		}

		public final SableCCResult process(SableCCSpec spec) {
			try {
				return doProcess(spec);
			} catch (ClassNotFoundException e) {
				// this shouldn't happen if you call check availability with #available first
				throw new GradleException("Cannot process antlr sources", e);
			}
		}

		/**
		 * process used for antlr3/4
		 */
		public SableCCResult doProcess(SableCCSpec spec) throws ClassNotFoundException {
			for (File grammarFile : spec.getGrammarFiles()) {
				invoke(spec.getArguments(), grammarFile, spec.getOutputDirectory());
			}
			return new SableCCResult(0);
		}

		protected static String[] toArray(List<String> strings) {
			return strings.toArray(new String[0]);
		}

	}

}
