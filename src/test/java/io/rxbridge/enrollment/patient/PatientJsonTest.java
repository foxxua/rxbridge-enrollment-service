package io.rxbridge.enrollment.patient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

import static io.rxbridge.enrollment.patient.PatientController.PatientRegisterRequest;
import static io.rxbridge.enrollment.patient.PatientController.PersonNameRequest;
import static io.rxbridge.enrollment.patient.PatientController.AddressRequest;
import static io.rxbridge.enrollment.patient.PatientController.DuplicatesRequest;
import static io.rxbridge.enrollment.patient.PatientController.EmailRequest;
import static io.rxbridge.enrollment.common.web.TestHelpers.*;
import static io.rxbridge.enrollment.patient.PatientFixtures.*;

@JsonTest
class PatientJsonTest {

    @Autowired
    JacksonTester<PatientView> view;

    @Autowired
    JacksonTester<PatientRegisterRequest> register;

    @Autowired
    JacksonTester<PersonNameRequest> personName;

    @Autowired
    JacksonTester<AddressRequest> address;

    @Autowired
    JacksonTester<DuplicatesRequest> duplicates;

    @Autowired
    JacksonTester<EmailRequest> email;

    @Test
    void viewKeepsAgreedContract() throws Exception {
        Patient patient = withId(aPatient(), PATIENT_ID);
        var patientView = PatientView.from(patient);

        assertThat(view.write(patientView))
            .isStrictlyEqualToJson("""
                {
                  "id":"00000000-0000-0000-0000-000000000001",
                  "personName":{
                    "firstName":"William",
                    "lastName":"Shakespeare"
                  },
                  "email":"william_shakespeare@example.com",
                  "address":{
                    "street":"31 Milk Street",
                    "city":"Boston",
                    "country":"US",
                    "region":"MA",
                    "zipcode":"02196"
                  },
                  "sex":"MALE",
                  "birthdate":"1991-04-11",
                  "active":true
                }
                """);
    }

    @Test
    void registerReadsValidValues() throws Exception {
        String registerJson = """
            {
              "personName":{
                "firstName":"William",
                "lastName":"Shakespeare"
              },
              "email":"william_shakespeare@example.com",
              "address":{
                "street":"31 Milk Street",
                "city":"Boston",
                "country":"US",
                "region":"MA",
                "zipcode":"02196"
              },
              "sex":"MALE",
              "birthdate":"1991-04-11"
            }
            """;

        assertThat(register.parseObject(registerJson))
            .extracting(
                PatientRegisterRequest::personName,
                PatientRegisterRequest::email,
                PatientRegisterRequest::address,
                PatientRegisterRequest::sex,
                PatientRegisterRequest::birthdate
            ).containsExactly(
                new PersonNameRequest("William", "Shakespeare"),
                "william_shakespeare@example.com",
                new AddressRequest(
                    "31 Milk Street", "Boston",
                    "US", "MA", "02196"
                ),
                Sex.MALE,
                LocalDate.of(1991, 4, 11)
            );
    }

    @ParameterizedTest
    @EnumSource(Sex.class)
    void registerReadsValidSexValues(Sex sex) throws Exception {
        String registerJson = """
            {
              "personName":{
                "firstName":"William",
                "lastName":"Shakespeare"
              },
              "email":"william_shakespeare@example.com",
              "address":{
                "street":"31 Milk Street",
                "city":"Boston",
                "country":"US",
                "region":"MA",
                "zipcode":"02196"
              },
              "sex":"%s",
              "birthdate":"1991-04-11"
            }
            """.formatted(sex);

        assertThat(register.parseObject(registerJson).sex())
            .isEqualTo(sex);
    }

