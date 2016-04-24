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

import com.qwazr.search.index.FieldConsumer;
import com.qwazr.search.index.QueryDefinition;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.SortField;

import java.io.IOException;

class IntDocValuesType extends FieldTypeAbstract {

	IntDocValuesType(final String fieldName, final FieldDefinition fieldDef) {
		super(fieldName, fieldDef);
	}

	@Override
	final public void fillValue(final Object value, final FieldConsumer consumer) {
		if (value instanceof Number)
			consumer.accept(new NumericDocValuesField(fieldName, ((Number) value).intValue()));
		else
			consumer.accept(new NumericDocValuesField(fieldName, Integer.parseInt(value.toString())));
	}

	@Override
	final public SortField getSortField(final QueryDefinition.SortEnum sortEnum) {
		final SortField sortField = new SortField(fieldName, SortField.Type.INT, SortUtils.sortReverse(sortEnum));
		SortUtils.sortIntMissingValue(sortEnum, sortField);
		return sortField;
	}

	@Override
	final public ValueConverter getConverter(final IndexReader reader) throws IOException {
		NumericDocValues docValues = MultiDocValues.getNumericValues(reader, fieldName);
		if (docValues == null)
			return super.getConverter(reader);
		return new ValueConverter.IntegerDVConverter(docValues);
	}
}
