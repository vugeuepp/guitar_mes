package com.example.guitarmes.neck;

import static com.example.guitarmes.common.NeckProcessConstants.*;
import static com.example.guitarmes.common.StatusConstants.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.guitarmes.dto.ComponentStatusCountResponse;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.master.neck.NeckMaster;
import com.example.guitarmes.master.neck.NeckMasterRepository;
import com.example.guitarmes.product.Product;

@Service
public class NeckService {

    private final NeckRepository neckRepository;
    private final NeckMasterRepository neckMasterRepository;

    public NeckService(
            NeckRepository neckRepository,
            NeckMasterRepository neckMasterRepository) {

        this.neckRepository = neckRepository;
        this.neckMasterRepository = neckMasterRepository;
    }

    public List<Neck> getNecks() {
        return neckRepository.findAll();
    }

    public Neck getNeckById(Long id) {
        return findNeckOrThrow(id);
    }

    public Neck createNeck(Long neckMasterId) {

        NeckMaster neckMaster =
                neckMasterRepository.findById(neckMasterId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "指定されたネックマスタが存在しません。"));

        Neck neck = new Neck();

        neck.setSerialNo(generateSerialNo());
        neck.setNeckMaster(neckMaster);

        // 移行期間中は旧フィールドにも値を保存する
        neck.setModelName(neckMaster.getModelName());

        neck.setCurrentProcess(PLEK);
        neck.setStatus(WAITING);

        return neckRepository.save(neck);
    }
    
    private String generateSerialNo() {
    	String year = String.valueOf(LocalDate.now().getYear()).substring(2);
    	
    	String prefix = "DN" + year;
    	
    	Neck lastNeck = neckRepository.findTopBySerialNoStartingWithOrderBySerialNoDesc(prefix).orElse(null);
    	
    	int nextNumber = 1;
    	
    	if (lastNeck != null) {
    		String lastSerial = lastNeck.getSerialNo();
    		nextNumber = Integer.parseInt(lastSerial.substring(4)) + 1;
    	}
    	
    	return prefix + String.format("%04d", nextNumber);
    }

    public List<Neck> getAvailableNecks() {
        return neckRepository.findByStatus(AVAILABLE);
    }

    public long getAvailableNeckCount() {
        return getAvailableNecks().size();
    }

    private Neck findNeckOrThrow(Long id) {
        return neckRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "指定されたネックが存在しません。"));
    }
    public List<ComponentStatusCountResponse>
    getStatusCounts() {

		List<ComponentStatusCountResponse> responses =
		        new ArrayList<>();
		
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
		                AVAILABLE,
		                "組立待ち",
		                "status-available"));
		
		responses.add(
		        createStatusCount(
		                RETURNED,
		                "塗装前工程へ差し戻し",
		                "status-returned"));
		
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
		        neckRepository.countByStatus(status),
		        cssClass);
	}
	public List<Neck> getAvailableNecksByProduct(
	        Product product) {

	    if (product == null) {
	        throw new BusinessException(
	                "製品が指定されていません。");
	    }

	    if (product.getNeckMaster() == null) {
	        throw new BusinessException(
	                "製品に対応するネックマスタが"
	                + "設定されていません。");
	    }

	    return neckRepository
	            .findByStatusAndNeckMaster_Id(
	                    AVAILABLE,
	                    product.getNeckMaster()
	                            .getId());
	}
}