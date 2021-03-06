/**
 * Copyright 2017 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.search.index;

import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LockUtils;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.store.Directory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.UUID;

class IndexInstanceManager implements Closeable {

	private final LockUtils.ReadWriteLock rwl;

	private final SchemaInstance schema;
	private final IndexFileSet fileSet;
	private final UUID indexUuid;

	private IndexSettingsDefinition settings;
	private IndexInstance indexInstance;

	IndexInstanceManager(final SchemaInstance schema, final File indexDirectory) throws IOException {

		rwl = new LockUtils.ReadWriteLock();
		this.schema = schema;
		this.fileSet = new IndexFileSet(indexDirectory);

		fileSet.checkIndexDirectory();
		indexUuid = fileSet.checkUuid();
		settings = fileSet.loadSettings();

	}

	private IndexInstance ensureOpen() throws ReflectiveOperationException, IOException, URISyntaxException {
		if (indexInstance == null)
			indexInstance = new IndexInstanceBuilder(schema, fileSet, settings, indexUuid).build();
		return indexInstance;
	}

	IndexInstance open() throws Exception {
		return rwl.writeEx(this::ensureOpen);
	}

	IndexInstance createUpdate(final IndexSettingsDefinition settings) throws Exception {
		return rwl.writeEx(() -> {
			final boolean same = Objects.equals(settings, this.settings);
			if (same && indexInstance != null)
				return indexInstance;
			closeIndex();
			if (settings != null && !same) {
				this.settings = settings;
				fileSet.writeSettings(settings);
			}
			return ensureOpen();
		});
	}

	CheckIndex.Status check() throws IOException {
		return rwl.writeEx(() -> {
			closeIndex();
			try (final Directory directory = IndexInstanceBuilder.getDirectory(settings, fileSet.dataDirectory)) {
				try (final CheckIndex checkIndex = new CheckIndex(directory)) {
					return checkIndex.checkIndex();
				}
			}
		});
	}

	/**
	 * Return the loaded instance or null if the index cannot be loaded
	 *
	 * @return the loaded instance
	 */
	IndexInstance getIndexInstance() {
		return rwl.read(() -> indexInstance);
	}

	private void closeIndex() {
		if (indexInstance == null)
			return;
		IOUtils.closeQuietly(indexInstance);
		indexInstance = null;
	}

	@Override
	public void close() throws IOException {
		rwl.writeEx(this::closeIndex);
	}

	public void delete() {
		rwl.writeEx(() -> {
			closeIndex();
			if (indexInstance != null)
				indexInstance.delete();
			indexInstance = null;
		});
	}

}
