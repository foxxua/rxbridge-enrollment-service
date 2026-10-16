package io.rxbridge.enrollment.patient;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

import static io.rxbridge.enrollment.common.validation.DomainPreconditions.*;
import static io.rxbridge.enrollment.patient.PatientConstants.*;

@Embeddable
public record Address(

    @NotBlank
    @Size(max = STREET_MAX_LENGTH)
    String street,

    @NotBlank
    @Size(max = CITY_MAX_LENGTH)
    String city,

    @NotNull
    @Pattern(regexp = COUNTRY_PATTERN)
    String country,

    @Size(max = REGION_MAX_LENGTH)
    String region,

    @NotBlank
    @Size(max = ZIPCODE_MAX_LENGTH)
    String zipcode
) {

    public Address {
        street = requireText(street, "street", STREET_MAX_LENGTH);
        city = requireText(city, "city", CITY_MAX_LENGTH);
        country = validateCountry(country);
        region = optionalText(region, "region", REGION_MAX_LENGTH);
        zipcode = requireText(zipcode, "zipcode", ZIPCODE_MAX_LENGTH);
    }

    private String validateCountry(String country) {
        country = requireText(country, "country", COUNTRY_MAX_LENGTH);
        country = country.toUpperCase(Locale.ROOT);
        country = requireMatch(country, "country", COUNTRY_PATTERN);

        return country;
    }

}
