package deu.team.jsp.report;

import deu.team.jsp.OneTimeKey.OneTimeKeyService;
import deu.team.jsp.account.domain.Role;
import deu.team.jsp.admin.managelab.ManageLabService;
import deu.team.jsp.alert.AlertLastUser;
import deu.team.jsp.interceptor.CheckSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Controller
public class ReportController {

    @Autowired
    ReportService reportService;

    @Autowired
    OneTimeKeyService oneTimeKeyService;

    @CheckSession
    @AlertLastUser
    @GetMapping("/reportPage")
    public String reportPage(HttpSession session, Model model){
        return "/WEB-INF/student/reportPage.jsp";
    }

    @CheckSession
    @GetMapping("/confirmReportPage")
    public String confirmReportPage(Model model){
        model.addAttribute("reportList",reportService.getReportList());
        model.addAttribute("keyStudent", oneTimeKeyService.getOneTimeKey(Role.STUDENT));
        model.addAttribute("keyProfessor", oneTimeKeyService.getOneTimeKey(Role.PROFESSOR));
        return "/WEB-INF/manager/confirmReport.jsp";
    }

    @CheckSession
    @PostMapping("/reportPost")
    public String reportPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        reportService.reportPost(request,response);
        return "/WEB-INF/student/studentMain.jsp";
    }

    @CheckSession
    @RequestMapping(value = "/getReport/{id}",method = {RequestMethod.POST,RequestMethod.GET})
    public String detailReport(@PathVariable("id") Long id,Model model){
        model.addAttribute("detailReport",reportService.getDetailReport(id));
        return "/WEB-INF/manager/detailReport.jsp";
    }



}
