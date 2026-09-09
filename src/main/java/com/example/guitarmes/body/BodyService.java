package com.example.guitarmes.body;

import static com.example.guitarmes.body.process.BodyProcessConstants.*;
import static com.example.guitarmes.common.StatusConstants.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.master.body.BodyMaster;
import com.example.guitarmes.master.body.BodyMasterRepository;
import com.example.guitarmes.process.analysis.ComponentStatusCountResponse;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionschedule.ProductionSchedule;

@Service
public class BodyService {

    private final BodyRepository bodyRepository;
    private final BodyMasterRepository bodyMasterRepository;

    public BodyService(
            BodyRepository bodyRepository,
            BodyMasterRepository bodyMasterRepository) {

        this.bodyRepository = bodyRepository;
        this.bodyMasterRepository = bodyMasterRepository;
    }

    public List<Body> getBodies() {
        return bodyRepository.findAll();
    }

    public static final int PAGE_SIZE = 20;

    public List<String> getCategoryStatuses(String category) {
        return switch (normalizeCategory(category)) {
            case "attention" -> List.of(REWORK, RETURNED);
            case "passed" -> List.of(AVAILABLE, ASSEMBLED, REJECTED);
            default -> List.of(WAITING_INSPECTION, WAITING, WORKING);
        };
    }

    private BodySearchCriteria searchCriteria(String category, String serial, String modelName,
            String currentProcess, String status) {
        String selected = normalizeCategory(category);
        return new BodySearchCriteria(selected, getCategoryStatuses(selected), normalize(serial),
                normalize(modelName), normalize(currentProcess), normalize(status));
    }

    @Transactional(readOnly = true)
    public long countCategory(String category) {
        return bodyRepository.countMatching(searchCriteria(category, null, null, null, null));
    }

    @Transactional(readOnly = true)
    public Page<Body> searchBodiesPaged(String category,
            String serial, String modelName, String currentProcess, String status, int page) {
        return bodyRepository.search(searchCriteria(category, serial, modelName, currentProcess, status),
                PageRequest.of(Math.max(0, page), PAGE_SIZE));
    }

    /** 未指定・空・未知のカテゴリは対応中へ正規化する。 */
    public String normalizeCategory(String category) {
        String normalized = normalize(category);
        if ("attention".equals(normalized) || "passed".equals(normalized)) {
            return normalized;
        }
        return "active";
    }

    /** Bodyの状態から一覧カテゴリを導出して絞り込む。 */
    public List<Body> filterByCategory(List<Body> bodies, String category) {
        String normalizedCategory = normalizeCategory(category);
        return bodies.stream()
                .filter(body -> belongsToCategory(body, normalizedCategory))
                .toList();
    }

    private boolean belongsToCategory(Body body, String category) {
        String status = body == null ? "" : normalize(body.getStatus());
        return switch (category) {
        case "attention" -> normalize(REWORK).equals(status)
                || normalize(RETURNED).equals(status);
        case "passed" -> normalize(AVAILABLE).equals(status)
                || normalize(ASSEMBLED).equals(status)
                || normalize(REJECTED).equals(status);
        default -> normalize(WAITING_INSPECTION).equals(status)
                || normalize(WAITING).equals(status)
                || normalize(WORKING).equals(status);
        };
    }

    public List<Body> filterBodies(List<Body> bodies, String serial,
            String model, String currentProcess, String status) {
        String serialCondition = normalize(serial);
        String modelCondition = normalize(model);
        String processCondition = normalize(currentProcess);
        String statusCondition = normalize(status);
        return bodies.stream()
                .filter(body -> serialCondition.isEmpty()
                        || normalize(body.getSerialNo()).contains(serialCondition))
                .filter(body -> modelCondition.isEmpty()
                        || normalize(body.getModelName()).contains(modelCondition))
                .filter(body -> processCondition.isEmpty()
                        || normalize(body.getCurrentProcess()).equals(processCondition))
                .filter(body -> statusCondition.isEmpty()
                        || normalize(body.getStatus()).equals(statusCondition))
                .toList();
    }

    public boolean hasSearchCondition(String serial, String model,
            String currentProcess, String status) {
        return !normalize(serial).isEmpty()
                || !normalize(model).isEmpty()
                || !normalize(currentProcess).isEmpty()
                || !normalize(status).isEmpty();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public Body getBodyById(Long id) {
        return findBodyOrThrow(id);
    }

    public Body createBody(Long bodyMasterId) {

        BodyMaster bodyMaster =
                bodyMasterRepository.findById(bodyMasterId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "指定されたボディマスタが存在しません。"));

        Body body = new Body();

        body.setSerialNo(generateSerialNo());
        body.setBodyMaster(bodyMaster);

        // 移行期間中は旧フィールドにも値を保存する
        body.setModelName(bodyMaster.getModelName());
        body.setColor(bodyMaster.getColor());

        /*
        * ボディは塗装上がり状態で登録されるため、
        * 最初の工程は塗装後検品とする。
        */
        body.setCurrentProcess(POST_PAINT_INSPECTION);
        body.setStatus(WAITING_INSPECTION);

        return bodyRepository.save(body);
    }
    
