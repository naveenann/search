/**
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
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
 **/
package com.qwazr.search.index;

import com.qwazr.search.analysis.AnalyzerContext;
import com.qwazr.search.analysis.AnalyzerDefinition;
import com.qwazr.search.analysis.UpdatableAnalyzer;
import com.qwazr.search.field.FieldDefinition;
import com.qwazr.server.ServerException;
import com.qwazr.utils.IOUtils;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

final class MultiSearchContext implements Closeable, AutoCloseable {

	final SchemaInstance schemaInstance;
	final ExecutorService executorService;
	final IndexReader[] indexReaders;
	final FieldMap fieldMap;
	final UpdatableAnalyzer indexAnalyzer;
	final UpdatableAnalyzer queryAnalyzer;

	MultiSearchContext(final SchemaInstance schemaInstance, final ExecutorService executorService,
			final Set<IndexInstance> indexInstances, final boolean failOnException)
			throws IOException, ServerException {
		this.schemaInstance = schemaInstance;
		this.executorService = executorService;
		if (indexInstances.isEmpty()) {
			indexReaders = null;
			indexAnalyzer = null;
			queryAnalyzer = null;
			fieldMap = null;
			return;
		}
		indexReaders = new IndexReader[indexInstances.size()];
		int i = 0;
		final Map<String, AnalyzerDefinition> analyzerMap = new HashMap<>();
		FileResourceLoader resourceLoader = null;
		final LinkedHashMap<String, FieldDefinition> fieldDefinitionMap = new LinkedHashMap<>();
		for (IndexInstance indexInstance : indexInstances) {
			indexReaders[i++] = DirectoryReader.open(indexInstance.getDataDirectory());
			indexInstance.fillFields(fieldDefinitionMap);
			indexInstance.fillAnalyzers(analyzerMap);
			resourceLoader = indexInstance.newResourceLoader(resourceLoader);
		}
		fieldMap = new FieldMap(fieldDefinitionMap);
		final AnalyzerContext analyzerContext =
				new AnalyzerContext(schemaInstance.getClassLoaderManager(), resourceLoader, analyzerMap,
						fieldDefinitionMap, failOnException);
		indexAnalyzer = new UpdatableAnalyzer(analyzerContext.indexAnalyzerMap);
		queryAnalyzer = new UpdatableAnalyzer(analyzerContext.queryAnalyzerMap);
	}

	final ResultDefinition search(final QueryDefinition queryDef,
			final ResultDocumentBuilder.BuilderFactory documentBuilderFactory)
			throws ServerException, IOException, QueryNodeException, ParseException, ReflectiveOperationException {
		return new MultiSearch(this).search(queryDef, documentBuilderFactory);
	}

	@Override
	final public void close() throws IOException {
		IOUtils.close(queryAnalyzer, indexAnalyzer);
	}
}
