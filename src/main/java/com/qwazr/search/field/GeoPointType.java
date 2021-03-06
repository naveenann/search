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
import com.qwazr.search.index.FieldMap;
import org.apache.lucene.spatial.geopoint.document.GeoPointField;

import java.util.Map;

class GeoPointType extends StorableFieldType {

	GeoPointType(final FieldMap.Item fieldMapItem) {
		super(fieldMapItem, null);
	}

	@Override
	protected void fillArray(final String fieldName, final double[] values, final FieldConsumer consumer) {
		if ((values.length & 1) != 0)
			throw new RuntimeException("Expect even double values, but got: " + values.length);
		for (int i = 0; i < values.length; )
			consumer.accept(fieldName, new GeoPointField(fieldName, values[i++], values[i++], store));
	}

	@Override
	protected void fillArray(final String fieldName, final float[] values, final FieldConsumer consumer) {
		if ((values.length & 1) != 0)
			throw new RuntimeException("Expect even float values, but got: " + values.length);
		for (int i = 0; i < values.length; )
			consumer.accept(fieldName, new GeoPointField(fieldName, values[i++], values[i++], store));
	}

	@Override
	protected void fillArray(final String fieldName, final Object[] values, final FieldConsumer consumer) {
		if ((values.length & 1) != 0)
			throw new RuntimeException("Expect even number values, but got: " + values.length);
		for (int i = 0; i < values.length; )
			consumer.accept(fieldName, new GeoPointField(fieldName, ((Number) values[i++]).doubleValue(),
					((Number) values[i++]).doubleValue(), store));
	}

	@Override
	protected void fillMap(final String fieldName, final Map<Object, Object> values, final FieldConsumer consumer) {
		final Number latitude = (Number) values.get("lat");
		TypeUtils.notNull(latitude, fieldName, "The latitude parameter (lat) is missing");
		final Number longitude = (Number) values.get("lon");
		TypeUtils.notNull(longitude, fieldName, "The longitude parameter (lon) is missing");
		consumer.accept(fieldName,
				new GeoPointField(fieldName, latitude.doubleValue(), longitude.doubleValue(), store));
	}

	@Override
	public void fillValue(final String fieldName, final Object value, final FieldConsumer fieldConsumer) {
		throw new RuntimeException(
				"Unsupported value type for GeoPoint: " + value.getClass() + ". An array of numbers is expected.");
	}

}
