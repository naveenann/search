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

import com.qwazr.server.ServerException;
import com.qwazr.utils.IOUtils;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.search.IndexSearcher;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

class MultiSearch implements Closeable, AutoCloseable {

	private final MultiSearchContext context;
	private final MultiReader multiReader;
	private final IndexSearcher indexSearcher;
	private final AtomicInteger ref = new AtomicInteger(1);

	MultiSearch(final MultiSearchContext context) throws IOException, ServerException {
		this.context = context;
		if (context.indexReaders == null) {
			multiReader = null;
			indexSearcher = null;
			return;
		}
		multiReader = new MultiReader(context.indexReaders);
		indexSearcher = new IndexSearcher(multiReader);
	}

	int numDocs() {
		incRef();
		try {
			return multiReader == null ? 0 : multiReader.numDocs();
		} finally {
			decRef();
		}
	}

	private synchronized void doClose() {
		IOUtils.close(multiReader);
	}

	@Override
	final public void close() {
		decRef();
	}

	final void incRef() {
		ref.incrementAndGet();
	}

	final void decRef() {
		if (ref.decrementAndGet() > 0)
			return;
		doClose();
	}

	ResultDefinition search(final QueryDefinition queryDef,
			final ResultDocumentBuilder.BuilderFactory documentBuilderFactory)
			throws ServerException, IOException, QueryNodeException, ParseException, ReflectiveOperationException {
		if (indexSearcher == null)
			return null;
		incRef();
		try {
			final SortedSetDocValuesReaderState state = IndexUtils.getNewFacetsState(indexSearcher.getIndexReader());
			final QueryContext queryContext =
					new QueryContext(context.schemaInstance, null, indexSearcher, null, context.executorService,
							context.indexAnalyzer, context.queryAnalyzer, context.fieldMap, state, queryDef);
			return new QueryExecution(queryContext).execute(documentBuilderFactory);
		} finally {
			decRef();
		}
	}
}
