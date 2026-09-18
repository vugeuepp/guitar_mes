package com.example.guitarmes.process.partsinstallation;

import static com.example.guitarmes.process.work.ProcessWorkItemKey.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.process.work.ProcessWorkItemKey;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.product.ProductClassificationService;
import com.example.guitarmes.product.parts.ProductPartsSpec;
import com.example.guitarmes.product.parts.ProductPartsSpecRepository;
import com.example.guitarmes.product.parts.ProductPartsSpecValidator;

/** Strat対象の保存前計画を作成する。工程開始・Entity生成・保存は行わない。 */
@Service
@Transactional(readOnly = true)
public class PartsInstallationWorkPlanGenerator {

    // InstrumentTypeMasterの正式楽器コード。表示名・PU構成から推測しない。
    private static final String STRAT_INSTRUMENT_CODE = "ST";

    private final ProductClassificationService classifier;
    private final ProductPartsSpecRepository specs;
    private final ProductPartsSpecValidator validator;

    public PartsInstallationWorkPlanGenerator(ProductClassificationService classifier,
            ProductPartsSpecRepository specs, ProductPartsSpecValidator validator) {
        this.classifier = classifier;
        this.specs = specs;
        this.validator = validator;
    }

    public enum Target {
        STRAT_TARGET, NON_TARGET, UNCLASSIFIABLE
    }

    public record Result(Target target, Optional<PartsInstallationWorkPlan> plan) {
        public Result {
            if (target == null || plan == null || (target == Target.STRAT_TARGET) != plan.isPresent()) {
                throw new IllegalArgumentException("対象分類とWork planの有無が一致しません。");
            }
        }
    }

    public Result generate(Product product) {
        var classification = classifier.classify(product == null ? null : product.getInternalModelCode());
        if (classification.isEmpty()) return new Result(Target.UNCLASSIFIABLE, Optional.empty());
        if (!STRAT_INSTRUMENT_CODE.equalsIgnoreCase(classification.get().instrumentCode().trim())) {
            return new Result(Target.NON_TARGET, Optional.empty());
        }
        if (product.getId() == null) throw new BusinessException("製品IDを指定してください。");
        ProductPartsSpec spec = specs.findByProductId(product.getId())
                .orElseThrow(() -> new BusinessException("パーツ取付仕様が登録されていません。"));
        validator.validateStored(spec);
        String pickupLayout = product.getPickupLayout();
        if (pickupLayout == null || pickupLayout.isBlank()) {
            throw new BusinessException("PU構成を入力してください。");
        }
        if (pickupLayout.codePointCount(0, pickupLayout.length()) > 255) {
            throw new BusinessException("PU構成は255文字以内で入力してください。");
        }
        var snapshot = new PartsInstallationWorkPlan.Snapshot(
                spec.getBridgeType(), spec.getBridgeModel(), spec.getRequiresStudHoleExpansion(),
                spec.getTunerModel(), spec.getTunerMountingType(), spec.getTunerBushRequired(),
                spec.getTunerLayout(), spec.getSelectorPositions(), spec.getControlLayout(),
                spec.getJackMountingType(), spec.getStringMaker(), spec.getStringModel(),
                spec.getStringGauge(), pickupLayout);
        List<ProcessWorkItemKey> keys = new ArrayList<>();
        switch (snapshot.bridgeType()) {
            case SIX_POINT -> keys.addAll(List.of(BRIDGE_SIX_POINT_INSTALL, BRIDGE_MOVEMENT_CHECK));
            case TWO_POINT -> {
                if (snapshot.requiresStudHoleExpansion()) keys.add(STUD_HOLE_EXPANSION);
                keys.add(STUD_INSTALL);
                keys.add(BRIDGE_TWO_POINT_INSTALL);
            }
            case FLOYD_ROSE -> {
                if (snapshot.requiresStudHoleExpansion()) keys.add(STUD_HOLE_EXPANSION);
                keys.add(STUD_INSTALL);
            }
        }
        keys.add(SPRING_HANGER_INSTALL);
        keys.addAll(List.of(PICKGUARD_INSTALL, JACK_PLATE_INSTALL, JACK_WIRING, GROUND_WIRING,
                ELECTRONICS_SOUND_CHECK, ELECTRONICS_PARTS_CHECK, ELECTRONICS_FINAL_FASTENING));
        if (snapshot.tunerBushRequired()) keys.add(TUNER_BUSHING_INSTALL);
        keys.add(TUNER_INSTALL);
        keys.add(STRING_INSTALL);
        List<PartsInstallationWorkPlan.Item> items = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            items.add(new PartsInstallationWorkPlan.Item(keys.get(i), i + 1));
        }
        return new Result(Target.STRAT_TARGET, Optional.of(new PartsInstallationWorkPlan(snapshot, items)));
    }
}
