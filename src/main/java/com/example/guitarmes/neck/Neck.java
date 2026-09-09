package com.example.guitarmes.neck;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.Transient;
import jakarta.persistence.Column;


import java.util.List;

import com.example.guitarmes.assembly.Assembly;
import com.example.guitarmes.master.neck.NeckMaster;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionschedule.ProductionSchedule;

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

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "available_at")
    private LocalDateTime availableAt;

    @Transient
    private LocalDateTime persistedUpdatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        // Serviceが設定したイベント時刻は保持し、通常編集だけ現在時刻にする。
        // 読込時の値と比較するため、detached Entityのmergeでも明示時刻を保持できる。
        if (Objects.equals(updatedAt, persistedUpdatedAt)) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    public void rememberUpdatedAt() {
        persistedUpdatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
    public LocalDateTime getAvailableAt() { return availableAt; }
    public void setAvailableAt(LocalDateTime value) { availableAt = value; }

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
    @ManyToOne
    @JoinColumn(name = "production_order_id")
    private ProductionOrder productionOrder;
    @ManyToOne
    @JoinColumn(name = "production_schedule_id")
    private ProductionSchedule productionSchedule;
	
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

    public ProductionOrder getProductionOrder() { return productionOrder; }
    public void setProductionOrder(ProductionOrder productionOrder) { this.productionOrder = productionOrder; }
    public ProductionSchedule getProductionSchedule() { return productionSchedule; }
    public void setProductionSchedule(ProductionSchedule productionSchedule) { this.productionSchedule = productionSchedule; }
}
