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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.base.CaseFormat;

import org.gradle.api.NonNullApi;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.internal.MutableBoolean;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.reflect.JavaMethod;
import org.sablecc.sablecc.lexer.LexerException;
import org.sablecc.sablecc.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates parsers from SableCC grammars.
 */
@NonNullApi
@CacheableTask
public class SableCCTask extends SourceTask {

	private static final Logger LOGGER = LoggerFactory.getLogger(SableCCTask.class);

	private File outputDirectory;

	/**
	 * Returns the directory to generate the parser source files into.
	 *
	 * @return The output directory.
	 */
	@OutputDirectory
	public File getOutputDirectory() {
		return outputDirectory;
	}

	/**
	 * Specifies the directory to generate the parser source files into.
	 *
	 * @param outputDirectory
	 *            The output directory. Must not be null.
	 */
	public void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	@TaskAction
	public void execute(IncrementalTaskInputs inputs) {
		final Set<File> grammarFiles = new HashSet<>();
		final Set<File> sourceFiles = getSource().getFiles();
		final MutableBoolean cleanRebuild = new MutableBoolean();
		inputs.outOfDate(details -> {
			File input = details.getFile();
			if (sourceFiles.contains(input)) {
				grammarFiles.add(input);
			}
			else {
				// classpath change?
				cleanRebuild.set(true);
			}
		});
		inputs.removed(details -> {
			if (details.isRemoved()) {
				cleanRebuild.set(true);
			}
		});
		if (cleanRebuild.get()) {
			try {
				Path directory = outputDirectory.toPath();
				Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						if (dir != directory)
							Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			grammarFiles.addAll(sourceFiles);
		}

		// SableCCSpec spec = new SableCCSpec(grammarFiles, getOutputDirectory());
		try {
			final Class<?> sableccClass = Class.forName(SABLECC_MAIN_CLASS);
			JavaMethod<?, Void> method = JavaMethod.ofStatic(sableccClass, Void.class, "processGrammar", File.class, File.class);
			LOGGER.info("Processing with SableCC");
			SableCCResult result = new SableCCResult();
			for (File grammarFile : grammarFiles) {
				PrintStream originalStream = System.out;
				try (PrintStream dummyStream = new PrintStream(new OutputStream() {
					@Override
					public void write(int b) { }
				})) {
					System.setOut(dummyStream);
					try {
						method.invokeStatic(grammarFile, getOutputDirectory());
					} catch (UncheckedException e) {
						Throwable cause = e.getCause();
						if (cause instanceof ParserException) {
							ParserException ex = (ParserException) cause;
							result.addException(getException("Parse", ex.getMessage(), " found " + ex.getToken().toString()));
						}
						else if (cause instanceof LexerException) {
							LexerException ex = (LexerException) cause;
							result.addException(getException("Lexing", ex.getMessage(), ""));
						}
						else
							result.addException(cause);
                                        } catch (RuntimeException e) {
                                            String message = e.getMessage();
                                            if (message.contains("shift/reduce conflict")) {
                                                result.addException(new Exception("Your grammar has conflicts: " + message));
                                            } else {
                                                Pattern p = Pattern.compile("\\[([0-9]*),([0-9]*)\\] P(\\w*) and T(\\w*) undefined.");
                                                Matcher matcher = p.matcher(message);
                                                try {
                                                    matcher.find();
                                                    result.addException(new Exception("Error in the grammar at line " + matcher.group(1) + " character " + matcher.group(2) + ", the symbol "
                                                                                      + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, matcher.group(3)) + " is undefined"));
                                                } catch (Exception ee) {
                                                    throw e;
                                                }
                                            }
					} finally {
						System.setOut(originalStream);
					}
				}
			}
			int errorCount = result.getErrorCount();
			if (errorCount == 0) {
				System.out.println("Generated code for " + grammarFiles.toString() + " in " + getOutputDirectory());
			}
			else if (errorCount < 0) {
				throw new SableCCSourceGenerationException("There were errors during grammar generation", result.getFirstException());
			}
			else if (errorCount == 1) {
				throw new SableCCSourceGenerationException("There was 1 error during grammar generation", result.getFirstException());
			}
			else if (errorCount > 1) {
				throw new SableCCSourceGenerationException("There were " + errorCount + " errors during grammar generation",
						result.getFirstException());
			}
		} catch (ClassNotFoundException cnf) {
			throw new IllegalStateException("No SableCC implementation available");
		}
	}

	private static Exception getException(String kind, String message, String posttext) {
		Pattern p = Pattern.compile("\\[([0-9]*),([0-9]*)\\] (.*)");
		Matcher matcher = p.matcher(message);
		matcher.find();
		return new Exception(kind + " error in the grammar at line " + matcher.group(1) + " character " + matcher.group(2) + ", "
				+ matcher.group(3) + posttext);

	}

	private static String SABLECC_MAIN_CLASS = "org.sablecc.sablecc.SableCC";

	/**
	 * Sets the source for this task. Delegates to {@link #setSource(Object)}.
	 *
	 * If the source is of type {@link SourceDirectorySet}, then the relative path of each source grammar files is used to determine the
	 * relative output path of the generated source If the source is not of type {@link SourceDirectorySet}, then the generated source files
	 * end up flattened in the specified output directory.
	 *
	 * @param source
	 *            The source.
	 * @since 4.0
	 */
	@Override
	public void setSource(FileTree source) {
		setSource((Object) source);
	}

	/**
	 * Sets the source for this task. Delegates to {@link SourceTask#setSource(Object)}.
	 *
	 * If the source is of type {@link SourceDirectorySet}, then the relative path of each source grammar files is used to determine the
	 * relative output path of the generated source If the source is not of type {@link SourceDirectorySet}, then the generated source files
	 * end up flattened in the specified output directory.
	 *
	 * @param source
	 *            The source.
	 */
	@Override
	public void setSource(Object source) {
		super.setSource(source);
	}

	/**
	 * Returns the source for this task, after the include and exclude patterns have been applied. Ignores source files which do not exist.
	 *
	 * @return The source.
	 */
	@Override
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileTree getSource() {
		return super.getSource();
	}
}
