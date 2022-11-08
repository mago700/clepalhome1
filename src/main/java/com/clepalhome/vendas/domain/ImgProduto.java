package com.clepalhome.vendas.domain;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class ImgProduto {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	private String imgProdUrl;
	
	@JsonIgnore
	@ManyToOne
	@JoinColumn(name = "produto_id")
	private Produto produto;
	
	public ImgProduto(){
		
	}

	public ImgProduto(Integer id, String imgProdUrl, Produto produto) {
		super();
		this.id = id;
		this.imgProdUrl = imgProdUrl;
		this.produto = produto;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getImgProdUrl() {
		return imgProdUrl;
	}

	public void setImgProdUrl(String imgProdUrl) {
		this.imgProdUrl = imgProdUrl;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImgProduto other = (ImgProduto) obj;
		return Objects.equals(id, other.id);
	}
	
}
