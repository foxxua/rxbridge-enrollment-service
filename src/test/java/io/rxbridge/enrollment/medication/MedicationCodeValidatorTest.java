package io.rxbridge.enrollment.medication;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class MedicationCodeValidatorTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', textBlock = """
        # code     | expected
        #-------------------#
                   | true
        '   '      | true
        ' m000000' | true
        'M123456'  | true
        'M999999 ' | true
        'A123456'  | false
        '1234567'  | false
        'MMMMMMM'  | false
        '1MMMMMM'  | false
        'M123'     | false
        """)
    void checksMedicationCode(String code, boolean expected) {
        assertThat(new MedicationCodeValidator().isValid(code, null))
            .isEqualTo(expected);
    }

}
