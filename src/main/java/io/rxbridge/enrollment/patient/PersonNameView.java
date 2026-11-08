package io.rxbridge.enrollment.patient;

public record PersonNameView(
    String firstName,
    String lastName
) {

    static PersonNameView from(PersonName personName) {
        return new PersonNameView(
            personName.firstName(),
            personName.lastName()
        );
    }

}
