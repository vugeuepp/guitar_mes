package com.example.guitarmes.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_assembly")
public class Assembly {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "guitar_id")
	private Guitar guitar;
	
	@ManyToOne
	@JoinColumn(name = "neck_id")
	private Neck neck;
	
	@ManyToOne
	@JoinColumn(name = "body_id")
	private Body body;
	
	private LocalDateTime assemblyDate;
	private String workerName;
	
	public Assembly() {
		
	}
	
	public Assembly(
			Guitar guitar,
			Neck neck,
			Body body,
			LocalDateTime assemblyDate,
			String workerName) {
		this.guitar = guitar;
		this.neck = neck;
		this.body = body;
		this.assemblyDate = assemblyDate;
		this.workerName = workerName;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Guitar getGuitar() {
		return guitar;
	}

	public void setGuitar(Guitar guitar) {
		this.guitar = guitar;
	}

	public Neck getNeck() {
		return neck;
	}

	public void setNeck(Neck neck) {
		this.neck = neck;
	}

	public Body getBody() {
		return body;
	}

	public void setBody(Body body) {
		this.body = body;
	}

	public LocalDateTime getAssemblyDate() {
		return assemblyDate;
	}

	public void setAssemblyDate(LocalDateTime assemblyDate) {
		this.assemblyDate = assemblyDate;
	}

	public String getWorkerName() {
		return workerName;
	}

	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}
	

}
