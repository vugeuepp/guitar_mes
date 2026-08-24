package com.example.guitarmes.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "m_product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String modelNo;

    private String productName;

    private String color;

    private String bodyMaterial;

    private String neckMaterial;

    private String fingerboardMaterial;

    private String pickupLayout;

    private Integer fretCount;

    private String scale;
    
    @ManyToOne
    @JoinColumn(name = "body_master_id")
    private BodyMaster bodyMaster;

    @ManyToOne
    @JoinColumn(name = "neck_master_id")
    private NeckMaster neckMaster;

    public Product() {
    }

    public Product(
            String modelNo,
            String productName,
            String color,
            String bodyMaterial,
            String neckMaterial,
            String fingerboardMaterial,
            String pickupLayout,
            Integer fretCount,
            String scale) {

        this.modelNo = modelNo;
        this.productName = productName;
        this.color = color;
        this.bodyMaterial = bodyMaterial;
        this.neckMaterial = neckMaterial;
        this.fingerboardMaterial = fingerboardMaterial;
        this.pickupLayout = pickupLayout;
        this.fretCount = fretCount;
        this.scale = scale;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getModelNo() {
		return modelNo;
	}

	public void setModelNo(String modelNo) {
		this.modelNo = modelNo;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getBodyMaterial() {
		return bodyMaterial;
	}

	public void setBodyMaterial(String bodyMaterial) {
		this.bodyMaterial = bodyMaterial;
	}

	public String getNeckMaterial() {
		return neckMaterial;
	}

	public void setNeckMaterial(String neckMaterial) {
		this.neckMaterial = neckMaterial;
	}

	public String getFingerboardMaterial() {
		return fingerboardMaterial;
	}

	public void setFingerboardMaterial(String fingerboardMaterial) {
		this.fingerboardMaterial = fingerboardMaterial;
	}

	public String getPickupLayout() {
		return pickupLayout;
	}

	public void setPickupLayout(String pickupLayout) {
		this.pickupLayout = pickupLayout;
	}

	public Integer getFretCount() {
		return fretCount;
	}

	public void setFretCount(Integer fretCount) {
		this.fretCount = fretCount;
	}

	public String getScale() {
		return scale;
	}

	public void setScale(String scale) {
		this.scale = scale;
	}

	public BodyMaster getBodyMaster() {
		return bodyMaster;
	}

	public void setBodyMaster(BodyMaster bodyMaster) {
		this.bodyMaster = bodyMaster;
	}

	public NeckMaster getNeckMaster() {
		return neckMaster;
	}

	public void setNeckMaster(NeckMaster neckMaster) {
		this.neckMaster = neckMaster;
	}
	
	

    
}