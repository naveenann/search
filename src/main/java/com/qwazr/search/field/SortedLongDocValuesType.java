/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.search.field;

import com.qwazr.search.field.Converters.MultiDVConverter;
import com.qwazr.search.field.Converters.ValueConverter;
import com.qwazr.search.index.BytesRefUtils;
import com.qwazr.search.index.FieldConsumer;
import com.qwazr.search.index.FieldMap;
import com.qwazr.search.index.QueryDefinition;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;

import java.io.IOException;

class SortedLongDocValuesType extends FieldTypeAbstract {

	SortedLongDocValuesType(final FieldMap.Item fieldMapItem) {
		super(fieldMapItem, BytesRefUtils.Converter.LONG);
	}

	@Override
	final public void fillValue(final String fieldName, final Object value, final FieldConsumer consumer) {
		if (value instanceof Number)
			consumer.accept(fieldName, new SortedNumericDocValuesField(fieldName, ((Number) value).longValue()));
		else
			consumer.accept(fieldName, new SortedNumericDocValuesField(fieldName, Long.parseLong(value.toString())));
	}

	@Override
	final public SortField getSortField(final String fieldName, final QueryDefinition.SortEnum sortEnum) {
		final SortField sortField =
				new SortedNumericSortField(fieldName, SortField.Type.LONG, SortUtils.sortReverse(sortEnum));
		SortUtils.sortLongMissingValue(sortEnum, sortField);
		return sortField;
	}

	@Override
	final public ValueConverter getConverter(final String fieldName, final IndexReader reader) throws IOException {
		final SortedNumericDocValues docValues = MultiDocValues.getSortedNumericValues(reader, fieldName);
		if (docValues == null)
			return super.getConverter(fieldName, reader);
		return new MultiDVConverter.LongSetDVConverter(docValues);
	}

}
