package org.parmeet.addressbookgrpc;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AddressBookProtobufTest {

    private static final String filePath = "address_book";

    @AfterAll
    public static void cleanup() throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
    }

    @Test
    public void givenGeneratedProtobufClass_WhenCreateClass_ThenShouldCreateJavaInstance() {
        // When
        String name = "Parmeet Singh";
        int id = new Random().nextInt();
        String email = "randomxyz@gmail.com";
        String number = "1231231231";
        AddressBookProtos.Person person = AddressBookProtos.Person.newBuilder()
                .setId(id)
                .setName(name)
                .setEmail(email)
                .addNumbers(number)
                .build();

        // Then
        assertEquals(id, person.getId());
        assertEquals(name, person.getName());
        assertEquals(email, person.getEmail());
        assertEquals(number, person.getNumbers(0));
    }

    @Test
    public void givenAddressBookWithOnePerson_WhenSaveAsAFile_ShouldLoadFromFileToJavaClass() throws IOException {
        // Given
        String name = "Parmeet Singh";
        int id = new Random().nextInt();
        String email = "randomxyz@gmail.com";
        String number = "1231231231";
        AddressBookProtos.Person person = AddressBookProtos.Person.newBuilder()
                .setId(id)
                .setName(name)
                .setEmail(email)
                .addNumbers(number)
                .build();

        // When
        AddressBookProtos.AddressBook addressBook =
                AddressBookProtos.AddressBook.newBuilder().addPeople(person).build();
        FileOutputStream fos = new FileOutputStream(filePath);
        addressBook.writeTo(fos);
        fos.close();

        // Then
        FileInputStream fis = new FileInputStream(filePath);
        AddressBookProtos.AddressBook deserialized =
                AddressBookProtos.AddressBook.newBuilder().mergeFrom(fis).build();
        fis.close();
        assertEquals(id, deserialized.getPeople(0).getId());
        assertEquals(name, deserialized.getPeople(0).getName());
        assertEquals(email, deserialized.getPeople(0).getEmail());
        assertEquals(number, deserialized.getPeople(0).getNumbers(0));

    }


}
