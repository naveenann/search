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
 */
package com.qwazr.search.field;

import com.qwazr.search.index.BytesRefUtils;
import com.qwazr.search.index.FieldConsumer;
import com.qwazr.search.index.FieldMap;
import com.qwazr.server.ServerException;
import org.apache.lucene.facet.taxonomy.AssociationFacetField;

import javax.ws.rs.core.Response;
import java.util.Objects;

class StringAssociationFacetType extends FieldTypeAbstract {

	StringAssociationFacetType(final FieldMap.Item fieldMapItem) {
		super(fieldMapItem, BytesRefUtils.Converter.STRING);
	}

	@Override
	protected void fillArray(final String fieldName, final Object[] values, final FieldConsumer consumer) {
		Objects.requireNonNull(values, "The value array is empty");
		if (values.length < 2)
			throw new ServerException(Response.Status.NOT_ACCEPTABLE, "Expected at least 2 values");
		final String assoc = values[0].toString();
		final String[] path = TypeUtils.getStringArray(fieldName, values, 1);
		consumer.accept(fieldName,
				new AssociationFacetField(BytesRefUtils.Converter.STRING.from(assoc), fieldName, path));
	}

}
