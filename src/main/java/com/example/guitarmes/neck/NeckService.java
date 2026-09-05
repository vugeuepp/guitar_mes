package com.example.guitarmes.neck;

import static com.example.guitarmes.common.StatusConstants.*;
import static com.example.guitarmes.neck.process.NeckProcessConstants.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.master.neck.NeckMaster;
import com.example.guitarmes.master.neck.NeckMasterRepository;
import com.example.guitarmes.process.analysis.ComponentStatusCountResponse;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionschedule.ProductionSchedule;

@Service
public class NeckService {
    private final NeckRepository neckRepository;
    private final NeckMasterRepository neckMasterRepository;

    public NeckService(NeckRepository neckRepository,
            NeckMasterRepository neckMasterRepository) {
        this.neckRepository = neckRepository;
        this.neckMasterRepository = neckMasterRepository;
    }

    public List<Neck> getNecks() {
        return neckRepository.findAll();
    }

    public List<Neck> filterNecks(List<Neck> necks, String serial,
            String model, String currentProcess, String status) {
        String serialCondition = normalize(serial);
        String modelCondition = normalize(model);
        String processCondition = normalize(currentProcess);
        String statusCondition = normalize(status);
        return necks.stream()
                .filter(neck -> serialCondition.isEmpty()
                        || normalize(neck.getSerialNo()).contains(serialCondition))
                .filter(neck -> modelCondition.isEmpty()
                        || normalize(neck.getModelName()).contains(modelCondition))
                .filter(neck -> processCondition.isEmpty()
                        || normalize(neck.getCurrentProcess()).equals(processCondition))
                .filter(neck -> statusCondition.isEmpty()
                        || normalize(neck.getStatus()).equals(statusCondition))
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
        return value == null ? "" : value.trim().toLowerCase();
    }

    public Neck getNeckById(Long id) { return findNeckOrThrow(id); }

    public Neck createNeck(Long neckMasterId) {
        NeckMaster neckMaster = neckMasterRepository.findById(neckMasterId)
                .orElseThrow(() -> new NotFoundException(
                        "指定されたネックマスタが存在しません。"));
        Neck neck = new Neck();
        neck.setSerialNo(generateSerialNo());
        neck.setNeckMaster(neckMaster);
        neck.setModelName(neckMaster.getModelName());
        neck.setCurrentProcess(PLEK);
        neck.setStatus(WAITING);
        return neckRepository.save(neck);
    }

    private String generateSerialNo() {
        String year = String.valueOf(LocalDate.now().getYear()).substring(2);
        String prefix = "DN" + year;
        Neck lastNeck = neckRepository
                .findTopBySerialNoStartingWithOrderBySerialNoDesc(prefix).orElse(null);
        int nextNumber = 1;
        if (lastNeck != null) {
            nextNumber = Integer.parseInt(lastNeck.getSerialNo().substring(4)) + 1;
        }
        return prefix + String.format("%04d", nextNumber);
    }

    public List<Neck> getAvailableNecks() { return neckRepository.findByStatus(AVAILABLE); }
    public long getAvailableNeckCount() { return getAvailableNecks().size(); }

    private Neck findNeckOrThrow(Long id) {
        return neckRepository.findById(id).orElseThrow(() ->
                new NotFoundException("指定されたネックが存在しません。"));
    }

    public List<ComponentStatusCountResponse> getStatusCounts() {
        List<ComponentStatusCountResponse> responses = new ArrayList<>();
        responses.add(createStatusCount(WAITING, "工程待ち", "status-waiting"));
        responses.add(createStatusCount(WORKING, "作業中", "status-working"));
        responses.add(createStatusCount(AVAILABLE, "組立待ち", "status-available"));
        responses.add(createStatusCount(RETURNED, "塗装前工程へ差し戻し", "status-returned"));
        responses.add(createStatusCount(ASSEMBLED, "組立済み", "status-assembled"));
        responses.add(createStatusCount(REJECTED, "製造終了", "status-rejected"));
        return responses;
    }

    private ComponentStatusCountResponse createStatusCount(
            String status, String displayName, String cssClass) {
        return new ComponentStatusCountResponse(status, displayName,
                neckRepository.countByStatus(status), cssClass);
    }

    public List<Neck> getAvailableNecksByProduct(Product product) {
        if (product == null) throw new BusinessException("製品が指定されていません。");
        if (product.getNeckMaster() == null) {
            throw new BusinessException("製品に対応するネックマスタが設定されていません。");
        }
        return neckRepository.findByStatusAndNeckMaster_Id(
                AVAILABLE, product.getNeckMaster().getId());
    }

    public Neck createNeck(Long neckMasterId, Product product,
            ProductionOrder productionOrder, ProductionSchedule productionSchedule) {
        Neck neck = createNeck(neckMasterId);
        neck.setProduct(product);
        neck.setProductionOrder(productionOrder);
        neck.setProductionSchedule(productionSchedule);
        return neckRepository.save(neck);
    }

    public List<Neck> getAvailableNecksByProductionSchedule(
            ProductionOrder productionOrder, ProductionSchedule productionSchedule) {
        validateScheduleSelection(productionOrder, productionSchedule);
        Product product = productionOrder.getProduct();
        if (product == null || product.getNeckMaster() == null) {
            throw new BusinessException("対象製品に対応するネックマスタが設定されていません。");
        }
        return neckRepository
                .findByStatusAndProductionOrder_IdAndProductionSchedule_IdAndNeckMaster_Id(
                        AVAILABLE, productionOrder.getId(), productionSchedule.getId(),
                        product.getNeckMaster().getId());
    }

    private void validateScheduleSelection(ProductionOrder productionOrder,
            ProductionSchedule productionSchedule) {
        if (productionOrder == null || productionSchedule == null
                || productionSchedule.getProductionOrder() == null
                || !productionOrder.getId().equals(
                        productionSchedule.getProductionOrder().getId())) {
            throw new BusinessException("日産計画が生産計画と一致していません。");
        }
    }
}
