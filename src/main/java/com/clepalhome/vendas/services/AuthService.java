package com.clepalhome.vendas.services;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.clepalhome.vendas.domain.Cliente;
import com.clepalhome.vendas.repositories.ClienteRepository;
import com.clepalhome.vendas.services.exception.ObjectNotFoundException;

@Service
public class AuthService {
	
	@Autowired
	private ClienteRepository clienteRepository;
	
	@Autowired
	private BCryptPasswordEncoder pe;
	
	private Random rand = new Random();
	
	@Autowired
	private EmailService emailService;
	
	public void sendNewPassword(String email) {
		
		Cliente cliente =  clienteRepository.findByEmail(email);
		
		if(cliente == null) {
			throw new ObjectNotFoundException("EMail não encontrado");
		}
		
		String newPass = newPassword();
		cliente.setSenha(pe.encode(newPass));
		
		clienteRepository.save(cliente);
		emailService.sendNewPasswordEmail(cliente, newPass);
	}

	private String newPassword() {
		char[] vet = new char[10]; 
		for(int i = 0; i < 10; i ++) {
			vet[i] = randomChar();
		}
		return new String(vet);
	}

	private char randomChar() {
		
		int opt = rand.nextInt(3);
		if(opt == 0) { // gera um digito
			return (char) (rand.nextInt(10) + 48);
		}
		else if(opt == 1) { // Gera letra maiuscula
			return (char) (rand.nextInt(26) + 65);
		}
		else { // Gera letra maiuscula
			return (char) (rand.nextInt(26) + 97);
		}
		
	}
}
