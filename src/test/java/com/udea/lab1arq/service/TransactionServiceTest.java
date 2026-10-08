package com.udea.lab1arq.service;

import com.github.javafaker.Faker;
import com.udea.lab1arq.DTO.TransactionDTO;
import com.udea.lab1arq.entity.Customer;
import com.udea.lab1arq.entity.Transaction;
import com.udea.lab1arq.repository.CustomerRepository;
import com.udea.lab1arq.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionServiceTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionService transactionService;

    private Faker faker;
    private Customer sender;
    private Customer receiver;

    @BeforeEach
    void setUp() {
        faker = new Faker();

        // Limpiar base de datos antes de cada test
        transactionRepository.deleteAll();
        customerRepository.deleteAll();

        // Crear clientes con datos generados por Faker
        sender = new Customer();
        sender.setAccountNumber(faker.code().asin());
        sender.setFirstName(faker.name().firstName());
        sender.setLastName(faker.name().lastName());
        sender.setBalance(1000.0);
        sender = customerRepository.save(sender);

        receiver = new Customer();
        receiver.setAccountNumber(faker.code().asin());
        receiver.setFirstName(faker.name().firstName());
        receiver.setLastName(faker.name().lastName());
        receiver.setBalance(500.0);
        receiver = customerRepository.save(receiver);
    }

    @Test
    void testTransferMoney_Success() {
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setSenderAccountNumber(sender.getAccountNumber());
        transactionDTO.setReceiverAccountNumber(receiver.getAccountNumber());
        transactionDTO.setAmount(100.0);

        double initialSenderBalance = sender.getBalance();
        double initialReceiverBalance = receiver.getBalance();

        TransactionDTO result = transactionService.transferMoney(transactionDTO);

        assertNotNull(result);
        assertEquals(sender.getAccountNumber(), result.getSenderAccountNumber());
        assertEquals(receiver.getAccountNumber(), result.getReceiverAccountNumber());
        assertEquals(100.0, result.getAmount());

        Customer updatedSender = customerRepository.findByAccountNumber(sender.getAccountNumber()).orElse(null);
        Customer updatedReceiver = customerRepository.findByAccountNumber(receiver.getAccountNumber()).orElse(null);

        assertNotNull(updatedSender);
        assertNotNull(updatedReceiver);
        assertEquals(initialSenderBalance - 100.0, updatedSender.getBalance());
        assertEquals(initialReceiverBalance + 100.0, updatedReceiver.getBalance());

        List<Transaction> transactions = transactionRepository.findBySenderAccountNumberOrReceiverAccountNumber(
                sender.getAccountNumber(), sender.getAccountNumber());
        assertFalse(transactions.isEmpty());
    }

    @Test
    void testTransferMoney_SuccessWithNullTimestamp() {
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setSenderAccountNumber(sender.getAccountNumber());
        transactionDTO.setReceiverAccountNumber(receiver.getAccountNumber());
        transactionDTO.setAmount(50.0);
        transactionDTO.setTimestamp(null);

        TransactionDTO result = transactionService.transferMoney(transactionDTO);

        assertNotNull(result);
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testTransferMoney_InsufficientBalance() {
        sender.setBalance(50.0);
        customerRepository.save(sender);

        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setSenderAccountNumber(sender.getAccountNumber());
        transactionDTO.setReceiverAccountNumber(receiver.getAccountNumber());
        transactionDTO.setAmount(100.0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.transferMoney(transactionDTO)
        );

        assertEquals("Saldo insuficiente en la cuenta del remitente.", exception.getMessage());

        List<Transaction> transactions = transactionRepository.findAll();
        assertEquals(0, transactions.size());
    }

    @Test
    void testTransferMoney_SenderAccountNotFound() {
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setSenderAccountNumber("NON_EXISTENT_ACCOUNT");
        transactionDTO.setReceiverAccountNumber(receiver.getAccountNumber());
        transactionDTO.setAmount(100.0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.transferMoney(transactionDTO)
        );

        assertEquals("La cuenta del remitente no existe.", exception.getMessage());
    }

    @Test
    void testTransferMoney_ReceiverAccountNotFound() {
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setSenderAccountNumber(sender.getAccountNumber());
        transactionDTO.setReceiverAccountNumber("NON_EXISTENT_ACCOUNT");
        transactionDTO.setAmount(100.0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.transferMoney(transactionDTO)
        );

        assertEquals("La cuenta del receptor no existe.", exception.getMessage());
    }

    @Test
    void testTransferMoney_NullSenderAccountNumber() {
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setSenderAccountNumber(null);
        transactionDTO.setReceiverAccountNumber(receiver.getAccountNumber());
        transactionDTO.setAmount(100.0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.transferMoney(transactionDTO)
        );

        assertEquals("Los números de cuenta del remitente y receptor son obligatorios.", exception.getMessage());
    }

    @Test
    void testTransferMoney_NullReceiverAccountNumber() {
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setSenderAccountNumber(sender.getAccountNumber());
        transactionDTO.setReceiverAccountNumber(null);
        transactionDTO.setAmount(100.0);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.transferMoney(transactionDTO)
        );

        assertEquals("Los números de cuenta del remitente y receptor son obligatorios.", exception.getMessage());
    }

    @Test
    void testTransferMoney_UpdatesBalancesCorrectly() {
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setSenderAccountNumber(sender.getAccountNumber());
        transactionDTO.setReceiverAccountNumber(receiver.getAccountNumber());
        transactionDTO.setAmount(250.0);

        double initialSenderBalance = sender.getBalance();
        double initialReceiverBalance = receiver.getBalance();
        double transferAmount = transactionDTO.getAmount();

        transactionService.transferMoney(transactionDTO);

        Customer updatedSender = customerRepository.findByAccountNumber(sender.getAccountNumber()).orElse(null);
        Customer updatedReceiver = customerRepository.findByAccountNumber(receiver.getAccountNumber()).orElse(null);

        assertNotNull(updatedSender);
        assertNotNull(updatedReceiver);
        assertEquals(initialSenderBalance - transferAmount, updatedSender.getBalance());
        assertEquals(initialReceiverBalance + transferAmount, updatedReceiver.getBalance());
    }

    @Test
    @DisplayName("Should successfully transfer entire balance")
    void testTransferMoney_ExactBalanceTransfer() {
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setSenderAccountNumber(sender.getAccountNumber());
        transactionDTO.setReceiverAccountNumber(receiver.getAccountNumber());
        transactionDTO.setAmount(sender.getBalance());

        TransactionDTO result = transactionService.transferMoney(transactionDTO);

        assertNotNull(result);
        Customer updatedSender = customerRepository.findByAccountNumber(sender.getAccountNumber()).orElse(null);
        assertNotNull(updatedSender);
        assertEquals(0.0, updatedSender.getBalance());
    }

    @Test
    @DisplayName("Should transfer minimum amount successfully")
    void testTransferMoney_TransferMinimumAmount() {
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setSenderAccountNumber(sender.getAccountNumber());
        transactionDTO.setReceiverAccountNumber(receiver.getAccountNumber());
        transactionDTO.setAmount(0.01);

        TransactionDTO result = transactionService.transferMoney(transactionDTO);

        assertNotNull(result);
        assertEquals(0.01, result.getAmount());
        
        Customer updatedSender = customerRepository.findByAccountNumber(sender.getAccountNumber()).orElse(null);
        assertNotNull(updatedSender);
        assertTrue(updatedSender.getBalance() > 0);
    }

    @Test
    void testGetTransactionsForAccount_WithTransactions() {
        TransactionDTO transaction1 = new TransactionDTO();
        transaction1.setSenderAccountNumber(sender.getAccountNumber());
        transaction1.setReceiverAccountNumber(receiver.getAccountNumber());
        transaction1.setAmount(100.0);
        transactionService.transferMoney(transaction1);

        TransactionDTO transaction2 = new TransactionDTO();
        transaction2.setSenderAccountNumber(receiver.getAccountNumber());
        transaction2.setReceiverAccountNumber(sender.getAccountNumber());
        transaction2.setAmount(50.0);
        transactionService.transferMoney(transaction2);

        List<TransactionDTO> result = transactionService.getTransactionsForAccount(sender.getAccountNumber());

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should return empty list when account has no transactions")
    void testGetTransactionsForAccount_EmptyList() {
        String accountNumber = faker.code().asin();

        List<TransactionDTO> result = transactionService.getTransactionsForAccount(accountNumber);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    void testGetTransactionsForAccount_SingleTransaction() {
        TransactionDTO transaction = new TransactionDTO();
        transaction.setSenderAccountNumber(sender.getAccountNumber());
        transaction.setReceiverAccountNumber(receiver.getAccountNumber());
        transaction.setAmount(200.0);
        transactionService.transferMoney(transaction);

        List<TransactionDTO> result = transactionService.getTransactionsForAccount(sender.getAccountNumber());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(200.0, result.get(0).getAmount());
    }
}
