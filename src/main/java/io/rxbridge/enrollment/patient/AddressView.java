package io.rxbridge.enrollment.patient;

public record AddressView(
    String street,
    String city,
    String country,
    String region,
    String zipcode
) {

    static AddressView from(Address address) {
        return new AddressView(
            address.street(),
            address.city(),
            address.country(),
            address.region(),
            address.zipcode()
        );
    }

}
