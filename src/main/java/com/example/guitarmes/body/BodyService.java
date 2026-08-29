package com.example.guitarmes.body;

import static com.example.guitarmes.body.process.BodyProcessConstants.*;
import static com.example.guitarmes.common.StatusConstants.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.master.body.BodyMaster;
import com.example.guitarmes.master.body.BodyMasterRepository;
import com.example.guitarmes.process.analysis.ComponentStatusCountResponse;
import com.example.guitarmes.product.Product;

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
}