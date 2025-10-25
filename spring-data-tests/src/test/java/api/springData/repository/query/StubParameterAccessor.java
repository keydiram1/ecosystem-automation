/*
 * Copyright 2012-2018 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package api.springData.repository.query;

import org.springframework.data.aerospike.repository.query.AerospikeParameterAccessor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Peter Milne
 * @author Jean Mercier
 */
public class StubParameterAccessor implements AerospikeParameterAccessor {

    private final Object[] values;

    public StubParameterAccessor(Object... values) {
        this.values = values;
    }

    @Override
    public ScrollPosition getScrollPosition() {
        return null;
    }

    @Override
    public Pageable getPageable() {
        return null;
    }

    @Override
    public Sort getSort() {
        return null;
    }

    @Override
    public Object getBindableValue(int index) {
        return values[index];
    }

    @Override
    public boolean hasBindableNullValue() {
        return false;
    }

    @Override
    public Iterator<Object> iterator() {
        return Arrays.asList(values).iterator();
    }

    @Override
    public Object[] getValues() {
        return this.values;
    }

    @Override
    public Class<?> findDynamicProjection() {
        return null;
    }
}
