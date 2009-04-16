/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ok2c.lightnio.testprotocol;

public class SimplePattern {

	private final String text;
	private final int count;
	
	public SimplePattern(final String text, int count) {
		super();
		if (text == null || text.length() == 0) {
			throw new IllegalArgumentException("Text may not be null or empty");
		}
		if (count < 0) {
			throw new IllegalArgumentException("Count may not be negative");
		}
		this.text = text;
		this.count = count;
	}

	public String getText() {
		return this.text;
	}

	public int getCount() {
		return this.count;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(this.text);
		buffer.append('x');
		buffer.append(this.count);
		return buffer.toString();
	}
	
}
