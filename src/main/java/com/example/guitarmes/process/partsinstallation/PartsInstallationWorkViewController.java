package com.example.guitarmes.process.partsinstallation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@Controller
public class PartsInstallationWorkViewController {
    private final PartsInstallationWorkViewService service;
    public PartsInstallationWorkViewController(PartsInstallationWorkViewService service){this.service=service;}
    @GetMapping("/processes/{historyId}/work")
    public String view(@PathVariable Long historyId, Model model){model.addAttribute("workView",service.get(historyId));return "parts-installation-work";}
}
