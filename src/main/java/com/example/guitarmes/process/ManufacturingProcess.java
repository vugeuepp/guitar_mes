package com.example.guitarmes.process;

import com.example.guitarmes.process.common.ProcessTargetConstants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "m_process")
public class ManufacturingProcess {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String processName;

	// 表示名とは独立した安定業務コード。未導入の工程はnull。
	@Column(name = "process_code", length = 64, unique = true)
	private String processCode;
	
	private Integer processOrder;
	
	@Column(nullable = false)
	private String targetType;
	
	public ManufacturingProcess() {
		
	}
	
    /*
     * 既存コードとの互換性を残すためのコンストラクタ。
     * 既存工程はギター工程として登録する。
     */
	public ManufacturingProcess(String processName, Integer processOrder) {
		this.targetType = ProcessTargetConstants.GUITAR;
		this.processName = processName;
		this.processOrder = processOrder;
	}
	
    /*
     * Body・Neckを含む新しい工程登録で使用する。
     */
    public ManufacturingProcess(String targetType, String processName, Integer processOrder) {
        this.targetType = targetType;
        this.processName = processName;
        this.processOrder = processOrder;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {
		this.processName = processName;
	}

	public String getProcessCode() {
		return processCode;
	}

	public void setProcessCode(String processCode) {
		this.processCode = processCode;
	}

	public Integer getProcessOrder() {
		return processOrder;
	}

	public void setProcessOrder(Integer processOrder) {
		this.processOrder = processOrder;
	}

	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}
	
}
