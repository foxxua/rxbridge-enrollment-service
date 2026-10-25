package io.rxbridge.enrollment.common.validation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.common.validation.DomainPreconditions.*;
import static io.rxbridge.enrollment.common.TestDates.*;

class DomainPreconditionsTest {

    static final int MAX = 5;
    static final String TEXT_PATTERN = "[A-Z]{2}";

    @Nested
    class RequireText {

        @Test
        void returnsValueWithinLimits() {
            assertThat(requireText("test", "testField", MAX))
                .isEqualTo("test");
        }

        @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
        @CsvSource({
            "'   test   ', 'test'",
            "'\ttest\t', 'test'",
            "'te st', 'te st'",
            "'  \tte st\t  ', 'te st'"
        })
        void stripsOuterWhitespace(String input, String expected) {
            assertThat(requireText(input, "testField", MAX))
                .isEqualTo(expected);
        }

        @Test
        void rejectsNullText() {
            assertThatNullPointerException()
                .isThrownBy(() -> requireText(null, "testField", MAX))
                .withMessage("testField");
        }

        @ParameterizedTest
        @EmptySource
        @ValueSource(strings = {" ", "\t", " \n "})
        void rejectsMissingText(String input) {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> requireText(input, "testField", MAX))
                .withMessageContaining("testField");
        }

        @Test
        void acceptsExactlyMaxLength() {
            assertThat(requireText("a".repeat(MAX), "testField", MAX))
                .hasSize(MAX);
        }

        @Test
        void rejectsMoreThanMaxLength() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> requireText("a".repeat(MAX + 1), "testField", MAX))
                .withMessageContaining("testField")
                .withMessageContaining(String.valueOf(MAX));
        }

        @Test
        void checksLengthAfterStripping() {
            String maxWithSpaces = "  \t \n  " + "a".repeat(MAX) + "  \n \t  ";
            assertThat(requireText(maxWithSpaces, "testField", MAX))
                .hasSize(MAX);
        }

    }

    @Nested
    class OptionalText{

        @Test
        void returnsValidValue() {
            assertThat(optionalText("test", "testField", MAX))
                .isEqualTo("test");
        }

        @Test
        void returnsNullForNull() {
            assertThat(optionalText(null, "testField", MAX))
                .isNull();
        }

        @Test
        void returnsNullForBlank() {
            assertThat(optionalText(" \t \n ", "testField", MAX))
                .isNull();
        }

        @Test
        void rejectsMoreThanMaxLength() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> optionalText("a".repeat(MAX + 1), "testField", MAX))
                .withMessageContaining("testField")
                .withMessageContaining(String.valueOf(MAX));
        }

        @Test
        void stripsOuterWhitespace() {
            assertThat(optionalText("  \t \n  " + "test" + "  \n \t  ", "testField", MAX))
                .isEqualTo("test");
        }

    }

    @Nested
    class RequireMatch {

        @Test
        void returnsMatchingValue() {
            assertThat(requireMatch("TE", "testField", TEXT_PATTERN))
                .isEqualTo("TE");
        }

        @Test
        void rejectsNullText() {
            assertThatNullPointerException()
                .isThrownBy(() -> requireMatch(null, "testField", TEXT_PATTERN))
                .withMessage("testField");
        }

        @ParameterizedTest
        @EmptySource
        @ValueSource(strings = {"  ", "\t", "\n", " \t \n "})
        void rejectsWhitespaceOnly(String input) {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> requireMatch(input, "testField", TEXT_PATTERN))
                .withMessageContaining("testField");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "te", "T", "TES", "T1", " TE", "щя"
        })
        void rejectsNonMatchingValues(String input) {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> requireMatch(input, "testField", TEXT_PATTERN))
                .withMessageContaining("testField");
        }

    }

    @Nested
    class RequireNotFuture {

        @ParameterizedTest
        @MethodSource("todayAndPastDates")
        void acceptsTodayAndPastDates(LocalDate input) {
            assertThat(requireNotFuture(input, TODAY, "testField"))
                .isEqualTo(input);
        }

        @Test
        void rejectsNullDate() {
            assertThatNullPointerException()
                .isThrownBy(() -> requireNotFuture(null, TODAY, "testField"))
                .withMessage("testField");
        }

        @Test
        void rejectsNullToday() {
            assertThatNullPointerException()
                .isThrownBy(() -> requireNotFuture(YESTERDAY, null, "testField"))
                .withMessageContaining("today");
        }

        @Test
        void rejectsTomorrow() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> requireNotFuture(TOMORROW, TODAY, "testField"))
                .withMessageContaining("testField");
        }

        static Stream<LocalDate> todayAndPastDates() {
            return Stream.of(
                YESTERDAY,
                TODAY
            );
        }

    }

    @Nested
    class RequireNotBefore {

        @ParameterizedTest
        @MethodSource("boundAndLaterDates")
        void acceptsTodayAndFutureDates(LocalDate input) {
            assertThat(requireNotBefore(input, TODAY, "testField"))
                .isEqualTo(input);
        }

        @Test
        void rejectsNullDate() {
            assertThatNullPointerException()
                .isThrownBy(() -> requireNotBefore(null, TODAY, "testField"))
                .withMessageContaining("testField");
        }

        @Test
        void rejectsNullLowerBound() {
            assertThatNullPointerException()
                .isThrownBy(() -> requireNotBefore(TOMORROW, null, "testField"))
                .withMessageContaining("lowerBound");
        }

        @Test
        void rejectsYesterday() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> requireNotBefore(YESTERDAY, TODAY, "testField"))
                .withMessageContaining("testField");
        }

        static Stream<LocalDate> boundAndLaterDates() {
            return Stream.of(
                TODAY,
                TOMORROW
            );
        }

    }

}
