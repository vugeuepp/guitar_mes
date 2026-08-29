package com.example.guitarmes.neck;

import java.util.List;

import com.example.guitarmes.assembly.Assembly;
import com.example.guitarmes.master.neck.NeckMaster;
import com.example.guitarmes.product.Product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_neck")
public class Neck {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String serialNo;
	
	private String modelName;
	
	private String currentProcess;
	
	private String status;
	
	@ManyToOne
	@JoinColumn(name = "product_id")
	private Product product;
	
	@OneToMany(mappedBy = "neck")
	private List<Assembly> assemblies;
	
	@ManyToOne
	@JoinColumn(name = "neck_master_id")
	private NeckMaster neckMaster;
	
	public Neck() {
		
	}
	
	public Neck(String serialNo, String modelName, String currentProcess, String status) {
		this.serialNo = serialNo;
		this.modelName = modelName;
		this.currentProcess = currentProcess;
		this.status = status;
	}
	

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getCurrentProcess() {
		return currentProcess;
	}

	public void setCurrentProcess(String currentProcess) {
		this.currentProcess = currentProcess;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public List<Assembly> getAssemblies() {
		return assemblies;
	}

	public void setAssemblies(List<Assembly> assemblies) {
		this.assemblies = assemblies;
	}

	public NeckMaster getNeckMaster() {
		return neckMaster;
	}

	public void setNeckMaster(NeckMaster neckMaster) {
		this.neckMaster = neckMaster;
	}
}
