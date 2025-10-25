/*
 * Copyright 2012-2020 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements WHICH ARE COMPATIBLE WITH THE APACHE LICENSE, VERSION 2.0.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package api.springData.query;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.aerospike.util.FilterOperationRegexpBuilder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;


/*
 * Tests to ensure that Qualifiers are built successfully for non-indexed bins.
 */
@TestPropertySource(properties = {"spring.data.aerospike.namespace=source-ns12"})
@Tag("SPRING-DATA-TESTS-1")
public class FilterOperationRegexpBuilderTest {

    @Test
    public void escapesBackslash() {
        String inputStr = "a\\b";
        String expectedStr = "a\\\\b"; // a\\b

        String escapedStr = FilterOperationRegexpBuilder.escapeBRERegexp(inputStr);

        assertThat(escapedStr).isEqualTo(expectedStr);
    }

    @Test
    public void escapesDot() {
        String inputStr = "a.b";
        String expectedStr = "a\\.b"; // a\.b

        String escapedStr = FilterOperationRegexpBuilder.escapeBRERegexp(inputStr);

        assertThat(escapedStr).isEqualTo(expectedStr);
    }

    @Test
    public void escapesSquareBracket() {
        String inputStr = "a[b";
        String expectedStr = "a\\[b"; // a\[b

        String escapedStr = FilterOperationRegexpBuilder.escapeBRERegexp(inputStr);

        assertThat(escapedStr).isEqualTo(expectedStr);
    }

    @Test
    public void escapesAsterisk() {
        String inputStr = "a*b";
        String expectedStr = "a\\*b"; // a\*b

        String escapedStr = FilterOperationRegexpBuilder.escapeBRERegexp(inputStr);

        assertThat(escapedStr).isEqualTo(expectedStr);
    }

    @Test
    public void escapesCircumflex() {
        String inputStr = "a^b";
        String expectedStr = "a\\^b"; // a\^b

        String escapedStr = FilterOperationRegexpBuilder.escapeBRERegexp(inputStr);

        assertThat(escapedStr).isEqualTo(expectedStr);
    }

    @Test
    public void escapesDollar() {
        String inputStr = "a$b";
        String expectedStr = "a\\$b"; // a\$b

        String escapedStr = FilterOperationRegexpBuilder.escapeBRERegexp(inputStr);

        assertThat(escapedStr).isEqualTo(expectedStr);
    }

    @Test
    public void doesNotEscapeOtherCharacter() {
        String inputStr = "abcdefghijklmnopqrstuvwxyz";

        String escapedStr = FilterOperationRegexpBuilder.escapeBRERegexp(inputStr);

        assertThat(escapedStr).isEqualTo(inputStr);
    }

    @Test
    public void escapesMultiplSpecialCharacters() {
        String inputStr = "\\^aerospike$"; // \\\^aerospike\$
        String expectedStr = "\\\\\\^aerospike\\$"; // \^aerospike\$

        String escapedStr = FilterOperationRegexpBuilder.escapeBRERegexp(inputStr);

        assertThat(escapedStr).isEqualTo(expectedStr);
    }

    @Test
    public void buildsEscapedStartsWith() {
        String inputStr = "*aero";
        String expectedStr = "^\\*aero";

        String escapedStr = FilterOperationRegexpBuilder.getStartsWith(inputStr);

        assertThat(escapedStr).isEqualTo(expectedStr);
    }

    @Test
    public void buildsEscapedEndsWith() {
        String inputStr = "*spike*";
        String expectedStr = "\\*spike\\*$";

        String escapedStr = FilterOperationRegexpBuilder.getEndsWith(inputStr);

        assertThat(escapedStr).isEqualTo(expectedStr);
    }

    @Test
    public void buildsEscapedContaining() {
        String inputStr = "*erospi*";
        String expectedStr = "\\*erospi\\*"; // \*erospi\*

        String escapedStr = FilterOperationRegexpBuilder.getContaining(inputStr);

        assertThat(escapedStr).isEqualTo(expectedStr);
    }

    @Test
    public void buildsEscapedEquals() {
        String inputStr = "\\*aerospike*";
        String expectedStr = "^\\\\\\*aerospike\\*$"; // ^\\\*aerospike\*$

        String escapedStr = FilterOperationRegexpBuilder.getStringEquals(inputStr);

        assertThat(escapedStr).isEqualTo(expectedStr);
    }
}
