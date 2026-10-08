package com.udea.lab1arq.service;

import com.github.javafaker.Faker;
import com.udea.lab1arq.DTO.CustomerDTO;
import com.udea.lab1arq.entity.Customer;
import com.udea.lab1arq.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerServiceTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService;

    private Faker faker;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        faker = new Faker();

        // Limpiar base de datos antes de cada test
        customerRepository.deleteAll();

        // Crear cliente de prueba con Faker
        testCustomer = new Customer();
        testCustomer.setAccountNumber(faker.code().asin());
        testCustomer.setFirstName(faker.name().firstName());
        testCustomer.setLastName(faker.name().lastName());
        testCustomer.setBalance(faker.number().randomDouble(2, 100, 10000));
        testCustomer = customerRepository.save(testCustomer);
    }

    @Test
    void testGetAllCustomer_Success() {
        Customer customer2 = new Customer();
        customer2.setAccountNumber(faker.code().asin());
        customer2.setFirstName(faker.name().firstName());
        customer2.setLastName(faker.name().lastName());
        customer2.setBalance(faker.number().randomDouble(2, 100, 10000));
        customerRepository.save(customer2);

        Customer customer3 = new Customer();
        customer3.setAccountNumber(faker.code().asin());
        customer3.setFirstName(faker.name().firstName());
        customer3.setLastName(faker.name().lastName());
        customer3.setBalance(faker.number().randomDouble(2, 100, 10000));
        customerRepository.save(customer3);

        List<CustomerDTO> result = customerService.getAllCustomer();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(c -> c.getAccountNumber().equals(testCustomer.getAccountNumber())));
    }

    @Test
    void testGetAllCustomer_EmptyList() {
        customerRepository.deleteAll();

        List<CustomerDTO> result = customerService.getAllCustomer();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    void testGetAllCustomer_SingleCustomer() {
        List<CustomerDTO> result = customerService.getAllCustomer();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCustomer.getAccountNumber(), result.get(0).getAccountNumber());
    }

    @Test
    void testGetCustomerById_Success() {
        CustomerDTO result = customerService.getCustomerById(testCustomer.getId());

        assertNotNull(result);
        assertEquals(testCustomer.getId(), result.getId());
        assertEquals(testCustomer.getAccountNumber(), result.getAccountNumber());
        assertEquals(testCustomer.getFirstName(), result.getFirstName());
        assertEquals(testCustomer.getLastName(), result.getLastName());
        assertEquals(testCustomer.getBalance(), result.getBalance());
    }

    @Test
    void testGetCustomerById_NotFound() {
        Long nonExistentId = 9999L;

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> customerService.getCustomerById(nonExistentId)
        );

        assertEquals("Customer not found", exception.getMessage());
    }

    @Test
    void testCreateCustomer_Success() {
        customerRepository.deleteAll();
        
        CustomerDTO newCustomerDTO = new CustomerDTO();
        newCustomerDTO.setAccountNumber(faker.code().asin());
        newCustomerDTO.setFirstName(faker.name().firstName());
        newCustomerDTO.setLastName(faker.name().lastName());
        newCustomerDTO.setBalance(faker.number().randomDouble(2, 100, 10000));

        CustomerDTO result = customerService.createCustomer(newCustomerDTO);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(newCustomerDTO.getAccountNumber(), result.getAccountNumber());
        assertEquals(newCustomerDTO.getFirstName(), result.getFirstName());
        assertEquals(newCustomerDTO.getLastName(), result.getLastName());
        assertEquals(newCustomerDTO.getBalance(), result.getBalance());

        assertTrue(customerRepository.findById(result.getId()).isPresent());
    }

    @Test
    void testCreateCustomer_PersistenceCheck() {
        customerRepository.deleteAll();
        
        CustomerDTO newCustomerDTO = new CustomerDTO();
        String accountNumber = faker.code().asin();
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();
        Double balance = 5000.0;

        newCustomerDTO.setAccountNumber(accountNumber);
        newCustomerDTO.setFirstName(firstName);
        newCustomerDTO.setLastName(lastName);
        newCustomerDTO.setBalance(balance);

        CustomerDTO createdDTO = customerService.createCustomer(newCustomerDTO);

        Customer persistedCustomer = customerRepository.findById(createdDTO.getId()).orElse(null);
        assertNotNull(persistedCustomer);
        assertEquals(accountNumber, persistedCustomer.getAccountNumber());
        assertEquals(firstName, persistedCustomer.getFirstName());
        assertEquals(lastName, persistedCustomer.getLastName());
        assertEquals(balance, persistedCustomer.getBalance());
    }

    @Test
    void testGetCustomerByAccountNumber_Success() {
        var foundCustomer = customerRepository.findByAccountNumber(testCustomer.getAccountNumber());

        assertTrue(foundCustomer.isPresent());
        assertEquals(testCustomer.getAccountNumber(), foundCustomer.get().getAccountNumber());
        assertEquals(testCustomer.getFirstName(), foundCustomer.get().getFirstName());
    }

    @Test
    void testGetCustomerByAccountNumber_NotFound() {
        var foundCustomer = customerRepository.findByAccountNumber("NON_EXISTENT_ACCOUNT");

        assertTrue(foundCustomer.isEmpty());
    }

    @Test
    void testCreateMultipleCustomers_WithFaker() {
        customerRepository.deleteAll();
        int customerCount = 5;

        for (int i = 0; i < customerCount; i++) {
            CustomerDTO dto = new CustomerDTO();
            dto.setAccountNumber(faker.code().asin());
            dto.setFirstName(faker.name().firstName());
            dto.setLastName(faker.name().lastName());
            dto.setBalance(faker.number().randomDouble(2, 100, 50000));
            customerService.createCustomer(dto);
        }

        List<CustomerDTO> allCustomers = customerService.getAllCustomer();
        assertEquals(customerCount, allCustomers.size());

        long uniqueAccountNumbers = allCustomers.stream()
                .map(CustomerDTO::getAccountNumber)
                .distinct()
                .count();
        assertEquals(customerCount, uniqueAccountNumbers);
    }

    @Test
    void testCustomerMapping_EntityToDTO() {
        CustomerDTO result = customerService.getCustomerById(testCustomer.getId());

        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getAccountNumber());
        assertNotNull(result.getFirstName());
        assertNotNull(result.getLastName());
        assertNotNull(result.getBalance());
    }

    @Test
    void testCreateCustomer_LargeBalance() {
        customerRepository.deleteAll();
        Double largeBalance = 999999.99;

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setAccountNumber(faker.code().asin());
        customerDTO.setFirstName(faker.name().firstName());
        customerDTO.setLastName(faker.name().lastName());
        customerDTO.setBalance(largeBalance);

        CustomerDTO result = customerService.createCustomer(customerDTO);

        assertNotNull(result);
        assertEquals(largeBalance, result.getBalance());
    }

    @Test
    void testCreateCustomer_ZeroBalance() {
        customerRepository.deleteAll();

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setAccountNumber(faker.code().asin());
        customerDTO.setFirstName(faker.name().firstName());
        customerDTO.setLastName(faker.name().lastName());
        customerDTO.setBalance(0.0);

        CustomerDTO result = customerService.createCustomer(customerDTO);

        assertNotNull(result);
        assertEquals(0.0, result.getBalance());
    }
}
