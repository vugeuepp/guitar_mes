package com.example.guitarmes.body;

import java.time.LocalDateTime;
import java.util.Objects;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.Transient;
import jakarta.persistence.Column;


import com.example.guitarmes.master.body.BodyMaster;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionschedule.ProductionSchedule;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_body")
public class Body {

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
	
	private String color;
	
	private String currentProcess;
	
	private String status;
	
	@ManyToOne
	@JoinColumn(name = "body_master_id")
	private BodyMaster bodyMaster;
    @ManyToOne
    @JoinColumn(name = "production_order_id")
    private ProductionOrder productionOrder;
    @ManyToOne
    @JoinColumn(name = "production_schedule_id")
    private ProductionSchedule productionSchedule;
	
	public Body() {
		
	}
	
	public Body(
			String serialNo,
			String modelName,
			String color,
			String currentProcess,
			String status) {
		this.serialNo = serialNo;
		this.modelName = modelName;
		this.color = color;
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

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
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

	public BodyMaster getBodyMaster() {
		return bodyMaster;
	}

	public void setBodyMaster(BodyMaster bodyMaster) {
		this.bodyMaster = bodyMaster;
	}
	
	
	

    public ProductionOrder getProductionOrder() { return productionOrder; }
    public void setProductionOrder(ProductionOrder productionOrder) { this.productionOrder = productionOrder; }
    public ProductionSchedule getProductionSchedule() { return productionSchedule; }
    public void setProductionSchedule(ProductionSchedule productionSchedule) { this.productionSchedule = productionSchedule; }
}
