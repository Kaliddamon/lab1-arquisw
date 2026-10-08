package com.udea.lab1arq;

import com.udea.lab1arq.controller.CustomerController;
import com.udea.lab1arq.controller.TransactionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Lab1arqApplicationTests {
    @Autowired
    CustomerController customerController;
    @Autowired
    TransactionController transactionController;


}
