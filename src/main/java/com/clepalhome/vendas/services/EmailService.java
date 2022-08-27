package com.clepalhome.vendas.services;

import org.springframework.mail.SimpleMailMessage;

import com.clepalhome.vendas.domain.Cliente;
import com.clepalhome.vendas.domain.Pedido;

public interface EmailService {
	
	void sendOrderConfirmationEmail(Pedido obj); 
	
	void sendEmail(SimpleMailMessage msg); 
	
	void sendNewPasswordEmail(Cliente cliente, String newPass);

}