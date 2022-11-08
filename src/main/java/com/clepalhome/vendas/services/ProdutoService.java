package com.clepalhome.vendas.services;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.clepalhome.vendas.domain.Categoria;
import com.clepalhome.vendas.domain.ImgProduto;
import com.clepalhome.vendas.domain.Produto;
import com.clepalhome.vendas.dto.ProdutoCategoriaDTO;
import com.clepalhome.vendas.dto.ProdutoDTO;
import com.clepalhome.vendas.repositories.CategoriaRepository;
import com.clepalhome.vendas.repositories.ImgProdutoRepository;
import com.clepalhome.vendas.repositories.ProdutoRepository;
import com.clepalhome.vendas.security.UserSS;
import com.clepalhome.vendas.services.exception.AuthorizationException;
import com.clepalhome.vendas.services.exception.EmptyDataAccessException;
import com.clepalhome.vendas.services.exception.ObjectNotFoundException;

@Service
public class ProdutoService {

	@Autowired
	private ProdutoRepository repo;

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Autowired
	private CategoriaService categoriaService;
	
	@Autowired
	private S3Service s3Service;
	
	@Autowired
	private ImgProdutoRepository imgRepository;
	
	@Autowired
	private ImageService imageService;
	
	@Value("${image.prefixo.prod.profile}")
	private String prefixo;

	public Produto find(Integer id) {
		Optional<Produto> obj = repo.findById(id);
		return obj.orElseThrow(() -> new ObjectNotFoundException(
				"Objeto não encontrado! Id: " + id + ", Tipo: " + Produto.class.getName()));
	}

//==================================== INSERIR =============================================
	@Transactional
	public Produto insert(Produto obj) {
		obj.setId(null);
		obj = repo.save(obj);
		categoriaRepository.save(obj.getCategorias());
		return obj;
	}

	public Produto fromDto(ProdutoCategoriaDTO objDto) {
		Categoria cat = categoriaService.find(objDto.getCategoria());
		Produto obj = new Produto(null, objDto.getNome(), objDto.getDescricao(), objDto.getPreco(), cat);
		return obj;
	}

//==================================== ALTERAR =============================================
	public Produto update(Produto obj) {
		Produto newObj = find(obj.getId());
		updateData(newObj, obj);
		return repo.save(newObj);
	}

	private void updateData(Produto newObj, Produto obj) {
		newObj.setNome(obj.getNome());
		newObj.setDescricao(obj.getDescricao());
		newObj.setPreco(obj.getPreco());
	}

	public Produto fromDto(ProdutoDTO objDto) {
		return new Produto(objDto.getId(), objDto.getNome(), objDto.getDescricao(), objDto.getPreco(), null);
	}

//==================================== DELETAR =============================================
	public void delete(Integer id) {
		try {
			repo.deleteById(id);
		} catch (EmptyResultDataAccessException e) {
			throw new EmptyDataAccessException("O produto não existe na base de dados.");
		}
	}

//==================================== ALTERAR =============================================
	public Page<Produto> search(String nome, List<Integer> ids, Integer page, Integer linesPerPage, String orderBy,
			String direction) {
		PageRequest pageRequest = PageRequest.of(page, linesPerPage, Direction.valueOf(direction), orderBy);
		List<Categoria> categorias = categoriaRepository.findAllById(ids);
		return repo.search(nome, categorias, pageRequest);
	}

// ============================= UPLOAD DE IMAGEM DO PRODUTO ========================================
	public URI uploadProdutoPicture(MultipartFile multipartFile, Integer id) {
		UserSS user = UserService.authenticated();
		if (user == null) {
			throw new AuthorizationException("Acesso negado");
		}
		
		Produto obj = find(id);
		ImgProduto lastId = imgRepository.findTopByOrderByIdDesc();
		
		BufferedImage jpgImage = imageService.getJpgImageFromFile(multipartFile);
		String fileName = prefixo + lastId.getId() + obj.getNome() + ".jpg";
		
		URI uri = s3Service.uploadFile(imageService.getInputStream(jpgImage, "jpg"), fileName, "image");
		
		ImgProduto imgProduto = new ImgProduto(null, uri.toString(), obj);
		
		String test = uri.toString();
		System.out.println("URL DA IMAGEM PARA SALVAR NO BANCO: " + test);
		
		repo.save(obj);
		imgRepository.save(imgProduto);
		return s3Service.uploadFile(imageService.getInputStream(jpgImage, "jpg"), fileName, "image");
	}

}
