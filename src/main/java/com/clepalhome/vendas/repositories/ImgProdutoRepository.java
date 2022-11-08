package com.clepalhome.vendas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.clepalhome.vendas.domain.ImgProduto;

@Repository
public interface ImgProdutoRepository extends JpaRepository<ImgProduto, Integer>{
	
	ImgProduto findTopByOrderByIdDesc();

}