    private String generateSerialNo() {

        String year = String.valueOf(LocalDate.now().getYear()).substring(2);

        String prefix = "DB" + year;

        Body lastBody = bodyRepository.findTopBySerialNoStartingWithOrderBySerialNoDesc(prefix).orElse(null);

        int nextNumber = 1;

        if (lastBody != null) {
            String lastSerial = lastBody.getSerialNo();
            nextNumber = Integer.parseInt(lastSerial.substring(4)) + 1;
        }

        return prefix + String.format("%04d", nextNumber);
    }

    public List<Body> getAvailableBodies() {
        return bodyRepository.findByStatus(AVAILABLE);
    }

    private Body findBodyOrThrow(Long id) {
        return bodyRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "指定されたボディが存在しません。"));
    }

    public long getAvailableBodyCount() {
        return getAvailableBodies().size();
    }
    public List<ComponentStatusCountResponse>
    getStatusCounts() {

		List<ComponentStatusCountResponse> responses =
		        new ArrayList<>();
		
		responses.add(
		        createStatusCount(
		                WAITING_INSPECTION,
		                "検品待ち",
		                "status-inspection"));
		
		responses.add(
		        createStatusCount(
		                WAITING,
		                "工程待ち",
		                "status-waiting"));
		
		responses.add(
		        createStatusCount(
		                WORKING,
		                "作業中",
		                "status-working"));
		
		responses.add(
		        createStatusCount(
		                REWORK,
		                "手直し待ち",
		                "status-rework"));
		
		responses.add(
		        createStatusCount(
		                AVAILABLE,
		                "組立待ち",
		                "status-available"));
		
		responses.add(
		        createStatusCount(
		                ASSEMBLED,
		                "組立済み",
		                "status-assembled"));
		
		responses.add(
		        createStatusCount(
		                REJECTED,
		                "製造終了",
		                "status-rejected"));
		
		return responses;
		}
		
		private ComponentStatusCountResponse
		    createStatusCount(
		            String status,
		            String displayName,
		            String cssClass) {
		
		return new ComponentStatusCountResponse(
		        status,
		        displayName,
		        bodyRepository.countByStatus(status),
		        cssClass);
	}
	
	public List<Body> getAvailableBodiesByProduct(
	        Product product) {

	    if (product == null) {
	        throw new BusinessException(
	                "製品が指定されていません。");
	    }

	    if (product.getBodyMaster() == null) {
	        throw new BusinessException(
	                "製品に対応するボディマスタが"
	                + "設定されていません。");
	    }

	    return bodyRepository
	            .findByStatusAndBodyMaster_Id(
	                    AVAILABLE,
	                    product.getBodyMaster()
	                            .getId());
	}

    public Body createBody(Long bodyMasterId, ProductionOrder productionOrder, ProductionSchedule productionSchedule) {
        Body body = createBody(bodyMasterId);
        body.setProductionOrder(productionOrder);
        body.setProductionSchedule(productionSchedule);
        return bodyRepository.save(body);
    }

    public List<Body> getAvailableBodiesByProductionSchedule(
            ProductionOrder productionOrder,
            ProductionSchedule productionSchedule) {
        validateScheduleSelection(productionOrder, productionSchedule);
        Product product = productionOrder.getProduct();
        if (product == null || product.getBodyMaster() == null) {
            throw new BusinessException(
                    "対象製品に対応するボディマスタが設定されていません。");
        }
        return bodyRepository
                .findByStatusAndProductionOrder_IdAndProductionSchedule_IdAndBodyMaster_Id(
                        AVAILABLE,
                        productionOrder.getId(),
                        productionSchedule.getId(),
                        product.getBodyMaster().getId());
    }

    private void validateScheduleSelection(
            ProductionOrder productionOrder,
            ProductionSchedule productionSchedule) {
        if (productionOrder == null || productionSchedule == null
                || productionSchedule.getProductionOrder() == null
                || !productionOrder.getId().equals(
                        productionSchedule.getProductionOrder().getId())) {
            throw new BusinessException(
                    "日産計画が生産計画と一致していません。");
        }
    }
}