    @Test
    void registerRefusesAnotherSexValue() throws Exception {
        String registerJson = """
            {
              "personName":{
                "firstName":"William",
                "lastName":"Shakespeare"
              },
              "email":"william_shakespeare@example.com",
              "address":{
                "street":"31 Milk Street",
                "city":"Boston",
                "country":"US",
                "region":"MA",
                "zipcode":"02196"
              },
              "sex":"ANOTHER",
              "birthdate":"1991-04-11"
            }
            """;

        assertThatThrownBy(() -> register.parseObject(registerJson))
            .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    void registerRefusesAnotherDateFormat() throws Exception {
        String registerJson = """
            {
              "personName":{
                "firstName":"William",
                "lastName":"Shakespeare"
              },
              "email":"william_shakespeare@example.com",
              "address":{
                "street":"31 Milk Street",
                "city":"Boston",
                "country":"US",
                "region":"MA",
                "zipcode":"02196"
              },
              "sex":"MALE",
              "birthdate":"11.04.1991"
            }
            """;

        assertThatThrownBy(() -> register.parseObject(registerJson))
            .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    void registerRefusesInvalidValues() throws Exception {
        String registerJson = """
            {
              "personName":{
                "firstName":[],
                "lastName":{}
              },
              "email":[],
              "address":true,
              "sex":"MALE",
              "birthdate":"1991-04-11"
            }
            """;

        assertThatThrownBy(() -> register.parseObject(registerJson))
            .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void personNameReadsValidValues() throws Exception {
        String personNameJson = """
            {
              "firstName":"William",
              "lastName":"Shakespeare"
            }
            """;

        assertThat(personName.parseObject(personNameJson))
            .extracting(
                PersonNameRequest::firstName,
                PersonNameRequest::lastName
            ).containsExactly(
                "William", "Shakespeare"
            );
    }

    @Test
    void personNameRefusesInvalidValues() throws Exception {
        String personNameJson = """
            {
              "firstName": {},
              "lastName": []
            }
            """;

        assertThatThrownBy(() -> personName.parseObject(personNameJson))
            .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void addressReadsValidValues() throws Exception {
        String addressJson = """
            {
              "street":"31 Milk Street",
              "city":"Boston",
              "country":"US",
              "region":"MA",
              "zipcode":"02196"
            }
            """;

        assertThat(address.parseObject(addressJson))
            .extracting(
                AddressRequest::street,
                AddressRequest::city,
                AddressRequest::country,
                AddressRequest::region,
                AddressRequest::zipcode
            ).containsExactly(
                "31 Milk Street", "Boston",
                "US", "MA", "02196"
            );
    }

    @Test
    void addressRefusesInvalidValues() throws Exception {
        String addressJson = """
            {
              "street":{},
              "city":[],
              "country":{},
              "region":[],
              "zipcode":{}
            }
            """;

        assertThatThrownBy(() -> address.parseObject(addressJson))
            .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void duplicatesReadsValidValues() throws Exception {
        String duplicatesJson = """
            {
              "personName":{
                "firstName":"William",
                "lastName":"Shakespeare"
              },
              "birthdate":"1991-04-11"
            }
            """;

        assertThat(duplicates.parseObject(duplicatesJson))
            .extracting(
                DuplicatesRequest::personName,
                DuplicatesRequest::birthdate
            ).containsExactly(
                new PersonNameRequest("William", "Shakespeare"),
                LocalDate.of(1991, 4, 11)
            );
    }

    @Test
    void duplicatesRefusesInvalidValues() throws Exception {
        String duplicatesJson = """
            {
              "personName":{
                "firstName":[],
                "lastName":{}
              },
              "birthdate":"1991-04-11"
            }
            """;

        assertThatThrownBy(() -> duplicates.parseObject(duplicatesJson))
            .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void duplicatesRefusesAnotherDateFormat() throws Exception {
        String duplicatesJson = """
            {
              "personName":{
                "firstName":"William",
                "lastName":"Shakespeare"
              },
              "birthdate":"11.04.1991"
            }
            """;

        assertThatThrownBy(() -> duplicates.parseObject(duplicatesJson))
            .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void emailReadsValidValue() throws Exception {
        String emailJson = """
            {"email":"william_shakespeare@example.com"}
            """;

        assertThat(email.parseObject(emailJson).email())
            .isEqualTo("william_shakespeare@example.com");
    }

    @Test
    void emailRefusesInvalidValue() throws Exception {
        String emailJson = """
            {"email":[]}
            """;

        assertThatThrownBy(() -> email.parseObject(emailJson))
            .isInstanceOf(MismatchedInputException.class);
    }

}
