package com.clepalhome.vendas.services;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.clepalhome.vendas.domain.Cliente;
import com.clepalhome.vendas.domain.Morada;
import com.clepalhome.vendas.domain.enums.Perfil;
import com.clepalhome.vendas.dto.ClienteDTO;
import com.clepalhome.vendas.dto.ClienteNewDTO;
import com.clepalhome.vendas.repositories.ClienteRepository;
import com.clepalhome.vendas.repositories.MoradaRepository;
import com.clepalhome.vendas.security.UserSS;
import com.clepalhome.vendas.services.exception.AuthorizationException;
import com.clepalhome.vendas.services.exception.DataIntegrityException;
import com.clepalhome.vendas.services.exception.ObjectNotFoundException;

@Service
public class ClienteService {
	
	@Autowired 
	private ClienteRepository repo;
	
	@Autowired
	private MoradaRepository moradaRepository;
	
	@Autowired
	private BCryptPasswordEncoder pe;
	
	@Autowired
	private S3Service s3Service;
	
	@Autowired
	private ImageService imageService;
	
	@Value("${img.prefix.client.profile}")
	private String prefix;
	
	@Value("${img.profile.size}")
	private Integer size;
	
//===================================== LISTAR =============================================
	public Cliente find(Integer id) {
		UserSS user = UserService.authenticated();
		if (user == null || !user.hasRole(Perfil.ADMIN) && !id.equals(user.getId())) {
			throw new AuthorizationException("Acesso negado");
		}
		
		Optional<Cliente> obj = repo.findById(id);
		return obj.orElseThrow(() -> new ObjectNotFoundException(
				"Objeto não encontrado! Id: " + id + ", Tipo: " + Cliente.class.getName()));
	}
	
//==================================== INSERIR =============================================	
	@Transactional
	public Cliente insert(Cliente obj) {
		obj.setId(null);
		obj = repo.save(obj);
		moradaRepository.saveAll(obj.getMorada());
		return obj;
	}
	
//==================================== INSERIR =============================================
		public Cliente fromDto(ClienteNewDTO objDto) {
			Cliente cli = new Cliente(null, objDto.getNome(), objDto.getEmail(), pe.encode(objDto.getSenha()));
			
			Morada end = new Morada(null, objDto.getPais(), objDto.getMorada(), objDto.getComlementoMorada(), 
					objDto.getCodigoPostal(), objDto.getLocalidade(), cli);
		
			cli.getMorada().add(end);
			
			cli.getTelefones().add(objDto.getTelefone1());
			
			if (objDto.getTelefone2() != null) {
				cli.getTelefones().add(objDto.getTelefone2());
			}
			if (objDto.getTelefone3() != null) {
				cli.getTelefones().add(objDto.getTelefone3());
			}
			return cli;
		}
	
//==================================== ALTERAR =============================================
	public Cliente update(Cliente obj) {
		Cliente newObj = find(obj.getId());
		updateData(newObj, obj);
		return repo.save(newObj);
	}
	
	private void updateData(Cliente newObj, Cliente obj) {
		newObj.setNome(obj.getNome());
		newObj.setEmail(obj.getEmail());
	}
	
	public Cliente fromDto(ClienteDTO objDto) {
		return new Cliente(objDto.getId(), objDto.getNome(), objDto.getEmail(), null);
	}
	
//==================================== DELETAR =============================================
	public void delete(Integer id) {
		find(id);
		try {
			repo.deleteById(id);
		} catch (DataIntegrityViolationException e) {
			throw new DataIntegrityException("Não é possivel excluir porque há pedidos relacionados");
		}
	}
	
//==================================== FIND ALL =============================================
	public List<Cliente> findAll() {
		return repo.findAll();
	}
	
//================================== FIND BY EMAIL ==========================================
	public Cliente findByEmail(String email) {
		UserSS user = UserService.authenticated();
		if (user == null || !user.hasRole(Perfil.ADMIN) && !email.equals(user.getUsername())) {
			throw new AuthorizationException("Acesso negado");
		}

		Cliente obj = repo.findByEmail(email);
		if (obj == null) {
			throw new ObjectNotFoundException(
					"Objeto não encontrado! Id: " + user.getId() + ", Tipo: " + Cliente.class.getName());
		}
		return obj;
	}

//==================================== FIND PAGE ============================================
	public Page<Cliente> findPage(Integer page, Integer linesPerPage, String orderBy, String direction) {
		PageRequest pageRequest = PageRequest.of(page, linesPerPage, Direction.valueOf(direction), orderBy);
		return repo.findAll(pageRequest);
	}
	
//============================= UPLOAD DE IMAGEM DO CLIENTE ========================================
	public URI uploadProfilePicture(MultipartFile multipartFile) {
		
		UserSS user = UserService.authenticated();
		if (user == null) {
			throw new AuthorizationException("Acesso negado");
		}
		
		BufferedImage jpgImage = imageService.getJpgImageFromFile(multipartFile);
		jpgImage = imageService.cropSquare(jpgImage);
		jpgImage = imageService.rezise(jpgImage, size);
		
		String fileName = prefix + user.getId() + ".jpg";
		
		URI uri = s3Service.uploadFile(imageService.getInputStream(jpgImage, "jpg"), fileName, "image");
		
		Cliente cli = find(user.getId());
		cli.setImageUrl(uri.toString());
		repo.save(cli);
		String test = uri.toString();
		
		System.out.println("URL DA IMAGEM PARA SALVAR NO BANCO: " + test);

		return s3Service.uploadFile(imageService.getInputStream(jpgImage, "jpg"), fileName, "image");
	}
	
}