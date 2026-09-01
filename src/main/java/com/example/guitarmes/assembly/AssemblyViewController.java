package com.example.guitarmes.assembly;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.guitarmes.body.BodyService;
import com.example.guitarmes.neck.NeckService;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderService;
import com.example.guitarmes.productionschedule.ProductionSchedule;
import com.example.guitarmes.productionschedule.ProductionScheduleService;

@Controller
public class AssemblyViewController {

    private final AssemblyService assemblyService;
    private final NeckService neckService;
    private final BodyService bodyService;
    private final ProductionOrderService
            productionOrderService;
    private final ProductionScheduleService
            productionScheduleService;

    public AssemblyViewController(
            AssemblyService assemblyService,
            NeckService neckService,
            BodyService bodyService,
            ProductionOrderService
                    productionOrderService,
            ProductionScheduleService
                    productionScheduleService) {

        this.assemblyService =
                assemblyService;

        this.neckService =
                neckService;

        this.bodyService =
                bodyService;

        this.productionOrderService =
                productionOrderService;
        this.productionScheduleService =
                productionScheduleService;
    }

    /**
     * ネック取付実績一覧。
     */
    @GetMapping("/assemblies/view")
    public String assemblyList(
            Model model) {

        model.addAttribute(
                "assemblies",
                assemblyService.getAssemblies());

        return "assembly-list";
    }

    /**
     * ProductionOrderからネック取付画面を開く。
     */
    @GetMapping("/assemblies/new")
    public String newAssemblyForm(
            @RequestParam Long productionOrderId,
            @RequestParam Long productionScheduleId,
            Model model) {

        ProductionOrder productionOrder =
                productionOrderService
                        .getProductionOrderById(
                                productionOrderId);

        ProductionSchedule productionSchedule =
                productionScheduleService
                        .getProductionScheduleById(
                                productionScheduleId);

        model.addAttribute(
                "productionOrder",
                productionOrder);

        model.addAttribute(
                "productionSchedule",
                productionSchedule);
        model.addAttribute(
                "necks",
                neckService
                        .getAvailableNecksByProductionSchedule(
                                productionOrder,
                                productionSchedule));

        model.addAttribute(
                "bodies",
                bodyService
                        .getAvailableBodiesByProductionSchedule(
                                productionOrder,
                                productionSchedule));

        return "assembly-form";
    }

    /**
     * ネック取付を登録する。
     *
     * AssemblyService内で以下が実行される。
     *
     * ・Guitar生成
     * ・DYシリアル採番
     * ・Assembly保存
     * ・Body／NeckのASSEMBLED化
     * ・ProductionOrder着手数更新
     */
    @PostMapping("/assemblies/create")
    public String createAssembly(
            @RequestParam Long productionOrderId,
            @RequestParam Long productionScheduleId,
            @RequestParam Long neckId,
            @RequestParam Long bodyId,
            @RequestParam String workerName) {

        Assembly assembly =
                assemblyService.createAssembly(
                        productionOrderId,
                        productionScheduleId,
                        neckId,
                        bodyId,
                        workerName);

        Long guitarId =
                assembly.getGuitar().getId();

        return "redirect:/guitars/"
                + guitarId
                + "/view";
    }

    /**
     * ネック取付実績詳細。
     */
    @GetMapping("/assemblies/{id}/view")
    public String assemblyDetail(
            @PathVariable Long id,
            Model model) {

        model.addAttribute(
                "assembly",
                assemblyService
                        .getAssemblyById(id));

        return "assembly-detail";
    }
}