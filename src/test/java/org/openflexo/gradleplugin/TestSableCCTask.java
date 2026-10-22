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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestSableCCTask {

	@Test
	public void upperCamelToLowerUnderscore() {
		assertEquals("class_body", SableCCTask.toLowerUnderscore("ClassBody"));
		assertEquals("a", SableCCTask.toLowerUnderscore("A"));
		assertEquals("expression", SableCCTask.toLowerUnderscore("Expression"));
		// A capital letter after a capital letter gets its underscore too, as with Guava
		assertEquals("x_m_l_parser", SableCCTask.toLowerUnderscore("XMLParser"));
		assertEquals("with_digit2_end", SableCCTask.toLowerUnderscore("WithDigit2End"));
		assertEquals("", SableCCTask.toLowerUnderscore(""));
	}
}
